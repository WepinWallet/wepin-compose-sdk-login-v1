<br/>

<p align="center">
  <a href="https://www.wepin.io/">
      <picture>
        <source media="(prefers-color-scheme: dark)">
        <img alt="wepin logo" src="https://github.com/WepinWallet/wepin-web-sdk-v1/blob/main/assets/wepin_logo_color.png?raw=true" width="250" height="auto">
      </picture>
</a>
</p>

<br>


# wepin-compose-sdk-login-v1

[![platform - android](https://img.shields.io/badge/platform-Android-3ddc84.svg?logo=android&style=for-the-badge)](https://www.android.com/)
[![platform - ios](https://img.shields.io/badge/platform-iOS-000.svg?logo=apple&style=for-the-badge)](https://developer.apple.com/ios/)

Wepin Login Library for ComposeMultiplatform. This package is exclusively available for use in Android and iOS environments.

## ⏩ Get App ID and Key
After signing up for [Wepin Workspace](https://workspace.wepin.io/), go to the development tools menu and enter the information for each app platform to receive your App ID and App Key.

## ⏩ Requirements
- Android API version 24 or newer is required.
- iOS 13+
- Swift 5.x

## ⏩ Installation
val commonMain by getting {
  implementation("io.wepin:wepin-compose-sdk-login-v1:0.0.10")
  api("io.wepin:wepin-compose-sdk-login-v1:0.0.10")
}

for iOS
add cocoapods plugin in build.gradle.kts
```
plugins {
    kotlin("native.cocoapods")
}

cocoapods {
  summary = "Some description for a Kotlin/Native module"
  homepage = "Link to a Kotlin/Native module homepage"
  ios.deploymentTarget = "13.0"
  version = "0.0.1"

  pod("AppAuth") {
    version = "~> 1.7.5"
  }

  pod("secp256k1") {
    version = "~> 0.1.0"
  }

  pod("JFBCrypt") {
    version = "~> 0.1"
  }
}
```  

## ⏩ Setting PodFile
For iOS, You must add Podfile in iosApp Folder for install ios dependencies.
```swift
# Uncomment the next line to define a global platform for your project
platform :ios, '13.0'

target 'iosApp' do
  # Comment the next line if you don't want to use dynamic frameworks
  use_frameworks!

  # Pods for iosApp
  pod 'shared', :path => '../shared'
end

post_install do |installer|
    installer.generated_projects.each do |project|
        project.targets.each do |target|
            target.build_configurations.each do |config|
                config.build_settings['IPHONEOS_DEPLOYMENT_TARGET'] = '13.0'
            end
        end
    end
end
```

After Sync Project with Gradle Files, Do pod install


## ⏩ Add Permission
Add the below line in your app's `AndroidManifest.xml` file

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.INTERNET" />

```

## ⏩ Configure Deep Link
- Android
  Deep Link scheme format : `wepin. + Your Wepin App ID`
  
  When a custom scheme is used, WepinLogin Library can be easily configured to capture all redirects using this custom scheme through a manifest placeholder:
  
  Add the below lines in your app's build.gradle.kts file
  ```kotlin
  // For Deep Link => RedirectScheme Format : wepin. + Wepin App ID
  android.defaultConfig.manifestPlaceholders = [
    'appAuthRedirectScheme': 'wepin.{{YOUR_WEPIN_APPID}}'
  ]
  ```
  Add the below line in your app's AndroidManifest.xml file
  
  ```xml
    android:name="com.wepin.cm.loginlib.RedirectUriReceiverActivity"
    android:exported="true">
    <intent-filter>
       <action android:name="android.intent.action.VIEW" />
  
       <category android:name="android.intent.category.DEFAULT" />
       <category android:name="android.intent.category.BROWSABLE" />
       <data
        android:host="oauth2redirect"
        android:scheme="${appAuthRedirectScheme}" />
    </intent-filter>
  </activity>
  ```
- iOS
  You must add the app's URL scheme to the Info.plist file. This is necessary for redirection back to the app after the authentication process.
  The value of the URL scheme should be `'wepin.' + your Wepin app id`.
```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <string>Editor</string>
  			<key>CFBundleURLName</key>
  			<string>unique name</string>
        <array>
            <string>wepin + your Wepin app id</string>
        </array>
    </dict>
</array>
```

### OAuth Login Provider Setup

If you want to use OAuth login functionality (e.g., loginWithOauthProvider), you need to set up OAuth login providers.
To do this, you must first register your OAuth login provider information in the [Wepin Workspace](https://workspace.wepin.io/)
Navigate to the Login tap under the Developer Tools menu, click the App or Set Login Provider button in the Login Provider section, and complete the registration.

## ⏩ Import
```kotlin
  import com.wepin.cm.loginlib.WepinLogin
```

### ⏩ Initialization
Create instance of WepinLoginLibrary in shared code to use wepin and pass your activity(Android) or ViewController(iOS) as a parameter
This method is a suspend method, so you can call it within another suspend method or in a coroutine.
```kotlin
    class LoginManager(context: Any) {
        private val appId = "WEPIN_APP_ID"
        private val appKey = "WEPIN_APP_KEY"
        private val privateKey = "WEPIN_APP_PRIVATE_KEY"
        private val initOptions = WepinLoginOptions(
            context = context,
            appId = appId,
            appKey = appKey
        )
        private val wepinLogin = WepinLogin(initOptions)
    
        private fun initLoginManager(coroutineScope: CoroutineScope, setText: (String) -> Unit) {
            coroutineScope.launch {
                setText(
                    if (wepinLogin.isInitialized()) {
                        "It's already Initialized"
                    } else {
                        wepinLogin.init().toString()
                    },
                )
            }
        }
    }
```

### isInitialized
```kotlin
wepinLogin.isInitialized()
```
The `isInitialized()` method checks Wepin Login Library is initialized.

#### Returns
- \<Boolean>
    - true if Wepin Login Library is already initialized.

## ⏩ Method
Methods can be used after initialization of Wepin Login Library.


### loginWithOauthProvider
```kotlin
wepinLogin.loginWithOauthProvider(params)
```

An in-app browser will open and proceed to log in to the OAuth provider. To retrieve Firebase login information, you need to execute either the loginWithIdToken() or loginWithAccessToken() method.
This method is a suspend method, so you can call it within another suspend method or in a coroutine.

#### Parameters
- `params` \<LoginOauth2Params>
    - `provider` \<'google'|'naver'|'discord'|'apple'> - Provider for login
    - `clientId` \<String>

#### Returns
- \<LoginOauthResult>
    - `provider` \<String> - login provider
    - `token` \<String> - accessToken (if provider is "naver" or "discord") or idToken (if provider is "google" or "apple")
    - `type` \<OauthTokenType> - type of token

#### Exception
- [Wepin Error](#wepin-error)

#### Example
- kotlin
  ```kotlin
    val loginOption = LoginOauth2Params(
                        provider = "discord",
                        clientId = getString(R.string.default_discord_client_id),
                      )
    coroutineScope.launch { 
        try {
            val loginResponse = wepinLogin.loginWithOauthProvider(loginOption)
        } catch (e: Exception) {
            setResponse(null)
            setText("fail - $e")
        }
    }

  ```

### signUpWithEmailAndPassword
```kotlin
wepinLogin.signUpWithEmailAndPassword(params)
```

This function signs up on Wepin Firebase with your email and password. It returns Firebase login information upon successful signup.
This method is a suspend method, so you can call it within another suspend method or in a coroutine.

#### Parameters
- `params` \<LoginWithEmailParams>
    - `email` \<String> - User email
    - `password` \<String> -  User password
    - `locale` \<String> - __optional__ Language for the verification email (default value: "en")

#### Returns
- \<LoginResult>
    - `provider` \<Providers.EMAIL>
    - `token` \<FBToken>
        - `idToken` \<String> - wepin firebase idToken
        - `refreshToken` \<String> - wepin firebase refreshToken

#### Exception
- [Wepin Error](#wepin-error)

#### Example
- kotlin
  ```kotlin
    val loginOption = LoginWithEmailParams(email, password)
    coroutineScope.launch {
        try {
            val response = wepinLogin.signUpWithEmailAndPassword(loginOption)
            setResponse(response)
            setText("$response")
        } catch (e: Exception) {
            setText("fail - $e")
        }
    }
  ```

### loginWithEmailAndPassword
```kotlin
wepinLogin.loginWithEmailAndPassword(params)
```

This function logs in to the Wepin Firebase using your email and password. It returns Firebase login information upon successful login.
This method is a suspend method, so you can call it within another suspend method or in a coroutine.

#### Parameters
- `params` \<LoginWithEmailParams>
    - `email` \<String> - User email
    - `password` \<String> -  User password

#### Returns
- \<LoginResult>
    - `provider` \<Providers.EMAIL>
    - `token` \<FBToken>
        - `idToken` \<String> - wepin firebase idToken
        - `refreshToken` \<String> - wepin firebase refreshToken

#### Exception
- [Wepin Error](#wepin-error)

#### Example
- kotlin
  ```kotlin
    val loginOption = LoginWithEmailParams(email, password)
    coroutineScope.launch {
        try {
            val response = wepinLogin.loginWithEmailAndPassword(loginOption)
            setResponse(response)
            setText("$response")
        } catch (e: Exception) {
            setText("fail - $e")
        }
    }
  ```

### loginWithIdToken
```kotlin
wepinLogin.loginWithIdToken(params)
```

This function logs in to the Wepin Firebase using an external ID token. It returns Firebase login information upon successful login.
This method is a suspend method, so you can call it within another suspend method or in a coroutine.

#### Parameters
- `params` \<LoginOauthIdTokenRequest>
  - `token` \<String> - ID token value to be used for login
  - `sign` \<String> - __optional__ The signature value for the token provided as the first parameter.(Returned value of [getSignForLogin()](#getSignForLogin))

> [!NOTE]
> Starting from wepin-compose-sdk-login-v1 version 0.0.10, the sign value is optional.
>
> If you choose to remove the authentication key issued from the [Wepin Workspace](https://workspace.wepin.io/), you may opt not to use the `sign` value.
>
> (Wepin Workspace > Development Tools menu > Login tab > Auth Key > Delete)
> > The Auth Key menu is visible only if an authentication key was previously generated.


#### Returns
- \<LoginResult>
  - `provider` \<Providers.EXTERNAL_TOKEN>
  - `token` \<FBToken>
    - `idToken` \<String> - wepin firebase idToken
    - `refreshToken` \<String> - wepin firebase refreshToken

#### Exception
- [Wepin Error](#wepin-error)

#### Example
- kotlin
  ```kotlin
    coroutineScope.launch {
        try {
            val loginOption = LoginOauthIdTokenRequest(idToken = token)
            val loginResponse = wepinLogin.loginWithIdToken(loginOption)
            setResponse(loginResponse)
            setText("$loginResponse")
        } catch (e: Exception) {
            setResponse(null)
            setText("fail - ${e.message}")
        }
    }

  ```

### loginWithAccessToken
```kotlin
wepinLogin.loginWithAccessToken(params)
```

This function logs in to the Wepin Firebase using an external access token. It returns Firebase login information upon successful login.
This method is a suspend method, so you can call it within another suspend method or in a coroutine.

#### Parameters
- `params` \<LoginOauthAccessTokenRequest>
  - `provider` \<"naver"|"discord"> - Provider that issued the access token
  - `accessToken` \<String> - Access token value to be used for login
  - `sign` \<String> - __optional__ The signature value for the token provided as the first parameter. (Returned value of [getSignForLogin()](#getSignForLogin))

> [!NOTE]
> Starting from wepin-compose-sdk-login-v1 version 0.0.10, the sign value is optional.
>
> If you choose to remove the authentication key issued from the [Wepin Workspace](https://workspace.wepin.io/), you may opt not to use the `sign` value.
>
> (Wepin Workspace > Development Tools menu > Login tab > Auth Key > Delete)
> > The Auth Key menu is visible only if an authentication key was previously generated.

#### Returns
- \<LoginResult>
  - `provider` \<Providers.EXTERNAL_TOKEN>
  - `token` \<FBToken>
    - `idToken` \<String> - wepin firebase idToken
    - `refreshToken` \<String> - wepin firebase refreshToken


#### Exception
- [Wepin Error](#wepin-error)

#### Example
- kotlin
  ```kotlin
    coroutineScope.launch {
        try {
            val loginOption = LoginOauthAccessTokenRequest(provider, token)
            val loginResponse = wepinLogin.loginWithAccessToken(loginOption)
            setResponse(loginResponse)
            setText("$loginResponse")
        } catch (e: Exception) {
            setResponse(null)
            setText("fail - ${e.message}")
        }
    }

  ```

### getRefreshFirebaseToken
```kotlin
wepinLogin.getRefreshFirebaseToken()
```

This method retrieves the current firebase token's information from the Wepin.
This method is a suspend method, so you can call it within another suspend method or in a coroutine.

#### Parameters
- void

#### Returns
- \<LoginResult>
  - `provider` \<Providers>
  - `token` \<FBToken>
    - `idToken` \<String> - wepin firebase idToken
    - `refreshToken` \<String> - wepin firebase refreshToken

#### Exception
- [Wepin Error](#wepin-error)

#### Example
- kotlin
  ```kotlin
  coroutineScope.launch {
    try {
        val response = wepinLogin.getRefreshFirebaseToken()
        setText("$response")
        setResponse(response)
    } catch (e: Exception) {
        setText("fail - $e")
    }
  }
  ```

### loginFirebaseWithOauthProvider
```kotlin
wepinLogin.loginFirebaseWithOauthProvider(params)
```

This method combines the functionality of `loginWithOauthProvider`, `loginWithIdToken`, `loginWithAccessToken`.
It opens an in-app browser to log in to Wepin Firebase through the specified OAuth login provider. Upon successful login, it returns Firebase login information.
This method is a suspend method, so you can call it within another suspend method or in a coroutine.

### Supported Version
Supported from version *`0.0.10`* and later.

#### Parameters
- `params` \<LoginOauth2Params>
  - `provider` \<'google'|'naver'|'discord'|'apple'> - Provider for login
  - `clientId` \<String>


#### Returns
- \<LoginResult>
  - `provider` \<Providers.EXTERNAL_TOKEN>
  - `token` \<FBToken>
    - `idToken` \<String> - wepin firebase idToken
    - `refreshToken` \<String> - wepin firebase refreshToken

#### Exception
- [Wepin Error](#wepin-error)

#### Example
- kotlin
  ```kotlin
    val loginOption = LoginOauth2Params(
                        provider = "google",
                        clientId = "google-client-id",
                      )
    coroutineScope.launch { 
        try {
            val loginResponse = wepinLogin.loginFirebaseWithOauthProvider(loginOption)
        } catch (e: Exception) {
            setResponse(null)
            setText("fail - $e")
        }
    }

  ```

### loginWepinWithOauthProvider
```kotlin
wepinLogin.loginWepinWithOauthProvider(params)
```

This method combines the functionality of `loginFirebaseWithOauthProvider` and `loginWepin`.
It opens an in-app browser to log in to Wepin through the specified OAuth login provider. Upon successful login, it returns Wepin user information.
This method is a suspend method, so you can call it within another suspend method or in a coroutine.

> [!CAUTION]
> This method can only be used after the authentication key has been deleted from the [Wepin Workspace](https://workspace.wepin.io/)
>
> (Wepin Workspace > Delvelopment Tools menu > Login tap > Auth Key > Delete)
> The Auth Key menu is visible only if an authentication key was previously generated.

### Supported Version
Supported from version *`0.0.10`* and later.

#### Parameters
- `params` \<LoginOauth2Params>
  - `provider` \<'google'|'naver'|'discord'|'apple'> - Provider for login
  - `clientId` \<String>


#### Returns
- \<WepinUser> __optional__ - A promise that resolves to an object containing the user's login status and information. The object includes:
  - status \<'success'|'fail'>  - The login status.
  - userInfo \<UserInfo> __optional__ - The user's information, including:
    - userId \<String> - The user's ID.
    - email \<String> - The user's email.
    - provider \<'google'|'apple'|'naver'|'discord'|'email'|'external_token'> - The login provider.
    - use2FA \<Boolean> - Whether the user uses two-factor authentication.
  - walletId \<String> = The user's wallet ID.
  - userStatus: \<UserStatus> - The user's status of wepin login. including:
    - loginStats: \<'complete' | 'pinRequired' | 'registerRequired'> - If the user's loginStatus value is not complete, it must be registered in the wepin.
    - pinRequired?: \<Boolean>
  - token: \<Token> - The user's token of wepin.
    - accessToken: \<String>
    - refreshToken \<String>

#### Exception
- [Wepin Error](#wepin-error)

#### Example
- kotlin
  ```kotlin
    val loginOption = LoginOauth2Params(
                        provider = "google",
                        clientId = "google-client-id",
                      )
    coroutineScope.launch { 
        try {
            val userInfo = wepinLogin.loginWepinWithOauthProvider(loginOption)
        } catch (e: Exception) {
            setResponse(null)
            setText("fail - $e")
        }
    }

  ```

### loginWepinWithEmailAndPassword
```kotlin
wepinLogin.loginWepinWithEmailAndPassword(params)
```

This method integrates the functions of `loginWithEmailAndPassword` and `loginWepin`.
The `loginWepinWithEmailAndPassword` method logs the user into Wepin using the provided email and password. Upon successful login, it returns Wepin user information.
This method is a suspend method, so you can call it within another suspend method or in a coroutine.

### Supported Version
Supported from version *`0.0.10`* and later.

#### Parameters
- `params` \<LoginWithEmailParams>
  - `email` \<String> - User email
  - `password` \<String> -  User password

#### Returns
- \<WepinUser> __optional__ - A promise that resolves to an object containing the user's login status and information. The object includes:
  - status \<'success'|'fail'>  - The login status.
  - userInfo \<UserInfo> __optional__ - The user's information, including:
    - userId \<String> - The user's ID.
    - email \<String> - The user's email.
    - provider \<'google'|'apple'|'naver'|'discord'|'email'|'external_token'> - The login provider.
    - use2FA \<Boolean> - Whether the user uses two-factor authentication.
  - walletId \<String> = The user's wallet ID.
  - userStatus: \<UserStatus> - The user's status of wepin login. including:
    - loginStats: \<'complete' | 'pinRequired' | 'registerRequired'> - If the user's loginStatus value is not complete, it must be registered in the wepin.
    - pinRequired?: \<Boolean>
  - token: \<Token> - The user's token of wepin.
    - accessToken: \<String>
    - refreshToken \<String>

#### Exception
- [Wepin Error](#wepin-error)

#### Example
- kotlin
  ```kotlin
    val loginOption = LoginWithEmailParams(email, password)
    coroutineScope.launch {
        try {
            val response = wepinLogin.loginWepinWithEmailAndPassword(loginOption)
            setResponse(response)
            setText("$response")
        } catch (e: Exception) {
            setText("fail - $e")
        }
    }
  ```

### loginWepinWithIdToken
```kotlin
wepinLogin.loginWepinWithIdToken(params)
```

This method integrates the funcions of `loginWithIdToken` and `loginWepin`.
The `loginWepinWithIdToken` method logs the user info Wepin using an external ID token. Upon successful login, it returns Wepin user information.
This method is a suspend method, so you can call it within another suspend method or in a coroutine.

### Supported Version
Supported from version *`0.0.10`* and later.

#### Parameters
- `params` \<LoginOauthIdTokenRequest>
  - `token` \<String> - ID token value to be used for login
  - `sign` \<String> - __optional__ The signature value for the token provided as the first parameter.(Returned value of [getSignForLogin()](#getSignForLogin))

> [!NOTE]
> Starting from wepin-compose-sdk-login-v1 version 0.0.10, the sign value is optional.
>
> If you choose to remove the authentication key issued from the [Wepin Workspace](https://workspace.wepin.io/), you may opt not to use the `sign` value.
>
> (Wepin Workspace > Development Tools menu > Login tab > Auth Key > Delete)
> > The Auth Key menu is visible only if an authentication key was previously generated.


#### Returns
- \<WepinUser> __optional__ - A promise that resolves to an object containing the user's login status and information. The object includes:
  - status \<'success'|'fail'>  - The login status.
  - userInfo \<UserInfo> __optional__ - The user's information, including:
    - userId \<String> - The user's ID.
    - email \<String> - The user's email.
    - provider \<'google'|'apple'|'naver'|'discord'|'email'|'external_token'> - The login provider.
    - use2FA \<Boolean> - Whether the user uses two-factor authentication.
  - walletId \<String> = The user's wallet ID.
  - userStatus: \<UserStatus> - The user's status of wepin login. including:
    - loginStats: \<'complete' | 'pinRequired' | 'registerRequired'> - If the user's loginStatus value is not complete, it must be registered in the wepin.
    - pinRequired?: \<Boolean>
  - token: \<Token> - The user's token of wepin.
    - accessToken: \<String>
    - refreshToken \<String>

#### Exception
- [Wepin Error](#wepin-error)

#### Example
- kotlin
  ```kotlin
    coroutineScope.launch {
        try {
            val loginOption = LoginOauthIdTokenRequest(idToken = token)
            val userInfo = wepinLogin.loginWepinWithIdToken(loginOption)
        } catch (e: Exception) {
            setResponse(null)
            setText("fail - ${e.message}")
        }
    }

  ```

### loginWepinWithAccessToken
```kotlin
wepinLogin.loginWithAccessToken(params)
```

This method integrates the functions of `loginWithAccessToken` and `loginWepin`.
The `loginWepinWithAccessToken` method logs the user into Wepin using an external access token. Upon successful login, it returns Wepin user information.
This method is a suspend method, so you can call it within another suspend method or in a coroutine.

### Supported Version
Supported from version *`0.0.10`* and later.

#### Parameters
- `params` \<LoginOauthAccessTokenRequest>
  - `provider` \<"naver"|"discord"> - Provider that issued the access token
  - `accessToken` \<String> - Access token value to be used for login
  - `sign` \<String> - __optional__ The signature value for the token provided as the first parameter. (Returned value of [getSignForLogin()](#getSignForLogin))

> [!NOTE]
> Starting from wepin-compose-sdk-login-v1 version 0.0.10, the sign value is optional.
>
> If you choose to remove the authentication key issued from the [Wepin Workspace](https://workspace.wepin.io/), you may opt not to use the `sign` value.
>
> (Wepin Workspace > Development Tools menu > Login tab > Auth Key > Delete)
> > The Auth Key menu is visible only if an authentication key was previously generated.

#### Returns
- \<WepinUser> __optional__ - A promise that resolves to an object containing the user's login status and information. The object includes:
  - status \<'success'|'fail'>  - The login status.
  - userInfo \<UserInfo> __optional__ - The user's information, including:
    - userId \<String> - The user's ID.
    - email \<String> - The user's email.
    - provider \<'google'|'apple'|'naver'|'discord'|'email'|'external_token'> - The login provider.
    - use2FA \<Boolean> - Whether the user uses two-factor authentication.
  - walletId \<String> = The user's wallet ID.
  - userStatus: \<UserStatus> - The user's status of wepin login. including:
    - loginStats: \<'complete' | 'pinRequired' | 'registerRequired'> - If the user's loginStatus value is not complete, it must be registered in the wepin.
    - pinRequired?: \<Boolean>
  - token: \<Token> - The user's token of wepin.
    - accessToken: \<String>
    - refreshToken \<String>


#### Exception
- [Wepin Error](#wepin-error)

#### Example
- kotlin
  ```kotlin
    coroutineScope.launch {
        try {
            val loginOption = LoginOauthAccessTokenRequest(provider, token)
            val userInfo = wepinLogin.loginWepinWithAccessToken(loginOption)
            setResponse(userInfo)
            setText("$userInfo")
        } catch (e: Exception) {
            setResponse(null)
            setText("fail - ${e.message}")
        }
    }

  ```

### loginWepin
```kotlin
wepinLogin.loginWepin(param)
```

This method logs the user into the Wepin application using the specified provider and token.
This method is a suspend method, so you can call it within another suspend method or in a coroutine.

#### Parameters
The parameters should utilize the return values from the `loginWithEmailAndPassword()`, `loginWithIdToken()`, and `loginWithAccessToken()` methods within this module.

- \<LoginResult>
  - `provider` \<Providers>
  - `token` \<FBToken>
    - `idToken` \<String> - Wepin Firebase idToken
    - `refreshToken` \<String> - Wepin Firebase refreshToken

#### Returns
- \<WepinUser> __optional__ - A promise that resolves to an object containing the user's login status and information. The object includes:
  - status \<'success'|'fail'>  - The login status.
  - userInfo \<UserInfo> __optional__ - The user's information, including:
    - userId \<String> - The user's ID.
    - email \<String> - The user's email.
    - provider \<'google'|'apple'|'naver'|'discord'|'email'|'external_token'> - The login provider.
    - use2FA \<Boolean> - Whether the user uses two-factor authentication.
  - walletId \<String> = The user's wallet ID.
  - userStatus: \<UserStatus> - The user's status of wepin login. including:
    - loginStats: \<'complete' | 'pinRequired' | 'registerRequired'> - If the user's loginStatus value is not complete, it must be registered in the wepin.
    - pinRequired?: \<Boolean>
  - token: \<Token> - The user's token of wepin.
    - accessToken: \<String>
    - refreshToken \<String>

#### Exception
- [Wepin Error](#wepin-error)

#### Example
- kotlin
  ```kotlin
    coroutineScope.launch {
        try {
            val response = wepinLogin.loginWepin(loginResult!!)
            setText("$response")
        } catch (e: Exception) {
            setText("fail - ${e.message}")
        }
    }
  ```

### getCurrentWepinUser
```kotlin
wepinLogin.getCurrentWepinUser()
```

This method retrieves the current logged-in user's information from the Wepin.
This method is a suspend method, so you can call it within another suspend method or in a coroutine.

#### Parameters
- void

#### Returns
- \<WepinUser> __optional__ - A promise that resolves to an object containing the user's login status and information. The object includes:
  - status \<'success'|'fail'>  - The login status.
  - userInfo \<UserInfo> __optional__ - The user's information, including:
    - userId \<String> - The user's ID.
    - email \<String> - The user's email.
    - provider \<'google'|'apple'|'naver'|'discord'|'email'|'external_token'> - The login provider.
    - use2FA \<Boolean> - Whether the user uses two-factor authentication.
  - walletId \<String> = The user's wallet ID.
  - userStatus: \<UserStatus> - The user's status of wepin login. including:
    - loginStats: \<'complete' | 'pinRequired' | 'registerRequired'> - If the user's loginStatus value is not complete, it must be registered in the wepin.
    - pinRequired?: \<Boolean>
  - token: \<Token> - The user's token of wepin.
    - accessToken: \<String>
    - refreshToken \<String>

#### Exception
- [Wepin Error](#wepin-error)

#### Example
- kotlin
  ```kotlin
    coroutineScope.launch {
        try {
            val response = wepinLogin.getCurrentWepinUser()
            setText("$response")
        } catch (e: Exception) {
            setText("fail - $e")
        }
    }
  ```

### logoutWepin
```kotlin
wepinLogin.logoutWepin()
```

The `logoutWepin()` method logs out the user logged into Wepin.
This method is a suspend method, so you can call it within another suspend method or in a coroutine.

#### Parameters
- void
#### Returns
- \<Boolean>

#### Exception
- [Wepin Error](#wepin-error)

#### Example

```kotlin
coroutineScope.launch {
  try {
    val response = wepinLogin.logoutWepin()
    setText("$response")
  } catch (e: Exception) {
    setText("fail - ${e.message}")
  }
}
```

### getSignForLogin
Generates signatures to verify the issuer. It is mainly used to generate signatures for login-related information such as ID tokens and access tokens.

```kotlin
wepinLogin.getSignForLogin(privKey, message)
```

#### Parameters
- `privKey` \<String> - The authentication key used for signature generation.
- `message` \<String> - The message or payload to be signed.

#### Returns
- String - The generated signature.

> ‼️ Caution ‼️
>
> The authentication key (`privKey`) must be stored securely and must not be exposed to the outside. It is recommended to execute the `getSignForLogin()` method on the backend rather than the frontend for enhanced security and protection of sensitive information.

#### Example
- kotlin
  ```kotlin
  val response = wepinLogin.getSignForLogin(privateKey, token)
  ```

### finalize
```kotln
wepinLogin.finalize()
```

The `finalize()` method finalizes the Wepin Login Library.

#### Parameters
- void
#### Returns
- void

#### Example
```kotlin
wepinLogin.finalize()
```


### Wepin Error
| Error Code                   | Error Message                      | Error Description                                                                                   |
|------------------------------|------------------------------------|-----------------------------------------------------------------------------------------------------|
| `INVALID_APP_KEY`            | "Invalid app key"                  | The Wepin app key is invalid.                                                                       |
| `INVALID_PARAMETER` `        | "Invalid parameter"                | One or more parameters provided are invalid or missing.                                             |
| `INVALID_LOGIN_PROVIDER`     | "Invalid login provider"           | The login provider specified is not supported or is invalid.                                        |
| `INVALID_TOKEN`              | "Token does not exist"             | The token does not exist.                                                                           |
| `INVALID_LOGIN_SESSION`      | "Invalid Login Session"            | The login session information does not exist.                                                                           |
| `NOT_INITIALIZED_ERROR`      | "Not initialized error"            | The WepinLoginLibrary has not been properly initialized.                                            |
| `ALREADY_INITIALIZED_ERROR`  | "Already initialized"              | The WepinLoginLibrary is already initialized, so the logout operation cannot be performed again.    |
| `NOT_ACTIVITY`               | "Context is not activity"          | The Context is not an activity                                                                      |
| `USER_CANCELLED`             | "User cancelled"                   | The user has cancelled the operation.                                                               |
| `UNKNOWN_ERROR`              | "An unknown error occurred"        | An unknown error has occurred, and the cause is not identified.                                     |
| `NOT_CONNECTED_INTERNET`     | "No internet connection"           | The system is unable to detect an active internet connection.                                       |
| `FAILED_LOGIN`               | "Failed to Oauth log in"           | The login attempt has failed due to incorrect credentials or other issues.                          |
| `ALREADY_LOGOUT`             | "Already Logout"                   | The user is already logged out, so the logout operation cannot be performed again.                  |
| `INVALID_EMAIL_DOMAIN`       | "Invalid email domain"             | The provided email address's domain is not allowed or recognized by the system.                     |
| `FAILED_SEND_EMAIL`          | "Failed to send email"             | The system encountered an error while sending an email. This is because the email address is invalid or we sent verification emails too often. Please change your email or try again after 1 minute.                   |
| `REQUIRED_EMAIL_VERIFIED`    | "Email verification required"      | Email verification is required to proceed with the requested operation.                             |
| `INCORRECT_EMAIL_FORM`       | "Incorrect email format"           | The provided email address does not match the expected format.                                      |
| `INCORRECT_PASSWORD_FORM`    | "Incorrect password format"        | The provided password does not meet the required format or criteria.                                |
| `NOT_INITIALIZED_NETWORK`    | "Network Manager not initialized"  | The network or connection required for the operation has not been properly initialized.             |
| `REQUIRED_SIGNUP_EMAIL`      | "Email sign-up required."          | The user needs to sign up with an email address to proceed.                                         |
| `FAILED_EMAIL_VERIFIED`      | "Failed to verify email."          | The WepinLoginLibrary encountered an issue while attempting to verify the provided email address.   |
| `FAILED_PASSWORD_SETTING`    | "Failed to set password."          | The WepinLoginLibrary failed to set the password.                                                   |
| `EXISTED_EMAIL`              | "Email already exists."            | The provided email address is already registered in Wepin.                                          |
