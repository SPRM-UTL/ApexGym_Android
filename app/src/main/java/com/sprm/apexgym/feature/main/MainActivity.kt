package com.sprm.apexgym.feature.main

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.client.LoginRequest
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.feature.auth.RegistroFragment
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.Console
import java.io.IOException

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        RetrofitClient.initialize(this)

        val btnMostrar = findViewById<Button>(R.id.btnMostrarLogin)
        val btnProbar = findViewById<Button>(R.id.btnProbar)

        btnMostrar.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_login, RegistroFragment())
                .addToBackStack(null)
                .commit()
        }

        btnProbar.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.authService.probarcookie()

                    if (response.isSuccessful) {
                        val resultado = response.body()

                        if (resultado != null) {
                            Toast.makeText(
                                this@MainActivity,
                                "Funciono",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "No Funciono",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                } catch (e: IOException) {
                    Toast.makeText(
                        this@MainActivity,
                        "No se pudo conectar con el servidor",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: HttpException) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error HTTP: ${e.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Log.e("Api-Error",e.toString())
                    Toast.makeText(
                        this@MainActivity,
                        "Ocurrió un error al iniciar sesión: ",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}