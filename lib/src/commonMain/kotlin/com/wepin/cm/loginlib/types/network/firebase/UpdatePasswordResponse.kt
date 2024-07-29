package com.wepin.cm.loginlib.types.network.firebase

sealed class UpdatePasswordResponse

data class UpdatePasswordSuccess(
    var localId: String,
    var email: String,
    var passwordHash: String,
    var providerUserInfo: List<VerifyProviderUserInfo>,
    var idToken: String,
    var refreshToken: String,
    var expiresIn: String,
) : UpdatePasswordResponse()

data class UpdatePasswordError(val error: FirebaseAuthError) : UpdatePasswordResponse()
