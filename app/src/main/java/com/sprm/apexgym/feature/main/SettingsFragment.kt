package com.sprm.apexgym.feature.main

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.feature.auth.LoginFragment
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var cerrandoSesion = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(ContextCompat.getColor(context, R.color.apex_bg_screen))
            setPadding(dpToPx(18), dpToPx(16), dpToPx(18), dpToPx(24))
        }

        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(56)
            ).apply {
                bottomMargin = dpToPx(20)
            }
        }

        val btnBack = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(44), dpToPx(44))
            setBackgroundResource(R.drawable.bg_circle_white_elevated)
            elevation = dpToPx(2).toFloat()
            isClickable = true
            isFocusable = true

            val icon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(22), dpToPx(22), Gravity.CENTER)
                setImageResource(R.drawable.ic_arrow_left)
                ImageViewCompat.setImageTintList(
                    this,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.apex_text_primary))
                )
            }
            addView(icon)

            setOnClickListener {
                if (!cerrandoSesion) {
                    parentFragmentManager.popBackStack()
                }
            }
        }
        headerRow.addView(btnBack)

        val tvHeaderTitle = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER
            text = "Ajustes"
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
            setTypeface(typeface, Typeface.BOLD)
        }
        headerRow.addView(tvHeaderTitle)

        val spacerRight = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(44), dpToPx(44))
        }
        headerRow.addView(spacerRight)
        root.addView(headerRow)

        val cardPush = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(16)
            }
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            radius = dpToPx(16).toFloat()
            cardElevation = 0f
            strokeWidth = dpToPx(1)
            strokeColor = ContextCompat.getColor(context, R.color.apex_stroke_light)
        }

        val pushRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }

        val pushIconBox = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(42), dpToPx(42))
            setBackgroundResource(R.drawable.bg_chip_unselected)

            val icon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(20), dpToPx(20), Gravity.CENTER)
                setImageResource(R.drawable.ic_notifications_active)
                ImageViewCompat.setImageTintList(
                    this,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.apex_orange))
                )
            }
            addView(icon)
        }
        pushRow.addView(pushIconBox)

        val pushTextCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dpToPx(14)
                marginEnd = dpToPx(10)
            }
        }

        val tvPushTitle = TextView(context).apply {
            text = "Notificaciones Push"
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
            setTypeface(typeface, Typeface.BOLD)
        }
        pushTextCol.addView(tvPushTitle)

        val tvPushSubtitle = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(2)
            }
            text = "Permiso para recibir avisos y alertas"
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
        }
        pushTextCol.addView(tvPushSubtitle)
        pushRow.addView(pushTextCol)

        val switchPush = SwitchMaterial(context).apply {
            isChecked = true
            thumbTintList = ColorStateList.valueOf(Color.WHITE)
            trackTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(
                    ContextCompat.getColor(context, R.color.apex_orange),
                    ContextCompat.getColor(context, R.color.apex_stroke_light)
                )
            )
        }
        pushRow.addView(switchPush)

        cardPush.addView(pushRow)
        root.addView(cardPush)

        lateinit var tvLogoutTitle: TextView
        val btnLogout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundResource(R.drawable.bg_logout_option_card)
            setPadding(dpToPx(16), dpToPx(15), dpToPx(16), dpToPx(15))
            isClickable = true
            isFocusable = true

            val logoutIconBox = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(42), dpToPx(42))
                setBackgroundResource(R.drawable.bg_circle_white_elevated)

                val icon = ImageView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(dpToPx(20), dpToPx(20), Gravity.CENTER)
                    setImageResource(R.drawable.ic_logout)
                }
                addView(icon)
            }
            addView(logoutIconBox)

            val logoutTextCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dpToPx(14)
                }
            }

            tvLogoutTitle = TextView(context).apply {
                text = "Cerrar Sesión"
                setTextColor(Color.parseColor("#EF4444"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                setTypeface(typeface, Typeface.BOLD)
            }
            logoutTextCol.addView(tvLogoutTitle)

            val tvLogoutSub = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(2)
                }
                text = "Salir de tu cuenta en este dispositivo"
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            }
            logoutTextCol.addView(tvLogoutSub)
            addView(logoutTextCol)
        }

        btnLogout.setOnClickListener {
            if (cerrandoSesion) return@setOnClickListener
            cerrandoSesion = true
            btnLogout.isEnabled = false
            tvLogoutTitle.text = "Cerrando sesión..."

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    RetrofitClient.authService.logout()
                } catch (_: Exception) {
                } finally {
                    RetrofitClient.cookieJar.clear()
                    if (isAdded) {
                        CartManager.vaciarCarrito(requireContext())
                        val hostFragmentManager = requireActivity().supportFragmentManager
                        hostFragmentManager.popBackStack(
                            null,
                            FragmentManager.POP_BACK_STACK_INCLUSIVE
                        )
                        hostFragmentManager.beginTransaction()
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

        root.addView(btnLogout)

        return root
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
