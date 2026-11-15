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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tvcleaner.MainViewModel
import app.tvcleaner.UiState

@Composable
fun LauncherScreen(s: UiState, vm: MainViewModel, modifier: Modifier = Modifier) {
    if (!s.connected) {
        Column(modifier.padding(16.dp)) { InfoCard("Нет подключения", "Сначала подключись к ТВ на первой вкладке.") }
        return
    }
    val pickApk = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.installLauncher(uri)
    }

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InfoCard(
            "Свой лаунчер вместо тормозного",
            "Родной главный экран часто самое тяжёлое на ТВ. Поставь лёгкий лаунчер (APK скачай заранее на телефон — например, " +
                "open-source лаунчеры с GitHub), установи его на ТВ отсюда и сделай основным.",
        )
        Button(
            onClick = { pickApk.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream")) },
            enabled = s.busy == null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Установить APK лаунчера с телефона") }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Лаунчеры на ТВ", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = vm::refreshLaunchers, enabled = s.busy == null) { Text("Обновить") }
        }

        if (s.launchers.isEmpty()) {
            Text("Список пуст. Нажми «Обновить».", style = MaterialTheme.typography.bodyMedium)
        }
        s.launchers.forEach { l ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(l.pkg, style = MaterialTheme.typography.bodyLarge)
                        if (l.isDefault) Text("сейчас основной", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    if (!l.isDefault) {
                        Button(onClick = { vm.setLauncher(l) }, enabled = s.busy == null) { Text("Сделать основным") }
                    }
                }
            }
        }

        if (s.prevLauncher != null) {
            OutlinedButton(onClick = vm::restorePrevLauncher, enabled = s.busy == null, modifier = Modifier.fillMaxWidth()) {
                Text("Вернуть прежний лаунчер")
            }
            Text("Прежний: ${s.prevLauncher}", style = MaterialTheme.typography.bodySmall)
        }

        InfoCard(
            "Важно",
            "Родной лаунчер не удаляй и не отключай — его всегда можно вернуть кнопкой выше. " +
                "На некоторых ТВ кнопка «Домой» на пульте всё равно может открывать фирменный экран — это ограничение производителя.",
        )
    }
}
