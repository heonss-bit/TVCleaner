package app.tvcleaner.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

private data class Tab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("ТВ", Icons.Filled.Tv),
    Tab("Чистка", Icons.Filled.CleaningServices),
    Tab("Лаунчер", Icons.Filled.Home),
    Tab("Бэкапы", Icons.Filled.Backup),
)

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

@Composable
fun App(c: Controller) {
    val s by c.ui.collectAsState()
    val snack = remember { SnackbarHostState() }
    var tab by remember { mutableStateOf(0) }

    LaunchedEffect(s.message) {
        s.message?.let {
            snack.showSnackbar(it)
            c.messageShown()
        }
    }

    MaterialTheme(colorScheme = Scheme) {
        Row(Modifier.fillMaxSize().background(Ui.bg)) {
            NavigationRail(containerColor = Ui.surface, modifier = Modifier.fillMaxHeight()) {
                tabs.forEachIndexed { i, t ->
                    NavigationRailItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Ui.onAccent,
                            selectedTextColor = Ui.accent,
                            indicatorColor = Ui.accent,
                            unselectedIconColor = Ui.muted,
                            unselectedTextColor = Ui.muted,
                        ),
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = 900.dp).fillMaxHeight()) {
                    val m = Modifier.fillMaxSize()
                    when (tab) {
                        0 -> ConnectScreen(s, c, onGo = { tab = it }, modifier = m)
                        1 -> CleanScreen(s, c, m)
                        2 -> LauncherScreen(s, c, m)
                        else -> BackupScreen(s, c, m)
                    }
                    SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).padding(16.dp))
                    s.busy?.let { label ->
                        Surface(
                            color = Ui.surface2,
                            shape = RoundedCornerShape(18.dp),
                            shadowElevation = 8.dp,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                CircularProgressIndicator(Modifier.size(22.dp), color = Ui.accent, strokeWidth = 2.5.dp)
                                Text(label, color = Ui.text)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun main() = application {
    val controller = remember { Controller() }
    Window(
        onCloseRequest = {
            controller.shutdown()
            exitApplication()
        },
        title = "TV Cleaner",
        state = rememberWindowState(width = 1060.dp, height = 780.dp),
        icon = painterResource("icon.png"),
    ) {
        App(controller)
    }
}
