package com.wepin.cm.loginlib.types

import kotlinx.serialization.Serializable

@Serializable
sealed class StorageDataType {
    @Serializable
    data class FirebaseWepin(val provider: String, val idToken: String, val refreshToken: String) : StorageDataType()

    @Serializable
    data class OauthProviderPending(val provider: Providers) : StorageDataType()

    @Serializable
    data class StringValue(val value: String) : StorageDataType()

    @Serializable
    data class WepinToken(val accessToken: String, val refreshToken: String) : StorageDataType()

    @Serializable
    data class UserInfo(val status: String, val userInfo: UserInfoDetails?, val walletId: String? = null) : StorageDataType()

    @Serializable
    data class AppLanguage(val locale: Locale, val currency: String?) : StorageDataType()

    @Serializable
    data class UserStatus(val loginStatus: String, val pinRequired: Boolean?) : StorageDataType()

    @Serializable
    data class WepinProviderSelectedAddress(val addresses: List<SelectedAddress>) : StorageDataType()
}

@Serializable
data class UserInfoDetails(
    val userId: String,
    val email: String,
    val provider: String,
    val use2FA: Boolean,
)

enum class Locale {
    Ko,
    En,
}

enum class LoginStatus {
    Complete,
    PinRequired,
    RegisterRequired,
}

@Serializable
data class SelectedAddress(
    val userId: String,
    val address: String,
    val network: String,
)
