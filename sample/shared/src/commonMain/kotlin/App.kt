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
    private val appId = "d91d31ce2562adfa7bdceb31986ee270"
    private val appKey = "ak_dev_B1M18MQJYwr0L0fgbzO8QAd1US1XJUhmJrGa5S8tBbB"
    private val privateKey = "f49a54f62a2705e4371ad751532cb852b1d4e2d4392293c000b403c1c48d5c62"
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
        coroutineScope: CoroutineScope,
        setResponse: (LoginResult?) -> Unit,
        setText: (String) -> Unit,
    ) {
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
    }

    private fun loginOauth(
        provider: String,
        clientId: String,
        tokenType: OauthTokenType,
        coroutineScope: CoroutineScope,
        setResponse: (LoginResult?) -> Unit,
        setText: (String) -> Unit,
    ) {
        val loginOption = LoginOauth2Params(provider, clientId)
        coroutineScope.launch {
            try {
                val loginResponse = wepinLogin.loginWithOauthProvider(loginOption)
                when (tokenType) {
                    OauthTokenType.ID_TOKEN -> {
                        loginIdToken(
                            loginResponse.token,
                            setResponse,
                            setText,
                            coroutineScope,
                        )
                    }

                    OauthTokenType.ACCESS_TOKEN -> {
                        loginAccessToken(
                            loginResponse.provider,
                            loginResponse.token,
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
        setResponse: (LoginResult?) -> Unit,
        setText: (String) -> Unit,
        coroutineScope: CoroutineScope,
    ) {
        coroutineScope.launch {
            try {
                val sign = wepinLogin.getSignForLogin(
                    privateKey,
                    token,
                )
                val loginOption = LoginOauthIdTokenRequest(idToken = token, sign = sign)
                val loginResponse = wepinLogin.loginWithIdToken(loginOption)
                setResponse(loginResponse)
                setText("$loginResponse")
            } catch (e: Exception) {
                setResponse(null)
                setText("fail - ${e.message}")
            }
        }
    }

    private fun loginAccessToken(
        provider: String,
        token: String,
        setResponse: (LoginResult?) -> Unit,
        setText: (String) -> Unit,
        coroutineScope: CoroutineScope,
    ) {
        coroutineScope.launch {
            try {
                val sign = wepinLogin.getSignForLogin(
                    privateKey,
                    token,
                )
                val loginOption = LoginOauthAccessTokenRequest(provider, token, sign)
                val loginResponse = wepinLogin.loginWithAccessToken(loginOption)
                setResponse(loginResponse)
                setText("$loginResponse")
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
            "Email Signup" -> signupWithEmail(
                email = "email",
                password = "password",
                coroutineScope = coroutineScope,
                setResponse = setResponse,
                setText = setText,
            )

            "Email Login" -> loginWithEmail(
                email = "qzsejjbqj2@mxscout.com",
                password = "abc1234!",
                coroutineScope = coroutineScope,
                setResponse = setResponse,
                setText = setText,
            )

            "Oauth Login(Google)" -> loginOauth(
                provider = "google",
                clientId = "914682313325-c9kqcpmh0vflkqflsgh6cp35b4ife95q.apps.googleusercontent.com",
                tokenType = OauthTokenType.ID_TOKEN,
                coroutineScope = coroutineScope,
                setResponse = setResponse,
                setText = setText,
            )

            "Oauth Login(Apple)" -> loginOauth(
                provider = "apple",
                clientId = "io.wepin.testlocal",
                tokenType = OauthTokenType.ID_TOKEN,
                coroutineScope = coroutineScope,
                setResponse = setResponse,
                setText = setText,
            )

            "Oauth Login(Discord)" -> loginOauth(
                provider = "discord",
                clientId = "1244924865098551296",
                tokenType = OauthTokenType.ACCESS_TOKEN,
                coroutineScope = coroutineScope,
                setResponse = setResponse,
                setText = setText,
            )

            "Oauth Login(Naver)" -> loginOauth(
                provider = "naver",
                clientId = "cMHFoFq6Ep2FzDM0fnFP",
                tokenType = OauthTokenType.ACCESS_TOKEN,
                coroutineScope = coroutineScope,
                setResponse = setResponse,
                setText = setText,
            )

            "IdToken Login" -> {
                setText("isProcessing")
                val token = "IdToken"
                loginIdToken(
                    token = token,
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
