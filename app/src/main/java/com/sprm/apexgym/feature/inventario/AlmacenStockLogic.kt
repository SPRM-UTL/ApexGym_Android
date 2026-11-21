package com.sprm.apexgym.feature.inventario

/**
 * Lógica pura JVM-testeable para validaciones, filtros y cálculos de stock
 * en el módulo multialmacén por sucursal.
 */
object AlmacenStockLogic {

    const val MAX_NOMBRE_ALMACEN = 120
    const val MAX_CODIGO_ALMACEN = 40
    const val MAX_UBICACION_ALMACEN = 200
    const val MAX_UBICACION_ESTANTE = 100

    data class ItemStockAlmacen(
        val productoId: Int,
        val nombre: String,
        val codigoBarras: String? = null,
        val categoria: String? = null,
        val stockActual: Int,
        val stockMinimo: Int,
        val stockTotalSucursal: Int = stockActual,
        val ubicacionEstante: String? = null,
        val precioVenta: Double = 0.0,
        val precioCompra: Double? = null
    ) {
        val esStockBajo: Boolean
            get() = stockActual <= stockMinimo
    }

    data class ResumenAlmacen(
        val totalProductos: Int,
        val productosConStock: Int,
        val totalUnidades: Int,
        val productosStockBajo: Int
    )

    data class ResultadoValidacionAlmacen(
        val valido: Boolean,
        val mensajeError: String? = null
    )

    fun validarCreacionAlmacen(
        nombre: String?,
        codigo: String? = null,
        ubicacion: String? = null,
        nombresExistentes: Set<String> = emptySet()
    ): ResultadoValidacionAlmacen {
        val nombreLimpio = nombre?.trim().orEmpty()
        if (nombreLimpio.isEmpty()) {
            return ResultadoValidacionAlmacen(false, "Ingresa el nombre del almacén.")
        }
        if (nombreLimpio.length > MAX_NOMBRE_ALMACEN) {
            return ResultadoValidacionAlmacen(
                false,
                "El nombre del almacén no puede superar $MAX_NOMBRE_ALMACEN caracteres."
            )
        }
        if (nombresExistentes.any { it.trim().equals(nombreLimpio, ignoreCase = true) }) {
            return ResultadoValidacionAlmacen(
                false,
                "Ya existe un almacén llamado \"$nombreLimpio\" en esta sucursal."
            )
        }

        val codigoLimpio = codigo?.trim().orEmpty()
        if (codigoLimpio.length > MAX_CODIGO_ALMACEN) {
            return ResultadoValidacionAlmacen(
                false,
                "El código del almacén no puede superar $MAX_CODIGO_ALMACEN caracteres."
            )
        }

        val ubicacionLimpia = ubicacion?.trim().orEmpty()
        if (ubicacionLimpia.length > MAX_UBICACION_ALMACEN) {
            return ResultadoValidacionAlmacen(
                false,
                "La ubicación no puede superar $MAX_UBICACION_ALMACEN caracteres."
            )
        }

        return ResultadoValidacionAlmacen(true)
    }

    fun validarAjusteStock(
        stockActual: Int?,
        stockMinimo: Int?,
        ubicacionEstante: String? = null
    ): ResultadoValidacionAlmacen {
        if (stockActual == null || stockActual < 0) {
            return ResultadoValidacionAlmacen(false, "El stock actual debe ser 0 o mayor.")
        }
        if (stockMinimo == null || stockMinimo < 0) {
            return ResultadoValidacionAlmacen(false, "El stock mínimo debe ser 0 o mayor.")
        }
        val estante = ubicacionEstante?.trim().orEmpty()
        if (estante.length > MAX_UBICACION_ESTANTE) {
            return ResultadoValidacionAlmacen(
                false,
                "La ubicación en estante no puede superar $MAX_UBICACION_ESTANTE caracteres."
            )
        }
        return ResultadoValidacionAlmacen(true)
    }

    fun validarTransferencia(
        almacenOrigenId: Int?,
        almacenDestinoId: Int?,
        productoId: Int?,
        cantidad: Int?,
        stockDisponibleOrigen: Int
    ): ResultadoValidacionAlmacen {
        if (almacenOrigenId == null || almacenOrigenId <= 0) {
            return ResultadoValidacionAlmacen(false, "Selecciona un almacén de origen.")
        }
        if (almacenDestinoId == null || almacenDestinoId <= 0) {
            return ResultadoValidacionAlmacen(false, "Selecciona un almacén de destino.")
        }
        if (almacenOrigenId == almacenDestinoId) {
            return ResultadoValidacionAlmacen(
                false,
                "El almacén destino debe ser distinto al de origen."
            )
        }
        if (productoId == null || productoId <= 0) {
            return ResultadoValidacionAlmacen(false, "Selecciona un producto para transferir.")
        }
        if (cantidad == null || cantidad <= 0) {
            return ResultadoValidacionAlmacen(false, "La cantidad a transferir debe ser mayor a cero.")
        }
        if (cantidad > stockDisponibleOrigen) {
            return ResultadoValidacionAlmacen(
                false,
                "Stock insuficiente en origen (disponible: $stockDisponibleOrigen)."
            )
        }
        return ResultadoValidacionAlmacen(true)
    }

    fun simularTransferencia(
        stockOrigen: Int,
        stockDestino: Int,
        cantidad: Int
    ): Pair<Int, Int> {
        require(cantidad > 0) { "La cantidad a transferir debe ser positiva" }
        require(cantidad <= stockOrigen) { "Stock insuficiente en almacén origen" }
        return (stockOrigen - cantidad) to (stockDestino.coerceAtLeast(0) + cantidad)
    }

    fun calcularStockTotalProducto(
        stocksPorAlmacen: Map<Int, Int>,
        almacenesActivosIds: Set<Int>? = null
    ): Int {
        return stocksPorAlmacen.entries
            .filter { (almacenId, _) -> almacenesActivosIds == null || almacenId in almacenesActivosIds }
            .sumOf { (_, stock) -> stock.coerceAtLeast(0) }
    }

    fun calcularResumenAlmacen(items: List<ItemStockAlmacen>): ResumenAlmacen {
        return ResumenAlmacen(
            totalProductos = items.size,
            productosConStock = items.count { it.stockActual > 0 },
            totalUnidades = items.sumOf { it.stockActual.coerceAtLeast(0) },
            productosStockBajo = items.count { it.esStockBajo }
        )
    }

    fun filtrarInventario(
        items: List<ItemStockAlmacen>,
        busqueda: String,
        soloStockBajo: Boolean = false
    ): List<ItemStockAlmacen> {
        val termino = busqueda.trim().lowercase()
        return items
            .filter { item ->
                val coincideBajo = !soloStockBajo || item.esStockBajo
                val coincideTexto = termino.isEmpty() ||
                    item.nombre.lowercase().contains(termino) ||
                    item.codigoBarras.orEmpty().lowercase().contains(termino) ||
                    item.categoria.orEmpty().lowercase().contains(termino) ||
                    item.ubicacionEstante.orEmpty().lowercase().contains(termino)
                coincideBajo && coincideTexto
            }
            .sortedWith(
                compareBy<ItemStockAlmacen> { !it.esStockBajo }
                    .thenBy { it.stockActual }
                    .thenBy { it.nombre.lowercase() }
            )
    }
}
