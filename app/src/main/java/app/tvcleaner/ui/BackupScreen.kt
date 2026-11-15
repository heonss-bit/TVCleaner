package app.tvcleaner.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
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
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header(
            "Бэкапы",
            "Откат любых изменений",
            "Запись создаётся автоматически перед каждой чисткой. «Восстановить» возвращает удалённые " +
                "и включает отключённые приложения (нужно быть подключённым к ТВ). Экспорт сохраняет " +
                "список действий в файл, без самих APK.",
        )
        GhostButton("Импорт из файла", { importer.launch(arrayOf("application/json", "application/octet-stream")) }, Modifier.fillMaxWidth())

        if (s.backups.isEmpty()) EmptyState("Пока пусто", "Бэкап появится после первой чистки.")

        s.backups.forEach { b ->
            AppCard {
                Text(
                    formatDate(b.createdAt) + if (b.device.isNotBlank()) " · ${b.device}" else "",
                    color = Ui.text, style = MaterialTheme.typography.titleSmall,
                )
                val removed = b.actions.count { it.kind == "removed" }
                val disabled = b.actions.count { it.kind == "disabled" }
                val parts = buildList {
                    if (removed > 0) add("удалено: $removed")
                    if (disabled > 0) add("отключено: $disabled")
                    if (b.settings.isNotEmpty()) add("настройки анимаций")
                }
                Text(parts.joinToString(", ").ifBlank { "пусто" }, color = Ui.muted, style = MaterialTheme.typography.bodyMedium)
                if (b.actions.isNotEmpty()) {
                    Text(
                        b.actions.take(8).joinToString { it.title } + if (b.actions.size > 8) "…" else "",
                        color = Ui.muted, style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (b.restored) Tag("уже восстановлено", Ui.accent)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    PrimaryButton("Восстановить", { vm.restore(b.id) }, enabled = s.connected && s.busy == null)
                    GhostButton("Экспорт", { exportId = b.id; exporter.launch("tvcleaner-backup.json") })
                    TextButton(onClick = { vm.deleteBackup(b.id) }) { Text("Удалить", color = Ui.junk) }
                }
                if (!s.connected) Text("Для восстановления подключись к ТВ.", color = Ui.muted, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(72.dp))
    }
}
