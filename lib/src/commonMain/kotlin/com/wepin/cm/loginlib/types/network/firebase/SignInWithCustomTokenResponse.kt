package com.wepin.cm.loginlib.types.network.firebase

import kotlinx.serialization.Serializable

sealed class SignInWithCustomTokenResponse

@Serializable
data class SignInWithCustomTokenSuccess(
    var idToken: String,
    var refreshToken: String,
) : SignInWithCustomTokenResponse()

data class SignInWithCustomTokenError(val error: FirebaseAuthError) : SignInWithCustomTokenResponse()
