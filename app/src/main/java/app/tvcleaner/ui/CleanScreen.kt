package app.tvcleaner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tvcleaner.MainViewModel
import app.tvcleaner.UiState
import app.tvcleaner.data.AppItem

@Composable
fun CleanScreen(s: UiState, vm: MainViewModel, modifier: Modifier = Modifier) {
    if (!s.connected) {
        Column(modifier.padding(16.dp)) { InfoCard("Нет подключения", "Сначала подключись к ТВ на первой вкладке.") }
        return
    }
    val scan = s.scan
    var showUser by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Button(onClick = vm::scanTv, enabled = s.busy == null, modifier = Modifier.fillMaxWidth()) {
                Text(if (scan == null) "Просканировать ТВ" else "Сканировать заново")
            }
        }
        s.report?.let { r -> item { InfoCard("Результат", r) } }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = vm::trimCaches, enabled = s.busy == null) { Text("Очистить кэш") }
                OutlinedButton(onClick = vm::freeRam, enabled = s.busy == null) { Text("Освободить RAM") }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Ускорить интерфейс", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Меньше анимаций — меню на слабом ТВ листаются живее. Откатывается кнопкой «Как было» или во вкладке «Бэкапы».",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.setAnimations("0.5") }, enabled = s.busy == null) { Text("×0.5") }
                        OutlinedButton(onClick = { vm.setAnimations("0") }, enabled = s.busy == null) { Text("Выкл.") }
                        OutlinedButton(onClick = { vm.setAnimations("1") }, enabled = s.busy == null) { Text("Как было") }
                    }
                }
            }
        }

        if (scan == null) return@LazyColumn

        scan.totalCacheBytes?.let { c ->
            item { Text("Кэш приложений: ${formatBytes(c)}", style = MaterialTheme.typography.bodyMedium) }
        }

        section(
            title = "Мусор (${scan.junk.size})",
            subtitle = "Безопасно убрать — ничего не сломается. Уже отмечено.",
            items = scan.junk, s = s, vm = vm,
        )
        section(
            title = "Можно отключить (${scan.optional.size})",
            subtitle = "Работает без них, но часть функций пропадёт. Читай пояснение у каждого. По умолчанию — только отключение.",
            items = scan.optional, s = s, vm = vm,
            header = {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Удалять вместо отключения", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = s.removeOptional, onCheckedChange = vm::setRemoveOptional)
                }
            },
        )

        item {
            OutlinedButton(onClick = { showUser = !showUser }, modifier = Modifier.fillMaxWidth()) {
                Text((if (showUser) "Скрыть" else "Показать") + " установленные приложения (${scan.userApps.size})")
            }
        }
        if (showUser) {
            items(scan.userApps, key = { "u_" + it.pkg }) { a -> AppRow(a, a.pkg in s.selected) { vm.toggle(a.pkg) } }
            item {
                Text(
                    "Перед удалением таких приложений APK сохраняется на телефон — их можно вернуть из «Бэкапов».",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        item {
            Button(
                onClick = vm::runClean,
                enabled = s.busy == null && s.selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Применить к выбранному (${s.selected.size})") }
        }
        item {
            Text(
                "Перед чисткой автоматически создаётся бэкап. После изменений перезагрузи ТВ и проверь, что всё работает.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    subtitle: String,
    items: List<AppItem>,
    s: UiState,
    vm: MainViewModel,
    header: (@Composable () -> Unit)? = null,
) {
    if (items.isEmpty()) return
    item {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
            header?.invoke()
        }
    }
    items(items, key = { title + it.pkg }) { a -> AppRow(a, a.pkg in s.selected) { vm.toggle(a.pkg) } }
}

@Composable
private fun AppRow(a: AppItem, checked: Boolean, onToggle: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(a.title, fontWeight = FontWeight.Medium)
                if (a.title != a.pkg) Text(a.pkg, style = MaterialTheme.typography.labelSmall)
                if (a.note.isNotBlank()) Text(a.note, style = MaterialTheme.typography.bodySmall)
                if (a.isDisabled) Text("уже отключено", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            if (a.cacheBytes != null && a.cacheBytes > 0) {
                Text(formatBytes(a.cacheBytes), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
