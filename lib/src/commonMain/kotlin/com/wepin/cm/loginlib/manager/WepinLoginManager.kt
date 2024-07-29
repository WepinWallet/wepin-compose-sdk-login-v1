package com.wepin.cm.loginlib.manager

import com.wepin.cm.loginlib.appAuth.LoginHelper
import com.wepin.cm.loginlib.network.WepinFirebaseManager
import com.wepin.cm.loginlib.network.WepinNetworkManager
import com.wepin.cm.loginlib.utils.getVersionMetaDataValue
import com.wepin.cm.loginlib.utils.urlEncoder

class WepinLoginManager {
    var wepinNetworkManager: WepinNetworkManager? = null
    var wepinFirebaseManager: WepinFirebaseManager? = null
    private var _appKey: String? = null
    private var _appId: String? = null
    private var _packageName: String? = null
    private var _version: String? = null

    internal var appAuthRedirectUrl: String = ""
    internal var appAuthBaseUrl: String = ""
    internal var loginHelper: LoginHelper? = null
    internal var loginResultManager: WepinLoginResultManager? = null

    companion object {
        var _instance: WepinLoginManager? = null

        fun getInstance(): WepinLoginManager {
            if (null == _instance) {
                _instance = WepinLoginManager()
            }
            return _instance as WepinLoginManager
        }
    }

    fun init(
        appKey: String,
        appId: String,
        packageName: String,
    ) {
        _version = getVersionMetaDataValue()
        _packageName = packageName
        _appKey = appKey
        _appId = appId
        loginResultManager = WepinLoginResultManager()
        loginResultManager!!.initLoginResult()
        wepinNetworkManager = WepinNetworkManager(_appKey!!, _packageName!!, _version!!)
        appAuthBaseUrl = "${wepinNetworkManager?.wepinBaseUrl}user/oauth/callback"
        appAuthRedirectUrl = "${wepinNetworkManager?.wepinBaseUrl}user/oauth/callback?uri=${
            urlEncoder(
                "wepin.$_appId:/oauth2redirect",
                "UTF-8",
            )
        }"
        loginHelper = LoginHelper(this)
    }

    fun setFirebase(key: String) {
        wepinFirebaseManager = WepinFirebaseManager(key)
    }
}
