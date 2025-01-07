package com.wepin.cm.loginlib.types.network

import kotlinx.serialization.Serializable

@Serializable
data class LoginOauthIdTokenRequest(
    var idToken: String,
    var sign: String? = null,
)
