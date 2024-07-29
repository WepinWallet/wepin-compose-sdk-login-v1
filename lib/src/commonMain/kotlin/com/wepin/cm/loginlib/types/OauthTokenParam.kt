package com.wepin.cm.loginlib.types

data class OauthTokenParam(
    val provider: String,
    val clientId: String,
    val codeVerifier: String? = null,
    val code: String,
    val state: String? = null,
)
