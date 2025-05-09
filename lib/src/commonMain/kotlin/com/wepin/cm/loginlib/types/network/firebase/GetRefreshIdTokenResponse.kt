package com.wepin.cm.loginlib.types.network.firebase

import kotlinx.serialization.Serializable

sealed class GetRefreshIdTokenResponse

@Serializable
data class GetRefreshIdTokenSuccess(
    var expires_in: String,
    var token_type: String,
    var refresh_token: String,
    var id_token: String,
    var user_id: String,
    var project_id: String,
) : GetRefreshIdTokenResponse()

data class GetRefreshIdTokenError(val error: FirebaseAuthError) : GetRefreshIdTokenResponse()
