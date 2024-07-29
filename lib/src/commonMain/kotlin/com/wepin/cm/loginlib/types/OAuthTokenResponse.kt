package com.wepin.cm.loginlib.types

import kotlinx.serialization.Serializable

@Serializable
data class OAuthTokenResponse(
    val id_token: String? = null,
    val access_token: String,
    val token_type: String,
    val expires_in: Int? = -1,
    val refresh_token: String? = null,
    val scope: String? = null,
)
