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
    private val db = FirebaseDatabase.getInstance().reference
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