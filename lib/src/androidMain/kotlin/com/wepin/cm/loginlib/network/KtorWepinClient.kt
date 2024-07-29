package com.wepin.cm.loginlib.network

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.header
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual object KtorWepinClient {
    private var clients: MutableMap<String, HttpClient> = mutableMapOf()

    actual fun createHttpClient(
        baseUrl: String,
        appDomain: String?,
        appKey: String?,
        version: String?,
    ): HttpClient {
        return clients.getOrPut(baseUrl) {
            HttpClient(OkHttp) {
                install(Logging) {
                    logger =
                        object : Logger {
                            override fun log(message: String) {
                                android.util.Log.d("HTTP call", message)
                            }
                        }
                    level = LogLevel.NONE
                }

                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            encodeDefaults = true
                            prettyPrint = true
                            isLenient = true
                        },
                    )
                }

                defaultRequest {
                    appDomain?.let { header("X-API-KEY", it) }
                    appKey?.let { header("X-API-DOMAIN", it) }
                    appKey?.let { header("X-SDK-TYPE", "android-login") }
                    version?.let { header("X-SDK-VERSION", it) }
                    url {
                        takeFrom(baseUrl)
                    }
                }
            }
        }
    }

    actual fun closeAllClients() {
        clients.values.forEach { client -> client.close() }
        clients.clear()
    }
}
