package com.sprm.apexgym.core.network.api

import com.sprm.apexgym.core.network.client.ApiResponse
import com.sprm.apexgym.core.network.client.LoginRequest
import com.sprm.apexgym.core.network.client.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// ── Catálogos ────────────────────────────────────────────────────────────────

data class CatalogoDto(
    val id: Int = 0,
    val nombre: String? = null,
    val descripcion: String? = null,
    val estado: String? = null,
    val salarioBase: Double? = null
)

data class CajaDto(
    val id: Int = 0,
    val nombre: String? = null,
    val ubicacion: String? = null,
    val estado: String? = null
)

data class TipoVisitaDto(
    val id: Int = 0,
    val nombre: String? = null,
    val descripcion: String? = null,
    val costo: Double = 0.0,
    val estado: String? = null
)

data class MetodoPagoDto(
    val id: Int = 0,
    val nombre: String? = null,
    val codigo: String? = null,
    val descripcion: String? = null,
    val estado: String? = null
)

data class CrearCajaRequest(
    val nombre: String,
    val ubicacion: String? = null,
    val estado: String = "ACTIVA"
)

// ── Empleados ────────────────────────────────────────────────────────────────

data class UsuarioEmpleadoDto(
    val id: Int = 0,
    val nombre: String? = null,
    val email: String? = null
)

data class SucursalDto(
    val id: Int = 1,
    val empresaId: Int = 1,
    val nombre: String? = null,
    val codigo: String? = null,
    val direccion: String? = null,
    val ciudad: String? = null,
    val estado: String? = null,
    val codigoPostal: String? = null,
    val telefono: String? = null,
    val email: String? = null,
    val activo: Boolean = true,
    val puedeCambiarSucursal: Boolean? = null
)

data class EmpleadoDto(
    val id: Int = 0,
    val sucursalId: Int? = null,
    val puestoId: Int = 0,
    val areaTrabajoId: Int = 0,
    val estadoEmpleadoId: Int = 0,
    val usuarioId: Int? = null,
    val nombre: String? = null,
    val apellidoPaterno: String? = null,
    val apellidoMaterno: String? = null,
    val telefono: String? = null,
    val correo: String? = null,
    val calle: String? = null,
    val numeroExterior: String? = null,
    val numeroInterior: String? = null,
    val colonia: String? = null,
    val codigoPostal: String? = null,
    val municipio: String? = null,
    val estado: String? = null,
    val entreCalles: String? = null,
    val fechaNacimiento: String? = null,
    val fechaIngreso: String? = null,
    val imagenUrl: String? = null,
    val puesto: CatalogoDto? = null,
    val areaTrabajo: CatalogoDto? = null,
    val estadoEmpleado: CatalogoDto? = null,
    val usuario: UsuarioEmpleadoDto? = null
) {
    val nombreCompleto: String
        get() = listOf(nombre, apellidoPaterno, apellidoMaterno)
            .filterNotNull()
            .filter { it.isNotBlank() }
            .joinToString(" ")
}

data class EmpleadoRequest(
    val puestoId: Int,
    val areaTrabajoId: Int,
    val estadoEmpleadoId: Int,
    val usuarioId: Int? = null,
    val nombre: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String? = null,
    val telefono: String,
    val correo: String? = null,
    val calle: String? = null,
    val numeroExterior: String? = null,
    val numeroInterior: String? = null,
    val colonia: String? = null,
    val codigoPostal: String? = null,
    val municipio: String? = null,
    val estado: String? = null,
    val entreCalles: String? = null,
    val fechaNacimiento: String,
    val fechaIngreso: String,
    val imagenUrl: String? = null,
    val imagenPublicId: String? = null
)

// ── Asistencias / recepción ──────────────────────────────────────────────────

data class UsuarioFotoDto(
    val fotoUrl: String? = null
)

