package com.sprm.apexgym.feature.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sprm.apexgym.core.network.client.RegistroRequest
import com.sprm.apexgym.core.network.client.RetrofitClient.authService
import kotlinx.coroutines.launch

class RegistroViewModel: ViewModel() {

    private val _mensaje = MutableLiveData<String>()
    val mensaje: LiveData<String> = _mensaje

    // Datos del usuario
    var usuario: String = ""
    var email: String = ""
    var contrasenia: String = ""
    var fotoUrl: String = ""

    // Datos del cliente
    var nombre: String = ""
    var apellidoPaterno: String = ""
    var apellidoMaterno: String = ""
    var fechaNacimiento: String = ""
    var genero: String = ""
    var telefono: String = ""
    var direccion: String = ""
    var contactoEmergenciaNombre: String = ""
    var contactoEmergenciaTelefono: String = ""

    fun obtenerDatos(): RegistroRequest {
        return RegistroRequest(
            usuario = usuario,
            email = email,
            contrasenia = contrasenia,
            fotoUrl = fotoUrl,
            nombre = nombre,
            apellidoPaterno = apellidoPaterno,
            apellidoMaterno = apellidoMaterno,
            fechaNacimiento = fechaNacimiento,
            genero = genero,
            telefono = telefono,
            direccion = direccion,
            contactoEmergenciaNombre = contactoEmergenciaNombre,
            contactoEmergenciaTelefono = contactoEmergenciaTelefono
        )
    }
    fun registrarCliente(datos: RegistroRequest) {
        viewModelScope.launch {
            try {
                val response = authService.registrarCliente(datos)

                if (response.isSuccessful) {
                    _mensaje.value = "Registro exitoso"
                } else {
                    _mensaje.value = "Error al registrar: ${response.code()}"
                }
            } catch (e: Exception) {
                _mensaje.value = "Error de conexión: ${e.message}"
            }
        }
    }
}