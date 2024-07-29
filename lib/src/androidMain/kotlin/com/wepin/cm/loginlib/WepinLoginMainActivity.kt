package com.wepin.cm.loginlib

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import com.wepin.cm.loginlib.const.AppAuthConst
import com.wepin.cm.loginlib.error.WepinError
import com.wepin.cm.loginlib.manager.WepinLoginManager
import com.wepin.cm.loginlib.types.ErrorCode
import com.wepin.cm.loginlib.types.LoginOauthResult
import com.wepin.cm.loginlib.types.OauthTokenParam
import com.wepin.cm.loginlib.types.WepinLoginError
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest

internal class WepinLoginMainActivity : ComponentActivity() {
    private val RC_AUTH = 100
    private var authService: AuthorizationService? = null
    private var authState: AuthState? = null

    private val wepinLoginManager = WepinLoginManager.getInstance()
    private var redirectUri: Uri? = null
    private var provider: String? = null
    private var clientId: String? = null
    private var token: String? = null
//    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

//    private var jwt : JWT? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            Log.d("WepinLoginMainActivity", "onCreate")
            super.onCreate(savedInstanceState)
            // 배경을 투명하게 설정
            window.setBackgroundDrawableResource(android.R.color.transparent)
            setContentView(R.layout.activity_wepin_login_main)
            val bundle = intent.extras
            provider = bundle?.getString("provider").toString()
            clientId = bundle?.getString("clientId").toString()
            redirectUri = Uri.parse(wepinLoginManager.appAuthRedirectUrl)
            processLoginOauth2(provider!!, clientId!!)
        } catch (e: Exception) {
//            e.message?.let { loginHelper?.onWepinOauthLoginError(null, it) }
            e.printStackTrace()
        }
    }

    private fun processLoginOauth2(
        provider: String,
        clientId: String,
    )  {
        val authUri = AppAuthConst.getAuthorizationEndpoint(provider) as Uri
        val tokenUri = AppAuthConst.getTokenEndpoint(provider) as Uri

        val serviceConfig =
            AuthorizationServiceConfiguration(
                authUri, // authorization endpoint
                tokenUri,
            )
        authState = AuthState(serviceConfig)
        loginAppauth(serviceConfig, clientId, provider)
    }

    private fun loginAppauth(
        serviceConfig: AuthorizationServiceConfiguration,
        clientId: String,
        provider: String,
    ) {
        if (redirectUri === null) {
            wepinLoginManager.loginResultManager!!.loginOauthCompletableFuture.completeExceptionally(
                WepinError.generalUnKnownEx("invalid rediract uri"),
            )
        }
        val builder =
            AuthorizationRequest.Builder(
                serviceConfig,
                clientId,
                ResponseTypeValues.CODE,
                redirectUri!!,
            )

        // apple의 경우, scope에 email을 추가하면 response mode를 POST 로 해줘야 함!!!
        if (provider == "apple") builder.setResponseMode("form_post")

        if (provider == "discord") {
            builder.setScopes("identify", "email")
        } else {
            builder.setScopes("email")
        }
        builder.setPrompt("select_account")

        val authRequest = builder.build()
        authService = AuthorizationService(this)
        val authIntent = authService!!.getAuthorizationRequestIntent(authRequest)
        if (authIntent != null) {
            startActivityForResult(authIntent, RC_AUTH)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("WepinLoginMainActivity", "WepinLoginMainActivity - onActivityResult ")
        Log.d("WepinLoginMainActivity", "resCode $requestCode")
        if (requestCode == RC_AUTH) {
            val resp = AuthorizationResponse.fromIntent(data!!)
            val ex = AuthorizationException.fromIntent(data)
            // ... process the response or exception ...
            Log.d("WepinLoginMainActivity", "WepinLoginMainActivity - onActivityResult - resp $resp")
            Log.d("WepinLoginMainActivity", "WepinLoginMainActivity - onActivityResult - resp ${resp?.authorizationCode}")
            Log.d("WepinLoginMainActivity", "WepinLoginMainActivity - onActivityResult - ex $ex")
            // ex?.errorDescription
            authState = AuthState(resp, ex)

            if (ex != null) {
//                        authState = AuthState()
                Log.d("WepinLoginMainActivity", "provider exception = ${ex.message}")

                val message = ex.message ?: ex.errorDescription ?: "unknown"
                wepinLoginManager.loginResultManager!!.loginOauthCompletableFuture.completeExceptionally(
                    WepinError.generalUnKnownEx(message),
                )
            } else if (resp == null)
                {
                    wepinLoginManager.loginResultManager!!.loginOauthCompletableFuture.completeExceptionally(
                        WepinError(WepinLoginError.getError(ErrorCode.FAILED_LOGIN)),
                    )
                } else {
                getOauthToken(resp)
            }
        } else {
            // ...
        }
        authService?.customTabManager?.dispose()
        finish()
    }

    // 우선은 SUSPEND 로 하고 후에 COMPLETABLE로 변경해야 할 듯
    @OptIn(DelicateCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.S)
    private fun getOauthToken(resp: AuthorizationResponse) {
        val tokenExchangeRequest = resp.createTokenExchangeRequest()

        if (provider == "discord") {
            // PKCE를 지원하는 discord만 AppAuth를 통해 토큰 받아옴..
            getOauthTokenWithAppAuth(tokenExchangeRequest)
        } else {
            val param =
                OauthTokenParam(
                    provider = provider!!,
                    clientId = clientId!!,
                    codeVerifier = tokenExchangeRequest.codeVerifier,
                    code = resp.authorizationCode!!,
                    state = resp.state,
                )
            GlobalScope.launch(Dispatchers.Main) {
                val result = wepinLoginManager.loginHelper?.getOauthTokenWithWepin(param)
                wepinLoginManager.loginResultManager!!.loginOauthCompletableFuture.complete(result)
            }
//            wepinLoginManager.loginResultManager!!.loginOauthCompletableFuture.complete(result)
        }
    }

    private fun getOauthTokenWithAppAuth(tokenExchangeRequest: TokenRequest) {
        Log.d("WepinLoginMainActivity", "WepinLoginMainActivity - onActivityResult - tokenExchangeRequest $tokenExchangeRequest")
        authService?.performTokenRequest(tokenExchangeRequest) { response, exception ->
            Log.d("WepinLoginMainActivity", "onActivityResult - response-accessToken ${response?.accessToken}")
            Log.d("WepinLoginMainActivity", "onActivityResult - response-idToken ${response?.idToken}")

            if (exception != null) {
//                        authState = AuthState()
                Log.d("WepinLoginMainActivity", "provider exception = ${exception.message}")
                val message = exception.message ?: "unknown"
//                wepinLoginManager.loginHelper?.onWepinOauthLoginError(null, message)
                throw Exception(message)
            } else {
                if (response != null) {
                    Log.d("WepinLoginMainActivity", "provider = $provider")
                    when (provider) {
                        "google", "apple" -> token = response.idToken
                        "naver", "discord" -> token = response.accessToken
                    }
                    Log.d("WepinLoginMainActivity", "activity - token $token")

                    val result = token?.let {
                        provider?.let { it1 ->
                            wepinLoginManager.loginHelper?.onWepinOauthLoginResult(
                                it1,
                                it,
                            )
                        }
                    }
                    authState!!.update(response, null)
                    wepinLoginManager.loginResultManager!!.loginOauthCompletableFuture.complete(result)
                }
            }
        }
    }
}
