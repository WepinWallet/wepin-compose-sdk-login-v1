package com.wepin.cm.loginlib.types.network.firebase

sealed class SignUpResponse

data class SignUpSuccess(
    var idToken: String,
    var email: String,
    var refreshToken: String,
    var expiresIn: String,
    var localId: String,
) : SignUpResponse()

data class SignUpError(val error: FirebaseAuthError) : SignUpResponse()
