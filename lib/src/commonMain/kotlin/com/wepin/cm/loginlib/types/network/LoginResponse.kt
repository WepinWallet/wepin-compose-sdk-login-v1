package com.wepin.cm.loginlib.types.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val loginStatus: String,
    val pinRequired: Boolean? = false,
    val walletId: String? = "",
    val token: Token,
    val userInfo: AppUser,
)

@Serializable
data class Token(
    val refresh: String,
    val access: String,
)
@Serializable
data class AppUser(
    val userId: String,
    val email: String,
    val name: String,
    val locale: String,
    val currency: String,
    val lastAccessDevice: String,
    val lastSessionIP: String,
    val userJoinStage: UserJoinStage,
    val profileImage: String,
    val userState: UserState,
    val use2FA: Int,
)
@Serializable
enum class UserJoinStage(val stage: Int) {
    @SerialName("1")
    EMAIL_REQUIRE(1),
    @SerialName("2")
    PIN_REQUIRE(2),
    @SerialName("3")
    COMPLETE(3),
}
@Serializable
enum class UserState(val state: Int) {
    @SerialName("1")
    ACTIVE(1),
    @SerialName("2")
    DELETED(2),
}