data class ClienteAsistenciaDto(
    val id: Int = 0,
    val estadoClienteId: Int? = null,
    val nombre: String? = null,
    val apellidoPaterno: String? = null,
    val apellidoMaterno: String? = null,
    val telefono: String? = null,
    val correo: String? = null,
    val usuario: UsuarioFotoDto? = null,
    val estadoCliente: CatalogoDto? = null
) {
    val nombreCompleto: String
        get() = listOf(nombre, apellidoPaterno, apellidoMaterno)
            .filterNotNull()
            .filter { it.isNotBlank() }
            .joinToString(" ")
}

data class TipoMembresiaAsistenciaDto(
    val id: Int = 0,
    val nombre: String? = null,
    val duracionDias: Int = 0,
    val precio: Double = 0.0
)

data class EstadoMembresiaAsistenciaDto(
    val id: Int = 0,
    val nombre: String? = null,
    val color: String? = null
)

data class ClienteMembresiaAsistenciaDto(
    val id: Int = 0,
    val clienteId: Int = 0,
    val tipoMembresiaId: Int = 0,
    val estadoMembresiaId: Int = 0,
    val fechaInicio: String? = null,
    val fechaFin: String? = null,
    val tipoMembresia: TipoMembresiaAsistenciaDto? = null,
    val estadoMembresia: EstadoMembresiaAsistenciaDto? = null
)

data class AsistenciaDto(
    val id: Int = 0,
    val clienteId: Int = 0,
    val clienteMembresiaId: Int? = null,
    val fechaHora: String? = null,
    val observaciones: String? = null,
    val cliente: ClienteAsistenciaDto? = null,
    val clienteMembresia: ClienteMembresiaAsistenciaDto? = null
)

data class AsistenciasHoyDto(
    val total: Int = 0,
    val asistencias: List<AsistenciaDto> = emptyList()
)

data class ValidarAccesoRequest(
    val clienteId: Int,
    val observaciones: String? = null
)

data class ValidarQrRequest(
    val qr: String
)

data class ValidarAccesoDto(
    val acceso: Boolean = false,
    val codigo: String? = null,
    val cliente: ClienteAsistenciaDto? = null,
    val membresia: ClienteMembresiaAsistenciaDto? = null,
    val asistencia: AsistenciaDto? = null,
    val diasRestantes: Int? = null,
    val fechaExpiracion: String? = null,
    val mensaje: String? = null
)

// ── Caja ─────────────────────────────────────────────────────────────────────

data class AperturaCajaDto(
    val id: Int = 0,
    val cajaId: Int = 0,
    val empleadoId: Int = 0,
    val montoInicial: Double = 0.0,
    val fechaApertura: String? = null,
    val estado: String? = null,
    val desgloseInicio: String? = null,
    val observaciones: String? = null,
    val caja: CajaDto? = null,
    val empleado: EmpleadoDto? = null
)

data class MovimientoCajaDto(
    val id: Int = 0,
    val aperturaCajaId: Int = 0,
    val empleadoId: Int = 0,
    val tipo: String? = null,
    val monto: Double = 0.0,
    val concepto: String? = null,
    val estado: String? = null,
    val desglose: String? = null,
    val observaciones: String? = null,
    val createdAt: String? = null
)

data class CorteCajaDto(
    val id: Int = 0,
    val aperturaCajaId: Int = 0,
    val empleadoId: Int = 0,
    val efectivoContado: Double = 0.0,
    val totalEntradas: Double = 0.0,
    val totalSalidas: Double = 0.0,
    val totalVentas: Double = 0.0,
    val totalVentasEfectivo: Double = 0.0,
    val diferencia: Double = 0.0,
    val estado: String? = null,
    val desgloseInicio: String? = null,
    val desgloseFin: String? = null,
    val fechaValidacion: String? = null,
    val observaciones: String? = null,
    val fechaCorte: String? = null
)

data class VentaMetodoDto(
    val metodoPagoId: Int = 0,
    val nombre: String? = null,
    val total: Double = 0.0,
    val transacciones: Int = 0
)

