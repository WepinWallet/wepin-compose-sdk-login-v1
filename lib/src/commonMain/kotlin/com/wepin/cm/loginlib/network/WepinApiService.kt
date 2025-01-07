package com.wepin.cm.loginlib.network

import com.wepin.cm.loginlib.types.OAuthTokenRequest
import com.wepin.cm.loginlib.types.network.LoginOauthAccessTokenRequest
import com.wepin.cm.loginlib.types.network.LoginOauthIdTokenRequest
import com.wepin.cm.loginlib.types.network.LoginRequest
import com.wepin.cm.loginlib.types.network.PasswordStateRequest
import com.wepin.cm.loginlib.types.network.VerifyRequest
import com.wepin.cm.loginlib.types.network.firebase.EmailAndPasswordRequest
import com.wepin.cm.loginlib.types.network.firebase.GetCurrentUserRequest
import com.wepin.cm.loginlib.types.network.firebase.GetRefreshIdTokenRequest
import com.wepin.cm.loginlib.types.network.firebase.ResetPasswordRequest
import com.wepin.cm.loginlib.types.network.firebase.SignInWithCustomTokenRequest
import com.wepin.cm.loginlib.types.network.firebase.UpdatePasswordRequest
import com.wepin.cm.loginlib.types.network.firebase.VerifyEmailRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.contentType
import io.ktor.http.path

interface WepinApiService {
    suspend fun getInfo(): HttpResponse

    suspend fun getFirebaseConfig(): HttpResponse

    suspend fun login(loginRequest: LoginRequest): HttpResponse

    suspend fun getUserPasswordState(email: String): HttpResponse

    suspend fun updateUserPasswordState(
        accessToken: String,
        userId: String,
        passwordStateRequest: PasswordStateRequest,
    ): HttpResponse

    suspend fun checkEmailExist(email: String): HttpResponse

    suspend fun oauthTokenRequest(
        provider: String,
        params: OAuthTokenRequest,
    ): HttpResponse

    suspend fun loginOauthIdToken(loginOauthIdTokenRequest: LoginOauthIdTokenRequest): HttpResponse

    suspend fun loginOauthAccessToken(loginOauthAccessTokenRequest: LoginOauthAccessTokenRequest): HttpResponse

    suspend fun verify(verifyRequest: VerifyRequest): HttpResponse

    suspend fun getAccessToken(
        userId: String,
        refreshToken: String,
    ): HttpResponse

    suspend fun logout(accessToken: String?, userId: String): HttpResponse

    suspend fun getRegex(): HttpResponse
    suspend fun getOAuthProviderInfoList(): HttpResponse
}

fun createWepinApiService(okHttpClient: HttpClient): WepinApiService =
    object : WepinApiService {
        override suspend fun getInfo(): HttpResponse {
            return okHttpClient.get("app/info")
        }

        override suspend fun getFirebaseConfig(): HttpResponse {
            return okHttpClient.get("user/firebase-config")
        }

        override suspend fun login(loginRequest: LoginRequest): HttpResponse {
            return okHttpClient.post("user/login") {
                contentType(Json)
                setBody(loginRequest)
            }
        }

        override suspend fun getUserPasswordState(email: String): HttpResponse {
            return okHttpClient.get("user/password-state") {
                url {
                    parameters.append("email", email)
                }
            }
        }

        override suspend fun updateUserPasswordState(
            accessToken: String,
            userId: String,
            passwordStateRequest: PasswordStateRequest,
        ): HttpResponse {
            return okHttpClient.patch("user/$userId/password-state") {
                headers {
                    append("Authorization", "Bearer $accessToken")
                }
            }
        }

        override suspend fun checkEmailExist(email: String): HttpResponse {
            return okHttpClient.get("user/check-user") {
                url {
                    parameters.append("email", email)
                }
            }
        }

        override suspend fun oauthTokenRequest(
            provider: String,
            params: OAuthTokenRequest,
        ): HttpResponse {
            return okHttpClient.post("user/oauth/token/$provider") {
                contentType(Json)
                setBody(params)
            }
        }

        override suspend fun loginOauthIdToken(loginOauthIdTokenRequest: LoginOauthIdTokenRequest): HttpResponse {
            return okHttpClient.post("user/oauth/login/id-token") {
                contentType(Json)
                setBody(loginOauthIdTokenRequest)
            }
        }

        override suspend fun loginOauthAccessToken(loginOauthAccessTokenRequest: LoginOauthAccessTokenRequest): HttpResponse {
            return okHttpClient.post("user/oauth/login/access-token") {
                contentType(Json)
                setBody(loginOauthAccessTokenRequest)
            }
        }

        override suspend fun verify(verifyRequest: VerifyRequest): HttpResponse {
            return okHttpClient.post("user/verify") {
                contentType(Json)
                setBody(verifyRequest)
            }
        }

        override suspend fun getAccessToken(
            userId: String,
            refreshToken: String,
        ): HttpResponse {
            return okHttpClient.get("user/access-token") {
                url {
                    parameters.append("userId", userId)
                    parameters.append("refresh_token", refreshToken)
                }
            }
        }

        override suspend fun logout(accessToken: String?, userId: String): HttpResponse {
            return okHttpClient.post("user/$userId/logout") {
                headers {
                    append("Authorization", "Bearer $accessToken")
                }
            }
        }

        override suspend fun getRegex(): HttpResponse {
            return okHttpClient.get("user/regex")
        }

        override suspend fun getOAuthProviderInfoList(): HttpResponse {
            return okHttpClient.get("user/oauth-provider")
        }
    }

