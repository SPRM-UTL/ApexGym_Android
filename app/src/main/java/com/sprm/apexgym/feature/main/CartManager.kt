package com.sprm.apexgym.feature.main

import android.content.Context
import com.sprm.apexgym.core.network.client.ProductoClienteDto
import com.sprm.apexgym.core.network.client.TipoMembresiaDto
import org.json.JSONArray
import org.json.JSONObject

enum class CartItemType {
    PRODUCTO,
    MEMBRESIA
}

data class CartItem(
    val tipo: CartItemType,
    val id: Int,
    val nombre: String,
    val subtitulo: String,
    val precioUnitario: Double,
    val cantidad: Int,
    val stockMaximo: Int,
    val imagenUrl: String? = null
) {
    val subtotal: Double
        get() = precioUnitario * cantidad
}

data class CartActionResult(
    val exito: Boolean,
    val mensaje: String
)

object CartManager {

    private const val PREFS_NAME = "carrito_apexgym"
    private const val KEY_ITEMS = "cart_items_json"

    private val items = mutableListOf<CartItem>()
    private var inicializado = false
    private val listeners = mutableSetOf<() -> Unit>()

    private fun asegurarInicializado(context: Context) {
        if (inicializado) return
        inicializado = true
        cargarDesdePreferencias(context.applicationContext)
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notificarCambios() {
        listeners.toList().forEach { it.invoke() }
    }

    fun obtenerItems(context: Context): List<CartItem> {
        asegurarInicializado(context)
        return items.toList()
    }

    fun obtenerCantidadTotalArticulos(context: Context): Int {
        asegurarInicializado(context)
        return items.sumOf { it.cantidad }
    }

    fun obtenerTotalPrecio(context: Context): Double {
        asegurarInicializado(context)
        return items.sumOf { it.subtotal }
    }

    fun obtenerSubtotalProductos(context: Context): Double {
        asegurarInicializado(context)
        return items.filter { it.tipo == CartItemType.PRODUCTO }.sumOf { it.subtotal }
    }

    fun obtenerMembresiaActual(context: Context): CartItem? {
        asegurarInicializado(context)
        return items.firstOrNull { it.tipo == CartItemType.MEMBRESIA }
    }

    fun agregarProducto(
        context: Context,
        producto: ProductoClienteDto,
        cantidadAgregar: Int = 1
    ): CartActionResult {
        asegurarInicializado(context)

        if (producto.stockActual <= 0) {
            return CartActionResult(
                exito = false,
                mensaje = "El producto \"${producto.nombre}\" está agotado temporalmente."
            )
        }

        val cantidadSegura = cantidadAgregar.coerceAtLeast(1)
        val indexExistente = items.indexOfFirst {
            it.tipo == CartItemType.PRODUCTO && it.id == producto.id
        }

        if (indexExistente >= 0) {
            val actual = items[indexExistente]
            val stockMax = if (producto.stockActual > 0) producto.stockActual else actual.stockMaximo
            if (actual.cantidad >= stockMax) {
                return CartActionResult(
                    exito = false,
                    mensaje = "Ya alcanzaste el stock máximo disponible ($stockMax uds.) de ${producto.nombre} en tu carrito."
                )
            }

            val nuevaCantidad = (actual.cantidad + cantidadSegura).coerceAtMost(stockMax)
            val ajustadoPorStock = (actual.cantidad + cantidadSegura) > stockMax

            items[indexExistente] = actual.copy(
                nombre = producto.nombre,
                subtitulo = producto.categoria?.nombre ?: "Producto",
                precioUnitario = producto.precioVenta,
                cantidad = nuevaCantidad,
                stockMaximo = stockMax,
                imagenUrl = producto.imagenUrl
            )
            guardarEnPreferencias(context.applicationContext)
            notificarCambios()

            return if (ajustadoPorStock) {
                CartActionResult(
                    exito = true,
                    mensaje = "Se actualizó ${producto.nombre} al máximo disponible en stock ($stockMax uds.)."
                )
            } else {
                CartActionResult(
                    exito = true,
                    mensaje = "${producto.nombre} agregado al carrito ($nuevaCantidad en carrito)."
                )
            }
        } else {
            val stockMax = producto.stockActual
            val cantidadInicial = cantidadSegura.coerceAtMost(stockMax)
            items.add(
                CartItem(
                    tipo = CartItemType.PRODUCTO,
                    id = producto.id,
                    nombre = producto.nombre,
                    subtitulo = producto.categoria?.nombre ?: "Producto",
                    precioUnitario = producto.precioVenta,
                    cantidad = cantidadInicial,
                    stockMaximo = stockMax,
                    imagenUrl = producto.imagenUrl
                )
            )
            guardarEnPreferencias(context.applicationContext)
            notificarCambios()

            return CartActionResult(
                exito = true,
                mensaje = "${producto.nombre} agregado al carrito."
            )
        }
    }

    fun agregarMembresia(
        context: Context,
        plan: TipoMembresiaDto
    ): CartActionResult {
        asegurarInicializado(context)

        val membresiaExistente = items.firstOrNull { it.tipo == CartItemType.MEMBRESIA }
        if (membresiaExistente != null) {
            return if (membresiaExistente.id == plan.id) {
                CartActionResult(
                    exito = false,
                    mensaje = "Ya tienes el plan \"${plan.nombre}\" en tu carrito."
                )
            } else {
                CartActionResult(
                    exito = false,
                    mensaje = "Ya tienes la membresía \"${membresiaExistente.nombre}\" en tu carrito."
                )
            }
        }

        items.add(
            0,
            CartItem(
                tipo = CartItemType.MEMBRESIA,
                id = plan.id,
                nombre = plan.nombre,
                subtitulo = "${plan.duracionDias} días de vigencia",
                precioUnitario = plan.precio,
                cantidad = 1,
                stockMaximo = 1,
                imagenUrl = null
            )
        )
        guardarEnPreferencias(context.applicationContext)
        notificarCambios()

        return CartActionResult(
            exito = true,
            mensaje = "Membresía \"${plan.nombre}\" agregada al carrito."
        )
    }

    fun cambiarCantidadProducto(
        context: Context,
        productoId: Int,
        delta: Int
    ): CartActionResult {
        asegurarInicializado(context)

        val index = items.indexOfFirst {
            it.tipo == CartItemType.PRODUCTO && it.id == productoId
        }
        if (index < 0) {
            return CartActionResult(false, "Producto no encontrado en el carrito")
        }

        val item = items[index]
        val nuevaCantidad = item.cantidad + delta

        if (nuevaCantidad <= 0) {
            items.removeAt(index)
            guardarEnPreferencias(context.applicationContext)
            notificarCambios()
            return CartActionResult(true, "${item.nombre} eliminado del carrito")
        }

        if (nuevaCantidad > item.stockMaximo) {
            return CartActionResult(
                exito = false,
                mensaje = "Stock máximo disponible: ${item.stockMaximo} unidades"
            )
        }

        items[index] = item.copy(cantidad = nuevaCantidad)
        guardarEnPreferencias(context.applicationContext)
        notificarCambios()
        return CartActionResult(true, "Cantidad actualizada")
    }

    fun eliminarItem(
        context: Context,
        tipo: CartItemType,
        id: Int
    ) {
        asegurarInicializado(context)
        val eliminado = items.removeAll { it.tipo == tipo && it.id == id }
        if (eliminado) {
            guardarEnPreferencias(context.applicationContext)
            notificarCambios()
        }
    }

    fun vaciarCarrito(context: Context) {
        asegurarInicializado(context)
        if (items.isNotEmpty()) {
            items.clear()
            guardarEnPreferencias(context.applicationContext)
            notificarCambios()
        }
    }

    private fun guardarEnPreferencias(appContext: Context) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("tipo", item.tipo.name)
                    put("id", item.id)
                    put("nombre", item.nombre)
                    put("subtitulo", item.subtitulo)
                    put("precioUnitario", item.precioUnitario)
                    put("cantidad", item.cantidad)
                    put("stockMaximo", item.stockMaximo)
                    put("imagenUrl", item.imagenUrl ?: "")
                }
            )
        }
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ITEMS, array.toString())
            .apply()
    }

    private fun cargarDesdePreferencias(appContext: Context) {
        items.clear()
        val raw = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ITEMS, null) ?: return

        try {
            val array = JSONArray(raw)
            var tieneMembresia = false
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val tipoStr = obj.optString("tipo", CartItemType.PRODUCTO.name)
                val tipo = try {
                    CartItemType.valueOf(tipoStr)
                } catch (_: Exception) {
                    CartItemType.PRODUCTO
                }

                if (tipo == CartItemType.MEMBRESIA) {
                    if (tieneMembresia) continue
                    tieneMembresia = true
                }

                val stockMaximo = obj.optInt("stockMaximo", 1).coerceAtLeast(1)
                val cantidad = if (tipo == CartItemType.MEMBRESIA) {
                    1
                } else {
                    obj.optInt("cantidad", 1).coerceIn(1, stockMaximo)
                }

                items.add(
                    CartItem(
                        tipo = tipo,
                        id = obj.optInt("id", 0),
                        nombre = obj.optString("nombre", ""),
                        subtitulo = obj.optString("subtitulo", ""),
                        precioUnitario = obj.optDouble("precioUnitario", 0.0),
                        cantidad = cantidad,
                        stockMaximo = stockMaximo,
                        imagenUrl = obj.optString("imagenUrl", "").takeIf { it.isNotBlank() }
                    )
                )
            }
        } catch (_: Exception) {
            items.clear()
        }
    }
}
