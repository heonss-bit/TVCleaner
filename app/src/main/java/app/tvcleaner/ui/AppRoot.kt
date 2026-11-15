package app.tvcleaner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tvcleaner.MainViewModel

private data class Tab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("ТВ", Icons.Filled.Tv),
    Tab("Чистка", Icons.Filled.CleaningServices),
    Tab("Лаунчер", Icons.Filled.Home),
    Tab("Бэкапы", Icons.Filled.Backup),
)

@Composable
fun AppRoot(vm: MainViewModel) {
    val s by vm.ui.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(s.message) {
        s.message?.let {
            snack.showSnackbar(it)
            vm.messageShown()
        }
    }

    Scaffold(
        containerColor = Ui.bg,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            NavigationBar(containerColor = Ui.surface, tonalElevation = 0.dp) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Ui.onAccent,
                            selectedTextColor = Ui.accent,
                            indicatorColor = Ui.accent,
                            unselectedIconColor = Ui.muted,
                            unselectedTextColor = Ui.muted,
                        ),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize().background(Ui.bg)) {
            val m = Modifier.fillMaxSize()
            when (tab) {
                0 -> ConnectScreen(s, vm, m, onGo = { tab = it })
                1 -> CleanScreen(s, vm, m)
                2 -> LauncherScreen(s, vm, m)
                else -> BackupScreen(s, vm, m)
            }
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
