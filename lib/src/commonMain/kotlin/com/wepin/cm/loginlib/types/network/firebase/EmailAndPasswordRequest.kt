package com.wepin.cm.loginlib.types.network.firebase

import kotlinx.serialization.Serializable

@Serializable
data class EmailAndPasswordRequest(
    var email: String,
    var password: String,
    var returnSecureToken: Boolean = true,
)
