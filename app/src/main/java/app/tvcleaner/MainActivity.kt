package app.tvcleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import app.tvcleaner.ui.AppRoot

private val Scheme = darkColorScheme(
    primary = Color(0xFF7FB2FF),
    onPrimary = Color(0xFF00315F),
    background = Color(0xFF101418),
    surface = Color(0xFF101418),
    surfaceVariant = Color(0xFF1C2229),
    surfaceContainerHighest = Color(0xFF232B33),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = Scheme) {
                val vm: MainViewModel = viewModel()
                AppRoot(vm)
            }
        }
    }
}
