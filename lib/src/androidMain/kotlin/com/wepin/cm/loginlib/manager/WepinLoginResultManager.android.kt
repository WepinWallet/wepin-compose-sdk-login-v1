package com.wepin.cm.loginlib.manager

import com.wepin.cm.loginlib.types.LoginOauthResult
import com.wepin.cm.loginlib.types.LoginResult
import com.wepin.cm.loginlib.types.WepinUser
import java.util.concurrent.CompletableFuture

actual class WepinLoginResultManager {
    internal var loginCompletableFuture: CompletableFuture<LoginResult> = CompletableFuture()
    internal var loginOauthCompletableFuture: CompletableFuture<LoginOauthResult> = CompletableFuture()
    internal var loginWepinCompletableFutre: CompletableFuture<WepinUser> = CompletableFuture<WepinUser>()

    actual fun initLoginResult() {
        loginCompletableFuture = CompletableFuture<LoginResult>()
        loginOauthCompletableFuture = CompletableFuture<LoginOauthResult>()
        loginWepinCompletableFutre = CompletableFuture<WepinUser>()
    }
}
