package com.sprm.apexgym.core.network

import com.sprm.apexgym.core.network.client.ApiResponse
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Devuelve `data` solo cuando la respuesta es exitosa y el backend marca responseFlag = 0. */
fun <T> Response<ApiResponse<T>>.dataExitosa(): T? {
    val body = body()
    return if (isSuccessful && body?.responseFlag == 0) body.data else null
}

/** Extrae el mensaje del envelope de error del backend: { data, statusCode, message, responseFlag }. */
fun Response<*>.mensajeDeError(): String? {
    val raw = try {
        errorBody()?.string()
    } catch (_: Exception) {
        null
    }
    if (raw.isNullOrBlank()) return null

    return try {
        val json = JSONObject(raw)
        when {
            json.optString("message").isNotBlank() -> json.optString("message")
            json.optJSONObject("data")?.optString("mensaje")?.isNotBlank() == true ->
                json.optJSONObject("data")?.optString("mensaje")
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

fun Throwable.mensajeUsuario(): String = when (this) {
    is IOException -> "No se pudo conectar con el servidor. Verifica tu conexión."
    else -> localizedMessage ?: "Error desconocido"
}

/** Formatea un monto con separador de miles y dos decimales. */
fun Double.formatearMoneda(): String =
    "$" + String.format(Locale.US, "%,.2f", this) + " MXN"

fun Double.formatearMonedaSinSufijo(): String =
    "$" + String.format(Locale.US, "%,.2f", this)

private val FORMATOS_ENTRADA = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSSX",
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    "yyyy-MM-dd'T'HH:mm:ssX",
    "yyyy-MM-dd'T'HH:mm:ss'Z'",
    "yyyy-MM-dd'T'HH:mm:ss",
    "yyyy-MM-dd"
)

fun formatearFecha(
    valor: String?,
    patronSalida: String = "dd/MM/yyyy HH:mm",
    zona: TimeZone = TimeZone.getDefault()
): String {
    if (valor.isNullOrBlank()) return "—"

    val esSoloFechaSalida = !patronSalida.contains("H") &&
        !patronSalida.contains("h") &&
        !patronSalida.contains("m")
    val matchSoloFecha = Regex("^(\\d{4}-\\d{2}-\\d{2})(?:T00:00:00(?:\\.000)?Z?)?$")
        .matchEntire(valor.trim())

    if (esSoloFechaSalida && matchSoloFecha != null) {
        return try {
            val utc = TimeZone.getTimeZone("UTC")
            val formatoIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = utc
                isLenient = false
            }
            val fecha = formatoIso.parse(matchSoloFecha.groupValues[1]) ?: return valor
            SimpleDateFormat(patronSalida, Locale("es", "MX")).apply {
                timeZone = utc
            }.format(fecha)
        } catch (_: Exception) {
            valor
        }
    }

    for (patron in FORMATOS_ENTRADA) {
        try {
            val formato = SimpleDateFormat(patron, Locale.US).apply {
                this.timeZone = if (patron.endsWith("X") || patron.endsWith("'Z'")) {
                    TimeZone.getTimeZone("UTC")
                } else {
                    zona
                }
                isLenient = false
            }
            val fecha = formato.parse(valor) ?: continue
            val salida = SimpleDateFormat(patronSalida, Locale("es", "MX"))
            salida.timeZone = zona
            return salida.format(fecha)
        } catch (_: Exception) {
            // Se prueba el siguiente formato.
        }
    }
    return valor
}

fun formatearHora(valor: String?): String = formatearFecha(valor, "HH:mm")

fun normalizarFechaAEnviar(texto: String): String? {
    val limpio = texto.trim()
    if (limpio.isEmpty()) return null
    if (Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(limpio)) return limpio

    val salida = try {
        SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }.parse(limpio)
    } catch (_: Exception) {
        null
    }
    return salida?.let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it) }
}

fun fechaHoyIso(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(System.currentTimeMillis())

fun inicioDeMesIso(): String =
    SimpleDateFormat("yyyy-MM-01", Locale.US).format(System.currentTimeMillis())
