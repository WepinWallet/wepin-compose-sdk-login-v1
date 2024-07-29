package com.wepin.cm.loginlib

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wepin.cm.loginlib.const.RegExpConst
import com.wepin.cm.loginlib.error.WepinError
import com.wepin.cm.loginlib.manager.WepinLoginManager
import com.wepin.cm.loginlib.network.KtorWepinClient.closeAllClients
import com.wepin.cm.loginlib.network.WepinNetworkManager
import com.wepin.cm.loginlib.storage.StorageManager
import com.wepin.cm.loginlib.types.ErrorCode
import com.wepin.cm.loginlib.types.FBToken
import com.wepin.cm.loginlib.types.LoginOauth2Params
import com.wepin.cm.loginlib.types.LoginOauthResult
import com.wepin.cm.loginlib.types.LoginResult
import com.wepin.cm.loginlib.types.LoginWithEmailParams
import com.wepin.cm.loginlib.types.Providers
import com.wepin.cm.loginlib.types.StorageDataType
import com.wepin.cm.loginlib.types.WepinLoginError
import com.wepin.cm.loginlib.types.WepinLoginOptions
import com.wepin.cm.loginlib.types.WepinUser
import com.wepin.cm.loginlib.types.network.CheckEmailExistResponse
import com.wepin.cm.loginlib.types.network.LoginOauthAccessTokenRequest
import com.wepin.cm.loginlib.types.network.LoginOauthIdTokenRequest
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
            throw Exception(WepinLoginError.getError(ErrorCode.NOT_ACTIVITY))
        }
        val packageName = (_contex as Activity).packageName
        _wepinLoginManager.init(_appKey!!, _appId!!, packageName)
        _wepinNetworkManager = _wepinLoginManager.wepinNetworkManager
        val firebaseKey = _wepinNetworkManager!!.getFirebaseConfig()
        Log.d("getFirebaseConfigResult", firebaseKey)
        _wepinLoginManager.setFirebase(firebaseKey)

        StorageManager.init(_contex as Activity, _appId!!)
        StorageManager.deleteAllIfAppIdDataNotExists()
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

        if (params.email.isEmpty() || !RegExpConst.validateEmail(email = params.email)) {
            throw WepinError.INCORRECT_EMAIL_FORM
        }

        if (params.password.isEmpty() || !RegExpConst.validatePassword(password = params.password)) {
            throw WepinError.INCORRECT_PASSWORD_FORM
        }
        val checkEmailResponse: CheckEmailExistResponse?
        try {
            checkEmailResponse = _wepinNetworkManager?.checkEmailExist(params.email)
        } catch (e: Exception) {
            throw WepinError.NOT_INITIALIZED_NETWORK
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
                throw WepinError.NOT_INITIALIZED_NETWORK
            }
        } else {
            throw WepinError.REQUIRED_SIGNUP_EMAIL
        }
    }

    actual suspend fun loginWithOauthProvider(params: LoginOauth2Params): LoginOauthResult {
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
        StorageManager.deleteAllStorage()
        if (_wepinNetworkManager == null) {
            throw WepinError.NOT_INITIALIZED_NETWORK
        }
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

    actual suspend fun loginWithAccessToken(params: LoginOauthAccessTokenRequest): LoginResult {
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }
        if (Providers.isNotAccessTokenProvider(params.provider)) {
            throw WepinError.INVALID_LOGIN_PROVIDER
        }

        StorageManager.deleteAllStorage()
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

    actual suspend fun loginWepin(params: LoginResult?): WepinUser {
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }

//        if (params == null) {
//            checkExistWepinLoginSession()
//            val wepinUser = StorageManager.getWepinUser()
//            if (wepinUser != null) {
//                return wepinUser
//            } else {
//                throw WepinError.NOT_INITIALIZED_ERROR
//            }
//        }
        if (params == null) {
            throw WepinError.INVALID_PARAMETER
        }

        if (params.token.idToken.isEmpty() || params.token.refreshToken.isEmpty()) {
            throw WepinError.INVALID_PARAMETER
        }

        try {
            val loginResponse = _wepinLoginManager.wepinNetworkManager?.login(params.token.idToken)
            StorageManager.setWepinUser(params, loginResponse!!)
            val wepinUser = StorageManager.getWepinUser()
            return wepinUser!!
        } catch (e: Exception) {
            throw WepinError(e.message)
        }
    }

    actual suspend fun logoutWepin(): Boolean {
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }

        val userId = StorageManager.getStorage("user_id") ?: throw WepinError.ALREADY_LOGOUT

//        return _wepinLoginManager.wepinNetworkManager?.logout(userId as String)!!
        try {
            val loginResponse = _wepinLoginManager.wepinNetworkManager?.logout(userId as String)
            StorageManager.deleteAllStorage()
            return loginResponse!!
        } catch (error: Exception) {
            throw error
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
        StorageManager.deleteAllStorage()
        closeAllClients()
        _isInitialized = false
    }

    private suspend fun checkExistWepinLoginSession(): Boolean {
        val token = StorageManager.getStorage("wepin:connectUser")
        val userId = StorageManager.getStorage("user_id")
        if (token != null && userId != null) {
            _wepinNetworkManager?.setAuthToken(
                (token as StorageDataType.WepinToken).accessToken,
                (token.refreshToken),
            )
            try {
                val accessToken = _wepinNetworkManager?.getAccessToken(userId as String)

                StorageManager.setStorage(
                    "wepin:connectUser",
                    StorageDataType.WepinToken(
                        accessToken = accessToken!!,
                        refreshToken = (token as StorageDataType.WepinToken).refreshToken,
                    ),
                )
                _wepinNetworkManager?.setAuthToken(accessToken, token.refreshToken)
                return true
            } catch (e: Exception) {
                _wepinNetworkManager?.clearAuthToken()
                StorageManager.deleteAllStorageWithAppId()
                return true
            }
        } else {
            _wepinNetworkManager?.clearAuthToken()
            StorageManager.deleteAllStorageWithAppId()
            return true
        }
    }

    actual suspend fun signUpWithEmailAndPassword(params: LoginWithEmailParams): LoginResult {
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }
        if (params.email.isEmpty() || !RegExpConst.validateEmail(email = params.email)) {
            throw WepinError.INCORRECT_EMAIL_FORM
        }
        if (params.password.isEmpty() || !RegExpConst.validatePassword(password = params.password)) {
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
                return _wepinLoginManager.loginHelper?.verifySignUpFirebase(params)!!
            }
        } catch (e: Exception) {
            throw e
        }
    }

    actual suspend fun getRefreshFirebaseToken(): LoginResult {
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }
        val firebaseToken = StorageManager.getStorage("firebase:wepin")

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
                StorageManager.setFirebaseUser(
                    idToken = token.idToken,
                    refreshToken = token.refreshToken,
                    providers = loginResult.provider
                )
                return loginResult
            } catch (e: Exception) {
                throw e
            }
        } else {
            throw WepinError.INVALID_LOGIN_SESSION
        }
    }

    actual suspend fun getCurrentWepinUser(): WepinUser {
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }
        checkExistWepinLoginSession()
        val wepinUser = StorageManager.getWepinUser()
        if (wepinUser != null) {
            return wepinUser
        } else {
            throw WepinError.INVALID_LOGIN_SESSION
        }
    }
}
