package top.aryun.token

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import top.aryun.token.ui.AppRoot
import top.aryun.token.ui.theme.GetCookieTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            GetCookieTheme {
                AppRoot()
            }
        }
    }
}
