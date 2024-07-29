package com.wepin.cm.loginlib.types.network

sealed class AppInfoResponse<out T> {
    data class Success<out T>(val data: T) : AppInfoResponse<T>()

    data class Error(val error: ErrorResponse) : AppInfoResponse<ErrorResponse>()
}
