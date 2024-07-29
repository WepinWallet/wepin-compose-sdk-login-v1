package com.wepin.cm.loginlib.appAuth

import com.wepin.cm.loginlib.error.WepinError
import com.wepin.cm.loginlib.manager.WepinLoginManager
import com.wepin.cm.loginlib.storage.StorageManager
import com.wepin.cm.loginlib.types.FBToken
import com.wepin.cm.loginlib.types.LoginOauthResult
import com.wepin.cm.loginlib.types.LoginResult
import com.wepin.cm.loginlib.types.LoginWithEmailParams
import com.wepin.cm.loginlib.types.OAuthTokenRequest
import com.wepin.cm.loginlib.types.OAuthTokenResponse
import com.wepin.cm.loginlib.types.OauthTokenParam
import com.wepin.cm.loginlib.types.OauthTokenType
import com.wepin.cm.loginlib.types.Providers
import com.wepin.cm.loginlib.types.network.PasswordStateRequest
import com.wepin.cm.loginlib.types.network.PasswordStateResponse
import com.wepin.cm.loginlib.types.network.VerifyRequest
import com.wepin.cm.loginlib.types.network.VerifyResponse
import com.wepin.cm.loginlib.types.network.firebase.EmailAndPasswordRequest
import com.wepin.cm.loginlib.types.network.firebase.ResetPasswordRequest
import com.wepin.cm.loginlib.types.network.firebase.ResetPasswordResponse
import com.wepin.cm.loginlib.types.network.firebase.SignInResponse
import com.wepin.cm.loginlib.types.network.firebase.VerifyEmailRequest
import com.wepin.cm.loginlib.utils.hashPassword
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class LoginHelper(
    private val wepinLoginManager: WepinLoginManager,
) {
    fun onWepinOauthLoginResult(
        provider: String,
        token: String,
    ): LoginOauthResult {
        val providerValue = Providers.fromValue(provider)
        if (providerValue == null) {
            throw WepinError.INVALID_LOGIN_PROVIDER
        } else {
            return setLoginOauthResult(token, provider)
        }
    }

    suspend fun loginWithEmailAndResetPasswordState(
        email: String,
        password: String,
    ): LoginResult {
        StorageManager.deleteAllStorage()

        var response: PasswordStateResponse? = null
        try {
            response = wepinLoginManager.wepinNetworkManager?.getUserPasswordState(email)
        } catch (e: Exception) {
//            println("userPWState: " + e.toString())
//            println("userPWState: " + e.message.toString())
//            println("userPWState: " + isFirstEmailUser(e.message.toString()).toString())
//            println("userPWState: " + (!e.message?.contains("not exist")!! || !e.message?.contains("400")!!).toString())
            if (!isFirstEmailUser(e.message.toString())) {
                throw e
            }
        }
        val encryptedPassword = hashPassword(password)
        val isChangeRequired =
            if (response!!.isPasswordResetRequired) {
                true
            } else {
                false
            }

        val firstPw = if (isChangeRequired) password else encryptedPassword
        var firebaseRes: SignInResponse? = null
        runBlocking {
            firebaseRes =
                wepinLoginManager.wepinFirebaseManager?.signInWithEmailPassword(
                    EmailAndPasswordRequest(
                        email = email,
                        password = firstPw,
                    ),
                )
        }

        val idToken: String = firebaseRes!!.idToken
        val refreshToken: String = firebaseRes!!.refreshToken

        if (isChangeRequired) {
            val changePasswordRes =
                changePassword(encryptedPassword, FBToken(idToken, refreshToken))
            StorageManager.setFirebaseUser(
                changePasswordRes!!.idToken,
                changePasswordRes.refreshToken,
                Providers.EMAIL,
            )
            return LoginResult(
                provider = Providers.EMAIL,
                token = changePasswordRes,
            )
        } else {
            return LoginResult(
                provider = Providers.EMAIL,
                token = FBToken(idToken, refreshToken),
            )
        }
    }

    fun isFirstEmailUser(errorString: String): Boolean {
        val jsonString = errorString.substringAfter("java.lang.Exception: ")

        try {
            // JSON 파싱
            val jsonObject = Json.parseToJsonElement(jsonString).jsonObject

            // 필요한 필드 값 추출
            val status = jsonObject.get("status") as Int
            val message = jsonObject.get("message") as String

            // 조건 검사
            val isStatus400 = (status == 400)
            val isMessageContainsNotExist = message.contains("not exist")

            // 결과 출력
            return isStatus400 && isMessageContainsNotExist
        } catch (e: Exception) {
            return false
        }
    }

    suspend fun getOauthTokenWithWepin(param: OauthTokenParam): LoginOauthResult {
        val networkManger = wepinLoginManager.wepinNetworkManager
        val body =
            OAuthTokenRequest(
                code = param.code,
                clientId = param.clientId,
                state = param.state,
                redirectUri = wepinLoginManager.appAuthRedirectUrl,
                codeVerifier = param.codeVerifier,
            )
//        Log.d("LoginHelper", "body: $body")
        val oauthTokenResponse: OAuthTokenResponse
        try {
            oauthTokenResponse = networkManger!!.oauthTokenRequest(param.provider, body)
        } catch (e: Exception) {
            throw WepinError.FAILED_LOGIN
        }
        return if (param.provider == "naver") {
            onWepinOauthLoginResult(
                param.provider,
                oauthTokenResponse.access_token,
            )
        } else {
            onWepinOauthLoginResult(
                param.provider,
                oauthTokenResponse.id_token!!,
            )
        }
    }

    suspend fun doFirebaseLoginWithCustomToken(
        token: String,
        type: Providers,
    ): LoginResult {
        val signInResponse = wepinLoginManager.wepinFirebaseManager?.signInWithCustomToken(token)
        StorageManager.setFirebaseUser(signInResponse!!.idToken, signInResponse.refreshToken, type)
        return LoginResult(type, FBToken(signInResponse.idToken, signInResponse.refreshToken))
    }

    private fun setLoginOauthResult(
        token: String,
        provider: String,
    ): LoginOauthResult {
        when (provider) {
            "google", "apple" -> {
                return LoginOauthResult(
                    provider,
                    token,
                    OauthTokenType.ID_TOKEN,
                )
            }

            "discord", "naver" -> {
                return LoginOauthResult(
                    provider,
                    token,
                    OauthTokenType.ACCESS_TOKEN,
                )
            }

            else -> {
                throw WepinError.INVALID_LOGIN_PROVIDER
            }
        }
    }

    private suspend fun changePassword(
        password: String,
        token: FBToken,
    ): FBToken? {
        try {
            val loginResponse = wepinLoginManager.wepinNetworkManager?.login(token.idToken)
            val updatePasswordRes =
                wepinLoginManager.wepinFirebaseManager?.updatePassword(token.idToken, password)
            val passwordStateRequest = PasswordStateRequest(false)
            wepinLoginManager.wepinNetworkManager?.updateUserPasswordState(
                loginResponse!!.userInfo.userId,
                passwordStateRequest,
            )
            return FBToken(updatePasswordRes!!.idToken, updatePasswordRes.refreshToken)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun verifySignUpFirebase(params: LoginWithEmailParams): LoginResult {
        val localeId = if (params.locale == "ko") 1 else 2
        try {
            val verifyResponse = wepinLoginManager.wepinNetworkManager?.verify(
                VerifyRequest(
                    type = "create",
                    email = params.email,
                    localeId = localeId
                )
            )
            if (verifyResponse!!.result) {
                if (verifyResponse.oobVerify !== null && verifyResponse.oobReset != null) {
                    return signUpFirebase(params, verifyResponse)
                } else {
                    throw WepinError.REQUIRED_EMAIL_VERIFIED
                }
            } else {
                throw WepinError.FAILED_EMAIL_VERIFIED
            }
        } catch (e: Exception) {
            if (e is WepinError)
                throw e
            else if (e.message?.contains("400") == true)
                throw WepinError.INVALID_EMAIL_DOMAIN
            else
                throw WepinError.FAILED_SEND_EMAIL
        }
    }

    private suspend fun signUpFirebase(
        params: LoginWithEmailParams,
        verifyResponse: VerifyResponse
    ): LoginResult {
        var resetPasswordResponse: ResetPasswordResponse? = null
        try {
            resetPasswordResponse = wepinLoginManager.wepinFirebaseManager?.resetPassword(
                ResetPasswordRequest(
                    oobCode = verifyResponse.oobReset!!,
                    newPassword = params.password
                )
            )
        } catch (e: Exception) {
            throw WepinError.FAILED_PASSWORD_SETTING
        }
        if (resetPasswordResponse!!.email.trim().lowercase() !== params.email.trim()) {
            throw WepinError.FAILED_PASSWORD_SETTING
        } else {
            try {
                val verifyResponse =
                    wepinLoginManager.wepinFirebaseManager!!.verifyEmail(VerifyEmailRequest(oobCode = verifyResponse.oobVerify!!))
                if (verifyResponse.email.trim().lowercase() !== params.email.trim()) {
                    throw WepinError.FAILED_EMAIL_VERIFIED
                } else {
                    return loginWithEmailAndResetPasswordState(params.email, params.password)
                }
            } catch(e: Exception) {
                throw WepinError.FAILED_EMAIL_VERIFIED
            }
        }
    }
}
