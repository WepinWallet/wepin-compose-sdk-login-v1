package com.wepin.cm.loginlib.types.network.firebase

import kotlinx.serialization.Serializable

@Serializable
class ResetPasswordRequest(
    var oobCode: String,
    var newPassword: String,
)
