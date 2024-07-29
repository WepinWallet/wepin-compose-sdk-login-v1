package com.wepin.cm.loginlib.types.network

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    private var idToken: String,
)
