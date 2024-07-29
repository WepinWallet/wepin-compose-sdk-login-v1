package com.wepin.cm.loginlib.storage

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.wepin.cm.loginlib.types.LoginResult
import com.wepin.cm.loginlib.types.Providers
import com.wepin.cm.loginlib.types.StorageDataType
import com.wepin.cm.loginlib.types.Token
import com.wepin.cm.loginlib.types.UserInfo
import com.wepin.cm.loginlib.types.UserInfoDetails
import com.wepin.cm.loginlib.types.UserStatus
import com.wepin.cm.loginlib.types.WepinLoginStatus
import com.wepin.cm.loginlib.types.WepinUser
import com.wepin.cm.loginlib.types.network.LoginResponse
import com.wepin.cm.loginlib.utils.convertJsonToLocalStorageData
import com.wepin.cm.loginlib.utils.convertLocalStorageDataToJson
import kotlinx.cinterop.*
import platform.Foundation.NSBundle
import platform.posix.`false`

actual object StorageManager {
    private lateinit var _appId: String
    private var PREFERENCE_NAME: String = "wepin" + (NSBundle.mainBundle.bundleIdentifier ?: "") //"wepin_encrypted_preferences"

    actual fun init(
        context: Any,
        appId: String,
    ) {
        this._appId = appId
    }

    @OptIn(ExperimentalSettingsImplementation::class)
    actual fun deleteAllStorage() {
        val settings = KeychainSettings()
        settings.keys.forEach { key ->
            settings.remove(key)
        }
    }

    actual fun setFirebaseUser(
        idToken: String,
        refreshToken: String,
        providers: Providers,
    ) {
        deleteAllStorage()
        setStorage(
            "firebase:wepin",
            StorageDataType.FirebaseWepin(
                idToken = idToken,
                refreshToken = refreshToken,
                provider = providers.value
            )
        )
    }

    actual fun setWepinUser(
        request: LoginResult,
        response: LoginResponse,
    ) {
        deleteAllStorage()
        setStorage(
            "firebase:wepin",
            StorageDataType.FirebaseWepin(
                idToken = request.token.idToken,
                refreshToken = request.token.refreshToken,
                provider = request.provider.value
            )
        )
        setStorage(
            "wepin:connectUser",
            StorageDataType.WepinToken(
                accessToken = response.token.access,
                refreshToken = response.token.refresh
            )
        )
        setStorage("user_id", response.userInfo.userId)
        setStorage(
            "user_status",
            StorageDataType.UserStatus(
                loginStatus = response.loginStatus,
                pinRequired = (if (response.loginStatus == "registerRequired") response.pinRequired else false)
            )
        )

        if (response.loginStatus != "pinRequired" && response.walletId != null) {
            setStorage("wallet_id", response.walletId)
            setStorage(
                "user_info",
                StorageDataType.UserInfo(
                    status = "success",
                    userInfo = UserInfoDetails(
                        userId = response.userInfo.userId,
                        email = response.userInfo.email,
                        provider = request.provider.value,
                        use2FA = (response.userInfo.use2FA >= 2)
                    ),
                    walletId = response.walletId
                )
            )
        } else {
            val userInfo = StorageDataType.UserInfo(
                status = "success",
                userInfo = UserInfoDetails(
                    userId = response.userInfo.userId,
                    email = response.userInfo.email,
                    provider = request.provider.value,
                    use2FA = (response.userInfo.use2FA >= 2)
                )
            )
            setStorage("user_info", userInfo)
        }
        setStorage("oauth_provider_pending", request.provider.value)
    }

    @OptIn(ExperimentalSettingsImplementation::class)
    actual fun setStorage(
        key: String,
        data: Any,
    ) {
        val prefix = "$PREFERENCE_NAME$_appId"
        try {
            val settings = KeychainSettings(prefix)
            when (data) {
                is StorageDataType -> {
                    val keyChainData = convertLocalStorageDataToJson(data)
                    settings.putString(key, keyChainData)
                }

                is String -> {
                    settings.putString(key, data)
                }
            }
        } catch (e: Exception) {
//            println("setStorage Error")
        }
    }

    actual fun getWepinUser(): WepinUser? {
        val walletId = getStorage("wallet_id")
        val userInfo = getStorage("user_info")
        val wepinToken = getStorage("wepin:connectUser")
        val userStatus = getStorage("user_status")

        if (userInfo == null || wepinToken == null || userStatus == null) {
            return null
        }
        var wepinWallet: String? = null
        if (walletId != null) {
            wepinWallet = walletId as String
        }
        return WepinUser(
            status = "success",
            userInfo =
            UserInfo(
                userId = (userInfo as StorageDataType.UserInfo).userInfo!!.userId,
                email = userInfo.userInfo!!.email,
                provider =
                Providers.fromValue(
                    userInfo.userInfo!!.provider.toString(),
                )!!,
                use2FA = userInfo.userInfo!!.use2FA,
            ),
            userStatus =
            UserStatus(
                loginStatus = WepinLoginStatus.fromValue((userStatus as StorageDataType.UserStatus).loginStatus)!!,
                pinRequired = userStatus.pinRequired,
            ),
            walletId = wepinWallet,
            token =
            Token(
                accessToken = (wepinToken as StorageDataType.WepinToken).accessToken,
                refreshToken = wepinToken.refreshToken,
            ),
        )
    }

    @OptIn(ExperimentalSettingsImplementation::class)
    actual fun getStorage(key: String): Any? {
        val prefix = "$PREFERENCE_NAME$_appId"
        var stringData: String? = null

        try {
            val settings = KeychainSettings(prefix)
            stringData = settings.getStringOrNull(key) ?: return null
            return convertJsonToLocalStorageData(stringData)
        } catch (e: Exception) {
            return stringData
        }
    }

    @OptIn(ExperimentalSettingsImplementation::class)
    actual fun deleteAllIfAppIdDataNotExists() {
        var appIdDataExists = false
        val settings = KeychainSettings("$PREFERENCE_NAME$_appId")
        if (settings.keys.isEmpty()) {
            return
        } else {
            appIdDataExists = true
        }

        if (!appIdDataExists) {
            val deleteSettings = KeychainSettings()
            deleteSettings.keys.forEach { key ->
                deleteSettings.remove(key)
            }
        }
    }

    @OptIn(ExperimentalSettingsImplementation::class)
    actual fun deleteAllStorageWithAppId() {
        val settings = KeychainSettings("$PREFERENCE_NAME$_appId")
        settings.keys.forEach { key ->
            settings.remove(key)
        }
    }
}
