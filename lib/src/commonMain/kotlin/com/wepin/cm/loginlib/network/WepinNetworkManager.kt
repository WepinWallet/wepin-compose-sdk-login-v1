package com.wepin.cm.loginlib.network

import com.wepin.cm.loginlib.error.WepinError
import com.wepin.cm.loginlib.types.ErrorCode
import com.wepin.cm.loginlib.types.GetAccessTokenResponse
import com.wepin.cm.loginlib.types.KeyType
import com.wepin.cm.loginlib.types.OAuthProviderInfo
import com.wepin.cm.loginlib.types.OAuthTokenRequest
import com.wepin.cm.loginlib.types.OAuthTokenResponse
import com.wepin.cm.loginlib.types.WepinLoginError
import com.wepin.cm.loginlib.types.WepinRegex
import com.wepin.cm.loginlib.types.network.AppInfoResponse
import com.wepin.cm.loginlib.types.network.CheckEmailExistResponse
import com.wepin.cm.loginlib.types.network.LoginOauthAccessTokenRequest
import com.wepin.cm.loginlib.types.network.LoginOauthIdTokenRequest
import com.wepin.cm.loginlib.types.network.LoginOauthIdTokenResponse
import com.wepin.cm.loginlib.types.network.LoginRequest
import com.wepin.cm.loginlib.types.network.LoginResponse
import com.wepin.cm.loginlib.types.network.PasswordStateRequest
import com.wepin.cm.loginlib.types.network.PasswordStateResponse
import com.wepin.cm.loginlib.types.network.VerifyRequest
import com.wepin.cm.loginlib.types.network.VerifyResponse
import com.wepin.cm.loginlib.utils.decodeBase64
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class WepinNetworkManager(appKey: String, domain: String, version: String) {
    var wepinBaseUrl: String? = null
    private var appKey: String? = null
    private var domain: String? = null
    private var version: String? = null
    private var accessToken: String? = null
    private var refreshToken: String? = null

    private var httpClient: HttpClient? = null
    private var wepinApiService: WepinApiService? = null

    init {
        wepinBaseUrl = getSdkUrl(appKey)
        this.appKey = appKey
        this.domain = domain
        this.version = version

        httpClient = KtorWepinClient.createHttpClient(wepinBaseUrl!!, appKey, domain, version)
        wepinApiService = createWepinApiService(httpClient!!)
    }

    suspend fun getAppInfo(): Boolean {
        val result =
            withContext(Dispatchers.IO) {
                val response: HttpResponse = wepinApiService!!.getInfo()
                if (response.status.equals(HttpStatusCode.OK)) {
                    AppInfoResponse.Success(response)
                    true
                } else {
                    false
                }
            }
        return result
    }

    suspend fun getFirebaseConfig(): String {
        val result =
            withContext(Dispatchers.IO) {
                val response = wepinApiService!!.getFirebaseConfig()
                if (response.status.equals(HttpStatusCode.OK)) {
                    val decodeString = response.bodyAsText().let { decodeBase64(it) }
                    val jsonObject = Json.parseToJsonElement(decodeString).jsonObject
                    val key = jsonObject["apiKey"].toString().replace("\"", "")

                    key
                } else {
                    ""
                }
            }.toString()
        return result
    }

    suspend fun checkEmailExist(email: String): CheckEmailExistResponse {
        val result: CheckEmailExistResponse =
            withContext(Dispatchers.IO) {
                val response = wepinApiService!!.checkEmailExist(email)
                if (response.status.equals(HttpStatusCode.OK)) {
                    val data: CheckEmailExistResponse = response.body()
                    data
                } else {
                    throw WepinError(WepinError.API_REQUEST_ERROR.code, "code: ${response.status.value}, body: ${response.bodyAsText()}")
                }
            }
        return result
    }

    suspend fun getUserPasswordState(email: String): PasswordStateResponse {
        val result: PasswordStateResponse =
            withContext(Dispatchers.IO) {
                val response = wepinApiService!!.getUserPasswordState(email)
                if (response.status.equals(HttpStatusCode.OK)) {
                    val data: PasswordStateResponse = response.body()
                    data
                } else {
                    throw WepinError(WepinError.API_REQUEST_ERROR.code, "code: ${response.status.value}, body: ${response.bodyAsText()}")
                }
            }
        return result
    }

    suspend fun login(idToken: String): LoginResponse {
        val result =
            withContext(Dispatchers.IO) {
                val response = wepinApiService!!.login(LoginRequest(idToken))
                if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                    val data = response.body<LoginResponse>()

                    data
                } else {
                    throw WepinError(WepinError.API_REQUEST_ERROR.code, "code: ${response.status.value}, body: ${response.bodyAsText()}")
                }
            }
        accessToken = result.token.access
        refreshToken = result.token.refresh
        return result
    }

    suspend fun loginOAuthIdToken(params: LoginOauthIdTokenRequest): LoginOauthIdTokenResponse {
        val result: LoginOauthIdTokenResponse =
            withContext(Dispatchers.IO) {
                val response = wepinApiService!!.loginOauthIdToken(params)
                if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                    val data = response.body<LoginOauthIdTokenResponse>()
                    if (!data.result) throw WepinError.generalUnKnownEx("loginOAuthIdToken result fail")
                    data
                } else {
                    throw WepinError(WepinError.API_REQUEST_ERROR.code, "code: ${response.status.value}, body: ${response.bodyAsText()}")
                }
            }
        return result
    }

    suspend fun loginOAuthAccessToken(params: LoginOauthAccessTokenRequest): LoginOauthIdTokenResponse {
        val result: LoginOauthIdTokenResponse =
            withContext(Dispatchers.IO) {
                val response = wepinApiService!!.loginOauthAccessToken(params)
                if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                    val data = response.body<LoginOauthIdTokenResponse>()
                    data
                } else {
                    throw WepinError(WepinError.API_REQUEST_ERROR.code, "code: ${response.status.value}, body: ${response.bodyAsText()}")
                }
            }
        return result
    }

    suspend fun updateUserPasswordState(
        userId: String,
        passwordStateRequest: PasswordStateRequest,
    ): PasswordStateResponse {
        val result: PasswordStateResponse =
            withContext(Dispatchers.IO) {
                val response = wepinApiService!!.updateUserPasswordState(accessToken!!, userId, passwordStateRequest)
                if (response.status.equals(HttpStatusCode.OK)) {
                    val data = response.body<PasswordStateResponse>()
                    data
                } else {
                    throw WepinError(WepinError.API_REQUEST_ERROR.code, "code: ${response.status.value}, body: ${response.bodyAsText()}")
                }
            }
        return result
    }

    suspend fun oauthTokenRequest(
        provider: String,
        params: OAuthTokenRequest,
    ): OAuthTokenResponse  {
        val result: OAuthTokenResponse =
            withContext(Dispatchers.IO) {
                val response = wepinApiService!!.oauthTokenRequest(provider, params)
                if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                    val data = response.body<OAuthTokenResponse>()
                    data
                } else {
                    throw WepinError(WepinError.API_REQUEST_ERROR.code, "code: ${response.status.value}, body: ${response.bodyAsText()}")
                }
            }
        return result
    }

    suspend fun getAccessToken(userId: String): GetAccessTokenResponse {
        val result =
            withContext(Dispatchers.IO) {
                val response = wepinApiService!!.getAccessToken(userId, refreshToken!!)
                if (response.status == HttpStatusCode.OK) {
                    val data = response.body<GetAccessTokenResponse>()
                    data
                } else {
                    throw WepinError(WepinError.API_REQUEST_ERROR.code, "code: ${response.status.value}, body: ${response.bodyAsText()}")
                }
            }
        return result
    }

    suspend fun logout(userId: String): Boolean {
        val result = withContext(Dispatchers.IO) {
            val response = wepinApiService!!.logout(accessToken, userId)
            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                clearAuthToken()
                true
            } else {
                false
            }
        }
        return result
    }

    suspend fun getRegex(): WepinRegex {
        val result = withContext(Dispatchers.IO) {
            val response = wepinApiService!!.getRegex()
            if (response.status == HttpStatusCode.OK) {
                val data = response.body<WepinRegex.RegexConfig>()
                WepinRegex(data)
            } else {
                throw WepinError(WepinError.API_REQUEST_ERROR.code, "code: ${response.status.value}, body: ${response.bodyAsText()}")
            }
        }
        return result
    }

    suspend fun getOAuthProviderInfo(): Array<OAuthProviderInfo> {
        val result = withContext(Dispatchers.IO) {
            val response = wepinApiService!!.getOAuthProviderInfoList()
            if (response.status == HttpStatusCode.OK) {
                val data = response.body<Array<OAuthProviderInfo>>()
                data
            } else {
                throw WepinError(WepinError.API_REQUEST_ERROR.code, "code: ${response.status.value}, body: ${response.bodyAsText()}")
            }
        }
        return result
    }

    suspend fun verify(params: VerifyRequest): VerifyResponse {
        val result = withContext(Dispatchers.IO) {
            val response = wepinApiService!!.verify(params)
            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                val data = response.body<VerifyResponse>()
                data
            } else {
                throw WepinError(WepinError.API_REQUEST_ERROR.code, "code: ${response.status.value}, body: ${response.bodyAsText()}")
            }
        }
        return result
    }

    internal fun setAuthToken(
        accessToken: String,
        refreshToken: String,
    ) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    internal fun clearAuthToken()  {
        this.accessToken = null
        this.refreshToken = null
    }

    private fun getSdkUrl(apiKey: String): String {
        return when (KeyType.fromAppKey(apiKey)) {
            KeyType.DEV -> {
                "https://dev-sdk.wepin.io/v1/"
            }

            KeyType.STAGE -> {
                "https://stage-sdk.wepin.io/v1/"
            }

            KeyType.PROD -> {
                "https://sdk.wepin.io/v1/"
            }

            else -> {
                throw WepinError.INVALID_APP_KEY
            }
        }
    }
}
