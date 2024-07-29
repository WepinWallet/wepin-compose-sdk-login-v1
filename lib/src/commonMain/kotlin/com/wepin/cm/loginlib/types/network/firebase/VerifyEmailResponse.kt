package com.wepin.cm.loginlib.types.network.firebase

data class VerifyEmailResponse(
    var localId: String,
    var email: String,
    var passwordHash: String,
    var providerUserInfo: VerifyProviderUserInfo,
)

interface VerifyProviderUserInfo {
    var providerId: String
    var federatedId: String
}
