package com.wepin.cm.loginlib

import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.UIKit.UIWindow
import platform.darwin.NSObject

class WepinPresentationContextProvider(private val window: UIWindow?): NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
    override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): ASPresentationAnchor? {
        return window ?: ASPresentationAnchor()
    }
}