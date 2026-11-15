package app.tvcleaner

import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.lifecycle.viewmodel.compose.viewModel
import app.tvcleaner.ui.AppRoot
import app.tvcleaner.ui.Ui

private val Scheme = darkColorScheme(
    primary = Ui.accent,
    onPrimary = Ui.onAccent,
    background = Ui.bg,
    onBackground = Ui.text,
    surface = Ui.surface,
    onSurface = Ui.text,
    surfaceVariant = Ui.surface2,
    onSurfaceVariant = Ui.muted,
    outline = Ui.line,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
        )
        setContent {
            MaterialTheme(colorScheme = Scheme) {
                val vm: MainViewModel = viewModel()
                AppRoot(vm)
            }
        }
    }
}
