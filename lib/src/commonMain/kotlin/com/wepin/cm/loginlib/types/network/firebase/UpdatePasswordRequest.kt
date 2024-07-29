package com.wepin.cm.loginlib.types.network.firebase

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePasswordRequest(
    var idToken: String,
    var password: String,
    var returnSecureToken: Boolean,
)
