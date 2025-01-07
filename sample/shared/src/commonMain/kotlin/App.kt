import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wepin.cm.loginlib.WepinLogin
import com.wepin.cm.loginlib.types.*
import com.wepin.cm.loginlib.types.network.LoginOauthAccessTokenRequest
import com.wepin.cm.loginlib.types.network.LoginOauthIdTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class LoginManager(
    context: Any
) {
    var loginResult: LoginResult? by mutableStateOf(null)
    private val appId = "6bf47fc3fbebd80d2792e359e0480f4c"
    private val appKey = "ak_dev_7zgBD2Oo5p2AfHKzU6xaEbSWU4XgknIVZgtj6PhWMYn"
    private val privateKey = "f4de6c55a8199107baf4afa95be40456688814bfc723ea2f22bb24675fcdf730"
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

    private fun isInitialized(setText: (String) -> Unit) {
        setText(
            if (wepinLogin.isInitialized()) {
                "Initialized"
            } else {
                "is NOT initialized"
            },
        )
    }

    private fun signupWithEmail(
        email: String,
        password: String,
        coroutineScope: CoroutineScope,
        setResponse: (LoginResult?) -> Unit,
        setText: (String) -> Unit,
    ) {
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
    }

    private fun loginWithEmail(
        email: String,
        password: String,
        withWepinLogin: Boolean,
        coroutineScope: CoroutineScope,
        setResponse: (LoginResult?) -> Unit,
        setText: (String) -> Unit,
    ) {
        val loginOption = LoginWithEmailParams(email, password)
        coroutineScope.launch {
            try {
                if (!withWepinLogin) {
                    val response = wepinLogin.loginWithEmailAndPassword(loginOption)
                    setResponse(response)
                    setText("$response")
                } else {
                    val wepinUser = wepinLogin.loginWepinWithEmailAndPassword(loginOption)
                    setText("$wepinUser")
                }
            } catch (e: Exception) {
                setText("fail - $e")
            }
        }
    }

    private fun loginOauth(
        provider: String,
        clientId: String,
        tokenType: OauthTokenType,
        withWepinLogin: Boolean,
        coroutineScope: CoroutineScope,
        setResponse: (LoginResult?) -> Unit,
        setText: (String) -> Unit,
    ) {
        val loginOption = LoginOauth2Params(provider, clientId)
        coroutineScope.launch {
            try {
                val loginResponse = wepinLogin.loginWithOauthProvider(loginOption)
                println("yskim_test idToken ${loginResponse.token}");
                when (tokenType) {
                    OauthTokenType.ID_TOKEN -> {
                        loginIdToken(
                            loginResponse.token,
                            withWepinLogin,
                            setResponse,
                            setText,
                            coroutineScope,
                        )
                    }

                    OauthTokenType.ACCESS_TOKEN -> {
                        loginAccessToken(
                            loginResponse.provider,
                            loginResponse.token,
                            withWepinLogin,
                            setResponse,
                            setText,
                            coroutineScope,
                        )
                    }

                    else -> {
                        setResponse(loginResponse as LoginResult)
                    }
                }
            } catch (e: Exception) {
                setResponse(null)
                setText("fail - $e")
            }
        }
    }

    private fun loginIdToken(
        token: String,
        withWepinLogin: Boolean,
        setResponse: (LoginResult?) -> Unit,
        setText: (String) -> Unit,
        coroutineScope: CoroutineScope,
    ) {
        coroutineScope.launch {
            try {
//                val sign = wepinLogin.getSignForLogin(
//                    privateKey,
//                    token,
//                )
//                val loginOption = LoginOauthIdTokenRequest(idToken = token, sign = sign)
                val loginOption = LoginOauthIdTokenRequest(idToken = token)
                if (!withWepinLogin) {
                    val loginResponse = wepinLogin.loginWithIdToken(loginOption)
                    setResponse(loginResponse)
                    setText("$loginResponse")
                } else {
                    val wepinUser = wepinLogin.loginWepinWithIdToken(loginOption)
                    setText("$wepinUser")
                }
            } catch (e: Exception) {
                setResponse(null)
                setText("fail - ${e.message}")
            }
        }
    }

    private fun loginAccessToken(
        provider: String,
        token: String,
        withWepinLogin: Boolean,
        setResponse: (LoginResult?) -> Unit,
        setText: (String) -> Unit,
        coroutineScope: CoroutineScope,
    ) {
        coroutineScope.launch {
            try {
//                val sign = wepinLogin.getSignForLogin(
//                    privateKey,
//                    token,
//                )
//                val loginOption = LoginOauthAccessTokenRequest(provider, token, sign)
                val loginOption = LoginOauthAccessTokenRequest(provider, token)
                if (!withWepinLogin) {
                    val loginResponse = wepinLogin.loginWithAccessToken(loginOption)
                    setResponse(loginResponse)
                    setText("$loginResponse")
                } else {
                    val wepinUser = wepinLogin.loginWepinWithAccessToken(loginOption)
                    setText("$wepinUser")
                }
            } catch (e: Exception) {
                setResponse(null)
                setText("fail - ${e.message}")
            }
        }
    }

    private fun loginWepin(coroutineScope: CoroutineScope, setText: (String) -> Unit) {
        coroutineScope.launch {
            try {
                val response = wepinLogin.loginWepin(loginResult!!)
                setText("$response")
            } catch (e: Exception) {
                setText("fail - ${e.message}")
            }
        }
    }

    private fun logoutWepin(coroutineScope: CoroutineScope, setText: (String) -> Unit) {
        coroutineScope.launch {
            try {
                val response = wepinLogin.logoutWepin()
                setText("$response")
            } catch (e: Exception) {
                setText("fail - ${e.message}")
            }
        }
    }

    private fun loginFirebaseWithOauth(coroutineScope: CoroutineScope, provider: String, setText: (String) -> Unit) {
        coroutineScope.launch {
            try {
                setText("Processing...")
                var clientId: String? = null
                if (provider == "google") clientId = "1006602884997-ti7v6ldm1obtu209uousvfkh2e58klf6.apps.googleusercontent.com"
                else if (provider == "discord") clientId = "DISCORD_CLIENT_ID"
                else if (provider == "naver") clientId = "NAVER_CLIENT_ID"
                else if (provider == "apple") clientId = "APPLE_SERVICE_ID"
                else setText("invalid provider")
                val parameter = LoginOauth2Params(provider = provider, clientId = clientId!!)
                val response = wepinLogin.loginFirebaseWithOauthProvider(parameter)
                setText("$response")
            } catch (e: Exception) {
                setText("$e")
            }
        }
    }

    private fun loginWepinWithOauthProvider(coroutineScope: CoroutineScope, provider: String, setText: (String) -> Unit) {
        coroutineScope.launch {
            try {
                setText("Processing...")
                var clientId: String? = null
                if (provider == "google") clientId = "1006602884997-ti7v6ldm1obtu209uousvfkh2e58klf6.apps.googleusercontent.com"
                else if (provider == "discord") clientId = "DISCORD_CLIENT_ID"
                else if (provider == "naver") clientId = "NAVER_CLIENT_ID"
                else if (provider == "apple") clientId = "APPLE_SERVICE_ID"
                else setText("invalid provider")
                val parameter = LoginOauth2Params(provider = provider, clientId = clientId!!)
                val response = wepinLogin.loginWepinWithOauthProvider(parameter)
                setText("$response")
            } catch (e: Exception) {
                setText("$e")
            }
        }
    }

    private fun wepinLoginIsInitialized(
        setText: (String) -> Unit,
        isInitialized: Boolean? = false
    ): Boolean {
        val isInit: Boolean = wepinLogin.isInitialized()
        if (!isInit) {
            setText("is NOT initialized")
        } else if (isInitialized == true) {
            setText("initialized")
        } else {
            setText("Processing...")
        }
        return isInit
    }

    fun handleItemClick(
        item: String,
        coroutineScope: CoroutineScope,
        setResponse: (LoginResult?) -> Unit,
        setItem: (String) -> Unit,
        setText: (String) -> Unit
    ) {
        setItem(item)
        if (item !== "init") {
            if (!wepinLoginIsInitialized(setText)) {
                return
            }
        }
        when (item) {
            "init" -> initLoginManager(coroutineScope, setText)
            "isInitialized" -> isInitialized(setText)
            "Login Firebase with Oauth Provider - google" -> {
                loginFirebaseWithOauth(coroutineScope, "google", setText)
            }
            "Login Firebase with Oauth Provider - apple" -> {
                loginFirebaseWithOauth(coroutineScope, "apple", setText)
            }
            "Login Firebase with Oauth Provider - discord" -> {
                loginFirebaseWithOauth(coroutineScope, "discord", setText)
            }
            "Login Firebase with Oauth Provider - naver" -> {
                loginFirebaseWithOauth(coroutineScope, "naver", setText)
            }
            "Login Wepin with Oauth Provider" -> {

                loginWepinWithOauthProvider(coroutineScope, "google", setText)
            }
            "Login Wepin with IdToken" -> {
                loginOauth(provider = "google",
                    clientId = "1006602884997-ti7v6ldm1obtu209uousvfkh2e58klf6.apps.googleusercontent.com",
                    tokenType = OauthTokenType.ID_TOKEN,
                    true,
                    coroutineScope = coroutineScope,
                    setResponse = setResponse,
                    setText = setText,)
            }
            "Login Wepin with AccessToken" -> {
                loginOauth(
                    provider = "discord",
                    clientId = "DISCORD_CLIENT_ID",
                    tokenType = OauthTokenType.ACCESS_TOKEN,
                    true,
                    coroutineScope = coroutineScope,
                    setResponse = setResponse,
                    setText = setText,
                )
            }
            "Login Wepin with Email And Password" -> {
                loginWithEmail(
                    email = "yiriysk@gmail.com",
                    //email = "yskim@iotrust.kr",
                    password = "wepintest1019!",
                    true,
                    coroutineScope = coroutineScope,
                    setResponse = setResponse,
                    setText = setText,
                )
            }
            "Email Signup" -> signupWithEmail(
                email = "yiriysk@gmail.com",
                //email = "tysust95@gmail.com",
                //email = "ystest976@gmail.com",
                password = "wepintest1019!",
//                email = "yskim@iotrust.kr",
//                password = "wepintest1019!",
                coroutineScope = coroutineScope,
                setResponse = setResponse,
                setText = setText,
            )

            "Email Login" -> loginWithEmail(
                email = "yiriysk@gmail.com",
                //email = "tysust95@gmail.com",
                //email = "ystest976@gmail.com",
                //password = "wepintest1019!",
                password = "wepintest1019!!!!!",
//                email = "yskim@iotrust.kr",
//                password = "wepintest1019!",
//                email = "js.hong@iotrust.kr",
//                password = "doublebk13!@",
                false,
                coroutineScope = coroutineScope,
                setResponse = setResponse,
                setText = setText,
            )

            "Oauth Login(Google)" -> loginOauth(
                provider = "google",
                clientId = "1006602884997-ti7v6ldm1obtu209uousvfkh2e58klf6.apps.googleusercontent.com",
                tokenType = OauthTokenType.ID_TOKEN,
                false,
                coroutineScope = coroutineScope,
                setResponse = setResponse,
                setText = setText,
            )

            "Oauth Login(Apple)" -> loginOauth(
                provider = "apple",
                clientId = "APPLE_SERVICE_ID",
                tokenType = OauthTokenType.ID_TOKEN,
                false,
                coroutineScope = coroutineScope,
                setResponse = setResponse,
                setText = setText,
            )

            "Oauth Login(Discord)" -> loginOauth(
                provider = "discord",
                clientId = "DISCORD_CLIENT_ID",
                tokenType = OauthTokenType.ACCESS_TOKEN,
                false,
                coroutineScope = coroutineScope,
                setResponse = setResponse,
                setText = setText,
            )

            "Oauth Login(Naver)" -> loginOauth(
                provider = "naver",
                clientId = "NAVER_CLIENT_ID",
                tokenType = OauthTokenType.ACCESS_TOKEN,
                false,
                coroutineScope = coroutineScope,
                setResponse = setResponse,
                setText = setText,
            )

            "IdToken Login" -> {
                setText("isProcessing")
                //val token = "IdToken"
                val token = "eyJhbGciOiJSUzI1NiIsImtpZCI6Ijg5Y2UzNTk4YzQ3M2FmMWJkYTRiZmY5NWU2Yzg3MzY0NTAyMDZmYmEiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiIxMDA2NjAyODg0OTk3LXRpN3Y2bGRtMW9idHUyMDl1b3VzdmZraDJlNThrbGY2LmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwiYXVkIjoiMTAwNjYwMjg4NDk5Ny10aTd2NmxkbTFvYnR1MjA5dW91c3Zma2gyZTU4a2xmNi5hcHBzLmdvb2dsZXVzZXJjb250ZW50LmNvbSIsInN1YiI6IjEwMDc1NjE3MjA4MjIzMTY3OTA5OSIsImhkIjoiaW90cnVzdC5rciIsImVtYWlsIjoieXNraW1AaW90cnVzdC5rciIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJhdF9oYXNoIjoiVFBLeUhzNXMxT0J6eEdMeldPaGxKQSIsIm5vbmNlIjoiRHRtdWNqR2dGSng2b2pDRl9ZcEtvdyIsIm5hbWUiOiLquYDsmIHsg4EiLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2EvQUNnOG9jTDRycUxTRUFzSHpISEFFTTJKdEZPZWkzdkJJZ21DSDNxNGM2NnNiQi03Y1h6ZlNnPXM5Ni1jIiwiZ2l2ZW5fbmFtZSI6IuyYgeyDgSIsImZhbWlseV9uYW1lIjoi6rmAIiwiaWF0IjoxNzM2MTM0MDE5LCJleHAiOjE3MzYxMzc2MTl9.FNzAU6jaCpPXFZSUJ_6ckYYh6__4fXNQNGFhy5v6WTjNPU5HdmEybVTom299sAhFscVHjokLGAuKhdicXvEriQprM4A8Etd-upCAhR0OZVbq3c6uWAU1GhvoIi_nq4DMXUefn0Je2ZFZ26SLhGNDFN_5OojREJ3rNepV6c2RjwjTwh3jABfwfi7bzprawLjsGO3tTZrC3cojFsTVkUzXuHnKeDjyw3G2BWrDUfuHH2D3R0Nn49BYNS-o-duTqgtTY2LLA0HzswvWDVL31JymDe0qZ7hQTmUcfHzqI4ka6zLIyNqOnCCfzt8on-U1TnNeNtmi1FmoeRGaRy3IphxpmQ" // yskim@iotrust.kr
                loginIdToken(
                    token = token,
                    false,
                    setResponse = setResponse,
                    setText = setText,
                    coroutineScope = coroutineScope
                )
            }

            "AccessToken Login" -> {
                val token = "AccessToken"
                loginAccessToken(
                    provider = "discord",
                    token = token,
                    false,
                    setResponse = setResponse,
                    setText = setText,
                    coroutineScope = coroutineScope,
                )
            }

            "RefreshFirebaseToken" -> {
                coroutineScope.launch {
                    try {
                        val response = wepinLogin.getRefreshFirebaseToken()
                        setText("$response")
                        setResponse(response)
                    } catch (e: Exception) {
                        setText("fail - $e")
                    }
                }

            }

            "Wepin Login" -> loginWepin(coroutineScope, setText)
            "Wepin Logout" -> logoutWepin(coroutineScope, setText)
            "Get WepinUser" -> {
                coroutineScope.launch {
                    try {
                        val response = wepinLogin.getCurrentWepinUser()
                        setText("$response")
                    } catch (e: Exception) {
                        setText("fail - $e")
                    }
                }
            }

            "getSignForLogin" -> {
                val token = ""
                try {
                    val response = wepinLogin.getSignForLogin(
                        privateKey,
                        token
                    )
                    setText(response)
                } catch (e: Exception) {
                    setText("fail - $e")
                }
            }

            "Finalize" -> {
                val response = wepinLogin.finalize()
                setText("$response")
            }

            else -> {}
        }
    }
}