interface WepinFirebaseApiService {
    suspend fun signInWithCustomToken(customToken: String): HttpResponse

    suspend fun signInWithEmailPassword(signInRequest: EmailAndPasswordRequest): HttpResponse

    suspend fun getCurrentUser(getCurrentUserRequest: GetCurrentUserRequest): HttpResponse

    suspend fun getRefreshIdToken(getRefreshIdTokenRequest: GetRefreshIdTokenRequest): HttpResponse

    suspend fun resetPassword(resetPasswordRequest: ResetPasswordRequest): HttpResponse

    suspend fun verifyEmail(verifyEmailRequest: VerifyEmailRequest): HttpResponse

    suspend fun updatePassword(
        idToken: String,
        password: String,
    ): HttpResponse
}

fun createWepinFirebaseApiService(
    firebaseKey: String,
    okHttpClient: HttpClient,
): WepinFirebaseApiService =
    object : WepinFirebaseApiService {
//        private var firebaseKey: String = firebaseKey

        override suspend fun signInWithCustomToken(customToken: String): HttpResponse {
            val signInRequest = SignInWithCustomTokenRequest(
                token = customToken,
                returnSecureToken = true
            )
            return okHttpClient.post {
                url {
                    path("accounts:signInWithCustomToken")
                    parameters.append("key", firebaseKey)
                }
                contentType(Json)
                setBody(signInRequest)
            }
        }

        override suspend fun signInWithEmailPassword(signInRequest: EmailAndPasswordRequest): HttpResponse {
            return okHttpClient.post {
                url {
                    path("accounts:signInWithPassword")
                    parameters.append("key", firebaseKey)
                }
                contentType(Json)
                setBody(signInRequest)
            }
        }

        override suspend fun getCurrentUser(getCurrentUserRequest: GetCurrentUserRequest): HttpResponse {
            return okHttpClient.post {
                url {
                    path("accounts:lookup")
                    parameters.append("key", firebaseKey)
                }
                contentType(Json)
                setBody(getCurrentUserRequest)
            }
        }

        override suspend fun getRefreshIdToken(getRefreshIdTokenRequest: GetRefreshIdTokenRequest): HttpResponse {
            return okHttpClient.post {
                url {
                    path("token")
                    parameters.append("key", firebaseKey)
                }
                contentType(Json)
                setBody(getRefreshIdTokenRequest)
            }
        }

        override suspend fun resetPassword(resetPasswordRequest: ResetPasswordRequest): HttpResponse {
            return okHttpClient.post {
                url {
                    path("accounts:resetPassword")
                    parameters.append("key", firebaseKey)
                }
                contentType(Json)
                setBody(resetPasswordRequest)
            }
        }

        override suspend fun verifyEmail(verifyEmailRequest: VerifyEmailRequest): HttpResponse {
            return okHttpClient.post {
                url {
                    path("accounts:update")
                    parameters.append("key", firebaseKey)
                }
                contentType(Json)
                setBody(verifyEmailRequest)
            }
        }

        override suspend fun updatePassword(
            idToken: String,
            password: String,
        ): HttpResponse {
            return okHttpClient.post {
                url {
                    path("accounts:update")
                    parameters.append("key", firebaseKey)
                }
                contentType(Json)
                setBody(UpdatePasswordRequest(idToken, password, returnSecureToken = true))
            }
        }
    }