data class TotalesCajaDto(
    val totalVentas: Double = 0.0,
    val transaccionesVentas: Int = 0,
    val ticketPromedio: Double = 0.0,
    val totalVentasEfectivo: Double = 0.0,
    val totalVentasTerminal: Double = 0.0,
    val totalVentasDigital: Double = 0.0,
    val ventasPorMetodoPago: List<VentaMetodoDto>? = null
)

data class ResumenAperturaDto(
    val apertura: AperturaCajaDto? = null,
    val montoInicial: Double = 0.0,
    val totalEntradas: Double = 0.0,
    val totalSalidas: Double = 0.0,
    val saldoDisponible: Double = 0.0,
    val totalVentasEfectivo: Double = 0.0,
    val movimientos: List<MovimientoCajaDto>? = null,
    val movimientosPendientes: Int = 0,
    val movimientosValidados: Int = 0,
    val totales: TotalesCajaDto? = null
)

data class CrearAperturaRequest(
    val cajaId: Int,
    val empleadoId: Int,
    val montoInicial: Double,
    val fechaApertura: String? = null,
    val observaciones: String? = null,
    val desgloseInicio: String? = null
)

data class CrearMovimientoRequest(
    val aperturaCajaId: Int,
    val empleadoId: Int,
    val tipo: String,
    val monto: Double,
    val concepto: String,
    val observaciones: String? = null,
    val desglose: String? = null
)

data class CrearCorteRequest(
    val aperturaCajaId: Int,
    val empleadoId: Int,
    val efectivoContado: Double,
    val observaciones: String? = null,
    val desgloseInicio: String? = null,
    val desgloseFin: String? = null
)

// ── Visitas ──────────────────────────────────────────────────────────────────

data class VisitaDto(
    val id: Int = 0,
    val nombreVisitante: String? = null,
    val tipoVisitaId: Int = 0,
    val costo: Double = 0.0,
    val fechaHora: String? = null,
    val observaciones: String? = null,
    val tipoVisita: TipoVisitaDto? = null
)

data class CrearVisitaRequest(
    val nombreVisitante: String,
    val tipoVisitaId: Int,
    val observaciones: String? = null
)

// ── Pagos / POS ──────────────────────────────────────────────────────────────

data class CrearCobroRequest(
    val concepto: String,
    val monto: Double,
    val proveedor: String,
    val correoCliente: String? = null,
    val folioVoucher: String? = null,
    val uuidIdempotencia: String? = null,
    val metadatos: Map<String, Any>? = null
)

data class TransaccionPagoDto(
    val id: Int = 0,
    val uuidIdempotencia: String? = null,
    val proveedor: String? = null,
    val referenciaExterna: String? = null,
    val monto: Double = 0.0,
    val moneda: String? = null,
    val estado: String? = null,
    val concepto: String? = null,
    val correoCliente: String? = null,
    val urlCheckout: String? = null,
    val folioVoucher: String? = null,
    val createdAt: String? = null
)

data class CrearCobroDto(
    val idempotente: Boolean = false,
    val mensaje: String? = null,
    val transaccion: TransaccionPagoDto? = null,
    val urlCheckout: String? = null
)

// ── Ventas y Checkout Digital ───────────────────────────────────────────────

data class DetalleVentaRequest(
    val tipoItem: String,
    val cantidad: Int,
    val productoId: Int? = null,
    val servicioId: Int? = null,
    val tipoMembresiaId: Int? = null,
    val descripcion: String? = null
)

data class PagoVentaRequest(
    val correoCliente: String? = null,
    val folioVoucher: String? = null,
    val tipoTarjeta: String? = null,
    val marcaTarjeta: String? = null,
    val ultimosDigitos: String? = null,
    val referencia: String? = null
)

