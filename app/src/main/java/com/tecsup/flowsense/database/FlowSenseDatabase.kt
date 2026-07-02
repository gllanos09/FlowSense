package com.tecsup.flowsense.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tecsup.flowsense.model.Alerta
import com.tecsup.flowsense.model.Negocio
import com.tecsup.flowsense.model.RegistroAforo
import com.tecsup.flowsense.model.Usuario

@Database(entities = [Negocio::class, Usuario::class, Alerta::class, RegistroAforo::class], version = 3, exportSchema = false)
abstract class FlowSenseDatabase : RoomDatabase() {
    abstract fun negocioDao(): NegocioDao
    abstract fun usuarioDao(): UsuarioDao

    companion object {
        @Volatile
        private var INSTANCE: FlowSenseDatabase? = null

        fun getDatabase(context: Context): FlowSenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FlowSenseDatabase::class.java,
                    "flowsense_db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
