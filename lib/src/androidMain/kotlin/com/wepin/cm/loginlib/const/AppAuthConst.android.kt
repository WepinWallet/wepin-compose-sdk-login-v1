package com.wepin.cm.loginlib.const

import android.net.Uri

internal actual class AppAuthConst {
    actual companion object {
        actual fun getAuthorizationEndpoint(provider: String): Any {
            return when (provider) {
                "google" -> Uri.parse("https://accounts.google.com/o/oauth2/v2/auth")
                "apple" -> Uri.parse("https://appleid.apple.com/auth/authorize")
                "discord" -> Uri.parse("https://discord.com/api/oauth2/authorize")
                "naver" -> Uri.parse("https://nid.naver.com/oauth2.0/authorize")
                else -> Uri.parse("")
            }
        }

        actual fun getTokenEndpoint(provider: String): Any {
            return when (provider) {
                "google" -> Uri.parse("https://oauth2.googleapis.com/token")
                "apple" -> Uri.parse("https://appleid.apple.com/auth/token")
                "discord" -> Uri.parse("https://discord.com/api/oauth2/token")
                "naver" -> Uri.parse("https://nid.naver.com/oauth2.0/token")
                else -> Uri.parse("")
            }
        }
    }
}