data class CrearVentaRequest(
    val clienteId: Int? = null,
    val empleadoId: Int? = null,
    val aperturaCajaId: Int? = null,
    val metodoPagoId: Int,
    val subtotal: Double,
    val total: Double,
    val montoRecibido: Double? = null,
    val detalleVenta: List<DetalleVentaRequest>,
    val uuidIdempotencia: String? = null,
    val pago: PagoVentaRequest? = null
)

data class FolioVentaDto(
    val id: Int = 0
)

data class ItemNombreDto(
    val nombre: String? = null
)

data class DetalleVentaItemDto(
    val id: Int = 0,
    val cantidad: Int = 1,
    val subtotal: Double? = null,
    val descripcion: String? = null,
    val tipoItem: String? = null,
    val producto: ItemNombreDto? = null,
    val servicio: ItemNombreDto? = null,
    val tipoMembresia: ItemNombreDto? = null
)

data class PagoDigitalDto(
    val estado: String? = null,
    val urlCheckout: String? = null,
    val yaPagada: Boolean = false,
    val venceEn: String? = null
)

data class VentaDto(
    val id: Int = 0,
    val total: Double = 0.0,
    val subtotal: Double = 0.0,
    val montoRecibido: Double? = null,
    val cambio: Double? = null,
    val estado: String? = null,
    val clienteId: Int? = null,
    val empleadoId: Int? = null,
    val metodoPagoId: Int? = null,
    val folioVenta: FolioVentaDto? = null,
    val pagoDigital: PagoDigitalDto? = null,
    val metodoPago: MetodoPagoDto? = null,
    val detalleVenta: List<DetalleVentaItemDto>? = null,
    val transaccionesPago: List<TransaccionPagoDto>? = null,
    val createdAt: String? = null
)

data class VentasPaginadasDto(
    val registros: List<VentaDto> = emptyList(),
    val total: Int = 0,
    val totalPaginas: Int = 0,
    val paginaActual: Int = 1
)

data class EstadoPagoResponseDto(
    val venta: VentaDto? = null,
    val pago: TransaccionPagoDto? = null
)

interface EmpleadoApiService {

    // Autenticación de empleado
    @POST("api/usuarios/verificarCredenciales")
    suspend fun loginEmpleado(@Body request: LoginRequest): Response<LoginResponse>

    // Empleados
    @GET("api/empleados")
    suspend fun obtenerEmpleados(
        @Query("sucursalId") sucursalId: Int? = null
    ): Response<ApiResponse<List<EmpleadoDto>>>

    @GET("api/empleados/{id}")
    suspend fun obtenerEmpleado(
        @Path("id") id: Int,
        @Query("sucursalId") sucursalId: Int? = null
    ): Response<ApiResponse<EmpleadoDto>>

    @POST("api/empleados")
    suspend fun crearEmpleado(
        @Body request: EmpleadoRequest
    ): Response<ApiResponse<EmpleadoDto>>

    @PUT("api/empleados/{id}")
    suspend fun actualizarEmpleado(
        @Path("id") id: Int,
        @Body request: EmpleadoRequest
    ): Response<ApiResponse<EmpleadoDto>>

    @DELETE("api/empleados/{id}")
    suspend fun eliminarEmpleado(
        @Path("id") id: Int
    ): Response<ApiResponse<Any>>

    @GET("api/puestos")
    suspend fun obtenerPuestos(
        @Query("sucursalId") sucursalId: Int? = null
    ): Response<ApiResponse<List<CatalogoDto>>>

    @GET("api/areas-trabajo")
    suspend fun obtenerAreasTrabajo(
        @Query("sucursalId") sucursalId: Int? = null
    ): Response<ApiResponse<List<CatalogoDto>>>

    @GET("api/estados-empleado")
    suspend fun obtenerEstadosEmpleado(
        @Query("sucursalId") sucursalId: Int? = null
    ): Response<ApiResponse<List<CatalogoDto>>>

    @GET("api/usuarios")
    suspend fun obtenerUsuarios(): Response<ApiResponse<List<UsuarioEmpleadoDto>>>

