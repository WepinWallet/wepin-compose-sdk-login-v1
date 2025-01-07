package com.wepin.cm.loginlib.types

import kotlinx.serialization.Serializable

@Serializable
data class OAuthProviderInfo(
    val provider: String,
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    val oauthSpec: Array<String>
)