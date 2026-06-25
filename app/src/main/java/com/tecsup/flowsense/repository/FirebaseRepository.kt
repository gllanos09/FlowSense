package com.tecsup.flowsense.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.tecsup.flowsense.model.Alerta
import com.tecsup.flowsense.model.Negocio
import com.tecsup.flowsense.model.RegistroAforo
import com.tecsup.flowsense.model.Usuario
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    // ── AUTH ──────────────────────────────────────────

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = auth.signOut()

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

    fun observeNegocios(): Flow<List<Negocio>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = snapshot.children.mapNotNull {
                    it.getValue(Negocio::class.java)?.copy(id = it.key ?: "")
                }
                trySend(lista)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("negocios").addValueEventListener(listener)
        awaitClose { db.child("negocios").removeEventListener(listener) }
    }

    fun observeNegocio(negocioId: String): Flow<Negocio?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val negocio = snapshot.getValue(Negocio::class.java)?.copy(id = snapshot.key ?: "")
                trySend(negocio)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("negocios").child(negocioId).addValueEventListener(listener)
        awaitClose { db.child("negocios").child(negocioId).removeEventListener(listener) }
    }

    suspend fun crearNegocio(negocio: Negocio): Result<String> {
        return try {
            val key = db.child("negocios").push().key ?: ""
            db.child("negocios").child(key).setValue(negocio.copy(id = key)).await()
            Result.success(key)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── USUARIOS ──────────────────────────────────────

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