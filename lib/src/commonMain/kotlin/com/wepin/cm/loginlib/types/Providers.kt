package com.wepin.cm.loginlib.types

enum class Providers(val value: String) {
    GOOGLE("google"),
    APPLE("apple"),
    NAVER("naver"),
    DISCORD("discord"),
    EMAIL("email"),
    EXTERNAL_TOKEN("external_token"),
    ;

    companion object {
        fun fromValue(value: String): Providers? {
            return entries.find { it.value == value }
        }

        fun isNotCommonProvider(value: String): Boolean {
            val provider = fromValue(value)
            return provider != GOOGLE && provider != APPLE && provider != NAVER && provider != DISCORD
        }

        fun isNotAccessTokenProvider(value: String): Boolean {
            val provider = fromValue(value)
            return provider != NAVER && provider != DISCORD
        }
    }
}
