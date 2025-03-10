package com.wepin.cm.loginlib

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wepin.cm.loginlib.error.WepinError
import com.wepin.cm.loginlib.manager.WepinLoginManager
import com.wepin.cm.loginlib.network.KtorWepinClient.closeAllClients
import com.wepin.cm.loginlib.network.WepinNetworkManager
import com.wepin.cm.loginlib.storage.WepinStorageManager
import com.wepin.cm.loginlib.types.FBToken
import com.wepin.cm.loginlib.types.LoginOauth2Params
import com.wepin.cm.loginlib.types.LoginOauthResult
import com.wepin.cm.loginlib.types.LoginResult
import com.wepin.cm.loginlib.types.LoginWithEmailParams
import com.wepin.cm.loginlib.types.OauthTokenType
import com.wepin.cm.loginlib.types.Providers
import com.wepin.cm.loginlib.types.StorageDataType
import com.wepin.cm.loginlib.types.WepinLoginOptions
import com.wepin.cm.loginlib.types.WepinUser
import com.wepin.cm.loginlib.types.network.CheckEmailExistResponse
import com.wepin.cm.loginlib.types.network.LoginOauthAccessTokenRequest
import com.wepin.cm.loginlib.types.network.LoginOauthIdTokenRequest
import com.wepin.cm.loginlib.types.network.LoginOauthIdTokenResponse
import com.wepin.cm.loginlib.types.network.firebase.GetRefreshIdTokenRequest
import com.wepin.cm.loginlib.utils.bigIntegerToByteArrayTrimmed
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.bouncycastle.util.encoders.Hex
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class WepinLogin actual constructor(wepinLoginOptions: WepinLoginOptions) {
    private var _contex: Context? = wepinLoginOptions.context as Context
    private var _isInitialized: Boolean = false
    private var _appId: String? = wepinLoginOptions.appId
    private var _appKey: String? = wepinLoginOptions.appKey
    private var _wepinLoginManager: WepinLoginManager = WepinLoginManager.getInstance()
    private var _wepinNetworkManager: WepinNetworkManager? = null

    actual suspend fun init(): Boolean {
        if (_isInitialized) {
            throw WepinError.ALREADY_INITIALIZED_ERROR
        }
        if (_contex === null || _contex !is Activity) {
            throw WepinError.NOT_ACTIVITY
        }
        val packageName = (_contex as Activity).packageName
        _wepinLoginManager.init(_appKey!!, _appId!!, packageName)
        _wepinNetworkManager = _wepinLoginManager.wepinNetworkManager
        val firebaseKey = _wepinNetworkManager!!.getFirebaseConfig()
        _wepinLoginManager.setFirebase(firebaseKey)

        val providerList = _wepinNetworkManager!!.getOAuthProviderInfo()
        _wepinLoginManager.setOAuthProviderInfoList(providerList)
        _wepinLoginManager.regex = _wepinNetworkManager!!.getRegex()

        WepinStorageManager.init(_contex as Activity, _appId!!)
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
        Log.d("loginWithEmailAndPassword", "loginWithEmailAndPassword")

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
            if (e as Any? is WepinError) {
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
                return result ?: throw WepinError.NOT_INITIALIZED_NETWORK
            } catch (e: Exception) {
                if (e as Any is WepinError) {
                    throw e
                }
                throw WepinError.generalUnKnownEx(e.message)
            }
        } else {
            throw WepinError.REQUIRED_SIGNUP_EMAIL
        }
    }

    actual suspend fun loginWithOauthProvider(params: LoginOauth2Params): LoginOauthResult {
        _wepinLoginManager.loginResultManager?.initLoginResult()
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }
        if (_contex === null || _contex !is Activity) {
            throw WepinError.NOT_ACTIVITY
        }
