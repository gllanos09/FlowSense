package com.tecsup.flowsense.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.tecsup.flowsense.database.FlowSenseDatabase
import com.tecsup.flowsense.model.Alerta
import com.tecsup.flowsense.model.Negocio
import com.tecsup.flowsense.model.RegistroAforo
import com.tecsup.flowsense.model.Usuario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseRepository(context: Context) {

    private val databaseUrl = "https://flowsense-final-default-rtdb.firebaseio.com/"
    private val db = FirebaseDatabase.getInstance(databaseUrl).reference
    private val database = FlowSenseDatabase.getDatabase(context)
    private val negocioDao = database.negocioDao()
    private val usuarioDao = database.usuarioDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Iniciar sincronización automática al conectar
        escucharConexion()
    }

    private fun escucharConexion() {
        db.child(".info/connected").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    syncPendingData()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun syncPendingData() {
        scope.launch {
            val unsynced = negocioDao.getUnsynced()
            unsynced.forEach { negocio ->
                try {
                    db.child("negocios").child(negocio.id).setValue(negocio.copy(synced = true)).await()
                    negocioDao.markAsSynced(negocio.id)
                } catch (e: Exception) {}
            }
        }
    }

    // ── AUTH (DATABASE BASED) ─────────────────────────

    suspend fun login(username: String, pass: String): Result<Usuario> {
        return try {
            val snapshot = db.child("usuarios").get().await()
            val usuario = snapshot.children.mapNotNull { it.getValue(Usuario::class.java) }
                .find { it.email == username && it.password == pass }
            
            if (usuario != null) {
                usuarioDao.clearAll()
                usuarioDao.insert(usuario)
                Result.success(usuario)
            } else {
                Result.failure(Exception("Credenciales inválidas"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLocalSession(): Usuario? = usuarioDao.getLoggedUser()

    suspend fun saveLocalSession(usuario: Usuario) {
        usuarioDao.clearAll()
        usuarioDao.insert(usuario)
    }

    fun logout() {
        scope.launch { usuarioDao.clearAll() }
    }

    suspend fun getUsuario(username: String): Usuario? {
        return try {
            val snapshot = db.child("usuarios").get().await()
            snapshot.children.mapNotNull { it.getValue(Usuario::class.java) }
                .find { it.email == username }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUsuarioActual(): Usuario? {
        return getLocalSession()
    }

    // ── NEGOCIOS ──────────────────────────────────────

    fun observeNegocios(): Flow<List<Negocio>> {
        db.child("negocios").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = snapshot.children.mapNotNull {
                    it.getValue(Negocio::class.java)?.copy(id = it.key ?: "", synced = true)
                }
                scope.launch { negocioDao.insertAll(lista) }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        return negocioDao.observeNegocios()
    }

    fun observeNegocio(negocioId: String): Flow<Negocio?> {
        // Sincronizar este negocio específico desde Firebase
        db.child("negocios").child(negocioId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val negocio = snapshot.getValue(Negocio::class.java)?.copy(id = snapshot.key ?: "", synced = true)
                if (negocio != null) {
                    scope.launch { negocioDao.insert(negocio) }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        return negocioDao.observeNegocio(negocioId)
    }

    suspend fun crearNegocio(negocio: Negocio): Result<String> {
        val id = if (negocio.id.isEmpty()) db.child("negocios").push().key ?: System.currentTimeMillis().toString() else negocio.id
        val localNegocio = negocio.copy(id = id, synced = false)
        
        // Guardar localmente siempre
        negocioDao.insert(localNegocio)
        
        return try {
            db.child("negocios").child(id).setValue(localNegocio.copy(synced = true)).await()
            negocioDao.markAsSynced(id)
            Result.success(id)
        } catch (e: Exception) {
            // Si falla Firebase, retornamos éxito porque ya está en Room
            Result.success(id)
        }
    }

    // ── MONITOREO IoT (ESP8266) ─────────────────────────

    fun iniciarMonitoreoIoT(negocioId: String) {
        // Usar la referencia de base de datos con URL explícita para evitar desincronización
        val refEntrada = FirebaseDatabase.getInstance(databaseUrl).getReference("Negocio/Entrada")
        val refSalida = FirebaseDatabase.getInstance(databaseUrl).getReference("Negocio/Salida")
        
        android.util.Log.d("FlowSenseIoT", "Iniciando monitoreo IoT Global en: $databaseUrl")

        refEntrada.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val valor = snapshot.getValue()?.toString()?.toIntOrNull() ?: 0
                if (valor == 1) {
                    actualizarAforoIoT(true)
                    refEntrada.setValue(0) // Resetear sensor
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        refSalida.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val valor = snapshot.getValue()?.toString()?.toIntOrNull() ?: 0
                if (valor == 1) {
                    actualizarAforoIoT(false)
                    refSalida.setValue(0) // Resetear sensor
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun actualizarAforoIoT(esEntrada: Boolean) {
        scope.launch {
            try {
                val snapshot = db.child("negocios").get().await()
                snapshot.children.forEach { child ->
                    val key = child.key ?: return@forEach
                    val negocio = child.getValue(Negocio::class.java)?.copy(id = key)
                    if (negocio != null && negocio.diaActivo) {
                        val nuevoAforo = if (esEntrada) {
                            negocio.aforoActual + 1
                        } else {
                            (negocio.aforoActual - 1).coerceAtLeast(0)
                        }

                        val nuevasEntradas = if (esEntrada) negocio.totalEntradas + 1 else negocio.totalEntradas
                        val nuevasSalidas = if (!esEntrada) negocio.totalSalidas + 1 else negocio.totalSalidas

                        val updates = mapOf(
                            "aforoActual" to nuevoAforo,
                            "totalEntradas" to nuevasEntradas,
                            "totalSalidas" to nuevasSalidas
                        )
                        
                        db.child("negocios").child(key).updateChildren(updates)

                        // Registrar evento en el historial
                        val registroId = db.child("registros").child(key).push().key ?: ""
                        val registro = RegistroAforo(
                            id = registroId,
                            negocioId = key,
                            tipo = if (esEntrada) "ENTRADA" else "SALIDA",
                            aforoActual = nuevoAforo,
                            timestamp = System.currentTimeMillis()
                        )
                        db.child("registros").child(key).child(registroId).setValue(registro)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun setDiaEstado(negocioId: String, activo: Boolean) {
        try {
            // Actualizar el estado en Firebase para que Room lo reciba vía listener
            db.child("negocios").child(negocioId).child("diaActivo").setValue(activo).await()
            
            if (!activo) {
                // Al terminar el día, reseteamos contadores locales del negocio
                val updates = mapOf(
                    "aforoActual" to 0,
                    "totalEntradas" to 0,
                    "totalSalidas" to 0
                )
                db.child("negocios").child(negocioId).updateChildren(updates).await()
            }
        } catch (e: Exception) {
            android.util.Log.e("FlowSense", "Error al cambiar estado: ${e.message}")
        }
    }

    // ── USUARIOS ──────────────────────────────────────

    fun observeUsuarios(): Flow<List<Usuario>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = snapshot.children.mapNotNull { it.getValue(Usuario::class.java) }
                trySend(lista)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("usuarios").addValueEventListener(listener)
        awaitClose { db.child("usuarios").removeEventListener(listener) }
    }

    suspend fun eliminarUsuario(uid: String) {
        try {
            db.child("usuarios").child(uid).removeValue().await()
        } catch (e: Exception) {}
    }

    suspend fun actualizarUsuario(uid: String, nombre: String) {
        try {
            db.child("usuarios").child(uid).child("nombre").setValue(nombre).await()
        } catch (e: Exception) {}
    }

    suspend fun actualizarAforoMaximo(negocioId: String, nuevoMax: Int) {
        try {
            db.child("negocios").child(negocioId).child("aforoMaximo").setValue(nuevoMax).await()
        } catch (e: Exception) {}
    }

    suspend fun registrarAdmin(
        email: String,
        password: String,
        nombre: String
    ): Result<String> {
        return try {
            val id = db.child("usuarios").push().key ?: ""
            val usuario = Usuario(
                id = id,
                nombre = nombre,
                email = email, // Username
                password = password,
                rol = "ADMIN",
                negocioId = ""
            )
            db.child("usuarios").child(id).setValue(usuario).await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun crearUsuarioDueno(
        email: String,
        password: String,
        nombre: String,
        negocioId: String
    ): Result<String> {
        return try {
            val id = db.child("usuarios").push().key ?: ""
            val usuario = Usuario(
                id = id,
                nombre = nombre,
                email = email, // Username
                password = password,
                rol = "DUENO",
                negocioId = negocioId
            )
            db.child("usuarios").child(id).setValue(usuario).await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── REGISTROS ─────────────────────────────────────

    fun observeRegistrosHoy(negocioId: String): Flow<List<RegistroAforo>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = snapshot.children.mapNotNull {
                    it.getValue(RegistroAforo::class.java)
                }
                trySend(lista)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("registros").child(negocioId).addValueEventListener(listener)
        awaitClose { db.child("registros").child(negocioId).removeEventListener(listener) }
    }

    // ── ALERTAS ───────────────────────────────────────

    fun observeAlertas(negocioId: String): Flow<List<Alerta>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = snapshot.children.mapNotNull {
                    it.getValue(Alerta::class.java)?.copy(id = it.key ?: "")
                }.filter { !it.resuelta }
                trySend(lista)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("alertas").child(negocioId).addValueEventListener(listener)
        awaitClose { db.child("alertas").child(negocioId).removeEventListener(listener) }
    }

    suspend fun resolverAlerta(negocioId: String, alertaId: String) {
        try {
            db.child("alertas").child(negocioId).child(alertaId)
                .child("resuelta").setValue(true).await()
        } catch (e: Exception) {}
    }
}