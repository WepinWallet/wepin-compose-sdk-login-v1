package com.wepin.cm.loginlib.types.network

import kotlinx.serialization.Serializable

@Serializable
data class PasswordStateResponse(var isPasswordResetRequired: Boolean)

@Serializable
data class PasswordStateRequest(var isPasswordResetRequired: Boolean)
