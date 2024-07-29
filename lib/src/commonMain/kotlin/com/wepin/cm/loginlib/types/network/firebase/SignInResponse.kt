package com.wepin.cm.loginlib.types.network.firebase

import kotlinx.serialization.Serializable

@Serializable
data class SignInResponse(
    var localId: String,
    var email: String,
    var displayName: String,
    var idToken: String,
    var registered: Boolean,
    var refreshToken: String,
    var expiresIn: String,
)
