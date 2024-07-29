package com.wepin.cm.loginlib.error

import com.wepin.cm.loginlib.types.ErrorCode
import com.wepin.cm.loginlib.types.WepinLoginError
import kotlinx.serialization.json.Json

class WepinError : Exception {
    var errorReason: Json? = null
    var errorMessage: String? = null
    var code: Int = 0

    constructor() {
        errorReason = null
    }

    constructor(response: Json?) {
        errorReason = response
    }

    constructor(exceptionMessage: String?) : super(exceptionMessage) {
        errorMessage = null
    }

    constructor(exceptionMessage: String?, reason: Throwable?) : super(exceptionMessage, reason) {
        errorReason = null
    }

    constructor(cause: Throwable?) : super(cause) {
        errorReason = null
    }

    constructor(
        code: Int,
        errorDescription: String,
    ) : super(errorDescription) {
        this.code = code
        this.errorMessage = errorDescription
    }

    // package
    fun setErrorCode(errCode: Int) {
        this.code = errCode
    }

    fun getErrorCode(): Int {
        return code
    }

    companion object {
        val USER_CANCELED = WepinError.generalEx(ErrorCode.USER_CANCELLED)
        val INVALID_APP_KEY = WepinError.generalEx(ErrorCode.INVALID_APP_KEY)
        val INVALID_PARAMETER = WepinError.generalEx(ErrorCode.INVALID_PARAMETER)
        val INVALID_LOGIN_PROVIDER = WepinError.generalEx(ErrorCode.INVALID_LOGIN_PROVIDER)
        val INVALID_TOKEN = WepinError.generalEx(ErrorCode.INVALID_TOKEN)
        val INVALID_LOGIN_SESSION = WepinError.generalEx(ErrorCode.INVALID_LOGIN_SESSION)
        val NOT_INITIALIZED_ERROR = WepinError.generalEx(ErrorCode.NOT_INITIALIZED_ERROR)
        val ALREADY_INITIALIZED_ERROR = WepinError.generalEx(ErrorCode.ALREADY_INITIALIZED_ERROR)
        val NOT_ACTIVITY = WepinError.generalEx(ErrorCode.NOT_ACTIVITY)
        val NOT_CONNECTED_INTERNET = WepinError.generalEx(ErrorCode.NOT_CONNECTED_INTERNET)
        val UNKNOWN_ERROR = WepinError.generalEx(ErrorCode.UNKNOWN_ERROR)
        val FAILED_LOGIN = WepinError.generalEx(ErrorCode.FAILED_LOGIN)
        val INVALID_EMAIL_DOMAIN = WepinError.generalEx(ErrorCode.INVALID_EMAIL_DOMAIN)
        val FAILED_SEND_EMAIL = WepinError.generalEx(ErrorCode.FAILED_SEND_EMAIL)
        val REQUIRED_EMAIL_VERIFIED = WepinError.generalEx(ErrorCode.REQUIRED_EMAIL_VERIFIED)
        val INCORRECT_EMAIL_FORM = WepinError.generalEx(ErrorCode.INCORRECT_EMAIL_FORM)
        val INCORRECT_PASSWORD_FORM = WepinError.generalEx(ErrorCode.INCORRECT_PASSWORD_FORM)
        val NOT_INITIALIZED_NETWORK = WepinError.generalEx(ErrorCode.NOT_INITIALIZED_NETWORK)
        val REQUIRED_SIGNUP_EMAIL = WepinError.generalEx(ErrorCode.REQUIRED_SIGNUP_EMAIL)
        val FAILED_EMAIL_VERIFIED = WepinError.generalEx(ErrorCode.FAILED_EMAIL_VERIFIED)
        val FAILED_PASSWORD_SETTING = WepinError.generalEx(ErrorCode.FAILED_PASSWORD_SETTING)
        val EXISTED_EMAIL = WepinError.generalEx(ErrorCode.EXISTED_EMAIL)
        val ALREADY_LOGOUT = WepinError.generalEx(ErrorCode.ALREADY_LOGOUT)

        private fun generalEx(errorDescription: ErrorCode): WepinError {
            return WepinError(WepinLoginError.getError(errorDescription))
        }

        fun generalUnKnownEx(message: String?): WepinError {
            return WepinError("${WepinLoginError.getError(ErrorCode.UNKNOWN_ERROR)} - $message")
        }
    }
}
