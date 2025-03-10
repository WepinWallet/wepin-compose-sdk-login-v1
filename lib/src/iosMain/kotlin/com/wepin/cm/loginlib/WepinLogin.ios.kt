package com.wepin.cm.loginlib

import com.wepin.cm.loginlib.error.WepinError
import com.wepin.cm.loginlib.manager.WepinLoginManager
import com.wepin.cm.loginlib.network.WepinNetworkManager
import com.wepin.cm.loginlib.storage.WepinStorageManager
import com.wepin.cm.loginlib.types.FBToken
import com.wepin.cm.loginlib.types.LoginOauth2Params
import com.wepin.cm.loginlib.types.LoginOauthResult
import com.wepin.cm.loginlib.types.LoginResult
import com.wepin.cm.loginlib.types.LoginWithEmailParams
import com.wepin.cm.loginlib.types.Providers
import com.wepin.cm.loginlib.types.StorageDataType
import com.wepin.cm.loginlib.types.WepinLoginOptions
import com.wepin.cm.loginlib.types.WepinUser
import com.wepin.cm.loginlib.types.network.CheckEmailExistResponse
import com.wepin.cm.loginlib.types.network.LoginOauthAccessTokenRequest
import com.wepin.cm.loginlib.types.network.LoginOauthIdTokenRequest
import platform.AuthenticationServices.*
import cocoapods.AppAuth.*
import cocoapods.secp256k1.*
import com.wepin.cm.loginlib.error.extractNSError
import com.wepin.cm.loginlib.error.getOauthErrorMessage
import com.wepin.cm.loginlib.error.getOauthErrorCode
import com.wepin.cm.loginlib.network.KtorWepinClient.closeAllClients
import com.wepin.cm.loginlib.types.OAuthTokenRequest
import com.wepin.cm.loginlib.types.OauthTokenType
import com.wepin.cm.loginlib.types.network.LoginOauthIdTokenResponse
import com.wepin.cm.loginlib.types.network.firebase.GetRefreshIdTokenRequest
import com.wepin.cm.loginlib.utils.customURLEncode
import io.ktor.util.InternalAPI
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.CoreCrypto.CC_SHA256
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIViewController
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual class WepinLogin actual constructor(wepinLoginOptions: WepinLoginOptions) {
    private var _isInitialized: Boolean = false
    private var _appId: String? = wepinLoginOptions.appId
    private var _appKey: String? = wepinLoginOptions.appKey
    private var _wepinLoginManager: WepinLoginManager = WepinLoginManager.getInstance()
    private var _wepinNetworkManager: WepinNetworkManager? = null
    private val viewController: UIViewController = wepinLoginOptions.context as UIViewController

    actual suspend fun init(): Boolean {
        if (_isInitialized) {
            throw WepinError.ALREADY_INITIALIZED_ERROR
        }

        val packageName = NSBundle.mainBundle.bundleIdentifier ?: ""
        _wepinLoginManager.init(_appKey!!, _appId!!, packageName)
        _wepinNetworkManager = _wepinLoginManager.wepinNetworkManager

        val firebaseKey = _wepinNetworkManager!!.getFirebaseConfig()
        _wepinLoginManager.setFirebase(firebaseKey)

        val providerList = _wepinNetworkManager!!.getOAuthProviderInfo()
        _wepinLoginManager.setOAuthProviderInfoList(providerList)
        _wepinLoginManager.regex = _wepinNetworkManager!!.getRegex()

        WepinStorageManager.init("null", _appId!!)
        checkExistWepinLoginSession()

        _isInitialized = _wepinNetworkManager?.getAppInfo() == true
        return _isInitialized
    }

    actual fun isInitialized(): Boolean {
        return _isInitialized
    }

    actual suspend fun loginWithEmailAndPassword(params: LoginWithEmailParams): LoginResult {
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }

        if (params.email.isEmpty() || !_wepinLoginManager.regex!!.validateEmail(email = params.email)) {
            throw WepinError.INCORRECT_EMAIL_FORM
        }

        if (params.password.isEmpty() || !_wepinLoginManager.regex!!.validatePassword(password = params.password)) {
            throw WepinError.INCORRECT_PASSWORD_FORM
        }
        val checkEmailResponse: CheckEmailExistResponse?
        try {
            checkEmailResponse = _wepinNetworkManager?.checkEmailExist(params.email)
        } catch (e: Exception) {
            if (e is WepinError) {
                throw e
            }
            throw WepinError.generalUnKnownEx(e.toString())
        }

        if (checkEmailResponse!!.isEmailExist && checkEmailResponse.isEmailverified &&
            checkEmailResponse.providerIds.contains(
                "password",
            )
        ) {
            try {
                val result: LoginResult? =
                    _wepinLoginManager.loginHelper?.loginWithEmailAndResetPasswordState(
                        params.email,
                        params.password,
                    )
                return result ?: throw WepinError.REQUIRED_SIGNUP_EMAIL
            } catch (e: Exception) {
                if (e is WepinError) throw e
                throw WepinError.generalUnKnownEx(e.message)
            }
        }
        throw WepinError.REQUIRED_SIGNUP_EMAIL
    }

    actual fun finalize() {
        _wepinNetworkManager?.clearAuthToken()
        WepinStorageManager.deleteAllStorage()
        closeAllClients()
        _isInitialized = false
    }

    @OptIn(ExperimentalForeignApi::class, InternalAPI::class)
    actual suspend fun loginWithOauthProvider(params: LoginOauth2Params): LoginOauthResult {
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }

        if (Providers.isNotCommonProvider(params.provider)) {
            throw WepinError.INVALID_LOGIN_PROVIDER
        }

        //clear session
        _wepinNetworkManager?.clearAuthToken()
        WepinStorageManager.deleteAllStorage()

        val providerInfo = _wepinLoginManager.getOAuthProviderInfo(params.provider)
        val authorizationEndpoint = NSURL(string = providerInfo.authorizationEndpoint)
        val tokenEndpoint = NSURL(string = providerInfo.tokenEndpoint)
        val configuration =
            OIDServiceConfiguration(authorizationEndpoint as NSURL, tokenEndpoint as NSURL)

        val scope = if (params.provider == "discord") listOf("identify", OIDScopeEmail) else listOf(
            OIDScopeEmail
        )
        val scheme = "wepin.$_appId"
        val additParams = mutableMapOf("prompt" to "select_account")
        if (params.provider == "apple") {
            additParams["response_mode"] = "form_post"
        }

        val request =
            OIDAuthorizationRequest(
                configuration = configuration,
                clientId = params.clientId,
                clientSecret = null,
                scopes = scope,
                redirectURL = NSURL(string = _wepinLoginManager.appAuthRedirectUrl),
                responseType = OIDResponseTypeCode.toString(),
                additionalParameters = additParams.toMap()
            )

        val presentationContextProvider = withContext(Dispatchers.Main) {
            WepinPresentationContextProvider(viewController.view.window)
        }

        val requestUrl = request.externalUserAgentRequestURL().toString()
        val replaceUrl = requestUrl.replace(
            _wepinLoginManager.appAuthBaseUrl,
            customURLEncode(_wepinLoginManager.appAuthBaseUrl)
        )

        var callbackURL: NSURL? = null
        try {
            callbackURL =
                authenticateWithWebSession(replaceUrl, scheme, presentationContextProvider)
                    ?: throw WepinError.INVALID_TOKEN
        } catch(error: Exception) {
            if (error is WepinError) throw error

            val nsError = extractNSError(error) ?: throw WepinError.generalUnKnownEx(error.toString())
            val code = getOauthErrorCode(nsError, "authorization_failed")
            throw WepinError(WepinError.FAILED_LOGIN.code, code)
        }
        var  authResponse: OIDAuthorizationResponse? = null
        try {
            authResponse = OIDAuthorizationResponse(
                request = request,
                parameters = OIDURLQueryComponent(uRL = callbackURL).dictionaryValue
            )
        } catch(error: Exception) {
            val nsError = extractNSError(error) ?: throw error
            val code = getOauthErrorCode(nsError, "authorization_failed")
            throw WepinError(WepinError.FAILED_LOGIN.code, code)
        }

        val authState = OIDAuthState(authorizationResponse = authResponse)

        if (params.provider == "discord") {
            val authorizationCode =
                authState.lastAuthorizationResponse.authorizationCode
            val codeVerifier =
                authState.lastAuthorizationResponse.request.codeVerifier
            if (authorizationCode != null && codeVerifier != null) {
                val tokenRequest = OIDTokenRequest(
                    configuration = configuration,
                    grantType = OIDGrantTypeAuthorizationCode.toString(),
                    authorizationCode = authorizationCode,
                    redirectURL = request.redirectURL,
                    clientID = params.clientId,
                    clientSecret = null,
                    scope = request.scope,
                    refreshToken = null,
                    codeVerifier = codeVerifier,
                    additionalParameters = null
                )

                try {
                    val tokenResponse = authenticateByDiscord(tokenRequest)
                    return LoginOauthResult(
                        provider = params.provider,
                        token = tokenResponse!!.accessToken ?: "",
                        type = OauthTokenType.ACCESS_TOKEN
                    )
                } catch (error: Exception) {
                    if (error is WepinError) throw error

                    val nsError = extractNSError(error) ?: throw error
                    val code = getOauthErrorCode(nsError, "authorization_failed")
                    throw WepinError(WepinError.FAILED_LOGIN.code, code)
                }
            } else {
                throw WepinError(WepinError.FAILED_LOGIN.code, "Missing authorization code or code verifier")
            }
        } else {
            val authorizationCode =
                authState.lastAuthorizationResponse.authorizationCode
            val codeVerifier =
                authState.lastAuthorizationResponse.request.codeVerifier
            val state = authState.lastAuthorizationResponse.state

            if (authorizationCode != null && codeVerifier != null && state != null) {
                val requestParams = OAuthTokenRequest(
                    code = authorizationCode,
                    clientId = params.clientId,
                    redirectUri = _wepinLoginManager.appAuthRedirectUrl,
                    state = state,
                    codeVerifier = codeVerifier
                )

                try {
                    val res = _wepinNetworkManager?.oauthTokenRequest(
                        provider = params.provider,
                        params = requestParams
                    )

                    if (res?.access_token == null && res?.id_token == null) {
                        throw WepinError.INVALID_TOKEN
                    }
                    return if (params.provider == "google" || params.provider == "apple") {
                        LoginOauthResult(
                            provider = params.provider,
                            token = res.id_token ?: "",
                            type = OauthTokenType.ID_TOKEN
                        )
                    } else {
                        LoginOauthResult(
                            provider = params.provider,
                            token = res.access_token ?: "",
                            type = OauthTokenType.ACCESS_TOKEN
                        )
                    }
                } catch (e: Exception) {
                    throw WepinError.INVALID_TOKEN
                }
            } else {
                throw WepinError(WepinError.FAILED_LOGIN.code, "Missing authorization code or code verifier")
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    suspend fun authenticateByDiscord(tokenRequest: OIDTokenRequest): OIDTokenResponse? {
        return suspendCoroutine { continuation ->
            OIDAuthorizationService.performTokenRequest(
                request = tokenRequest,
                callback = { tokenResponse, error ->
                    if (error != null) {
                        continuation.resumeWithException(WepinError(WepinError.FAILED_LOGIN.code, getOauthErrorCode(error, "authentication_failed" + " - " + getOauthErrorMessage(error))))
                    } else if (tokenResponse != null) {
                        continuation.resume(tokenResponse)
                    }
                }
            )
        }
    }

    private suspend fun authenticateWithWebSession(
        authURL: String,
        callbackURLScheme: String,
        presentationContextProvider: WepinPresentationContextProvider
    ): NSURL? {
        return suspendCancellableCoroutine { continuation ->
            val session = ASWebAuthenticationSession(
                uRL = NSURL(string = authURL),
                callbackURLScheme = callbackURLScheme,
                completionHandler = { callbackURL, error ->
                    if (callbackURL != null) {
                        continuation.resume(callbackURL)
                    } else if (error != null) {
                        continuation.resumeWithException(WepinError(WepinError.FAILED_LOGIN.code, getOauthErrorCode(error, "authentication_failed" + " - " + getOauthErrorMessage(error))))
                    }
                })
            session.presentationContextProvider = presentationContextProvider

            session.start()

            continuation.invokeOnCancellation {
                session.cancel()
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
    actual fun getSignForLogin(
        privateKeyHex: String,
        message: String,
    ): String {
        val privateKeyData = privateKeyHex.hexToUByteArray()

        val ctx = secp256k1_context_create(SECP256K1_CONTEXT_SIGN.toUInt()) ?: return ""

        val privateKey = privateKeyData.refTo(0)
        val verify = secp256k1_ec_seckey_verify(ctx, privateKey)
        if (verify != 1) {
            secp256k1_context_destroy(ctx)
            return ""
        }

        val hash = sha256(message)

        val signatureString: String = memScoped {
            val signatureMem = allocArray<secp256k1_ecdsa_signature>(1)
            val signature = signatureMem[0]
            val result =
                secp256k1_ecdsa_sign(ctx, signature.ptr, hash.refTo(0), privateKey, null, null)
            if (result == 0) {
                secp256k1_context_destroy(ctx)
                return@memScoped ""
            }
            val signatureSerialized = allocArray<UByteVar>(64)
            secp256k1_ecdsa_signature_serialize_compact(ctx, signatureSerialized, signature.ptr)

            val signatureByteArray = ByteArray(64) { index ->
                signatureSerialized[index].toByte()
            }

            secp256k1_context_destroy(ctx)
            signatureByteArray.joinToString(separator = "") { it.toHexString() }
        }
        return signatureString
    }

    @OptIn(ExperimentalForeignApi::class)
    fun sha256(input: String): UByteArray {
        memScoped {
            val digestLength = 32
            val data = input.cstr.ptr
            val dataLength = input.length.toULong()
            val hash = UByteArray(digestLength)

            hash.usePinned {
                CC_SHA256(data, dataLength.convert(), it.addressOf(0))
            }
            return hash
        }
    }

    actual suspend fun loginWithAccessToken(params: LoginOauthAccessTokenRequest): LoginResult {
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }
        if (Providers.isNotAccessTokenProvider(params.provider)) {
            throw WepinError.INVALID_LOGIN_PROVIDER
        }

        _wepinNetworkManager?.clearAuthToken()
        WepinStorageManager.deleteAllStorage()
        val loginResponse = _wepinNetworkManager?.loginOAuthAccessToken(params)
        if (loginResponse!!.token !== null) {
            if (loginResponse != null) {
                return _wepinLoginManager.loginHelper?.doFirebaseLoginWithCustomToken(
                    loginResponse.token!!,
                    Providers.EXTERNAL_TOKEN,
                )!!
            } else {
                throw WepinError.INVALID_TOKEN
            }
        }
        return LoginResult(
            provider = Providers.EXTERNAL_TOKEN,
            token = FBToken(idToken = "", refreshToken = ""),
        )
    }

    actual suspend fun loginWithIdToken(params: LoginOauthIdTokenRequest): LoginResult {
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }
        if (_wepinNetworkManager == null) {
            throw WepinError.NOT_INITIALIZED_NETWORK
        }
        _wepinNetworkManager?.clearAuthToken()
        WepinStorageManager.deleteAllStorage()
        val loginResponse = _wepinNetworkManager?.loginOAuthIdToken(params)
        try {
            if (loginResponse!!.token !== null) {
                return _wepinLoginManager.loginHelper?.doFirebaseLoginWithCustomToken(
                    loginResponse!!.token!!,
                    Providers.EXTERNAL_TOKEN,
                )!!
            } else {
                throw WepinError.INVALID_TOKEN
            }
        } catch (e: Exception) {
            throw WepinError.NOT_INITIALIZED_NETWORK
        }
    }

    private suspend fun checkExistWepinLoginSession(): Boolean {
        val token = WepinStorageManager.getStorage<StorageDataType>("wepin:connectUser")
        val userId = WepinStorageManager.getStorage<String>("user_id")

        if (token != null && userId != null) {
            _wepinNetworkManager?.setAuthToken(
                (token as StorageDataType.WepinToken).accessToken,
                (token.refreshToken),
            )
            try {
                val accessToken = _wepinNetworkManager?.getAccessToken(userId as String)

                WepinStorageManager.setStorage(
                    "wepin:connectUser",
                    StorageDataType.WepinToken(
                        accessToken = accessToken!!.token,
                        refreshToken = (token as StorageDataType.WepinToken).refreshToken,
                    ),
                )
                _wepinNetworkManager?.setAuthToken(accessToken.token, token.refreshToken)
                return true
            } catch (e: Exception) {
                _wepinNetworkManager?.clearAuthToken()
                WepinStorageManager.deleteAllStorage()
                return true
            }
        } else {
            _wepinNetworkManager?.clearAuthToken()
            WepinStorageManager.deleteAllStorage()
            return true
        }
    }

    actual suspend fun loginWepin(params: LoginResult): WepinUser? {
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }

        if (params.token.idToken.isEmpty() || params.token.refreshToken.isEmpty()) {
            throw WepinError.INVALID_PARAMETER
        }

        if (_wepinNetworkManager == null || _wepinLoginManager.wepinFirebaseManager == null) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }

        try {
            val loginResponse = _wepinLoginManager.wepinNetworkManager?.login(params.token.idToken)
            WepinStorageManager.setWepinUser(params, loginResponse!!)
            return WepinStorageManager.getWepinUser()
        } catch (e: Exception) {
            if (e is WepinError) throw e
            throw WepinError.generalUnKnownEx(e.toString())
        }
    }

    actual suspend fun logoutWepin(): Boolean {
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }

        val userId = WepinStorageManager.getStorage<String>("user_id") ?: throw WepinError.ALREADY_LOGOUT

        try {
            val loginResponse = _wepinLoginManager.wepinNetworkManager?.logout(userId as String)
            WepinStorageManager.deleteAllStorage()
            return loginResponse!!
        } catch (error: Exception) {
            if (error is WepinError) throw error
            throw WepinError.generalUnKnownEx(error.toString())
        }
    }

    actual suspend fun signUpWithEmailAndPassword(params: LoginWithEmailParams): LoginResult {
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }
        if (_wepinNetworkManager == null) {
            throw WepinError.NOT_INITIALIZED_NETWORK
        }
        //clear session
        _wepinNetworkManager?.clearAuthToken()
        WepinStorageManager.deleteAllStorage()

        if (params.email.isEmpty() || !_wepinLoginManager.regex!!.validateEmail(email = params.email)) {
            throw WepinError.INCORRECT_EMAIL_FORM
        }

        if (params.password.isEmpty() || !_wepinLoginManager.regex!!.validatePassword(password = params.password)) {
            throw WepinError.INCORRECT_PASSWORD_FORM
        }

        try {
            val checkEmail = _wepinNetworkManager?.checkEmailExist(email = params.email)

            if (checkEmail?.isEmailExist == true && checkEmail.isEmailverified && checkEmail.providerIds.contains(
                    "password"
                )
            ) {
                throw WepinError.EXISTED_EMAIL
            } else {
                return _wepinLoginManager.loginHelper?.verifySignUpFirebase(params)!!
            }
        } catch (error: Exception) {
            if (error is WepinError) throw error
            throw WepinError.generalUnKnownEx(error.toString())
        }
    }

    actual suspend fun getRefreshFirebaseToken(): LoginResult {
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }
        val firebaseToken = WepinStorageManager.getStorage<StorageDataType>("firebase:wepin")

        if (firebaseToken != null) {
            val provider = (firebaseToken as StorageDataType.FirebaseWepin).provider
            val refreshToken = firebaseToken.refreshToken
            try {
                val refreshResponse = _wepinLoginManager.wepinFirebaseManager?.getRefreshIdToken(
                    GetRefreshIdTokenRequest(refreshToken)
                )

                val token = FBToken(
                    idToken = refreshResponse!!.id_token,
                    refreshToken = refreshResponse.refresh_token
                )
                val loginResult = LoginResult(
                    provider = Providers.fromValue(provider)!!,
                    token = token
                )
                WepinStorageManager.setFirebaseUser(
                    idToken = token.idToken,
                    refreshToken = token.refreshToken,
                    providers = loginResult.provider
                )
                return loginResult
            } catch (e: Exception) {
                if (e is WepinError) throw e
                throw WepinError.generalUnKnownEx(e.toString())
            }
        } else {
            throw WepinError.INVALID_LOGIN_SESSION
        }
    }

    actual suspend fun getCurrentWepinUser(): WepinUser? {
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }
        checkExistWepinLoginSession()
        return WepinStorageManager.getWepinUser()
    }

    actual suspend fun loginFirebaseWithOauthProvider(params: LoginOauth2Params): LoginResult? {
        var oauthRes: LoginOauthResult? = null
        try {
            oauthRes = loginWithOauthProvider(params)
        } catch(error: Exception) {
            if (error is WepinError) {
                throw error
            } else {
                throw WepinError(WepinError.FAILED_LOGIN.code, "failed oauth login")
            }
        }
        //clear session
        _wepinNetworkManager?.clearAuthToken()
        WepinStorageManager.deleteAllStorage()

        var res: LoginOauthIdTokenResponse? = null

        if (oauthRes.type == OauthTokenType.ID_TOKEN) {
            val loginOauthRequest = LoginOauthIdTokenRequest(idToken = oauthRes.token)
            val result = _wepinNetworkManager?.loginOAuthIdToken(loginOauthRequest)
            res = result
        } else {
            val loginOauthRequest = LoginOauthAccessTokenRequest(provider = params.provider, accessToken = oauthRes.token)
            res = _wepinNetworkManager?.loginOAuthAccessToken((loginOauthRequest))
        }
        return _wepinLoginManager.loginHelper?.doFirebaseLoginWithCustomToken(res?.token!!, Providers.fromValue(params.provider)!!)
    }

    actual suspend fun loginWepinWithOauthProvider(params: LoginOauth2Params): WepinUser? {
        val firebaseRes = loginFirebaseWithOauthProvider(params)
        if (firebaseRes == null) {
            throw WepinError(WepinError.FAILED_LOGIN.code, "failed oauth firebase login")
        }
        return loginWepin(firebaseRes)
    }

    actual suspend fun loginWepinWithIdToken(params: LoginOauthIdTokenRequest): WepinUser? {
        val firebaseRes = loginWithIdToken(params)
        return loginWepin(firebaseRes)
    }

    actual suspend fun loginWepinWithAccessToken(params: LoginOauthAccessTokenRequest): WepinUser? {
        val firebaseRes = loginWithAccessToken(params)
        return loginWepin(firebaseRes)
    }

    actual suspend fun loginWepinWithEmailAndPassword(params: LoginWithEmailParams): WepinUser? {
        val firebaseRes = loginWithEmailAndPassword(params)
        return loginWepin(firebaseRes)
    }

}
