package com.sprm.apexgym.feature.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Base64
import android.util.Patterns
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.core.network.mensajeUsuario
import com.sprm.apexgym.core.network.api.CrearVentaRequest
import com.sprm.apexgym.core.network.api.DetalleVentaRequest
import com.sprm.apexgym.core.network.api.PagoVentaRequest
import com.sprm.apexgym.core.network.api.TransaccionPagoDto
import com.sprm.apexgym.core.network.api.VentaDto
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.feature.tienda.TicketCompraFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

class CheckoutFragment : Fragment() {

    private enum class PaymentMethodType(val titulo: String) {
        MERCADO_PAGO("Mercado Pago / Pago en línea"),
        TARJETA_DEBITO("Tarjeta bancaria (Débito o Crédito)"),
        TRANSFERENCIA("Transferencia bancaria"),
        EFECTIVO("Pago en recepción / Efectivo")
    }

    private var metodoSeleccionado: PaymentMethodType = PaymentMethodType.MERCADO_PAGO

    private var tipoTarjetaSeleccionada: String = "DEBITO"
    private var numeroTarjetaRaw: String = ""
    private var nombreTitular: String = ""
    private var fechaVencimiento: String = ""
    private var cvvTarjeta: String = ""

    private var bancoEmisorSpei: String = ""
    private var referenciaSpei: String = ""

    private var correoPaypal: String = ""

    private val folioGenerado: String by lazy {
        val randomNum = (100000..999999).random()
        "APEX-$randomNum"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_checkout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        val items = CartManager.obtenerItems(context)
        if (items.isEmpty()) {
            parentFragmentManager.popBackStack()
            return
        }

        val prefs = context.getSharedPreferences("sesion_apexgym", Context.MODE_PRIVATE)
        if (correoPaypal.isBlank()) {
            correoPaypal = prefs.getString("usuario_email", "").orEmpty()
        }

        val total = CartManager.obtenerTotalPrecio(context)

        val flBack = view.findViewById<View>(R.id.flBack)
        val rvPaymentMethods = view.findViewById<LinearLayout>(R.id.rvPaymentMethods)
        val rvOrderSummary = view.findViewById<LinearLayout>(R.id.rvOrderSummary)
        val tvTotalPagar = view.findViewById<TextView>(R.id.tvTotalPagar)
        val btnContinuar = view.findViewById<MaterialButton>(R.id.btnContinuar)
        val rootContainer = view.findViewWithTag<ViewGroup>("checkout_root_container")

        flBack?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        renderizarMetodosPago(rvPaymentMethods, rootContainer, total)
        renderizarResumenPedido(rvOrderSummary, tvTotalPagar, items, total)

        btnContinuar?.setOnClickListener {
            validarYRegistrarPago()
        }
    }

    private fun renderizarMetodosPago(
        container: LinearLayout?,
        rootContainer: ViewGroup?,
        totalPagar: Double
    ) {
        if (container == null) return
        val context = requireContext()
        container.removeAllViews()

        PaymentMethodType.entries.forEach { metodo ->
            val seleccionado = metodo == metodoSeleccionado

            val card = MaterialCardView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dpToPx(12)
                }
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                radius = dpToPx(16).toFloat()
                cardElevation = 0f
                strokeWidth = dpToPx(if (seleccionado) 2f else 1f)
                strokeColor = ContextCompat.getColor(
                    context,
                    if (seleccionado) R.color.apex_orange else R.color.apex_stroke_light
                )
            }

