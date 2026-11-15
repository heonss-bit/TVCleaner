package app.tvcleaner.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tvcleaner.data.AppItem

// ---------------------------------------------------------------- ТВ

@Composable
fun ConnectScreen(s: UiState, c: Controller, onGo: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (s.connected && s.device != null) {
            Header(
                "Подключено", s.device.name,
                "Связь с ТВ работает. Дальше открой «Чистка» и нажми «Просканировать». Компьютер и ТВ должны оставаться в одной сети.",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Stat("Android", s.device.androidRelease.ifBlank { "?" }, Modifier.weight(1f))
                Stat("Свободно места", s.device.freeStorageMb?.let { "$it МБ" } ?: "—", Modifier.weight(1f))
                Stat("Свободно RAM", s.device.freeRamMb?.let { "$it МБ" } ?: "—", Modifier.weight(1f))
            }
            PrimaryButton("Перейти к чистке", { onGo(1) }, Modifier.fillMaxWidth())
            GhostButton("Сменить лаунчер", { onGo(2) }, Modifier.fillMaxWidth())
            GhostButton("Отключиться", c::disconnect, Modifier.fillMaxWidth())
        } else {
            Header(
                "TV Cleaner", "Чистка Android TV с компьютера",
                "Программа подключается к ТВ по сети. Ничего не нужно ставить на сам ТВ, консоль и adb.exe не нужны. " +
                    "Пока ты не нажал «Применить», ничего не удаляется.",
            )
            AppCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tag("BETA", Ui.warn)
                    Text("Проверено не на всех ТВ", color = Ui.text, style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    "Любое изменение можно откатить во вкладке «Бэкапы». Начни с «Мусора» и перезагрузи ТВ после чистки.",
                    color = Ui.muted, style = MaterialTheme.typography.bodySmall,
                )
            }
            AppCard {
                Text("Подготовка ТВ — один раз", color = Ui.text, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                StepRow(1, "Настройки → Устройство → О приложении, 7 раз нажми «Сборка»")
                StepRow(2, "Для разработчиков → включи «Отладка по сети»")
                StepRow(3, "Компьютер и ТВ — в одной сети (Wi-Fi или кабель)")
                StepRow(4, "На ТВ разреши запрос: «Всегда разрешать» → ОК")
            }
            OutlinedTextField(
                value = s.host,
                onValueChange = c::setHost,
                label = { Text("IP-адрес ТВ, например 192.168.1.50") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Ui.accent,
                    unfocusedBorderColor = Ui.line,
                    focusedLabelColor = Ui.accent,
                    unfocusedLabelColor = Ui.muted,
                    cursorColor = Ui.accent,
                    focusedTextColor = Ui.text,
                    unfocusedTextColor = Ui.text,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            if (s.found.isNotEmpty()) {
                Text("Найдено в сети — нажми, чтобы выбрать", color = Ui.muted, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    s.found.forEach { ip ->
                        AssistChip(
                            onClick = { c.setHost(ip) },
                            label = { Text(ip) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = Ui.surface2, labelColor = Ui.accent),
                            border = null,
                        )
                    }
                }
            }
            GhostButton("Найти ТВ в сети", c::findTv, Modifier.fillMaxWidth(), enabled = s.busy == null)
            PrimaryButton("Подключиться", c::connect, Modifier.fillMaxWidth(), enabled = s.busy == null)
        }
        Spacer(Modifier.height(72.dp))
    }
}

// ---------------------------------------------------------------- Чистка

@Composable
fun CleanScreen(s: UiState, c: Controller, modifier: Modifier = Modifier) {
    if (!s.connected) {
        Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Header("Чистка", "Скан и очистка ТВ")
            EmptyState("Нет подключения", "Сначала подключись к ТВ на вкладке «ТВ».")
        }
        return
    }
    val scan = s.scan
    var showUser by remember { mutableStateOf(false) }

    Column(modifier) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Header(
                    "Чистка", "Находит лишнее и показывает, что можно убрать",
                    "1. Нажми «Просканировать».\n" +
                        "2. «Мусор» отмечен заранее: это безопасно.\n" +
                        "3. «Можно отключить» — по желанию, читай пояснение у каждого.\n" +
                        "4. Нажми «Применить». Перед этим автоматически создаётся бэкап — откат во вкладке «Бэкапы».",
                )
            }
            item {
                PrimaryButton(
                    if (scan == null) "Просканировать ТВ" else "Сканировать заново",
                    c::scanTv, Modifier.fillMaxWidth(), enabled = s.busy == null,
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
                    GhostButton("Очистить кэш", c::trimCaches, Modifier.weight(1f), enabled = s.busy == null)
                    GhostButton("Освободить RAM", c::freeRam, Modifier.weight(1f), enabled = s.busy == null)
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
                        GhostButton("×0.5", { c.setAnimations("0.5") }, enabled = s.busy == null)
                        GhostButton("Выкл.", { c.setAnimations("0") }, enabled = s.busy == null)
                        GhostButton("Как было", { c.setAnimations("1") }, enabled = s.busy == null)
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

                group("Мусор", "Безопасно убрать — ничего не сломается. Уже отмечено.", scan.junk, s, c, Ui.junk)
                group(
                    "Можно отключить", "Работает и без них, но часть функций пропадёт. По умолчанию только отключаем.",
                    scan.optional, s, c, Ui.warn,
                    header = {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Удалять вместо отключения", color = Ui.text, style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = s.removeOptional,
                                onCheckedChange = c::setRemoveOptional,
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
                        AppRow(a, a.pkg in s.selected, null) { c.toggle(a.pkg) }
                    }
                    item {
                        Text(
                            "Перед удалением таких приложений их APK сохраняется на компьютер — вернуть можно из «Бэкапов».",
                            color = Ui.muted, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        if (scan != null) {
            Surface(color = Ui.surface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PrimaryButton(
                        "Применить к выбранному (${s.selected.size})",
                        c::runClean, Modifier.fillMaxWidth(),
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
    c: Controller,
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
    items(list, key = { title + it.pkg }) { a -> AppRow(a, a.pkg in s.selected, color) { c.toggle(a.pkg) } }
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

// ---------------------------------------------------------------- Лаунчер

private data class Suggested(val name: String, val about: String, val url: String)

private val suggested = listOf(
    Suggested("FLauncher", "Простой и лёгкий, открытый код. Хороший первый выбор.", "https://github.com/svrooij/flauncher/releases"),
    Suggested("FLauncher (форк osrosal)", "Тот же FLauncher, обновляется активнее.", "https://github.com/osrosal/FLauncher/releases"),
    Suggested("LTvLauncher", "Минималистичный, мало ест памяти.", "https://github.com/LeanBitLab/LtvLauncher/releases"),
)

@Composable
fun LauncherScreen(s: UiState, c: Controller, modifier: Modifier = Modifier) {
    if (!s.connected) {
        Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Header("Лаунчер", "Главный экран ТВ")
            EmptyState("Нет подключения", "Сначала подключись к ТВ на вкладке «ТВ».")
        }
        return
    }
    val uri = LocalUriHandler.current

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header(
            "Лаунчер", "Главный экран ТВ: тот, что с иконками приложений",
            "Лаунчер — это программа, которая показывает главный экран. Родной на многих ТВ тяжёлый и тормозит. " +
                "Лёгкий лаунчер работает быстрее. Родной не удаляется, его можно вернуть одной кнопкой. " +
                "Этот раздел необязателен: чистка работает и без него.",
        )
        AppCard {
            Text("Как сменить за 3 шага", color = Ui.text, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            StepRow(1, "Скачай APK лёгкого лаунчера на компьютер")
            StepRow(2, "Нажми «Установить на ТВ» и выбери этот файл")
            StepRow(3, "В списке ниже нажми «Сделать основным» и затем «Домой» на пульте")
        }

        Text("1 · Выбери и скачай лаунчер", color = Ui.text, style = MaterialTheme.typography.titleMedium)
        suggested.forEach { l ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(l.name, color = Ui.text, style = MaterialTheme.typography.titleSmall)
                        Text(l.about, color = Ui.muted, style = MaterialTheme.typography.bodySmall)
                    }
                    GhostButton("Открыть", { uri.openUri(l.url) })
                }
            }
        }
        Text(
            "На странице листай вниз до Assets и скачивай файл с расширением .apk.",
            color = Ui.muted, style = MaterialTheme.typography.bodySmall,
        )
        PrimaryButton(
            "2 · Установить на ТВ",
            { pickFile("Выбери APK лаунчера")?.let { c.installLauncher(it) } },
            Modifier.fillMaxWidth(), enabled = s.busy == null,
        )

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("3 · Лаунчеры на ТВ", color = Ui.text, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = c::refreshLaunchers, enabled = s.busy == null) { Text("Обновить", color = Ui.accent) }
        }
        if (s.launchers.isEmpty()) Text("Список пуст. Нажми «Обновить».", color = Ui.muted)
        s.launchers.forEach { l ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(l.pkg, color = Ui.text)
                        if (l.isDefault) Tag("сейчас основной", Ui.accent)
                    }
                    if (!l.isDefault) {
                        PrimaryButton("Сделать основным", { c.setLauncher(l) }, enabled = s.busy == null)
                    }
                }
            }
        }

        if (s.prevLauncher != null) {
            GhostButton("Вернуть прежний лаунчер", c::restorePrevLauncher, Modifier.fillMaxWidth(), enabled = s.busy == null)
        }
        Text(
            "Если после выбора «Домой» всё равно открывает старый экран — на этом ТВ прошивка не даёт сменить лаунчер так. Ничего не сломано.",
            color = Ui.muted, style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(24.dp))
    }
}

