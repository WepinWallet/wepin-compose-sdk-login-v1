package com.wepin.cm.loginlib.types

import kotlinx.serialization.Serializable

@Serializable
data class OAuthTokenRequest(
    val code: String,
    val state: String? = null,
    val clientId: String,
    val redirectUri: String,
    val codeVerifier: String? = null,
)