@Composable
fun App(context: Any) {
    val testItem: Array<String> = getMenuList()
    var item by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("Your long text goes here...") }
    val coroutineScope = rememberCoroutineScope()
    val loginManager = remember { LoginManager(context) }

    MaterialTheme {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Header()
            ScrollableContent(
                testItem = testItem,
                onItemClicked = { selectedItem ->
                    loginManager.handleItemClick(
                        item = selectedItem,
                        coroutineScope = coroutineScope,
                        setResponse = { loginManager.loginResult = it },
                        setItem = { item = it },
                        setText = { text = it }
                    )
                    item = selectedItem
                }
            )
            ResultBox(item = item, text = text)
        }
    }
}

@Composable
fun CustomButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors =
        ButtonDefaults.outlinedButtonColors(
            backgroundColor = Color.White,
            contentColor = Color.Black,
        ),
        border = null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            Text(text)
        }
    }
}

@Composable
fun Header() {
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Sample Login Library",
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
fun ScrollableContent(
    testItem: Array<String>,
    onItemClicked: (String) -> Unit
) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(400.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        testItem.forEach { menuItem ->
            CustomButton(onClick = { onItemClicked(menuItem) }, text = menuItem)
        }
    }
}

@Composable
fun ResultBox(
    item: String,
    text: String,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Result",
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.Center),
        )
    }
    Text(
        text = "Item: $item\nResult: $text",
        modifier =
        Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(color = Color.LightGray),
    )
}