// ---------------------------------------------------------------- Бэкапы

@Composable
fun BackupScreen(s: UiState, c: Controller, modifier: Modifier = Modifier) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header(
            "Бэкапы", "Откат любых изменений",
            "Запись создаётся автоматически перед каждой чисткой. «Восстановить» возвращает удалённые " +
                "и включает отключённые приложения (нужно быть подключённым к ТВ). Экспорт сохраняет " +
                "список действий в файл, без самих APK.",
        )
        GhostButton(
            "Импорт из файла",
            { pickFile("Выбери файл бэкапа")?.let { c.importBackup(it) } },
            Modifier.fillMaxWidth(),
        )
        Text("Папка бэкапов: ${c.backupFolder.absolutePath}", color = Ui.muted, style = MaterialTheme.typography.bodySmall)

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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton("Восстановить", { c.restore(b.id) }, enabled = s.connected && s.busy == null)
                    GhostButton("Экспорт", {
                        pickFile("Сохранить бэкап", save = true, defaultName = "tvcleaner-backup.json")
                            ?.let { c.exportBackup(b.id, it) }
                    })
                    TextButton(onClick = { c.deleteBackup(b.id) }) { Text("Удалить", color = Ui.junk) }
                }
                if (!s.connected) Text("Для восстановления подключись к ТВ.", color = Ui.muted, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
