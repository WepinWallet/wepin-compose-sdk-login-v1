package com.wepin.cm.loginlib.storage

import com.wepin.cm.loginlib.types.LoginResult
import com.wepin.cm.loginlib.types.Providers
import com.wepin.cm.loginlib.types.WepinUser
import com.wepin.cm.loginlib.types.network.LoginResponse

expect object StorageManager {
    fun init(
        context: Any,
        appId: String,
    )

    fun deleteAllStorage()

    fun setFirebaseUser(
        idToken: String,
        refreshToken: String,
        providers: Providers,
    )

    fun setWepinUser(
        request: LoginResult,
        response: LoginResponse,
    )

    fun getWepinUser(): WepinUser?

    fun setStorage(
        key: String,
        data: Any,
    )

    fun getStorage(key: String): Any?

    fun deleteAllIfAppIdDataNotExists()

    fun deleteAllStorageWithAppId()
}