//        if (!WepinNetworkManager.isInternetAvailable)
        if (Providers.isNotCommonProvider(params.provider)) {
            throw WepinError.INVALID_LOGIN_PROVIDER
        }

        val intent =
            Intent(_contex, WepinLoginMainActivity::class.java).apply {
                putExtra("provider", params.provider)
                putExtra("clientId", params.clientId)
            }

        return suspendCancellableCoroutine { continuation ->
            (_contex as Activity).startActivity(intent)
            _wepinLoginManager.loginResultManager!!.loginOauthCompletableFuture.whenComplete { result, throwable ->
                if (throwable != null) {
                    continuation.resumeWithException(throwable)
                } else {
                    continuation.resume(result)
                }
            }
        }
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
            if (e as Any is WepinError) {
                throw e
            }
            throw WepinError.NOT_INITIALIZED_NETWORK
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

        if (loginResponse != null) {
            if (loginResponse.token !== null) {
                return _wepinLoginManager.loginHelper?.doFirebaseLoginWithCustomToken(
                    loginResponse.token,
                    Providers.EXTERNAL_TOKEN,
                )!!
            } else {
                throw WepinError.INVALID_TOKEN
            }
        } else {
            throw WepinError.NOT_INITIALIZED_NETWORK
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
            if (e as Any is WepinError) {
                throw e
            }
            throw WepinError(e.message)
        }
    }

    actual suspend fun logoutWepin(): Boolean {
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }

        if (_wepinNetworkManager == null || _wepinLoginManager.wepinFirebaseManager == null) throw WepinError.ALREADY_LOGOUT

        val userId = WepinStorageManager.getStorage<String>("user_id") ?: throw WepinError.ALREADY_LOGOUT

//        return _wepinLoginManager.wepinNetworkManager?.logout(userId as String)!!
        try {
            val loginResponse = _wepinNetworkManager?.logout(userId as String)
            _wepinNetworkManager?.clearAuthToken()
            WepinStorageManager.deleteAllStorage()
            return loginResponse!!
        } catch (error: Exception) {
            if (error as Any is WepinError) {
                throw error
            }
            throw WepinError.generalUnKnownEx(error.toString())
        }
    }

    actual fun getSignForLogin(
        privateKeyHex: String,
        message: String,
    ): String {
        // 새로운 ECKey 생성
        val ecKey = ECKey.fromPrivate(Hex.decode(privateKeyHex))

        // 메시지의 SHA-256 해시 생성
        val sha256Hash = Sha256Hash.of(message.toByteArray())
        // 생성된 해시에 대한 서명 생성
        val ecdsaSignature = ecKey.sign(sha256Hash)

        // 서명을 DER 형식으로 변환
//            val derSignature = ecdsaSignature.encodeToDER()
        // 서명을 Hex 문자열로 변환 (옵션)
        val rhexSignature = Hex.toHexString(bigIntegerToByteArrayTrimmed(ecdsaSignature.r))
        val shexSignature = Hex.toHexString(bigIntegerToByteArrayTrimmed(ecdsaSignature.s))

        return rhexSignature + shexSignature
    }

    actual fun finalize() {
        WepinStorageManager.deleteAllStorage()
        closeAllClients()
        _isInitialized = false
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

    actual suspend fun signUpWithEmailAndPassword(params: LoginWithEmailParams): LoginResult {
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }
        if (params.email.isEmpty() || !_wepinLoginManager.regex!!.validateEmail(email = params.email)) {
            throw WepinError.INCORRECT_EMAIL_FORM
        }
        if (params.password.isEmpty() || !_wepinLoginManager.regex!!.validatePassword(password = params.password)) {
            throw WepinError.INCORRECT_PASSWORD_FORM
        }
        if (_wepinNetworkManager == null) {
            throw WepinError.NOT_INITIALIZED_NETWORK
        }
        try {
            val checkResponse = _wepinNetworkManager?.checkEmailExist(params.email)
            if (checkResponse!!.isEmailExist && checkResponse.isEmailverified && checkResponse.providerIds.contains(
                    "password"
                )
            ) {
                throw WepinError.EXISTED_EMAIL
            } else {
                _wepinNetworkManager?.clearAuthToken()
                WepinStorageManager.deleteAllStorage()
                return _wepinLoginManager.loginHelper?.verifySignUpFirebase(params)!!
            }
        } catch (e: Exception) {
            if (e as Any is WepinError) {
                throw e
            }
            throw WepinError.generalUnKnownEx(e.toString())
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
                if (e as Any is WepinError) {
                    throw e
                }
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
            if (error as Any is WepinError) {
                throw error
            }
            throw WepinError.generalUnKnownEx(error.toString())
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
