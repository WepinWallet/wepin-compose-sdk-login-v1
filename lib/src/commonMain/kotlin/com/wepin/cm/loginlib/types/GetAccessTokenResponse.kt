package com.wepin.cm.loginlib.types

import kotlinx.serialization.Serializable

@Serializable
data class GetAccessTokenResponse(
    val token: String
)
