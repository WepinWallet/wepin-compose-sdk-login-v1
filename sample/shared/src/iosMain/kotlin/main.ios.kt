import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController() = ComposeUIViewController {
    val viewController = UIViewController()
    App(viewController)
}
