package com.wepin.cm.loginlib.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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

actual object WepinStorageManager {
    private var _appId: String = ""
    private const val PREV_PREFERENCE_NAME = "wepin_encrypted_preferences"
    private lateinit var _prevStorage: EncryptedSharedPreferences
    private lateinit var _storage: StorageManager

    actual fun init(context: Any, appId: String) {
        this._appId = appId

        try {
            initializePrevStorage(context as Context)
        } catch(error: Exception) {
            (context as Context).getSharedPreferences(PREV_PREFERENCE_NAME + _appId, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
            initializePrevStorage(context)
        }
        _storage = StorageManager(context as Context)
        migrationOldStorage()
    }

    private fun initializePrevStorage(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        _prevStorage = EncryptedSharedPreferences.create(
            context,
            PREV_PREFERENCE_NAME + _appId,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }

    private fun migrationOldStorage() {
        try {
            val migrationState = getStorage<Boolean>("migration")
            if (migrationState == true) return

            val oldStorage = _prevStorageReadAll()
            oldStorage?.forEach { (key, value) ->
                setStorage(key, value)
            }
        } catch(e: Exception) {
//            println("Migration failed with an unexpected error - $e")
        } finally {
            setStorage("migration", true)
            _prevDeleteAll()
        }
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

    actual fun deleteAllStorage() {
        _storage.deleteAll()

        setStorage("migration", true);
    }

    actual fun setFirebaseUser(
        idToken: String,
        refreshToken: String,
        providers: Providers,
    ) {
//        deleteAllStorageWithAppId()
        deleteAllStorage()
        setStorage<StorageDataType>(
            "firebase:wepin",
            StorageDataType.FirebaseWepin(
                idToken = idToken,
                refreshToken = refreshToken,
                provider = providers.value,
            ),
        )
    }

    actual fun setWepinUser(
        request: LoginResult,
        response: LoginResponse,
    ) {
//        deleteAllStorageWithAppId()
        deleteAllStorage()
        setStorage<StorageDataType>(
            "firebase:wepin",
            StorageDataType.FirebaseWepin(
                idToken = request.token.idToken,
                refreshToken = request.token.refreshToken,
                provider = request.provider.value,
            ),
        )
        setStorage<StorageDataType>(
            "wepin:connectUser",
            StorageDataType.WepinToken(
                accessToken = response.token.access,
                refreshToken = response.token.refresh,
            ),
        )

        setStorage("user_id", response.userInfo.userId)

        setStorage<StorageDataType>(
            "user_status",
            StorageDataType.UserStatus(
                loginStatus = response.loginStatus,
                pinRequired = (if (response.loginStatus == "registerRequired") response.pinRequired else false),
            ),
        )

        if (response.loginStatus != "pinRequired" && response.walletId != null) {
            setStorage("wallet_id", response.walletId)
            val walletId = getStorage<String>("wallet_id")
            setStorage<StorageDataType>(
                "user_info",
                StorageDataType.UserInfo(
                    status = "success",
                    userInfo =
                    UserInfoDetails(
                        userId = response.userInfo.userId,
                        email = response.userInfo.email,
                        provider = request.provider.value,
                        use2FA = (response.userInfo.use2FA >= 2),
                    ),
                    walletId = walletId,
                ),
            )
        } else {
            val userInfo =
                StorageDataType.UserInfo(
                    status = "success",
                    userInfo =
                    UserInfoDetails(
                        userId = response.userInfo.userId,
                        email = response.userInfo.email,
                        provider = request.provider.value,
                        use2FA = (response.userInfo.use2FA >= 2),
                    ),
                )
            setStorage<StorageDataType>("user_info", userInfo)
        }
        setStorage("oauth_provider_pending", request.provider.value)
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
        val wepinUser =
            WepinUser(
                status = "success",
                userInfo =
                UserInfo(
                    userId = (userInfo as StorageDataType.UserInfo).userInfo!!.userId,
                    email = (userInfo as StorageDataType.UserInfo).userInfo!!.email,
                    provider =
                    Providers.fromValue(
                        (userInfo as StorageDataType.UserInfo).userInfo!!.provider.toString(),
                    )!!,
                    // Providers.fromValue(params.provider)!!,
                    use2FA = (userInfo as StorageDataType.UserInfo).userInfo!!.use2FA,
                ),
                userStatus =
                UserStatus(
                    loginStatus = WepinLoginStatus.fromValue((userStatus as StorageDataType.UserStatus).loginStatus.toString())!!,
                    pinRequired = (userStatus as StorageDataType.UserStatus).pinRequired,
                ),
                walletId = wepinWallet,
                token =
                Token(
                    accessToken = (wepinToken as StorageDataType.WepinToken).accessToken,
                    refreshToken = (wepinToken as StorageDataType.WepinToken).refreshToken,
                ),
            )
        return wepinUser
    }

    actual fun <T> setStorage(
        key: String,
        data: T,
    ) {
        try {
            val stringValue = _encodeValue(data)
            _storage.write(_appId, key, stringValue)
        } catch(e: Exception) {
            if (!e.message.toString().contains("already exists")){
                throw e
            }
        }
    }

    // Get EncryptedSharedPreferences
    actual fun <T> getStorage(key: String): T? {
        return try {
            _storage.read(_appId, key)?.let { _parseValue(it) }
        } catch (e: Exception) {
            null
        }
    }

//    private fun getEncryptedDataPair(data: String): Pair<ByteArray, ByteArray> {
//        val cipher = Cipher.getInstance(TRANSFORMATION)
//        cipher.init(Cipher.ENCRYPT_MODE, getKey())
//
//        val iv: ByteArray = cipher.iv
//        val encryptedData = cipher.doFinal(data.toByteArray(UTF_8))
//        return Pair(iv, encryptedData)
//    }
//
//    private fun getKey(): SecretKey {
//        val keyStore = KeyStore.getInstance("AndroidKeyStore")
//        keyStore.load(null)
//        val secreteKeyEntry: KeyStore.SecretKeyEntry =
//            keyStore.getEntry(PREFERENCE_NAME + this._appId, null) as KeyStore.SecretKeyEntry
//        return secreteKeyEntry.secretKey
//    }

//    private fun isAppIdDataExists(): Boolean {
//        val sharedPreferenceIds = sharedPreferences.all
//        sharedPreferenceIds.forEach {
//            if (it.key.startsWith(_appId)) {
//                return true
//            }
//        }
//        return false
//    }

//    actual fun deleteAllIfAppIdDataNotExists() {
//        if (!isAppIdDataExists()) {
//            deleteAllStorageWithAppId()
//        }
//    }
//
//    actual fun deleteAllStorageWithAppId() {
//        val sharedPreferenceIds = sharedPreferences.all
//        sharedPreferenceIds.forEach {
//            if (it.key.startsWith(_appId)) {
//                sharedPreferences.edit().remove(it.key).apply()
//            }
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

        } catch (e: Exception) {
            return null
        }
    }

    actual fun setAllStorage(data: Map<String, Any>) {
        data.forEach { (key, value) ->
            when (key) {
                "firebase:wepin" -> {
                    if (value is Map<*, *>) {
                        val idToken = value["idToken"] as? String ?: ""
                        val refreshToken = value["refreshToken"] as? String ?: ""
                        val provider = value["provider"] as? String ?: ""
                        setStorage<StorageDataType>(
                            key,
                            StorageDataType.FirebaseWepin(
                                idToken = idToken,
                                refreshToken = refreshToken,
                                provider = provider
                            )
                        )
                    } else if (value is StorageDataType) {
                        setStorage(key, value)
                    }
                }

                "wepin:connectUser" -> {
                    if (value is Map<*, *>) {
                        val accessToken = value["accessToken"] as? String ?: ""
                        val refreshToken = value["refreshToken"] as? String ?: ""
                        setStorage<StorageDataType>(
                            key,
                            StorageDataType.WepinToken(
                                accessToken = accessToken,
                                refreshToken = refreshToken
                            )
                        )
                    } else if (value is StorageDataType) {
                        setStorage(key, value)
                    }
                }

                "user_id" -> {
                    if (value is String) {
                        setStorage(key, value)
                    }
                }

                "user_status" -> {
                    if (value is Map<*, *>) {
                        val loginStatus = value["loginStatus"] as? String ?: ""
                        val pinRequired = value["pinRequired"] as? Boolean ?: false
                        setStorage<StorageDataType>(
                            key,
                            StorageDataType.UserStatus(
                                loginStatus = loginStatus,
                                pinRequired = pinRequired
                            )
                        )
                    } else if (value is StorageDataType) {
//                        println("userStatus value: $value")
                        setStorage(key, value)
                    }
                }

                "wallet_id" -> {
                    if (value is String) {
                        setStorage(key, value)
                    }
                }

                "user_info" -> {
                    if (value is Map<*, *>) {
                        val status = value["status"] as? String ?: "success"
                        val userInfoMap = value["userInfo"] as? Map<*, *>
                        val userId = userInfoMap?.get("userId") as? String ?: ""
                        val email = userInfoMap?.get("email") as? String ?: ""
                        val provider = userInfoMap?.get("provider") as? String ?: ""
                        val use2FA = userInfoMap?.get("use2FA") as? Boolean ?: false
                        val walletId = value["walletId"] as? String
                        val userInfo = UserInfoDetails(userId = userId, email = email, provider = provider, use2FA = use2FA)
                        setStorage<StorageDataType>(key, StorageDataType.UserInfo(status = status, userInfo = userInfo, walletId = walletId))
                    } else if (value is StorageDataType) {
                        setStorage(key, value)
                    }
                }

                "oauth_provider_pending" -> {
                    if (value is String) {
                        setStorage(key, value)
                    }
                }

                "app_language" -> {
                    if (value is Map<*, *>) {
                        val locale = value["locale"] as? String ?: ""
                        val currency = value["currency"] as? String ?: ""
                        setStorage<StorageDataType>(key, StorageDataType.AppLanguage(locale = locale, currency = currency))
                    } else if (value is StorageDataType) {
                        setStorage(key, value)
                    }
                }

                else -> {
                    throw IllegalArgumentException("Unsupported key: $key")
                }
            }
        }
    }

    private fun _prevStorageReadAll(): Map<String, Any?>? {
        return try {
            _prevStorage.all
                .filterKeys { it.startsWith(_appId) }
                .mapKeys { it.key.removePrefix("${_appId}_") }
        } catch (e: Exception) {
            _prevDeleteAll()
            null
        }
    }

    private fun _prevDeleteAll() {
        try {
            _prevStorage.edit().clear().apply()
        } catch(e: Exception) {
            return
        }
    }
}
