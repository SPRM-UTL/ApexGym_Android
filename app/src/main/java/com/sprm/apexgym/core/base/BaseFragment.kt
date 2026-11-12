package com.sprm.apexgym.core.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    val binding: VB
        get() = _binding
            ?: throw IllegalStateException("Binding can only be accessed between onCreateView and onDestroyView")

    @LayoutRes
    abstract fun getLayoutRes(): Int

    @Suppress("UNCHECKED_CAST")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(getLayoutRes(), container, false)
        try {
            var currentClass: Class<*>? = javaClass
            var bindingClass: Class<VB>? = null
            while (currentClass != null && bindingClass == null && currentClass != BaseFragment::class.java) {
                val superclass = currentClass.genericSuperclass
                if (superclass is ParameterizedType) {
                    for (arg in superclass.actualTypeArguments) {
                        if (arg is Class<*> && ViewBinding::class.java.isAssignableFrom(arg)) {
                            bindingClass = arg as Class<VB>
                            break
                        }
                    }
                }
                currentClass = currentClass.superclass
            }
            if (bindingClass != null) {
                val method = bindingClass.getMethod("bind", View::class.java)
                _binding = method.invoke(null, view) as VB
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
