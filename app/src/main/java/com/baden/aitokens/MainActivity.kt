package com.baden.aitokens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.baden.aitokens.ui.accounts.AddAccountScreen
import com.baden.aitokens.ui.accounts.ManageScreen
import com.baden.aitokens.ui.home.HomeScreen
import com.baden.aitokens.ui.theme.My_tokensTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            My_tokensTheme {
                AiTokensApp()
            }
        }
    }
}

private sealed interface Screen {
    data object Home : Screen
    data object Add : Screen
    data object Manage : Screen
}

@Composable
private fun AiTokensApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    BackHandler(enabled = screen != Screen.Home) {
        screen = Screen.Home
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (screen) {
            Screen.Home -> HomeScreen(
                onAdd = { screen = Screen.Add },
                onManage = { screen = Screen.Manage },
            )

            Screen.Add -> AddAccountScreen(
                onBack = { screen = Screen.Home },
                onSaved = { screen = Screen.Home },
            )

            Screen.Manage -> ManageScreen(
                onBack = { screen = Screen.Home },
            )
        }
    }
}
