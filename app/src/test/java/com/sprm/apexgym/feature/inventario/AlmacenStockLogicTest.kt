package com.sprm.apexgym.feature.inventario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlmacenStockLogicTest {

    private val items = listOf(
        AlmacenStockLogic.ItemStockAlmacen(
            productoId = 1,
            nombre = "Proteína Whey 2kg",
            codigoBarras = "7501001",
            categoria = "Proteínas",
            stockActual = 2,
            stockMinimo = 3,
            stockTotalSucursal = 10,
            ubicacionEstante = "A-01"
        ),
        AlmacenStockLogic.ItemStockAlmacen(
            productoId = 2,
            nombre = "Agua Natural 1L",
            codigoBarras = "7501002",
            categoria = "Bebidas",
            stockActual = 25,
            stockMinimo = 5,
            stockTotalSucursal = 40,
            ubicacionEstante = "Refrigerador 1"
        ),
        AlmacenStockLogic.ItemStockAlmacen(
            productoId = 3,
            nombre = "Creatina Monohidratada",
            codigoBarras = "7501003",
            categoria = "Creatinas",
            stockActual = 0,
            stockMinimo = 2,
            stockTotalSucursal = 6,
            ubicacionEstante = "B-04"
        )
    )

    @Test
    fun `validarCreacionAlmacen exige nombre unico y longitudes validas`() {
        assertTrue(
            AlmacenStockLogic.validarCreacionAlmacen(
                nombre = "Bodega Norte",
                codigo = "BOD-N",
                ubicacion = "Planta baja",
                nombresExistentes = setOf("Almacén Principal", "Bodega General")
            ).valido
        )

        assertFalse(
            AlmacenStockLogic.validarCreacionAlmacen(
                nombre = "  ",
                nombresExistentes = emptySet()
            ).valido
        )

        assertFalse(
            AlmacenStockLogic.validarCreacionAlmacen(
                nombre = "bodega general",
                nombresExistentes = setOf("Almacén Principal", "Bodega General")
            ).valido
        )

        assertFalse(
            AlmacenStockLogic.validarCreacionAlmacen(
                nombre = "A".repeat(121)
            ).valido
        )
    }

    @Test
    fun `validarAjusteStock rechaza valores negativos y acepta ceros`() {
        assertTrue(AlmacenStockLogic.validarAjusteStock(0, 1, "Estante C-2").valido)
        assertFalse(AlmacenStockLogic.validarAjusteStock(-1, 1, null).valido)
        assertFalse(AlmacenStockLogic.validarAjusteStock(10, -1, null).valido)
        assertFalse(AlmacenStockLogic.validarAjusteStock(10, 1, "X".repeat(101)).valido)
    }

    @Test
    fun `validarTransferencia y simularTransferencia conservan consistencia de stock`() {
        val valida = AlmacenStockLogic.validarTransferencia(
            almacenOrigenId = 1,
            almacenDestinoId = 2,
            productoId = 10,
            cantidad = 5,
            stockDisponibleOrigen = 8
        )
        assertTrue(valida.valido)

        val (nuevoOrigen, nuevoDestino) = AlmacenStockLogic.simularTransferencia(
            stockOrigen = 8,
            stockDestino = 3,
            cantidad = 5
        )
        assertEquals(3, nuevoOrigen)
        assertEquals(8, nuevoDestino)

        assertFalse(
            AlmacenStockLogic.validarTransferencia(
                almacenOrigenId = 1,
                almacenDestinoId = 1,
                productoId = 10,
                cantidad = 2,
                stockDisponibleOrigen = 5
            ).valido
        )

        assertFalse(
            AlmacenStockLogic.validarTransferencia(
                almacenOrigenId = 1,
                almacenDestinoId = 2,
                productoId = 10,
                cantidad = 9,
                stockDisponibleOrigen = 5
            ).valido
        )

        assertFalse(
            AlmacenStockLogic.validarTransferencia(
                almacenOrigenId = 1,
                almacenDestinoId = 2,
                productoId = 10,
                cantidad = 0,
                stockDisponibleOrigen = 5
            ).valido
        )
    }

    @Test
    fun `calcularStockTotalProducto suma solo almacenes activos`() {
        val stocks = mapOf(1 to 12, 2 to 8, 3 to 50)
        val activos = setOf(1, 2)

        assertEquals(20, AlmacenStockLogic.calcularStockTotalProducto(stocks, activos))
        assertEquals(70, AlmacenStockLogic.calcularStockTotalProducto(stocks))
    }

    @Test
    fun `calcularResumenAlmacen y filtrarInventario priorizan stock bajo y buscan por codigo o estante`() {
        val resumen = AlmacenStockLogic.calcularResumenAlmacen(items)
        assertEquals(3, resumen.totalProductos)
        assertEquals(2, resumen.productosConStock)
        assertEquals(27, resumen.totalUnidades)
        assertEquals(2, resumen.productosStockBajo)

        val filtradosBajo = AlmacenStockLogic.filtrarInventario(items, "", soloStockBajo = true)
        assertEquals(listOf(3, 1), filtradosBajo.map { it.productoId })

        val porEstante = AlmacenStockLogic.filtrarInventario(items, "refrigerador")
        assertEquals(listOf(2), porEstante.map { it.productoId })

        val porCodigo = AlmacenStockLogic.filtrarInventario(items, "7501001")
        assertEquals(listOf(1), porCodigo.map { it.productoId })
    }
}
