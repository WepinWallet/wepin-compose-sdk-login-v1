package com.wepin.cm.loginlib.const

internal expect class AppAuthConst {
    companion object {
        fun getAuthorizationEndpoint(provider: String): Any

        fun getTokenEndpoint(provider: String): Any
    }
}
