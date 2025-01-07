package com.wepin.cm.loginlib.types.network

import kotlinx.serialization.Serializable

@Serializable
data class LoginOauthAccessTokenRequest(
    val provider: String,
    val accessToken: String,
    val sign: String? = null,
)

enum class OauthAccessTokenProvider(val value: String) {
    NAVER("naver"),
    DISCORD("discord"),
}
