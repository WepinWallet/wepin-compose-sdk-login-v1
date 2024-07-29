package com.wepin.cm.loginlib.types

data class LoginOauth2Params(
    val provider: String,
    val clientId: String,
)

data class LoginClientParams(
    val clientId: String,
    val clientSecret: String? = null, // if provider is naver
    val clientName: String? = null, // if provider is naver
)
