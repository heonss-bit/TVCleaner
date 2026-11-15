package app.tvcleaner.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tvcleaner.MainViewModel
import app.tvcleaner.UiState

@Composable
fun BackupScreen(s: UiState, vm: MainViewModel, modifier: Modifier = Modifier) {
    var exportId by remember { mutableStateOf<String?>(null) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val id = exportId
        if (uri != null && id != null) vm.exportBackup(id, uri)
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importBackup(uri)
    }

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Бэкапы", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "application/octet-stream")) }) {
                Text("Импорт")
            }
        }
        Text(
            "Бэкап создаётся автоматически перед каждой чисткой. Восстановление возвращает удалённые и включает отключённые приложения.",
            style = MaterialTheme.typography.bodySmall,
        )
        if (s.backups.isEmpty()) Text("Пока пусто.", style = MaterialTheme.typography.bodyMedium)

        s.backups.forEach { b ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(formatDate(b.createdAt) + if (b.device.isNotBlank()) " · ${b.device}" else "", style = MaterialTheme.typography.titleSmall)
                    val removed = b.actions.count { it.kind == "removed" }
                    val disabled = b.actions.count { it.kind == "disabled" }
                    val parts = buildList {
                        if (removed > 0) add("удалено: $removed")
                        if (disabled > 0) add("отключено: $disabled")
                        if (b.settings.isNotEmpty()) add("настройки анимаций")
                    }
                    Text(parts.joinToString(", ").ifBlank { "пусто" }, style = MaterialTheme.typography.bodyMedium)
                    if (b.actions.isNotEmpty()) {
                        Text(
                            b.actions.take(8).joinToString { it.title } + if (b.actions.size > 8) "…" else "",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (b.restored) Text("уже восстановлено", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.restore(b.id) }, enabled = s.connected && s.busy == null) { Text("Восстановить") }
                        OutlinedButton(onClick = { exportId = b.id; exporter.launch("tvcleaner-backup.json") }) { Text("Экспорт") }
                        TextButton(onClick = { vm.deleteBackup(b.id) }) { Text("Удалить") }
                    }
                    if (!s.connected) Text("Для восстановления подключись к ТВ.", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
