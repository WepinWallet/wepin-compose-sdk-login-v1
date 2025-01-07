package com.wepin.cm.loginlib.types

import kotlinx.serialization.Serializable

@Serializable
data class FBToken(
    val idToken: String,
    val refreshToken: String,
)

@Serializable
data class LoginResult(
    val provider: Providers,
    val token: FBToken,
)
