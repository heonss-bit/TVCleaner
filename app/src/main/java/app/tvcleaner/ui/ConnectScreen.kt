package app.tvcleaner.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tvcleaner.MainViewModel
import app.tvcleaner.UiState

@Composable
fun ConnectScreen(s: UiState, vm: MainViewModel, modifier: Modifier = Modifier, onGo: (Int) -> Unit = {}) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (s.connected && s.device != null) {
            Header(
                "Подключено",
                s.device.name,
                "Связь с ТВ работает. Дальше открой «Чистка» и нажми «Просканировать». Телефон и ТВ должны оставаться в одной Wi-Fi.",
                key = "connected",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Stat("Android", s.device.androidRelease.ifBlank { "?" }, Modifier.weight(1f))
                Stat("Свободно места", s.device.freeStorageMb?.let { "$it МБ" } ?: "—", Modifier.weight(1f))
                Stat("Свободно RAM", s.device.freeRamMb?.let { "$it МБ" } ?: "—", Modifier.weight(1f))
            }
            PrimaryButton("Перейти к чистке", { onGo(1) }, Modifier.fillMaxWidth())
            GhostButton("Сменить лаунчер", { onGo(2) }, Modifier.fillMaxWidth())
            GhostButton("Отключиться", vm::disconnect, Modifier.fillMaxWidth())
        } else {
            Header(
                "TV Cleaner",
                "Чистка Android TV с телефона",
                "Приложение подключается к ТВ по Wi-Fi (ADB). Ничего не нужно ставить на сам ТВ и не нужен компьютер. " +
                    "Пока ты не нажал «Применить», ничего не удаляется.",
                key = "start",
            )
            AppCard {
                Text("Подготовка ТВ — один раз", color = Ui.text, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                StepRow(1, "Настройки → Устройство → О приложении, 7 раз нажми «Сборка»")
                StepRow(2, "Для разработчиков → включи «Отладка по сети»")
                StepRow(3, "Телефон и ТВ — в одной Wi-Fi")
                StepRow(4, "На ТВ разреши запрос: «Всегда разрешать» → ОК")
            }
            OutlinedTextField(
                value = s.host,
                onValueChange = vm::setHost,
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
                            onClick = { vm.setHost(ip) },
                            label = { Text(ip) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = Ui.surface2, labelColor = Ui.accent),
                            border = null,
                        )
                    }
                }
            }
            GhostButton("Найти ТВ в сети", vm::findTv, Modifier.fillMaxWidth(), enabled = s.busy == null)
            PrimaryButton("Подключиться", vm::connect, Modifier.fillMaxWidth(), enabled = s.busy == null)
        }
        Spacer(Modifier.height(72.dp))
    }
}
