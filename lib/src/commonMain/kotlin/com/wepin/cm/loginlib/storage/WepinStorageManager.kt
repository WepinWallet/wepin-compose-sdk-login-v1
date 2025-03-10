package com.wepin.cm.loginlib.storage

import com.wepin.cm.loginlib.types.LoginResult
import com.wepin.cm.loginlib.types.Providers
import com.wepin.cm.loginlib.types.WepinUser
import com.wepin.cm.loginlib.types.network.LoginResponse

expect object WepinStorageManager {
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

    fun <T> setStorage(
        key: String,
        data: T,
    )

    fun <T> getStorage(key: String): T?

//    fun deleteAllIfAppIdDataNotExists()

//    fun deleteAllStorageWithAppId()
    fun getAllStorage(): Map<String, Any?>?
    fun setAllStorage(data: Map<String, Any>)
}
