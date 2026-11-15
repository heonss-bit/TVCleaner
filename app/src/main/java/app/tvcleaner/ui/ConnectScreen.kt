package app.tvcleaner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tvcleaner.MainViewModel
import app.tvcleaner.UiState

@Composable
fun ConnectScreen(s: UiState, vm: MainViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (s.connected && s.device != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Подключено", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(s.device.name, style = MaterialTheme.typography.titleLarge)
                    Text("Android ${s.device.androidRelease} (API ${s.device.sdk})")
                    s.device.freeStorageMb?.let { Text("Свободно места: $it МБ") }
                    s.device.freeRamMb?.let { Text("Свободно RAM: $it МБ") }
                }
            }
            OutlinedButton(onClick = vm::disconnect, modifier = Modifier.fillMaxWidth()) { Text("Отключиться") }
            InfoCard("Дальше", "Вкладка «Чистка» — скан и очистка. «Лаунчер» — смена главного экрана. «Бэкапы» — откат изменений.")
        } else {
            InfoCard(
                "Один раз включи отладку на ТВ",
                "1. Настройки → Устройство → О приложении → 7 раз нажми на «Сборка».\n" +
                    "2. Настройки → Устройство → Для разработчиков → включи «Отладка по USB» и «Отладка по сети» (или «Беспроводная отладка»).\n" +
                    "3. IP-адрес ТВ смотри в Настройки → Сеть. Телефон должен быть в той же Wi-Fi.\n" +
                    "4. При первом подключении на ТВ появится запрос — выбери «Всегда разрешать» и нажми ОК.",
            )
            OutlinedTextField(
                value = s.host,
                onValueChange = vm::setHost,
                label = { Text("IP-адрес ТВ (например 192.168.1.50)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = vm::findTv, enabled = s.busy == null) { Text("Найти ТВ в сети") }
                Button(onClick = vm::connect, enabled = s.busy == null) { Text("Подключиться") }
            }
            if (s.found.isNotEmpty()) {
                Text("Найдено в сети:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    s.found.forEach { ip -> AssistChip(onClick = { vm.setHost(ip) }, label = { Text(ip) }) }
                }
            }
        }
    }
}
