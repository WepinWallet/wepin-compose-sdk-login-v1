package com.wepin.cm.loginlib.network

import com.wepin.cm.loginlib.types.network.firebase.EmailAndPasswordRequest
import com.wepin.cm.loginlib.types.network.firebase.GetRefreshIdTokenRequest
import com.wepin.cm.loginlib.types.network.firebase.GetRefreshIdTokenResponse
import com.wepin.cm.loginlib.types.network.firebase.GetRefreshIdTokenSuccess
import com.wepin.cm.loginlib.types.network.firebase.ResetPasswordRequest
import com.wepin.cm.loginlib.types.network.firebase.ResetPasswordResponse
import com.wepin.cm.loginlib.types.network.firebase.SignInResponse
import com.wepin.cm.loginlib.types.network.firebase.SignInWithCustomTokenSuccess
import com.wepin.cm.loginlib.types.network.firebase.UpdatePasswordSuccess
import com.wepin.cm.loginlib.types.network.firebase.VerifyEmailRequest
import com.wepin.cm.loginlib.types.network.firebase.VerifyEmailResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class WepinFirebaseManager(firebaseKey: String) {
    private val firebaseUrl: String = "https://identitytoolkit.googleapis.com/v1/"
    private var firebaseKey: String

    private var firebaseHttpClient: HttpClient? = null
    private var firebaseApiService: WepinFirebaseApiService? = null

    init {
        this.firebaseKey = firebaseKey
        this.firebaseHttpClient = KtorWepinClient.createHttpClient(firebaseUrl, null, null, null)
        this.firebaseApiService = createWepinFirebaseApiService(firebaseKey, firebaseHttpClient!!)
    }

    suspend fun getRefreshIdToken(getRefreshIdTokenRequest: GetRefreshIdTokenRequest): GetRefreshIdTokenSuccess {
        val result = withContext(Dispatchers.IO) {
            val response = firebaseApiService!!.getRefreshIdToken(getRefreshIdTokenRequest)
            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                try {
                    val data = response.body<GetRefreshIdTokenSuccess>()
                    data
                } catch (e: Exception) {
                    throw Exception("invalid response")
                }
            } else {
                throw Exception("HTTP ${response.status.value}: ${response.bodyAsText()}")
            }
        }
        return result
    }

    suspend fun verifyEmail(verifyEmailRequest: VerifyEmailRequest): VerifyEmailResponse {
        val result = withContext(Dispatchers.IO) {
            val response = firebaseApiService!!.verifyEmail(verifyEmailRequest)
            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                try {
                    val data = response.body<VerifyEmailResponse>()
                    data
                } catch (e: Exception) {
                    throw Exception("invalid response")
                }
            } else {
                throw Exception("HTTP ${response.status.value}: ${response.bodyAsText()}")
            }
        }
        return result
    }

    suspend fun signInWithEmailPassword(signInRequest: EmailAndPasswordRequest): SignInResponse {
        val result =
            withContext(Dispatchers.IO) {
                val response: HttpResponse =
                    firebaseApiService!!.signInWithEmailPassword(signInRequest)
                try {
                    response.body<SignInResponse>()
                } catch (e: Exception) {
                    throw Exception("invalid response")
                }
            }
        return result
    }

    suspend fun updatePassword(
        idToken: String,
        password: String,
    ): UpdatePasswordSuccess {
        val result =
            withContext(Dispatchers.IO) {
                val response = firebaseApiService!!.updatePassword(idToken, password)
                try {
                    val data = response.body<UpdatePasswordSuccess>()
                    data
                } catch (e: Exception) {
                    throw Exception("invalid response")
                }
            }
        return result
    }

    suspend fun signInWithCustomToken(customToken: String): SignInWithCustomTokenSuccess {
        val result: SignInWithCustomTokenSuccess =
            withContext(Dispatchers.IO) {
                val response = firebaseApiService!!.signInWithCustomToken(customToken)
                try {
                    val data = response.body<SignInWithCustomTokenSuccess>()
                    data
                } catch (e: Exception) {
                    throw Exception("invalid response")
                }
            }
        return result
    }

    suspend fun resetPassword(resetPasswordRequest: ResetPasswordRequest): ResetPasswordResponse {
        val result = withContext(Dispatchers.IO) {
            val response = firebaseApiService!!.resetPassword(resetPasswordRequest)
            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                try {
                    val data = response.body<ResetPasswordResponse>()
                    data
                } catch (e: Exception) {
                    throw Exception("invalid response")
                }
            } else {
                throw Exception("HTTP ${response.status.value}: ${response.bodyAsText()}")
            }
        }
        return result
    }
}
