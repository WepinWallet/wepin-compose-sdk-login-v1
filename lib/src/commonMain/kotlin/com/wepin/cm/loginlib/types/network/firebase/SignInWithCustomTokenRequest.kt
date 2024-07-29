package com.wepin.cm.loginlib.types.network.firebase

import kotlinx.serialization.Serializable

@Serializable
data class SignInWithCustomTokenRequest(
    var token: String,
    var returnSecureToken: Boolean? = true,
)
