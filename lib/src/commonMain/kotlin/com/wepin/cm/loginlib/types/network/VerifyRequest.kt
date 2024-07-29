package com.wepin.cm.loginlib.types.network

import kotlinx.serialization.Serializable

@Serializable
data class VerifyRequest(
    val type: String,
    val email: String,
    val localeId: Int? = 1,
)
