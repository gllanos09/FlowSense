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

    private val auth = FirebaseAuth.getInstance()
    private val databaseUrl = "https://flowsense-30b81-default-rtdb.firebaseio.com/"
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

    // ── AUTH ──────────────────────────────────────────

    suspend fun login(email: String, password: String): Result<Usuario> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: ""
            val usuario = getUsuario(uid)
            if (usuario != null) {
                usuarioDao.clearAll()
                usuarioDao.insert(usuario)
                Result.success(usuario)
            } else {
                Result.failure(Exception("Usuario no encontrado"))
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
        auth.signOut()
        scope.launch { usuarioDao.clearAll() }
    }

    fun getCurrentUserId() = auth.currentUser?.uid

    suspend fun getUsuario(uid: String): Usuario? {
        return try {
            val snapshot = db.child("usuarios").child(uid).get().await()
            snapshot.getValue(Usuario::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUsuarioActual(): Usuario? {
        return getUsuario(getCurrentUserId() ?: "")
    }

    // ── NEGOCIOS ──────────────────────────────────────

    fun observeNegocios(): Flow<List<Negocio>> {
        // Side effect: Sincronizar desde Firebase a local
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
        val refEntrada = FirebaseDatabase.getInstance().getReference("Negocio/Entrada")
        val refSalida = FirebaseDatabase.getInstance().getReference("Negocio/Salida")
        
        android.util.Log.d("FlowSenseIoT", "Iniciando monitoreo IoT para negocio: $negocioId")

        refEntrada.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val valor = snapshot.getValue()?.toString()?.toIntOrNull() ?: 0
                android.util.Log.d("FlowSenseIoT", "Cambio en Entrada: $valor")
                if (valor == 1) {
                    actualizarAforoIoT(negocioId, true)
                    refEntrada.setValue(0)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FlowSenseIoT", "Error en Entrada: ${error.message}")
            }
        })

        refSalida.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val valor = snapshot.getValue()?.toString()?.toIntOrNull() ?: 0
                android.util.Log.d("FlowSenseIoT", "Cambio en Salida: $valor")
                if (valor == 1) {
                    actualizarAforoIoT(negocioId, false)
                    refSalida.setValue(0)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FlowSenseIoT", "Error en Salida: ${error.message}")
            }
        })
    }

    private fun actualizarAforoIoT(negocioId: String, esEntrada: Boolean) {
        scope.launch {
            try {
                // Obtenemos el negocio actual de Firebase para consistencia
                val snapshot = db.child("negocios").child(negocioId).get().await()
                val negocio = snapshot.getValue(Negocio::class.java) ?: return@launch
                
                val nuevoAforo = if (esEntrada) {
                    negocio.aforoActual + 1
                } else {
                    (negocio.aforoActual - 1).coerceAtLeast(0)
                }

                val nuevasEntradas = if (esEntrada) negocio.totalEntradas + 1 else negocio.totalEntradas
                val nuevasSalidas = if (!esEntrada) negocio.totalSalidas + 1 else negocio.totalSalidas

                val actualizado = negocio.copy(
                    aforoActual = nuevoAforo,
                    totalEntradas = nuevasEntradas,
                    totalSalidas = nuevasSalidas
                )

                db.child("negocios").child(negocioId).setValue(actualizado).await()
                
                // Registrar evento en el historial
                val registroId = db.child("registros").child(negocioId).push().key ?: ""
                val registro = RegistroAforo(
                    id = registroId,
                    negocioId = negocioId,
                    tipo = if (esEntrada) "ENTRADA" else "SALIDA",
                    aforoActual = nuevoAforo,
                    timestamp = System.currentTimeMillis()
                )
                db.child("registros").child(negocioId).child(registroId).setValue(registro).await()
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ── USUARIOS ──────────────────────────────────────

    suspend fun registrarAdmin(
        email: String,
        password: String,
        nombre: String
    ): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: ""
            val usuario = Usuario(
                id = uid,
                nombre = nombre,
                email = email,
                rol = "ADMIN",
                negocioId = ""
            )
            db.child("usuarios").child(uid).setValue(usuario).await()
            Result.success(uid)
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
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: ""
            result.user?.sendEmailVerification()?.await()
            val usuario = Usuario(
                id = uid,
                nombre = nombre,
                email = email,
                rol = "DUENO",
                negocioId = negocioId
            )
            db.child("usuarios").child(uid).setValue(usuario).await()
            Result.success(uid)
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