package com.wepin.cm.loginlib.const

import platform.Foundation.NSURL

internal actual class AppAuthConst {
    actual companion object {
        actual fun getAuthorizationEndpoint(provider: String): Any {
            return when (provider) {
                "google" -> NSURL(string = "https://accounts.google.com/o/oauth2/v2/auth")
                "apple" -> NSURL(string = "https://appleid.apple.com/auth/authorize")
                "discord" -> NSURL(string = "https://discord.com/api/oauth2/authorize")
                "naver" -> NSURL(string = "https://nid.naver.com/oauth2.0/authorize")
                else -> NSURL(string = "")
            }
        }

        actual fun getTokenEndpoint(provider: String): Any {
            return when (provider) {
                "google" -> NSURL(string = "https://oauth2.googleapis.com/token")
                "apple" -> NSURL(string = "https://appleid.apple.com/auth/token")
                "discord" -> NSURL(string = "https://discord.com/api/oauth2/token")
                "naver" -> NSURL(string = "https://nid.naver.com/oauth2.0/token")
                else -> NSURL(string = "")
            }
        }
    }
}
