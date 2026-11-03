package com.sprm.apexgym.feature.membresia.model

import com.google.gson.annotations.SerializedName

data class BeneficioItem(
    val id: Any? = null,
    val titulo: String = "",
    val descripcion: String? = null,
    val icono: String? = null
)

data class MembresiaData(
    val id: Int = 0,
    val plan: String = "",
    val descripcionPlan: String? = null,
    val precio: Double = 0.0,
    val duracionDias: Int = 0,
    val estado: String = "Inactiva",
    val colorEstado: String? = "#e03131",
    val fechaInicio: String? = null,
    val fechaFin: String? = null,
    val diasRestantes: Int = 0,
    val renovacionAutomatica: Boolean = false,
    val beneficios: List<BeneficioItem> = emptyList()
)

data class ClienteInfo(
    val id: Int = 0,
    val nombreCompleto: String = "",
    val codigoAcceso: String = ""
)

data class MiMembresiaResponse(
    val encontrado: Boolean = false,
    val cliente: ClienteInfo? = null,
    val tieneMembresia: Boolean = false,
    val membresia: MembresiaData? = null,
    val mensaje: String? = null
)

data class DesgloseSaldoItem(
    val concepto: String = "",
    val periodo: String = "",
    val monto: Double = 0.0,
    val estado: String = ""
)

data class MiSaldoResponse(
    val encontrado: Boolean = false,
    val clienteId: Int? = null,
    val clienteNombre: String? = null,
    val alCorriente: Boolean = true,
    val saldoPendiente: Double = 0.0,
    val moneda: String = "MXN",
    val estadoCuenta: String = "Al corriente",
    val proximoVencimiento: String? = null,
    val proximoMontoAPagar: Double = 0.0,
    val desglose: List<DesgloseSaldoItem> = emptyList(),
    val mensaje: String? = null
)

data class PlanDisponible(
    val id: Int = 0,
    val nombre: String = "",
    val descripcion: String? = null,
    val duracionDias: Int = 0,
    val precio: Double = 0.0,
    val beneficios: List<BeneficioItem> = emptyList()
)
