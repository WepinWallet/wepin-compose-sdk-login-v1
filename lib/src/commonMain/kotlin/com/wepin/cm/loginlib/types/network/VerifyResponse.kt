package com.wepin.cm.loginlib.types.network

import kotlinx.serialization.Serializable

@Serializable
data class VerifyResponse(
    val result: Boolean,
    val oobReset: String? = null,
    val oobVerify: String? = null,
)
