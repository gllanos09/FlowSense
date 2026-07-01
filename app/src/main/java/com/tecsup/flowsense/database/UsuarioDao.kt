package com.tecsup.flowsense.database

import androidx.room.*
import com.tecsup.flowsense.model.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios LIMIT 1")
    suspend fun getLoggedUser(): Usuario?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usuario: Usuario)

    @Query("DELETE FROM usuarios")
    suspend fun clearAll()
}
