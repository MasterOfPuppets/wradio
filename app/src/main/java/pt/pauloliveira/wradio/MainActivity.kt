package pt.pauloliveira.wradio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import pt.pauloliveira.wradio.ui.navigation.MainScreen
import pt.pauloliveira.wradio.ui.theme.WRadioTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WRadioTheme {
                MainScreen()
            }
        }
    }
}