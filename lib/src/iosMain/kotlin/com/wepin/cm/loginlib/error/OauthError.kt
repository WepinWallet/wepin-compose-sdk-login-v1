package com.wepin.cm.loginlib.error

import cocoapods.AppAuth.OIDErrorCodeBrowserOpenError
import cocoapods.AppAuth.OIDErrorCodeNetworkError
import cocoapods.AppAuth.OIDErrorCodeOAuthAccessDenied
import cocoapods.AppAuth.OIDErrorCodeOAuthInvalidClient
import cocoapods.AppAuth.OIDErrorCodeOAuthInvalidRequest
import cocoapods.AppAuth.OIDErrorCodeOAuthInvalidScope
import cocoapods.AppAuth.OIDErrorCodeOAuthServerError
import cocoapods.AppAuth.OIDErrorCodeOAuthTemporarilyUnavailable
import cocoapods.AppAuth.OIDErrorCodeOAuthTokenInvalidGrant
import cocoapods.AppAuth.OIDErrorCodeOAuthTokenInvalidRequest
import cocoapods.AppAuth.OIDErrorCodeOAuthTokenInvalidScope
import cocoapods.AppAuth.OIDErrorCodeOAuthTokenUnauthorizedClient
import cocoapods.AppAuth.OIDErrorCodeOAuthTokenUnsupportedGrantType
import cocoapods.AppAuth.OIDErrorCodeOAuthUnauthorizedClient
import cocoapods.AppAuth.OIDErrorCodeOAuthUnsupportedResponseType
import cocoapods.AppAuth.OIDErrorCodeUserCanceledAuthorizationFlow
import cocoapods.AppAuth.OIDOAuthAuthorizationErrorDomain
import cocoapods.AppAuth.OIDOAuthErrorFieldErrorDescription
import cocoapods.AppAuth.OIDOAuthErrorResponseErrorKey
import cocoapods.AppAuth.OIDOAuthTokenErrorDomain
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AuthenticationServices.ASWebAuthenticationSessionErrorCodeCanceledLogin
import platform.AuthenticationServices.ASWebAuthenticationSessionErrorCodePresentationContextInvalid
import platform.AuthenticationServices.ASWebAuthenticationSessionErrorCodePresentationContextNotProvided
import platform.AuthenticationServices.ASWebAuthenticationSessionErrorDomain
import platform.Foundation.NSError
import platform.Foundation.NSLocalizedDescriptionKey

fun extractNSError(exception: Exception): NSError? {
    val message = exception.message ?: return null
    val regex = Regex("""\(.*? error (\d+)\.\)""")
    val match = regex.find(message)
    if (match != null) {
        val domain = message.substringAfter("(").substringBefore(" error")
        val code = match.groupValues[1].toInt()
        val userInfo = mapOf(
            NSLocalizedDescriptionKey to (exception.message ?: "Unknown error")
        )
        return NSError(domain, code.toLong(), userInfo.toMap())
    }
    return null
}

@OptIn(ExperimentalForeignApi::class)
fun getOauthErrorCode(error: NSError, defaultCode: String): String {
    when(error.domain) {
        OIDOAuthAuthorizationErrorDomain -> {
            when(error.code) {
                OIDErrorCodeOAuthInvalidRequest -> return "invalid_request"
                OIDErrorCodeOAuthUnauthorizedClient -> return "unauthorized_client"
                OIDErrorCodeOAuthAccessDenied -> return "access_denied"
                OIDErrorCodeOAuthUnsupportedResponseType -> return "unsupported_response_type"
                OIDErrorCodeOAuthInvalidScope -> return "invalid_scope"
                OIDErrorCodeOAuthServerError -> return "server_error"
                OIDErrorCodeOAuthTemporarilyUnavailable -> return "temporarily_unavailable"
                else -> {}
            }
        }
        OIDOAuthTokenErrorDomain -> {
            when(error.code) {
                OIDErrorCodeOAuthTokenInvalidRequest -> return "invalid_request"
                OIDErrorCodeOAuthInvalidClient -> return "invalid_client"
                OIDErrorCodeOAuthTokenInvalidGrant -> return "invalid_grant"
                OIDErrorCodeOAuthTokenUnauthorizedClient -> return "unauthorized_client"
                OIDErrorCodeOAuthTokenUnsupportedGrantType -> return "unsupported_grant_type"
                OIDErrorCodeOAuthTokenInvalidScope -> return "invalid_scope"
                else -> {}
            }
        }
        ASWebAuthenticationSessionErrorDomain -> {
            when(error.code) {
                ASWebAuthenticationSessionErrorCodeCanceledLogin -> return "user_canceled"
                ASWebAuthenticationSessionErrorCodePresentationContextNotProvided -> return "required_context"
                ASWebAuthenticationSessionErrorCodePresentationContextInvalid -> return "invalid_context"
                else -> {}
            }
        }
        else -> {
            when(error.code) {
                OIDErrorCodeUserCanceledAuthorizationFlow -> return "user_canceled"
                OIDErrorCodeBrowserOpenError -> return "browser_open_error"
                OIDErrorCodeNetworkError -> return "network_error"
            }
        }
    }
    return defaultCode
}

@OptIn(ExperimentalForeignApi::class)
fun getOauthErrorMessage(error: NSError): String {
    try {
        val userInfo = error.userInfo
        val oauthError = userInfo.getValue(OIDOAuthErrorResponseErrorKey) as Map<*, *>
        val errorDescription = oauthError[OIDOAuthErrorFieldErrorDescription] as String?
        return errorDescription ?: error.localizedDescription
    } catch(e: Exception) {
        return error.localizedDescription
    }
}