    // Clientes (para recepción y POS)
    @GET("api/clientes")
    suspend fun obtenerClientes(
        @Query("sucursalId") sucursalId: Int? = null
    ): Response<ApiResponse<List<ClienteAsistenciaDto>>>

    // Recepción / asistencias
    @GET("api/asistencias/hoy")
    suspend fun obtenerAsistenciasHoy(
        @Query("sucursalId") sucursalId: Int? = null
    ): Response<ApiResponse<AsistenciasHoyDto>>

    @GET("api/asistencias")
    suspend fun obtenerAsistencias(
        @Query("sucursalId") sucursalId: Int? = null
    ): Response<ApiResponse<List<AsistenciaDto>>>

    @GET("api/asistencias/buscar-cliente")
    suspend fun buscarCliente(
        @Query("q") termino: String,
        @Query("sucursalId") sucursalId: Int? = null
    ): Response<ApiResponse<List<ClienteAsistenciaDto>>>

    @POST("api/asistencias/validar-y-registrar")
    suspend fun validarYRegistrarAcceso(
        @Body request: ValidarAccesoRequest
    ): Response<ApiResponse<ValidarAccesoDto>>

    @POST("api/asistencias/validar-qr")
    suspend fun validarQr(
        @Body request: ValidarQrRequest
    ): Response<ApiResponse<ValidarAccesoDto>>

    @GET("api/clientes-membresias/cliente/{clienteId}/activa")
    suspend fun obtenerMembresiaActiva(
        @Path("clienteId") clienteId: Int,
        @Query("sucursalId") sucursalId: Int? = null
    ): Response<ApiResponse<ClienteMembresiaAsistenciaDto>>

    // Caja
    @GET("api/cajas")
    suspend fun obtenerCajas(
        @Query("sucursalId") sucursalId: Int? = null
    ): Response<ApiResponse<List<CajaDto>>>

    @POST("api/cajas")
    suspend fun crearCaja(
        @Body request: CrearCajaRequest
    ): Response<ApiResponse<CajaDto>>

    @GET("api/aperturas-caja")
    suspend fun obtenerAperturas(
        @Query("sucursalId") sucursalId: Int? = null
    ): Response<ApiResponse<List<AperturaCajaDto>>>

    @POST("api/aperturas-caja")
    suspend fun crearApertura(
        @Body request: CrearAperturaRequest
    ): Response<ApiResponse<AperturaCajaDto>>

    @POST("api/aperturas-caja/{id}/cerrar")
    suspend fun cerrarApertura(
        @Path("id") id: Int
    ): Response<ApiResponse<Any>>

    @GET("api/movimientos-caja/apertura/{aperturaId}")
    suspend fun obtenerMovimientos(
        @Path("aperturaId") aperturaId: Int,
        @Query("sucursalId") sucursalId: Int? = null
    ): Response<ApiResponse<List<MovimientoCajaDto>>>

    @GET("api/movimientos-caja/apertura/{aperturaId}/resumen")
    suspend fun obtenerResumenApertura(
        @Path("aperturaId") aperturaId: Int,
        @Query("sucursalId") sucursalId: Int? = null
    ): Response<ApiResponse<ResumenAperturaDto>>

    @POST("api/movimientos-caja")
    suspend fun crearMovimiento(
        @Body request: CrearMovimientoRequest
    ): Response<ApiResponse<MovimientoCajaDto>>

    @PUT("api/movimientos-caja/{id}/validar")
    suspend fun validarMovimiento(
        @Path("id") id: Int
    ): Response<ApiResponse<MovimientoCajaDto>>

    @GET("api/cortes-caja")
    suspend fun obtenerCortes(
        @Query("sucursalId") sucursalId: Int? = null
    ): Response<ApiResponse<List<CorteCajaDto>>>

    @GET("api/cortes-caja/apertura/{aperturaId}")
    suspend fun obtenerCortesPorApertura(
        @Path("aperturaId") aperturaId: Int,
        @Query("sucursalId") sucursalId: Int? = null
    ): Response<ApiResponse<List<CorteCajaDto>>>

