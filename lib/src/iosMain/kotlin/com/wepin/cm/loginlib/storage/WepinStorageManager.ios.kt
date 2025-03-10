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
import platform.Foundation.NSBundle

actual object WepinStorageManager {
    private lateinit var _appId: String
    private var PREV_PREFERENCE_NAME: String = "wepin" + (NSBundle.mainBundle.bundleIdentifier ?: "") //"wepin_encrypted_preferences"
    private lateinit var _storage: StorageManager

    actual fun init(
        context: Any,
        appId: String,
    ) {
        this._appId = appId
        _storage = StorageManager()
        migrationOldStorage()
    }

    @OptIn(ExperimentalSettingsImplementation::class)
    private fun migrationOldStorage() {
        try {
            val migrationState = getStorage<Boolean>("migration")
            if (migrationState == true) return

            val settings = KeychainSettings("${PREV_PREFERENCE_NAME}$_appId")

            settings.keys.forEach { key ->
                setStorage(key, settings.getStringOrNull(key))
            }
        } catch(e: Exception) {
//            println("Migration failed with an unexpected error - $e")
        } finally {
            setStorage("migration", true)
            _prevDeleteAll()
        }

//        println("storages: $oldStorages")

        //deleteAll
//        val settings = KeychainSettings()
//        settings.keys.forEach { key ->
//            settings.remove(key)
//        }

    }

    @OptIn(ExperimentalSettingsImplementation::class)
    private fun _prevDeleteAll() {
        val settings = KeychainSettings("${PREV_PREFERENCE_NAME}$_appId")

        settings.clear()
    }

    fun <T> _encodeValue(value: T): String {
        return when (value) {
            is String,
            is Int,
            is Double,
            is Boolean -> {
                value.toString()
            }

            is StorageDataType -> {
                convertLocalStorageDataToJson(value)
            }

            else -> throw IllegalArgumentException("Unsupported data type")
        }
    }

    fun <T> _parseValue(value: String): T? {
        val primitiveValue: Any? = when {
            value.equals("true", ignoreCase = true) -> true
            value.equals("false", ignoreCase = true) -> false
            value.toIntOrNull() != null -> value.toInt()
            value.toDoubleOrNull() != null -> value.toDouble()
            else -> null
        }

        @Suppress("UNCHECKED_CAST")
        return try {
            if (primitiveValue != null) {
                primitiveValue as? T
            } else {
                convertJsonToLocalStorageData(value) as? T ?: value as? T
            }
        } catch (e: Exception) {
            //String
            value as? T
        }
    }

    @OptIn(ExperimentalSettingsImplementation::class)
    actual fun deleteAllStorage() {
        _storage.deleteAll()

        setStorage("migration", true)
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

    actual fun <T> setStorage(
        key: String,
        data: T,
    ) {
        try {
            val stringValue = _encodeValue(data)
            _storage.write(_appId, key, stringValue)
        } catch(error: Exception) {
//            println("error: $error")
            throw error
        }
    }

    actual fun getWepinUser(): WepinUser? {
        val walletId = getStorage<String>("wallet_id")
        val userInfo = getStorage<StorageDataType>("user_info")
        val wepinToken = getStorage<StorageDataType>("wepin:connectUser")
        val userStatus = getStorage<StorageDataType>("user_status")

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
                    userInfo.userInfo.provider.toString(),
                )!!,
                use2FA = userInfo.userInfo.use2FA,
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
    actual fun <T> getStorage(key: String): T? {
        return try {
            _storage.read(_appId, key)?.let {
                _parseValue(it)
            }
        } catch (e: Exception) {
//            println("getStorageError: $e")
            null
        }
    }

//    @OptIn(ExperimentalSettingsImplementation::class)
//    actual fun deleteAllIfAppIdDataNotExists() {
//        var appIdDataExists = false
//        val settings = KeychainSettings("$PREFERENCE_NAME$_appId")
//        if (settings.keys.isEmpty()) {
//            return
//        } else {
//            appIdDataExists = true
//        }
//
//        if (!appIdDataExists) {
//            val deleteSettings = KeychainSettings()
//            deleteSettings.keys.forEach { key ->
//                deleteSettings.remove(key)
//            }
//        }
//    }
//
//    @OptIn(ExperimentalSettingsImplementation::class)
//    actual fun deleteAllStorageWithAppId() {
//        val settings = KeychainSettings("$PREFERENCE_NAME$_appId")
//        settings.keys.forEach { key ->
//            settings.remove(key)
//        }
//    }

    actual fun getAllStorage(): Map<String, Any?>? {
        try {
            val allData = _storage.readAll(_appId)

            val filteredData = mutableMapOf<String, Any?>()
            for (key in allData.keys) {
                val storageKey = key.replaceFirst("${_appId}_", "")
                try {
                    val jsonValue = _parseValue<StorageDataType>(allData[key] ?: "")
                    filteredData[storageKey] = jsonValue
                } catch (e: Exception) {
                    filteredData[storageKey] = allData[key]
                }
            }
            return if (filteredData.isEmpty()) null else filteredData
        } catch(e: Exception) {
//            println("getAllStorage error: $e")
            return null
        }
    }

    actual fun setAllStorage(data: Map<String, Any>) {
        data.forEach { (key, value) ->
            setStorage(key, value)
        }
    }
}
