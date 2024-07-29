package com.wepin.cm.loginlib

import com.wepin.cm.loginlib.types.LoginOauth2Params
import com.wepin.cm.loginlib.types.LoginOauthResult
import com.wepin.cm.loginlib.types.LoginResult
import com.wepin.cm.loginlib.types.LoginWithEmailParams
import com.wepin.cm.loginlib.types.WepinLoginOptions
import com.wepin.cm.loginlib.types.WepinUser
import com.wepin.cm.loginlib.types.network.LoginOauthAccessTokenRequest
import com.wepin.cm.loginlib.types.network.LoginOauthIdTokenRequest

expect class WepinLogin(wepinLoginOptions: WepinLoginOptions) {

    fun isInitialized(): Boolean

    suspend fun init(): Boolean

    suspend fun loginWithEmailAndPassword(params: LoginWithEmailParams): LoginResult

    suspend fun signUpWithEmailAndPassword(params: LoginWithEmailParams): LoginResult

    suspend fun loginWithOauthProvider(params: LoginOauth2Params): LoginOauthResult

    suspend fun loginWithIdToken(params: LoginOauthIdTokenRequest): LoginResult

    suspend fun loginWithAccessToken(params: LoginOauthAccessTokenRequest): LoginResult

    suspend fun loginWepin(params: LoginResult?): WepinUser

    suspend fun logoutWepin(): Boolean

    suspend fun getRefreshFirebaseToken(): LoginResult

    suspend fun getCurrentWepinUser(): WepinUser

    fun getSignForLogin(
        privateKeyHex: String,
        message: String,
    ): String

    fun finalize()

}
