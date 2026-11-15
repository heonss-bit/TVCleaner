package app.tvcleaner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tvcleaner.MainViewModel

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
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Filled.Tv, null) }, label = { Text("ТВ") })
                NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.Filled.CleaningServices, null) }, label = { Text("Чистка") })
                NavigationBarItem(tab == 2, { tab = 2 }, { Icon(Icons.Filled.Home, null) }, label = { Text("Лаунчер") })
                NavigationBarItem(tab == 3, { tab = 3 }, { Icon(Icons.Filled.Backup, null) }, label = { Text("Бэкапы") })
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            val m = Modifier.fillMaxSize()
            when (tab) {
                0 -> ConnectScreen(s, vm, m)
                1 -> CleanScreen(s, vm, m)
                2 -> LauncherScreen(s, vm, m)
                else -> BackupScreen(s, vm, m)
            }
            s.busy?.let { label ->
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(Modifier.padding(0.dp), strokeWidth = 3.dp)
                        Text(label)
                    }
                }
            }
        }
    }
}
