package com.sprm.apexgym.core.ui

import android.text.InputFilter
import android.text.SpannableStringBuilder
import android.text.Spanned

/**
 * Reglas puras de validación y filtrado por carácter reutilizadas por [InputFilters]
 * y probables directamente en JVM unit tests.
 */
object InputValidationRules {

    fun esDigitoValido(c: Char): Boolean = c in '0'..'9'

    fun esCaracterNombrePersona(c: Char): Boolean =
        c.isLetter() || c == ' ' || c == '\'' || c == '-' || c == '.'

    fun esNombrePersonaValido(texto: String): Boolean {
        val limpio = texto.trim()
        return limpio.isNotEmpty() &&
            limpio.any { it.isLetter() } &&
            limpio.all { esCaracterNombrePersona(it) }
    }

    fun esTelefonoValido(texto: String): Boolean = Regex("^\\d{10}$").matches(texto.trim())

    fun esCodigoPostalValido(texto: String): Boolean = Regex("^\\d{5}$").matches(texto.trim())

    fun esCaracterUsuario(c: Char): Boolean =
        c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '.' || c == '_' || c == '-'

    fun esCaracterCodigo(c: Char): Boolean =
        c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '-' || c == '_'

    fun esCaracterFolio(c: Char): Boolean =
        c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '-' || c == '_' || c == '/' || c == ' '

    fun esCaracterDireccionNumero(c: Char): Boolean =
        c.isLetterOrDigit() || c == '-' || c == '/' || c == '#' || c == ' '

    fun esCaracterBanco(c: Char): Boolean =
        c.isLetterOrDigit() || c == ' ' || c == '.' || c == '-'

    fun esCaracterSinEspacios(c: Char): Boolean = !c.isWhitespace() && c.code >= 0x20

    fun esCaracterTextoSeguro(c: Char, allowNewLines: Boolean = false): Boolean {
        if (c == '<' || c == '>') return false
        return if (allowNewLines) (c == '\n' || c.code >= 0x20) else c.code >= 0x20
    }

    fun esMontoDecimalParcialValido(
        resultado: String,
        maxEnteros: Int,
        maxDecimales: Int
    ): Boolean {
        if (resultado.isEmpty()) return true
        val maxEnt = maxEnteros.coerceAtLeast(1)
        val maxDec = maxDecimales.coerceAtLeast(0)
        val patron = if (maxDec == 0) {
            Regex("^\\d{0,$maxEnt}$")
        } else {
            Regex("^\\d{0,$maxEnt}(\\.\\d{0,$maxDec})?$")
        }
        return patron.matches(resultado)
    }
}

/**
 * Filtros Android reutilizables (`InputFilter`) para restringir tipo de carácter y longitud
 * al escribir o pegar en cualquier `EditText`.
 */
object InputFilters {

    fun digitsOnly(maxLength: Int): Array<InputFilter> =
        crearFiltrosPorCaracter(maxLength) { InputValidationRules.esDigitoValido(it) }

    fun personName(maxLength: Int): Array<InputFilter> =
        crearFiltrosPorCaracter(maxLength) { InputValidationRules.esCaracterNombrePersona(it) }

    fun username(maxLength: Int): Array<InputFilter> =
        crearFiltrosPorCaracter(maxLength) { InputValidationRules.esCaracterUsuario(it) }

    fun codeAlphanumeric(maxLength: Int, uppercase: Boolean = true): Array<InputFilter> {
        val base = crearFiltrosPorCaracter(maxLength) { InputValidationRules.esCaracterCodigo(it) }
        return if (uppercase) arrayOf(*base, InputFilter.AllCaps()) else base
    }

    fun voucherFolio(maxLength: Int): Array<InputFilter> =
        crearFiltrosPorCaracter(maxLength) { InputValidationRules.esCaracterFolio(it) }

    fun addressNumber(maxLength: Int = 20): Array<InputFilter> =
        crearFiltrosPorCaracter(maxLength) { InputValidationRules.esCaracterDireccionNumero(it) }

    fun bankName(maxLength: Int): Array<InputFilter> =
        crearFiltrosPorCaracter(maxLength) { InputValidationRules.esCaracterBanco(it) }

    fun noWhitespace(maxLength: Int): Array<InputFilter> =
        crearFiltrosPorCaracter(maxLength) { InputValidationRules.esCaracterSinEspacios(it) }

    fun plainText(maxLength: Int, allowNewLines: Boolean = false): Array<InputFilter> =
        crearFiltrosPorCaracter(maxLength) {
            InputValidationRules.esCaracterTextoSeguro(it, allowNewLines)
        }

    fun decimalAmount(maxIntegers: Int, maxDecimals: Int = 2): Array<InputFilter> {
        val totalLen = maxIntegers.coerceAtLeast(1) + if (maxDecimals > 0) 1 + maxDecimals else 0
        val decimalFilter = InputFilter { source, start, end, dest, dstart, dend ->
            val fragmento = source.subSequence(start, end)
            val filtrado = StringBuilder()
            for (i in fragmento.indices) {
                val c = fragmento[i]
                if (InputValidationRules.esDigitoValido(c) || (maxDecimals > 0 && c == '.')) {
                    filtrado.append(c)
                }
            }
            val prefijo = dest?.subSequence(0, dstart)?.toString().orEmpty()
            val sufijo = dest?.subSequence(dend, dest.length)?.toString().orEmpty()
            val candidato = prefijo + filtrado + sufijo
            if (!InputValidationRules.esMontoDecimalParcialValido(candidato, maxIntegers, maxDecimals)) {
                ""
            } else if (filtrado.length != fragmento.length) {
                filtrado.toString()
            } else {
                null
            }
        }
        return arrayOf(decimalFilter, InputFilter.LengthFilter(totalLen))
    }

    private fun crearFiltrosPorCaracter(
        maxLength: Int,
        permitido: (Char) -> Boolean
    ): Array<InputFilter> {
        val filtroCaracter = InputFilter { source, start, end, _, _, _ ->
            var modificada = false
            val sb = SpannableStringBuilder()
            for (i in start until end) {
                val c = source[i]
                if (permitido(c)) {
                    sb.append(c)
                } else {
                    modificada = true
                }
            }
            if (!modificada) {
                null
            } else if (source is Spanned && sb.isNotEmpty()) {
                sb
            } else {
                sb.toString()
            }
        }
        return arrayOf(filtroCaracter, InputFilter.LengthFilter(maxLength))
    }
}
