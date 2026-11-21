package com.sprm.apexgym.feature.main

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.repository.NotificacionRepository
import com.sprm.apexgym.core.network.repository.ResultadoNotificaciones
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.databinding.FragmentEmpleadoDashboardBinding
import com.sprm.apexgym.feature.auth.LoginFragment
import com.sprm.apexgym.feature.caja.ResumenAperturaFragment
import com.sprm.apexgym.feature.empleados.EmpleadosListFragment
import com.sprm.apexgym.feature.inventario.PedidosCompraFragment
import com.sprm.apexgym.feature.notificaciones.NotificacionesListFragment
import com.sprm.apexgym.feature.recepcion.RecepcionDashboardFragment
import com.sprm.apexgym.feature.tienda.TiendaFragment
import com.sprm.apexgym.feature.visitas.VisitasListFragment
import kotlinx.coroutines.launch

/**
 * Panel principal para usuarios con rol de empleado.
 * Permite seleccionar la sucursal activa (propagada en `x-sucursal-id`) y
 * acceder a los módulos operativos del gimnasio.
 */
class EmpleadoDashboardFragment : BaseFragment<FragmentEmpleadoDashboardBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_empleado_dashboard

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nombre = SessionManager.obtenerNombre(requireContext())
            ?.trim()
            ?.split(" ")
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }

        binding.tvEmployeeGreeting.text = if (nombre != null) "Hola, $nombre" else "Hola, Empleado"
        binding.tvEmployeeRole.text = SessionManager.obtenerRol(requireContext())
            ?.takeIf { it.isNotBlank() }
            ?.let { "Rol: $it" }
            ?: "Panel de empleado"

        actualizarBannerSucursal(view)
        sincronizarSucursalActiva(view)

        val cardSucursal = view.findViewWithTag<View>("card_sucursal_activa")
        cardSucursal?.setOnClickListener {
            if (!SessionManager.puedeCambiarSucursal(requireContext())) return@setOnClickListener
            val existing = childFragmentManager.findFragmentByTag(SucursalSelectorBottomSheetFragment.TAG)
            if (existing == null) {
                SucursalSelectorBottomSheetFragment().show(
                    childFragmentManager,
                    SucursalSelectorBottomSheetFragment.TAG
                )
            }
        }

        childFragmentManager.setFragmentResultListener(
            SucursalSelectorBottomSheetFragment.RESULT_KEY_SUCURSAL_ACTUALIZADA,
            viewLifecycleOwner
        ) { _, _ ->
            actualizarBannerSucursal(view)
        }

        binding.flAvatar.setOnClickListener {
            val existing = childFragmentManager.findFragmentByTag(ProfileBottomSheetFragment.TAG)
            if (existing == null) {
                ProfileBottomSheetFragment().show(
                    childFragmentManager,
                    ProfileBottomSheetFragment.TAG
                )
            }
        }

        binding.flEmpleadoNotificaciones.setOnClickListener {
            navegar(NotificacionesListFragment())
        }
        cargarContadorNotificaciones()

        binding.cvRecepcion.setOnClickListener { navegar(RecepcionDashboardFragment()) }
        binding.cvCaja.setOnClickListener { navegar(ResumenAperturaFragment()) }
        binding.cvEmpleados.setOnClickListener { navegar(EmpleadosListFragment()) }
        binding.cvVisitas.setOnClickListener { navegar(VisitasListFragment()) }
        binding.cvTienda.setOnClickListener { navegar(TiendaFragment()) }
        binding.cvPedidosCompra.setOnClickListener { navegar(PedidosCompraFragment()) }
        binding.cvNotificaciones.setOnClickListener { navegar(NotificacionesListFragment()) }

        binding.btnLogout.setOnClickListener {
            binding.btnLogout.isEnabled = false
            binding.btnLogout.text = "Cerrando sesión..."

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    RetrofitClient.authService.logout()
                } catch (_: Exception) {
                } finally {
                    RetrofitClient.cookieJar.clear()
                    SessionManager.limpiar(requireContext())
                    CartManager.vaciarCarrito(requireContext())

                    if (isAdded) {
                        val hostFm = requireActivity().supportFragmentManager
                        hostFm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                        hostFm.beginTransaction()
                            .setCustomAnimations(
                                android.R.anim.fade_in,
                                android.R.anim.fade_out
                            )
                            .replace(R.id.fragment_container, LoginFragment())
                            .commit()
                    }
                }
            }
        }
    }

    private fun actualizarBannerSucursal(rootView: View) {
        if (!isAdded) return
        val context = requireContext()
        val tvNombre = rootView.findViewWithTag<TextView>("tv_sucursal_nombre")
        val tvSubtitulo = rootView.findViewWithTag<TextView>("tv_sucursal_subtitulo")
        val tvCambiar = rootView.findViewWithTag<View>("tv_sucursal_cambiar")
        val cardSucursal = rootView.findViewWithTag<View>("card_sucursal_activa")
        val puedeCambiar = SessionManager.puedeCambiarSucursal(context)

        tvNombre?.text = SessionManager.obtenerSucursalNombre(context)
        tvSubtitulo?.text = SessionManager.obtenerSucursalSubtitulo(context)
        tvCambiar?.visibility = if (puedeCambiar) View.VISIBLE else View.GONE
        cardSucursal?.isClickable = puedeCambiar
        cardSucursal?.isFocusable = puedeCambiar
    }

    private fun sincronizarSucursalActiva(rootView: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val sucursal = RetrofitClient.empleadoService.obtenerSucursalActual().dataExitosa()
                if (sucursal != null && isAdded) {
                    SessionManager.guardarSucursal(
                        context = requireContext(),
                        sucursalId = sucursal.id,
                        nombre = sucursal.nombre,
                        codigo = sucursal.codigo,
                        ciudad = sucursal.ciudad,
                        puedeCambiar = sucursal.puedeCambiarSucursal ?: false
                    )
                    actualizarBannerSucursal(rootView)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun cargarContadorNotificaciones() {
        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = NotificacionRepository().contarNoLeidas()
            if (!isAdded || resultado !is ResultadoNotificaciones.Exito) return@launch

            val noLeidas = resultado.datos
            binding.tvEmpleadoNotificationsBadge.text =
                if (noLeidas > 99) "99+" else noLeidas.toString()
            binding.tvEmpleadoNotificationsBadge.visibility =
                if (noLeidas > 0) View.VISIBLE else View.GONE
        }
    }

    private fun navegar(fragmento: Fragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, fragmento)
            .addToBackStack(null)
            .commit()
    }
}