    @POST("api/cortes-caja")
    suspend fun crearCorte(
        @Body request: CrearCorteRequest
    ): Response<ApiResponse<CorteCajaDto>>

    // Visitas
    @GET("api/visitas")
    suspend fun obtenerVisitas(
        @Query("fechaInicio") fechaInicio: String? = null,
        @Query("fechaFin") fechaFin: String? = null,
        @Query("sucursalId") sucursalId: Int? = null
    ): Response<ApiResponse<List<VisitaDto>>>

    @GET("api/visitas/{id}")
    suspend fun obtenerVisita(
        @Path("id") id: Int,
        @Query("sucursalId") sucursalId: Int? = null
    ): Response<ApiResponse<VisitaDto>>

    @POST("api/visitas")
    suspend fun crearVisita(
        @Body request: CrearVisitaRequest
    ): Response<ApiResponse<VisitaDto>>

    @PUT("api/visitas/{id}")
    suspend fun actualizarVisita(
        @Path("id") id: Int,
        @Body request: CrearVisitaRequest
    ): Response<ApiResponse<VisitaDto>>

    @GET("api/tipos-visita")
    suspend fun obtenerTiposVisita(
        @Query("sucursalId") sucursalId: Int? = null
    ): Response<ApiResponse<List<TipoVisitaDto>>>

    // Sucursales
    @GET("api/sucursales")
    suspend fun obtenerSucursales(): Response<ApiResponse<List<SucursalDto>>>

    @GET("api/sucursales/disponibles")
    suspend fun obtenerSucursalesDisponibles(): Response<ApiResponse<List<SucursalDto>>>

    @GET("api/sucursales/actual")
    suspend fun obtenerSucursalActual(): Response<ApiResponse<SucursalDto>>

    // Métodos de pago
    @GET("api/metodos-pago")
    suspend fun obtenerMetodosPago(): Response<ApiResponse<List<MetodoPagoDto>>>

    // Catálogo POS (productos y servicios por sucursal)
    @GET("api/productos")
    suspend fun obtenerProductos(): Response<ApiResponse<List<com.sprm.apexgym.core.network.client.ProductoClienteDto>>>

    @GET("api/servicios")
    suspend fun obtenerServicios(): Response<ApiResponse<List<com.sprm.apexgym.core.network.client.ServicioClienteDto>>>

    // Pagos / POS
    @POST("api/pagos/crear-cobro")
    suspend fun crearCobro(
        @Body request: CrearCobroRequest
    ): Response<ApiResponse<CrearCobroDto>>

    @GET("api/pagos/transacciones")
    suspend fun obtenerTransacciones(
        @Query("sucursalId") sucursalId: Int? = null
    ): Response<ApiResponse<List<TransaccionPagoDto>>>

    // Ventas y Checkout Digital
    @GET("api/ventas")
    suspend fun obtenerVentas(
        @Query("pagina") pagina: Int = 1,
        @Query("limite") limite: Int = 50,
        @Query("clienteId") clienteId: Int? = null,
        @Query("estado") estado: String? = null
    ): Response<ApiResponse<VentasPaginadasDto>>

    @POST("api/ventas")
    suspend fun crearVenta(
        @Body request: CrearVentaRequest,
        @Header("x-idempotency-key") idempotencyKey: String? = null
    ): Response<ApiResponse<VentaDto>>

    @GET("api/ventas/{id}/pago")
    suspend fun consultarEstadoPagoVenta(
        @Path("id") ventaId: Int,
        @Query("payment_id") paymentId: String? = null
    ): Response<ApiResponse<EstadoPagoResponseDto>>

    @POST("api/ventas/{id}/pago/checkout")
    suspend fun obtenerCheckoutPagoVenta(
        @Path("id") ventaId: Int
    ): Response<ApiResponse<PagoDigitalDto>>
}
