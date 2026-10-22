package com.sprm.apexgym.core.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.sprm.apexgym.core.db.models.Usuario

@Dao
interface UsuarioDao {

    @Query("SELECT * FROM usuarios WHERE nombreUsuario = :usuario AND contrasena = :pass LIMIT 1")
    suspend fun login(usuario: String, pass: String): Usuario?
}