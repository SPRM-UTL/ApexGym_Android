package com.sprm.apexgym.core.network.client

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject

class SessionCookieJar(context: Context): CookieJar {
    private val preferences = context.applicationContext
        .getSharedPreferences("cookies_apexgym", Context.MODE_PRIVATE)

    private val lock = Any()
    private val cookieStore = mutableListOf<Cookie>()

    init {
        cargarCookies()
    }

    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>
    ) {
        synchronized(lock) {
            cookieStore.removeAll { existing ->
                cookies.any { incoming ->
                    existing.name == incoming.name &&
                            existing.domain == incoming.domain &&
                            existing.path == incoming.path
                }
            }

            cookieStore.addAll(
                cookies.filter {
                    it.expiresAt > System.currentTimeMillis()
                }
            )

            guardarCookiesPersistentes()
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        synchronized(lock) {
            cookieStore.removeAll {
                it.expiresAt <= System.currentTimeMillis()
            }

            guardarCookiesPersistentes()

            return cookieStore.filter { it.matches(url) }
        }
    }

    fun clear() {
        synchronized(lock) {
            cookieStore.clear()
            preferences.edit().remove("cookies").apply()
        }
    }

    private fun guardarCookiesPersistentes() {
        val array = JSONArray()

        cookieStore.filter { it.persistent }.forEach { cookie ->
            array.put(
                JSONObject().apply {
                    put("name", cookie.name)
                    put("value", cookie.value)
                    put("domain", cookie.domain)
                    put("path", cookie.path)
                    put("expiresAt", cookie.expiresAt)
                    put("hostOnly", cookie.hostOnly)
                    put("secure", cookie.secure)
                    put("httpOnly", cookie.httpOnly)
                }
            )
        }

        preferences.edit()
            .putString("cookies", array.toString())
            .apply()
    }

    private fun cargarCookies() {
        val json = preferences.getString("cookies", null) ?: return
        val ahora = System.currentTimeMillis()

        synchronized(lock) {
            try {
                val array = JSONArray(json)

                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)

                    val nombre = item.getString("name")
                    val valor = item.getString("value")
                    val dominio = item.getString("domain")
                    val ruta = item.getString("path")
                    val expiracion = item.getLong("expiresAt")

                    if (expiracion <= ahora) continue

                    val builder = Cookie.Builder()
                        .name(nombre)
                        .value(valor)
                        .path(ruta)
                        .expiresAt(expiracion)

                    if (item.getBoolean("hostOnly")) {
                        builder.hostOnlyDomain(dominio)
                    } else {
                        builder.domain(dominio)
                    }

                    if (item.getBoolean("secure")) {
                        builder.secure()
                    }

                    if (item.getBoolean("httpOnly")) {
                        builder.httpOnly()
                    }

                    val cookie = builder.build()

                    if (cookie.persistent) {
                        cookieStore.add(cookie)
                    }
                }
            } catch (_: Exception) {
                cookieStore.clear()
                preferences.edit().remove("cookies").apply()
            }
        }
    }
}