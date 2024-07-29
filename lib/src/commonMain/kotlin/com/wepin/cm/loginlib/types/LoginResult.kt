package com.wepin.cm.loginlib.types

data class FBToken(
    val idToken: String,
    val refreshToken: String,
)

data class LoginResult(
    val provider: Providers,
    val token: FBToken,
)
