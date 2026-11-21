package com.sprm.apexgym.feature.recepcion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.api.ValidarAccesoDto
import com.sprm.apexgym.core.network.api.ValidarAccesoRequest
import com.sprm.apexgym.core.network.api.ValidarQrRequest
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.databinding.FragmentQrScannerBinding
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QrScannerFragment : Fragment() {

    private var _binding: FragmentQrScannerBinding? = null
    private val binding get() = _binding!!

    private var camera: Camera? = null
    private var linternaActiva = false
    @Volatile
    private var procesandoQr = false

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner

    private val solicitarPermisoCamara = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            iniciarCamara()
        } else {
            binding.tvScannerInstruction.text = "Se requiere permiso de cámara para escanear QR"
            Toast.makeText(
                requireContext(),
                "Permiso de cámara denegado. Puedes usar Acceso Manual.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        val opciones = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        barcodeScanner = BarcodeScanning.getClient(opciones)

        binding.flBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnAccesoManual.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, AccesoManualFragment())
                .addToBackStack(null)
                .commit()
        }

        val alternarLinterna = View.OnClickListener {
            val cam = camera ?: return@OnClickListener
            if (!cam.cameraInfo.hasFlashUnit()) {
                Toast.makeText(requireContext(), "Este dispositivo no tiene flash", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            linternaActiva = !linternaActiva
            cam.cameraControl.enableTorch(linternaActiva)
            binding.ivFlashlight.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    if (linternaActiva) R.color.apex_orange else R.color.apex_text_primary
                )
            )
        }
        binding.flFlashlight.setOnClickListener(alternarLinterna)
        binding.ivFlashlight.setOnClickListener(alternarLinterna)

        verificarPermisoYArrancar()
    }

    private fun verificarPermisoYArrancar() {
        val concedido = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (concedido) {
            iniciarCamara()
        } else {
            solicitarPermisoCamara.launch(Manifest.permission.CAMERA)
        }
    }

    private fun iniciarCamara() {
        val providerFuture = ProcessCameraProvider.getInstance(requireContext())
        providerFuture.addListener({
            if (!isAdded || _binding == null) return@addListener
            try {
                val cameraProvider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = binding.previewView.surfaceProvider
                }

                val analisis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            analizarFrame(imageProxy)
                        }
                    }

                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analisis
                )
            } catch (_: Exception) {
                if (isAdded) {
                    binding.tvScannerInstruction.text = "No se pudo iniciar la cámara"
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analizarFrame(imageProxy: ImageProxy) {
        if (procesandoQr) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val rawQr = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                if (!rawQr.isNullOrBlank() && !procesandoQr) {
                    procesandoQr = true
                    requireActivity().runOnUiThread {
                        if (isAdded) validarCodigoQr(rawQr)
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun validarCodigoQr(rawQr: String) {
        binding.tvScannerInstruction.text = "Validando pase QR…"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.validarQr(ValidarQrRequest(rawQr))
                val resultado = response.dataExitosa()
                if (!isAdded) return@launch

                if (resultado == null) {
                    val dialog = AccesoResultadoDialogFragment.newInstance(
                        success = false,
                        mensaje = response.mensajeDeError() ?: "Código QR no válido",
                        botonPrimario = "Escanear otro"
                    )
                    dialog.onDismissAction = { reanudarEscaneo() }
                    dialog.show(parentFragmentManager, AccesoResultadoDialogFragment.TAG)
                    return@launch
                }

                mostrarPreValidacionQr(resultado)
            } catch (_: Exception) {
                if (isAdded) {
                    val dialog = AccesoResultadoDialogFragment.newInstance(
                        success = false,
                        mensaje = "No se pudo conectar con el servidor",
                        botonPrimario = "Reintentar"
                    )
                    dialog.onDismissAction = { reanudarEscaneo() }
                    dialog.show(parentFragmentManager, AccesoResultadoDialogFragment.TAG)
                }
            }
        }
    }

    private fun mostrarPreValidacionQr(resultado: ValidarAccesoDto) {
        val cliente = resultado.cliente
        val nombre = cliente?.nombreCompleto?.ifBlank { "Socio" } ?: "Socio"
        val plan = resultado.membresia?.tipoMembresia?.nombre ?: "Sin membresía"

        if (!resultado.acceso || cliente == null) {
            val dialog = AccesoResultadoDialogFragment.newInstance(
                success = false,
                mensaje = resultado.mensaje ?: "Membresía vencida o inactiva",
                nombre = nombre,
                plan = plan,
                botonPrimario = "Escanear otro"
            )
            dialog.onDismissAction = { reanudarEscaneo() }
            dialog.show(parentFragmentManager, AccesoResultadoDialogFragment.TAG)
            return
        }

        val vencimiento = resultado.fechaExpiracion
            ?.let { formatearFecha(it, "dd/MM/yyyy") }
            ?: "—"
        val dias = resultado.diasRestantes ?: 0

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pase QR Válido")
            .setMessage(
                "Socio: $nombre\n" +
                    "Plan: $plan\n" +
                    "Vence: $vencimiento ($dias días restantes)\n\n" +
                    "¿Deseas registrar su entrada ahora?"
            )
            .setCancelable(false)
            .setNegativeButton("Cancelar") { _, _ -> reanudarEscaneo() }
            .setPositiveButton("Registrar Entrada") { _, _ ->
                registrarAsistenciaPorQr(cliente.id, nombre, plan)
            }
            .show()
    }

    private fun registrarAsistenciaPorQr(clienteId: Int, nombre: String, plan: String) {
        binding.tvScannerInstruction.text = "Registrando asistencia…"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.validarYRegistrarAcceso(
                    ValidarAccesoRequest(
                        clienteId = clienteId,
                        observaciones = "Acceso por código QR"
                    )
                )
                val res = response.dataExitosa()
                if (!isAdded) return@launch

                if (res != null && res.acceso) {
                    val dialog = AccesoResultadoDialogFragment.newInstance(
                        success = true,
                        mensaje = "Entrada registrada por QR",
                        nombre = nombre,
                        plan = plan,
                        botonPrimario = "Escanear otro"
                    )
                    dialog.onDismissAction = { reanudarEscaneo() }
                    dialog.show(parentFragmentManager, AccesoResultadoDialogFragment.TAG)
                } else {
                    val dialog = AccesoResultadoDialogFragment.newInstance(
                        success = false,
                        mensaje = res?.mensaje ?: response.mensajeDeError() ?: "No se pudo registrar",
                        nombre = nombre,
                        plan = plan,
                        botonPrimario = "Escanear otro"
                    )
                    dialog.onDismissAction = { reanudarEscaneo() }
                    dialog.show(parentFragmentManager, AccesoResultadoDialogFragment.TAG)
                }
            } catch (_: Exception) {
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "Error de conexión al registrar la asistencia",
                        Toast.LENGTH_LONG
                    ).show()
                    reanudarEscaneo()
                }
            }
        }
    }

    private fun reanudarEscaneo() {
        if (!isAdded || _binding == null) return
        binding.tvScannerInstruction.text = "Apunta al código QR del socio"
        procesandoQr = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::barcodeScanner.isInitialized) barcodeScanner.close()
        if (::cameraExecutor.isInitialized) cameraExecutor.shutdown()
        _binding = null
    }
}