            val cardLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }

            val headerRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    if (metodoSeleccionado != metodo) {
                        metodoSeleccionado = metodo
                        rootContainer?.let {
                            TransitionManager.beginDelayedTransition(
                                it,
                                AutoTransition().apply { duration = 180L }
                            )
                        }
                        renderizarMetodosPago(container, rootContainer, totalPagar)
                        val rvOrderSummary = view?.findViewById<LinearLayout>(R.id.rvOrderSummary)
                        val tvTotalPagar = view?.findViewById<TextView>(R.id.tvTotalPagar)
                        renderizarResumenPedido(
                            rvOrderSummary,
                            tvTotalPagar,
                            CartManager.obtenerItems(context),
                            totalPagar
                        )
                    }
                }
            }

            val radioOuter = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(22), dpToPx(22))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ContextCompat.getColor(context, R.color.white))
                    setStroke(
                        dpToPx(if (seleccionado) 2f else 1.5f),
                        ContextCompat.getColor(
                            context,
                            if (seleccionado) R.color.apex_orange else R.color.apex_stroke_light
                        )
                    )
                }
                if (seleccionado) {
                    val innerDot = View(context).apply {
                        layoutParams = FrameLayout.LayoutParams(dpToPx(12), dpToPx(12), Gravity.CENTER)
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(ContextCompat.getColor(context, R.color.apex_orange))
                        }
                    }
                    addView(innerDot)
                }
            }
            headerRow.addView(radioOuter)

            val tvMetodoTitulo = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    marginStart = dpToPx(12)
                }
                text = metodo.titulo
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                setTypeface(typeface, Typeface.BOLD)
            }
            headerRow.addView(tvMetodoTitulo)

            cardLayout.addView(headerRow)

            if (seleccionado) {
                val divider = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dpToPx(1)
                    ).apply {
                        marginStart = dpToPx(16)
                        marginEnd = dpToPx(16)
                    }
                    setBackgroundColor(ContextCompat.getColor(context, R.color.apex_stroke_light))
                }
                cardLayout.addView(divider)

                val detailView = when (metodo) {
                    PaymentMethodType.MERCADO_PAGO -> construirFormularioMercadoPago(totalPagar)
                    PaymentMethodType.TRANSFERENCIA -> construirFormularioTransferencia(totalPagar)
                    PaymentMethodType.TARJETA_DEBITO -> construirFormularioTarjetaDebito()
                    PaymentMethodType.EFECTIVO -> construirFormularioEfectivo(totalPagar)
                }
                cardLayout.addView(detailView)
            }

            card.addView(cardLayout)
            container.addView(card)
        }
    }

    private fun construirFormularioTarjetaDebito(): View {
        val context = requireContext()
        val form = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(16))
        }

        val rowTipoSelector = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(12) }
        }

        val btnDebito = TextView(context).apply {
            text = "Tarjeta de Débito"
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dpToPx(6)
            }
        }

        val btnCredito = TextView(context).apply {
            text = "Tarjeta de Crédito"
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dpToPx(6)
            }
        }

        val previewCard = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(14)
            }
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.apex_card_dark))
            radius = dpToPx(14).toFloat()
            cardElevation = 0f
        }

        val previewContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
        }

        val tvPreviewTipo = TextView(context).apply {
            text = if (tipoTarjetaSeleccionada == "CREDITO") "TARJETA DE CRÉDITO" else "TARJETA DE DÉBITO"
            setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            setTypeface(typeface, Typeface.BOLD)
        }
        previewContent.addView(tvPreviewTipo)

        fun actualizarBotonesTipo() {
            val transparent = ContextCompat.getColor(context, android.R.color.transparent)
            if (tipoTarjetaSeleccionada == "DEBITO") {
                btnDebito.background = crearFondoRectangular(ContextCompat.getColor(context, R.color.apex_orange), transparent, 0f, 10f)
                btnDebito.setTextColor(ContextCompat.getColor(context, R.color.white))
                btnCredito.background = crearFondoRectangular(ContextCompat.getColor(context, R.color.apex_bg_screen), ContextCompat.getColor(context, R.color.apex_stroke_light), 1f, 10f)
                btnCredito.setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                tvPreviewTipo.text = "TARJETA DE DÉBITO"
            } else {
                btnCredito.background = crearFondoRectangular(ContextCompat.getColor(context, R.color.apex_orange), transparent, 0f, 10f)
                btnCredito.setTextColor(ContextCompat.getColor(context, R.color.white))
                btnDebito.background = crearFondoRectangular(ContextCompat.getColor(context, R.color.apex_bg_screen), ContextCompat.getColor(context, R.color.apex_stroke_light), 1f, 10f)
                btnDebito.setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                tvPreviewTipo.text = "TARJETA DE CRÉDITO"
            }
        }

        btnDebito.setOnClickListener {
            tipoTarjetaSeleccionada = "DEBITO"
            actualizarBotonesTipo()
        }
        btnCredito.setOnClickListener {
            tipoTarjetaSeleccionada = "CREDITO"
            actualizarBotonesTipo()
        }

        actualizarBotonesTipo()
        rowTipoSelector.addView(btnDebito)
        rowTipoSelector.addView(btnCredito)
        form.addView(rowTipoSelector)

        val tvPreviewNumero = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(10)
                bottomMargin = dpToPx(10)
            }
            text = formatearNumeroTarjetaPreview(numeroTarjetaRaw)
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
        }
        previewContent.addView(tvPreviewNumero)

        val previewBottomRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvPreviewTitular = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            text = if (nombreTitular.isBlank()) "NOMBRE DEL TITULAR" else nombreTitular.uppercase()
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_on_dark_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        previewBottomRow.addView(tvPreviewTitular)

        val tvPreviewVenc = TextView(context).apply {
            text = if (fechaVencimiento.isBlank()) "MM/AA" else fechaVencimiento
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
        }
        previewBottomRow.addView(tvPreviewVenc)

        previewContent.addView(previewBottomRow)
        previewCard.addView(previewContent)
        form.addView(previewCard)

        form.addView(crearEtiquetaCampo("Número de tarjeta (16 dígitos)"))
        val etNumero = crearCampoTexto(
            hint = "0000 0000 0000 0000",
            valorInicial = formatearDigitosTarjeta(numeroTarjetaRaw),
            inputType = InputType.TYPE_CLASS_NUMBER,
            maxLength = 19
        )
        etNumero.addTextChangedListener(object : TextWatcher {
            private var editando = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (editando) return
                editando = true
                val limpio = s?.toString()?.filter { it.isDigit() }?.take(16).orEmpty()
                numeroTarjetaRaw = limpio
                val formateado = formatearDigitosTarjeta(limpio)
                if (formateado != s?.toString()) {
                    etNumero.setText(formateado)
                    etNumero.setSelection(formateado.length)
                }
                tvPreviewNumero.text = formatearNumeroTarjetaPreview(limpio)
                editando = false
            }
        })
        form.addView(etNumero)

        form.addView(crearEtiquetaCampo("Nombre y apellido del titular"))
        val etTitular = crearCampoTexto(
            hint = "Como aparece en la tarjeta",
            valorInicial = nombreTitular,
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS,
            maxLength = 60
        )
        etTitular.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                nombreTitular = s?.toString()?.trim().orEmpty()
                tvPreviewTitular.text =
                    if (nombreTitular.isBlank()) "NOMBRE DEL TITULAR" else nombreTitular.uppercase()
            }
        })
        form.addView(etTitular)

        val rowVencCvv = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val colVenc = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dpToPx(6)
            }
        }
        colVenc.addView(crearEtiquetaCampo("Vencimiento (MM/AA)"))
        val etVenc = crearCampoTexto(
            hint = "MM/AA",
            valorInicial = fechaVencimiento,
            inputType = InputType.TYPE_CLASS_NUMBER,
            maxLength = 5
        )
        etVenc.addTextChangedListener(object : TextWatcher {
            private var editando = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (editando) return
                editando = true
                val digitos = s?.toString()?.filter { it.isDigit() }?.take(4).orEmpty()
                val formateado = if (digitos.length >= 3) {
                    "${digitos.substring(0, 2)}/${digitos.substring(2)}"
                } else {
                    digitos
                }
                fechaVencimiento = formateado
                if (formateado != s?.toString()) {
                    etVenc.setText(formateado)
                    etVenc.setSelection(formateado.length)
                }
                tvPreviewVenc.text = if (formateado.isBlank()) "MM/AA" else formateado
                editando = false
            }
        })
        colVenc.addView(etVenc)
        rowVencCvv.addView(colVenc)

        val colCvv = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dpToPx(6)
            }
        }
        colCvv.addView(crearEtiquetaCampo("Código CVV"))
        val etCvv = crearCampoTexto(
            hint = "123",
            valorInicial = cvvTarjeta,
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD,
            maxLength = 4
        )
        etCvv.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                cvvTarjeta = s?.toString()?.filter { it.isDigit() }.orEmpty()
            }
        })
        colCvv.addView(etCvv)
        rowVencCvv.addView(colCvv)

        form.addView(rowVencCvv)

        return form
    }

    private fun construirFormularioTransferencia(totalPagar: Double): View {
        val context = requireContext()
        val form = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(16))
        }

        val infoBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = crearFondoRectangular(
                colorFondo = ContextCompat.getColor(context, R.color.apex_bg_screen),
                colorBorde = ContextCompat.getColor(context, R.color.apex_stroke_light),
                anchoBordeDp = 1f,
                radioDp = 12f
            )
            setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(14)
            }
        }

        val tvInstruccion = TextView(context).apply {
            text = "Datos bancarios para transferencia"
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setTypeface(typeface, Typeface.BOLD)
        }
        infoBox.addView(tvInstruccion)

        infoBox.addView(crearFilaDatoCopiable("Banco destino", "BBVA México", null))
        infoBox.addView(crearFilaDatoCopiable("Beneficiario", "ApexGym S.A. de C.V.", null))
        infoBox.addView(
            crearFilaDatoCopiable(
                "CLABE Interbancaria",
                "0121 8000 1234 5678 90",
                "012180001234567890"
            )
        )
        infoBox.addView(
            crearFilaDatoCopiable(
                "Concepto / Referencia",
                folioGenerado,
                folioGenerado
            )
        )
        infoBox.addView(
            crearFilaDatoCopiable(
                "Total a pagar",
                "${formatearPrecio(totalPagar)} MXN",
                null
            )
        )

        form.addView(infoBox)

        form.addView(crearEtiquetaCampo("Banco desde el que realizas la transferencia"))
        val etBanco = crearCampoTexto(
            hint = "Ej. BBVA, Banorte, Santander, Nu",
            valorInicial = bancoEmisorSpei,
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS,
            maxLength = 40
        )
        etBanco.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                bancoEmisorSpei = s?.toString()?.trim().orEmpty()
            }
        })
        form.addView(etBanco)

        form.addView(crearEtiquetaCampo("Clave de rastreo o folio de comprobante"))
        val etReferencia = crearCampoTexto(
            hint = "Ingresa los dígitos de referencia de tu pago",
            valorInicial = referenciaSpei,
            inputType = InputType.TYPE_CLASS_TEXT,
            maxLength = 35
        )
        etReferencia.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                referenciaSpei = s?.toString()?.trim().orEmpty()
            }
        })
        form.addView(etReferencia)

        return form
    }

    private fun construirFormularioMercadoPago(totalPagar: Double): View {
        val context = requireContext()
        val form = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(16))
        }

        val infoBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = crearFondoRectangular(
                colorFondo = ContextCompat.getColor(context, R.color.apex_bg_screen),
                colorBorde = ContextCompat.getColor(context, R.color.apex_stroke_light),
                anchoBordeDp = 1f,
                radioDp = 12f
            )
            setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(14)
            }
        }

        infoBox.addView(crearFilaDatoCopiable("Comercio", "ApexGym S.A. de C.V.", null))
        infoBox.addView(crearFilaDatoCopiable("Pasarela", "Mercado Pago Checkout (Sandbox)", null))
        infoBox.addView(crearFilaDatoCopiable("Total a pagar", "${formatearPrecio(totalPagar)} MXN", null))
        form.addView(infoBox)

        val tvExplicacion = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(12) }
            text = "Al presionar 'Pagar y finalizar' se abrirá el checkout de Mercado Pago para procesar tu pago con tarjeta o saldo de cuenta."
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
        }
        form.addView(tvExplicacion)

        form.addView(crearEtiquetaCampo("Correo para tu comprobante digital"))
        val etCorreoPaypal = crearCampoTexto(
            hint = "ejemplo@correo.com",
            valorInicial = correoPaypal,
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            maxLength = 80
        )
        etCorreoPaypal.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                correoPaypal = s?.toString()?.trim().orEmpty()
            }
        })
        form.addView(etCorreoPaypal)

        return form
    }

    private fun construirFormularioEfectivo(totalPagar: Double): View {
        val context = requireContext()
        val form = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(16))
        }

        val infoBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = crearFondoRectangular(
                colorFondo = ContextCompat.getColor(context, R.color.apex_bg_screen),
                colorBorde = ContextCompat.getColor(context, R.color.apex_stroke_light),
                anchoBordeDp = 1f,
                radioDp = 12f
            )
            setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val tvInfo = TextView(context).apply {
            text = "Al presionar 'Pagar y finalizar' se registrará tu orden oficial con su folio para liquidar y recoger en el mostrador."
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setPadding(0, 0, 0, dpToPx(10))
        }
        infoBox.addView(tvInfo)
        infoBox.addView(crearFilaDatoCopiable("Punto de cobro", "Recepción Sucursal ApexGym", null))
        infoBox.addView(crearFilaDatoCopiable("Vigencia del pedido", "24 horas para liquidar en caja", null))
        infoBox.addView(crearFilaDatoCopiable("Total a pagar", "${formatearPrecio(totalPagar)} MXN", null))

        form.addView(infoBox)
        return form
    }

    private fun renderizarResumenPedido(
        container: LinearLayout?,
        tvTotalPagar: TextView?,
        items: List<CartItem>,
        total: Double
    ) {
        if (container == null) return
        val context = requireContext()
        container.removeAllViews()

        items.forEach { item ->
            val esMembresia = item.tipo == CartItemType.MEMBRESIA
            val itemRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dpToPx(10)
                }
            }

            val mediaBox = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(42), dpToPx(42))
                setBackgroundResource(R.drawable.bg_chip_unselected)
                clipToOutline = true

                val icon = ImageView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(dpToPx(18), dpToPx(18), Gravity.CENTER)
                    setImageResource(
                        if (esMembresia) R.drawable.ic_membership_card
                        else R.drawable.ic_supplement_bottle
                    )
                    ImageViewCompat.setImageTintList(
                        this,
                        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.apex_orange))
                    )
                }
                addView(icon)
            }
            if (!esMembresia && !item.imagenUrl.isNullOrBlank()) {
                cargarImagenResumen(item.imagenUrl, mediaBox)
            }
            itemRow.addView(mediaBox)

            val infoCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dpToPx(10)
                    marginEnd = dpToPx(8)
                }
            }

            val tvNombre = TextView(context).apply {
                text = item.nombre
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
            infoCol.addView(tvNombre)

            val tvMeta = TextView(context).apply {
                text = if (esMembresia) {
                    "Membresía • ${item.subtitulo}"
                } else {
                    "Cantidad: ${item.cantidad} × ${formatearPrecio(item.precioUnitario)} MXN"
                }
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            }
            infoCol.addView(tvMeta)
            itemRow.addView(infoCol)

            val tvSubtotal = TextView(context).apply {
                text = "${formatearPrecio(item.subtotal)} MXN"
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                setTypeface(typeface, Typeface.BOLD)
            }
            itemRow.addView(tvSubtotal)

            container.addView(itemRow)
        }

        val sep = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            ).apply {
                topMargin = dpToPx(4)
                bottomMargin = dpToPx(10)
            }
            setBackgroundColor(ContextCompat.getColor(context, R.color.apex_stroke_light))
        }
        container.addView(sep)

        val subtotalProductos = CartManager.obtenerSubtotalProductos(context)
        val membresia = CartManager.obtenerMembresiaActual(context)

        if (subtotalProductos > 0) {
            container.addView(
                crearFilaResumen("Subtotal de productos", "${formatearPrecio(subtotalProductos)} MXN")
            )
        }
        if (membresia != null) {
            container.addView(
                crearFilaResumen("Plan de membresía (${membresia.nombre})", "${formatearPrecio(membresia.subtotal)} MXN")
            )
        }
        container.addView(
            crearFilaResumen("Forma de pago", metodoSeleccionado.titulo)
        )

        tvTotalPagar?.text = "${formatearPrecio(total)} MXN"
    }

    private fun validarYRegistrarPago() {
        val context = requireContext()
        val items = CartManager.obtenerItems(context)
        if (items.isEmpty()) {
            Toast.makeText(context, "El carrito está vacío", Toast.LENGTH_SHORT).show()
            return
        }

        val bancoLimpio = bancoEmisorSpei.trim()
        val referenciaLimpia = referenciaSpei.trim()
        val numTarjetaLimpio = numeroTarjetaRaw.filter { it.isDigit() }
        val titularLimpio = nombreTitular.trim()
        val vencLimpio = fechaVencimiento.trim()
        val cvvLimpio = cvvTarjeta.filter { it.isDigit() }
        val correoLimpio = correoPaypal.trim()

        when (metodoSeleccionado) {
            PaymentMethodType.TRANSFERENCIA -> {
                if (bancoLimpio.isBlank()) {
                    Toast.makeText(context, "Indica el banco desde el que realizas la transferencia", Toast.LENGTH_SHORT).show()
                    return
                }
                if (bancoLimpio.length !in 3..50 || !Regex("^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ\\s\\.\\-]+$").matches(bancoLimpio)) {
                    Toast.makeText(context, "El nombre del banco contiene caracteres no permitidos", Toast.LENGTH_SHORT).show()
                    return
                }
                if (referenciaLimpia.isBlank()) {
                    Toast.makeText(context, "Ingresa la clave de rastreo o referencia de transferencia", Toast.LENGTH_SHORT).show()
                    return
                }
                if (referenciaLimpia.length !in 4..40 || !Regex("^[a-zA-Z0-9\\-\\s]+$").matches(referenciaLimpia)) {
                    Toast.makeText(context, "La clave de rastreo solo permite números, letras y guiones", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            PaymentMethodType.TARJETA_DEBITO -> {
                if (numTarjetaLimpio.length != 16) {
                    Toast.makeText(context, "Ingresa los 16 dígitos de tu tarjeta", Toast.LENGTH_SHORT).show()
                    return
                }
                if (titularLimpio.isBlank()) {
                    Toast.makeText(context, "Ingresa el nombre del titular de la tarjeta", Toast.LENGTH_SHORT).show()
                    return
                }
                if (titularLimpio.length !in 3..60 || !Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s\\.]+$").matches(titularLimpio)) {
                    Toast.makeText(context, "El nombre del titular solo puede contener letras y espacios", Toast.LENGTH_SHORT).show()
                    return
                }
                if (!Regex("^(0[1-9]|1[0-2])\\/([0-9]{2})$").matches(vencLimpio)) {
                    Toast.makeText(context, "Ingresa una fecha de vencimiento válida (MM/AA)", Toast.LENGTH_SHORT).show()
                    return
                }
                if (!Regex("^[0-9]{3,4}$").matches(cvvLimpio)) {
                    Toast.makeText(context, "Ingresa el código de seguridad (CVV) de 3 o 4 dígitos", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            PaymentMethodType.MERCADO_PAGO -> {
                if (correoLimpio.isNotBlank()) {
                    if (correoLimpio.contains(" ") || correoLimpio.length > 100 || !Patterns.EMAIL_ADDRESS.matcher(correoLimpio).matches() || correoLimpio.any { it in "<>'\"/\\;`" }) {
                        Toast.makeText(context, "Ingresa un correo electrónico válido", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
            }
            PaymentMethodType.EFECTIVO -> {
            }
        }

        val btnContinuar = view?.findViewById<MaterialButton>(R.id.btnContinuar)
        btnContinuar?.isEnabled = false
        btnContinuar?.text = "Procesando pedido…"

        val total = CartManager.obtenerTotalPrecio(context)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val metodos = try {
                    RetrofitClient.empleadoService.obtenerMetodosPago().dataExitosa().orEmpty()
                } catch (_: Exception) {
                    emptyList()
                }

                val metodoId = when (metodoSeleccionado) {
                    PaymentMethodType.MERCADO_PAGO -> metodos.firstOrNull { it.codigo.equals("MERCADO_PAGO", ignoreCase = true) || it.nombre.orEmpty().contains("Mercado", ignoreCase = true) }?.id ?: 4
                    PaymentMethodType.TARJETA_DEBITO -> metodos.firstOrNull { it.codigo.equals("TARJETA", ignoreCase = true) || it.nombre.orEmpty().contains("Tarjeta", ignoreCase = true) }?.id ?: 1
                    PaymentMethodType.TRANSFERENCIA -> metodos.firstOrNull { it.codigo.equals("TRANSFERENCIA", ignoreCase = true) || it.nombre.orEmpty().contains("Transferencia", ignoreCase = true) }?.id ?: 3
                    PaymentMethodType.EFECTIVO -> metodos.firstOrNull { it.codigo.equals("EFECTIVO", ignoreCase = true) || it.nombre.orEmpty().contains("Efectivo", ignoreCase = true) }?.id ?: 2
                }

                val detalle = items.map { item ->
                    DetalleVentaRequest(
                        tipoItem = item.tipo.name,
                        cantidad = item.cantidad,
                        productoId = if (item.tipo == CartItemType.PRODUCTO) item.id else null,
                        servicioId = if (item.tipo == CartItemType.SERVICIO) item.id else null,
                        tipoMembresiaId = if (item.tipo == CartItemType.MEMBRESIA) item.id else null,
                        descripcion = item.nombre
                    )
                }

                val uuidIdempotencia = UUID.randomUUID().toString()
                val pagoReq = when (metodoSeleccionado) {
                    PaymentMethodType.MERCADO_PAGO -> PagoVentaRequest(
                        correoCliente = correoLimpio.takeIf { it.isNotBlank() } ?: SessionManager.obtenerEmail(context)
                    )
                    PaymentMethodType.TARJETA_DEBITO -> PagoVentaRequest(
                        tipoTarjeta = tipoTarjetaSeleccionada,
                        marcaTarjeta = when {
                            numTarjetaLimpio.startsWith("4") -> "VISA"
                            numTarjetaLimpio.startsWith("5") || numTarjetaLimpio.startsWith("2") -> "MASTERCARD"
                            numTarjetaLimpio.startsWith("34") || numTarjetaLimpio.startsWith("37") -> "AMEX"
                            else -> "OTRA"
                        },
                        ultimosDigitos = numTarjetaLimpio.takeLast(4),
                        folioVoucher = "CARD-${numTarjetaLimpio.takeLast(4)}-${(1000..9999).random()}"
                    )
                    PaymentMethodType.TRANSFERENCIA -> PagoVentaRequest(
                        referencia = "$bancoLimpio - $referenciaLimpia",
                        folioVoucher = referenciaLimpia
                    )
                    PaymentMethodType.EFECTIVO -> null
                }

                val ventaReq = CrearVentaRequest(
                    metodoPagoId = metodoId,
                    subtotal = total,
                    total = total,
                    montoRecibido = total,
                    detalleVenta = detalle,
                    uuidIdempotencia = uuidIdempotencia,
                    pago = pagoReq
                )

                val response = RetrofitClient.empleadoService.crearVenta(ventaReq, idempotencyKey = uuidIdempotencia)
                val ventaCreada = response.dataExitosa()

                if (!isAdded) return@launch

                if (ventaCreada == null) {
                    Toast.makeText(
                        context,
                        response.mensajeDeError() ?: response.body()?.message ?: "No se pudo registrar la venta.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                if (metodoSeleccionado == PaymentMethodType.MERCADO_PAGO) {
                    val urlCheckout = ventaCreada.pagoDigital?.urlCheckout
                    if (!urlCheckout.isNullOrBlank()) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlCheckout))
                        startActivity(intent)

                        val dialog = AlertDialog.Builder(context)
                            .setTitle("Pago con Mercado Pago")
                            .setMessage(
                                "Se abrió el portal seguro de Mercado Pago en tu navegador.\n\n" +
                                    "Una vez que hayas completado el pago, presiona 'Comprobar pago' para confirmar tu compra."
                            )
                            .setCancelable(false)
                            .setPositiveButton("Comprobar pago", null)
                            .setNegativeButton("Cerrar") { d, _ -> d.dismiss() }
                            .create()

                        dialog.setOnShowListener {
                            val btnCheck = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            btnCheck.setOnClickListener {
                                btnCheck.isEnabled = false
                                btnCheck.text = "Verificando…"
                                viewLifecycleOwner.lifecycleScope.launch {
                                    val statusRes = try {
                                        RetrofitClient.empleadoService.consultarEstadoPagoVenta(ventaCreada.id).dataExitosa()
                                    } catch (_: Exception) {
                                        null
                                    }
                                    val estadoVenta = statusRes?.venta?.estado?.lowercase()
                                    val estadoPago = statusRes?.pago?.estado?.uppercase()

                                    if (estadoVenta == "completada" || estadoPago == "APROBADO") {
                                        dialog.dismiss()
                                        CartManager.vaciarCarrito(context)
                                        Toast.makeText(context, "¡Pago acreditado exitosamente!", Toast.LENGTH_LONG).show()
                                        abrirTicketVenta(ventaCreada, items)
                                    } else {
                                        btnCheck.isEnabled = true
                                        btnCheck.text = "Comprobar pago"
                                        Toast.makeText(
                                            context,
                                            "Tu pago aún está pendiente en Mercado Pago. Si ya lo realizaste, aguarda un momento.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                        dialog.show()
                        return@launch
                    }
                }

                // Efectivo, transferencia o tarjeta registrada
                CartManager.vaciarCarrito(context)
                Toast.makeText(context, "¡Pedido registrado exitosamente!", Toast.LENGTH_SHORT).show()
                abrirTicketVenta(ventaCreada, items)

            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(context, e.mensajeUsuario(), Toast.LENGTH_LONG).show()
                }
            } finally {
                if (isAdded) {
                    btnContinuar?.isEnabled = true
                    btnContinuar?.text = "Pagar y finalizar"
                }
            }
        }
    }

    private fun abrirTicketVenta(venta: VentaDto, items: List<CartItem>) {
        val transaccion = TransaccionPagoDto(
            id = venta.id,
            monto = venta.total,
            estado = venta.estado?.uppercase() ?: "APROBADO",
            concepto = "Compra en línea #${venta.id}",
            folioVoucher = venta.folioVenta?.id?.let { "APX-$it" } ?: "APX-${venta.id}",
            createdAt = venta.createdAt
        )
        val itemsJson = JSONArray().apply {
            items.forEach { item ->
                put(JSONObject().apply {
                    put("nombre", item.nombre)
                    put("cantidad", item.cantidad)
                    put("subtotal", item.subtotal)
                })
            }
        }.toString()

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(
                R.id.fragment_container,
                TicketCompraFragment.newInstance(
                    transaccion = transaccion,
                    itemsJson = itemsJson,
                    metodoPago = metodoSeleccionado.titulo
                )
            )
            .addToBackStack(null)
            .commit()
    }

    private fun crearEtiquetaCampo(texto: String): TextView {
        val context = requireContext()
        return TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(5)
            }
            text = texto
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
        }
    }

    private fun crearCampoTexto(
        hint: String,
        valorInicial: String,
        inputType: Int,
        maxLength: Int
    ): EditText {
        val context = requireContext()
        return EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(46)
            ).apply {
                bottomMargin = dpToPx(12)
            }
            background = crearFondoRectangular(
                colorFondo = ContextCompat.getColor(context, R.color.apex_bg_screen),
                colorBorde = ContextCompat.getColor(context, R.color.apex_stroke_light),
                anchoBordeDp = 1f,
                radioDp = 10f
            )
            setPadding(dpToPx(14), 0, dpToPx(14), 0)
            this.hint = hint
            setText(valorInicial)
            setHintTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            this.inputType = inputType
            filters = arrayOf(InputFilter.LengthFilter(maxLength))
            maxLines = 1
        }
    }

    private fun crearFondoRectangular(
        colorFondo: Int,
        colorBorde: Int,
        anchoBordeDp: Float,
        radioDp: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(radioDp).toFloat()
            setColor(colorFondo)
            setStroke(dpToPx(anchoBordeDp), colorBorde)
        }
    }

    private fun crearFilaDatoCopiable(
        etiqueta: String,
        valor: String,
        valorParaCopiar: String?
    ): View {
        val context = requireContext()
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(6)
            }
        }

        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvLabel = TextView(context).apply {
            text = etiqueta
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        col.addView(tvLabel)

        val tvVal = TextView(context).apply {
            text = valor
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
        }
        col.addView(tvVal)
        row.addView(col)

        if (!valorParaCopiar.isNullOrBlank()) {
            val btnCopiar = TextView(context).apply {
                background = crearFondoRectangular(
                    colorFondo = ContextCompat.getColor(context, R.color.white),
                    colorBorde = ContextCompat.getColor(context, R.color.apex_stroke_light),
                    anchoBordeDp = 1f,
                    radioDp = 8f
                )
                setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
                text = "Copiar"
                setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTypeface(typeface, Typeface.BOLD)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(etiqueta, valorParaCopiar))
                    Toast.makeText(context, "$etiqueta copiada", Toast.LENGTH_SHORT).show()
                }
            }
            row.addView(btnCopiar)
        }

        return row
    }

    private fun crearFilaResumen(etiqueta: String, valor: String): View {
        val context = requireContext()
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(6)
            }

            val tvIzq = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = etiqueta
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            }
            addView(tvIzq)

            val tvDer = TextView(context).apply {
                text = valor
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setTypeface(typeface, Typeface.BOLD)
            }
            addView(tvDer)
        }
    }

    private fun formatearDigitosTarjeta(digitos: String): String {
        return digitos.chunked(4).joinToString(" ")
    }

    private fun formatearNumeroTarjetaPreview(digitos: String): String {
        val rellenado = digitos.padEnd(16, '•')
        return rellenado.chunked(4).joinToString(" ")
    }

    private fun cargarImagenResumen(rutaImagen: String, contenedor: FrameLayout) {
        val urlCompleta = RetrofitClient.resolverUrlImagen(rutaImagen) ?: return

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val bitmap = try {
                if (urlCompleta.startsWith("data:image")) {
                    val base64Part = urlCompleta.substringAfter("base64,", "")
                    if (base64Part.isNotEmpty()) {
                        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } else null
                } else {
                    URL(urlCompleta).openStream().use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                }
            } catch (_: Exception) {
                null
            }

            if (bitmap != null) {
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        contenedor.removeAllViews()
                        val imageView = ImageView(requireContext()).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            setImageBitmap(bitmap)
                        }
                        contenedor.addView(imageView)
                    }
                }
            }
        }
    }

    private fun formatearPrecio(monto: Double): String {
        val formato = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = if (monto % 1.0 == 0.0) 0 else 2
            maximumFractionDigits = 2
        }
        return "$${formato.format(monto)}"
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }
}
