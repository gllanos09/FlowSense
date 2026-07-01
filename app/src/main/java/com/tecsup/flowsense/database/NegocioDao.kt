package com.tecsup.flowsense.database

import androidx.room.*
import com.tecsup.flowsense.model.Negocio
import kotlinx.coroutines.flow.Flow

@Dao
interface NegocioDao {
    @Query("SELECT * FROM negocios")
    fun observeNegocios(): Flow<List<Negocio>>

    @Query("SELECT * FROM negocios WHERE id = :id")
    fun observeNegocio(id: String): Flow<Negocio?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(negocio: Negocio)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(negocios: List<Negocio>)

    @Query("SELECT * FROM negocios WHERE synced = 0")
    suspend fun getUnsynced(): List<Negocio>

    @Query("UPDATE negocios SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
