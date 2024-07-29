package com.wepin.cm.loginlib.types.network

import kotlinx.serialization.Serializable

@Serializable
data class LoginOauthIdTokenResponse(
    val result: Boolean,
    val token: String? = null,
    val error: String? = null,
)
