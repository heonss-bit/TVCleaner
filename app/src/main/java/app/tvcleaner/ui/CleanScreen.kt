package app.tvcleaner.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tvcleaner.MainViewModel
import app.tvcleaner.UiState
import app.tvcleaner.data.AppItem
import app.tvcleaner.data.Tier

@Composable
fun CleanScreen(s: UiState, vm: MainViewModel, modifier: Modifier = Modifier) {
    if (!s.connected) {
        Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Header("Чистка", "Скан и очистка ТВ")
            EmptyState("Нет подключения", "Сначала подключись к ТВ на вкладке «ТВ».")
        }
        return
    }
    val scan = s.scan
    var showUser by rememberSaveable { mutableStateOf(false) }

    Column(modifier) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Header(
                    "Чистка",
                    "Находит лишнее и показывает, что можно убрать",
                    "1. Нажми «Просканировать».\n" +
                        "2. «Мусор» отмечен заранее: это безопасно.\n" +
                        "3. «Можно отключить» — по желанию, читай пояснение у каждого.\n" +
                        "4. Нажми «Применить». Перед этим автоматически создаётся бэкап — откат во вкладке «Бэкапы».",
                )
            }
            item {
                PrimaryButton(
                    if (scan == null) "Просканировать ТВ" else "Сканировать заново",
                    vm::scanTv, Modifier.fillMaxWidth(), enabled = s.busy == null,
                )
            }
            s.report?.let { r ->
                item {
                    AppCard {
                        Text("Готово", color = Ui.accent, fontWeight = FontWeight.SemiBold)
                        Text(r, color = Ui.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    GhostButton("Очистить кэш", vm::trimCaches, Modifier.weight(1f), enabled = s.busy == null)
                    GhostButton("Освободить RAM", vm::freeRam, Modifier.weight(1f), enabled = s.busy == null)
                }
            }
            item {
                AppCard {
                    Text("Ускорить интерфейс", color = Ui.text, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Меньше анимаций — меню на слабом ТВ листаются живее. Откат: «Как было».",
                        color = Ui.muted, style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton("×0.5", { vm.setAnimations("0.5") }, enabled = s.busy == null)
                        GhostButton("Выкл.", { vm.setAnimations("0") }, enabled = s.busy == null)
                        GhostButton("Как было", { vm.setAnimations("1") }, enabled = s.busy == null)
                    }
                }
            }

            if (scan != null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Stat("Мусор", "${scan.junk.size}", Modifier.weight(1f))
                        Stat("Можно отключить", "${scan.optional.size}", Modifier.weight(1f))
                        Stat("Кэш", scan.totalCacheBytes?.let { formatBytes(it) } ?: "—", Modifier.weight(1f))
                    }
                }

                group(
                    "Мусор", "Безопасно убрать — ничего не сломается. Уже отмечено.",
                    scan.junk, s, vm, Ui.junk,
                )
                group(
                    "Можно отключить", "Работает и без них, но часть функций пропадёт. По умолчанию только отключаем.",
                    scan.optional, s, vm, Ui.warn,
                    header = {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Удалять вместо отключения", color = Ui.text, style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = s.removeOptional,
                                onCheckedChange = vm::setRemoveOptional,
                                colors = SwitchDefaults.colors(checkedTrackColor = Ui.accent, checkedThumbColor = Ui.onAccent),
                            )
                        }
                    },
                )

                item {
                    GhostButton(
                        (if (showUser) "Скрыть" else "Показать") + " установленные приложения (${scan.userApps.size})",
                        { showUser = !showUser }, Modifier.fillMaxWidth(),
                    )
                }
                if (showUser) {
                    items(scan.userApps, key = { "u_" + it.pkg }) { a ->
                        AppRow(a, a.pkg in s.selected, null) { vm.toggle(a.pkg) }
                    }
                    item {
                        Text(
                            "Перед удалением таких приложений их APK сохраняется на телефон — вернуть можно из «Бэкапов».",
                            color = Ui.muted, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        if (scan != null) {
            Surface(color = Ui.surface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PrimaryButton(
                        "Применить к выбранному (${s.selected.size})",
                        vm::runClean, Modifier.fillMaxWidth(),
                        enabled = s.busy == null && s.selected.isNotEmpty(),
                    )
                    Text(
                        "После изменений перезагрузи ТВ и проверь, что всё работает.",
                        color = Ui.muted, style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.group(
    title: String,
    subtitle: String,
    list: List<AppItem>,
    s: UiState,
    vm: MainViewModel,
    color: Color,
    header: (@Composable () -> Unit)? = null,
) {
    if (list.isEmpty()) return
    item(key = "h_$title") {
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, color = Ui.text, style = MaterialTheme.typography.titleMedium)
                Tag("${list.size}", color)
            }
            Text(subtitle, color = Ui.muted, style = MaterialTheme.typography.bodySmall)
            header?.invoke()
        }
    }
    items(list, key = { title + it.pkg }) { a -> AppRow(a, a.pkg in s.selected, color) { vm.toggle(a.pkg) } }
}

@Composable
private fun AppRow(a: AppItem, checked: Boolean, tagColor: Color?, onToggle: () -> Unit) {
    AppCard(onClick = onToggle) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Ui.accent, checkmarkColor = Ui.onAccent, uncheckedColor = Ui.muted,
                ),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(a.title, color = Ui.text, fontWeight = FontWeight.Medium)
                if (a.title != a.pkg) Text(a.pkg, color = Ui.muted, style = MaterialTheme.typography.labelSmall)
                if (a.note.isNotBlank()) Text(a.note, color = Ui.muted, style = MaterialTheme.typography.bodySmall)
                if (a.isDisabled) Tag("уже отключено", Ui.accent)
            }
            if (a.cacheBytes != null && a.cacheBytes > 0) {
                Text(formatBytes(a.cacheBytes), color = tagColor ?: Ui.muted, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
