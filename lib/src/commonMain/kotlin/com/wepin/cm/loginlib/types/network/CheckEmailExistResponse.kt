package com.wepin.cm.loginlib.types.network

import kotlinx.serialization.Serializable

@Serializable
data class CheckEmailExistResponse(
    val isEmailExist: Boolean,
    val isEmailverified: Boolean,
    val providerIds: List<String>,
)
