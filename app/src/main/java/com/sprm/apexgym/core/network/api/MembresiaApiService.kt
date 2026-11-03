package com.sprm.apexgym.core.network.api

import com.sprm.apexgym.core.network.client.ApiResponse
import com.sprm.apexgym.feature.membresia.model.MiMembresiaResponse
import com.sprm.apexgym.feature.membresia.model.MiSaldoResponse
import com.sprm.apexgym.feature.membresia.model.PlanDisponible
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MembresiaApiService {

    @GET("api/app/membresia/mi-membresia")
    suspend fun obtenerMiMembresia(
        @Query("clienteId") clienteId: Int? = null
    ): Response<ApiResponse<MiMembresiaResponse>>

    @GET("api/app/membresia/mi-saldo")
    suspend fun obtenerMiSaldo(
        @Query("clienteId") clienteId: Int? = null
    ): Response<ApiResponse<MiSaldoResponse>>

    @GET("api/app/membresia/planes")
    suspend fun obtenerPlanes(): Response<ApiResponse<List<PlanDisponible>>>
}
