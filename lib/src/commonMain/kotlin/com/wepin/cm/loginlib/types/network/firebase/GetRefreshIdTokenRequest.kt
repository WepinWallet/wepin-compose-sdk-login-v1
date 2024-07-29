package com.wepin.cm.loginlib.types.network.firebase

import kotlinx.serialization.Serializable

@Serializable
data class GetRefreshIdTokenRequest(
    var refresh_token: String,
    var grant_type: String = "refresh_token",
)
