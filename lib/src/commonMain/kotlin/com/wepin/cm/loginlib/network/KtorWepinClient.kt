package com.wepin.cm.loginlib.network

import io.ktor.client.HttpClient

expect object KtorWepinClient {
//    fun getClient(baseUrl: String, appDomain: String, appKey: String, version: String): HttpClient
//    fun closeClients()
    fun createHttpClient(
        baseUrl: String,
        appDomain: String?,
        appKey: String?,
        version: String?,
    ): HttpClient

    fun closeAllClients()
}
