package com.sprm.apexgym.feature.membresia

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.feature.membresia.model.MiMembresiaResponse
import com.sprm.apexgym.feature.membresia.model.MiSaldoResponse
import kotlinx.coroutines.launch

class MembresiaViewModel : ViewModel() {

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> = _cargando

    private val _membresiaData = MutableLiveData<MiMembresiaResponse?>()
    val membresiaData: LiveData<MiMembresiaResponse?> = _membresiaData

    private val _saldoData = MutableLiveData<MiSaldoResponse?>()
    val saldoData: LiveData<MiSaldoResponse?> = _saldoData

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun cargarInformacion(clienteId: Int? = null) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null

            try {
                val membresiaResp = RetrofitClient.membresiaService.obtenerMiMembresia(clienteId)
                if (membresiaResp.isSuccessful && membresiaResp.body() != null) {
                    _membresiaData.value = membresiaResp.body()?.data
                } else {
                    _error.value = "Error al consultar membresía: ${membresiaResp.code()}"
                }

                val saldoResp = RetrofitClient.membresiaService.obtenerMiSaldo(clienteId)
                if (saldoResp.isSuccessful && saldoResp.body() != null) {
                    _saldoData.value = saldoResp.body()?.data
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Error de conexión con el servidor"
            } finally {
                _cargando.value = false
            }
        }
    }
}
