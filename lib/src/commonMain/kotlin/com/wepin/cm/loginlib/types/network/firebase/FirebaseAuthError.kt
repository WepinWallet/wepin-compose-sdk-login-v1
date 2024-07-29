package com.wepin.cm.loginlib.types.network.firebase

data class FirebaseAuthError(
    val error: FirebaseError,
)

data class FirebaseError(
    val code: Int,
    val message: String,
)
