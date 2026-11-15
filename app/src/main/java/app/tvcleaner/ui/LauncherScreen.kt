package app.tvcleaner.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
        Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Header("Лаунчер", "Свой главный экран для ТВ")
            EmptyState("Нет подключения", "Сначала подключись к ТВ на вкладке «ТВ».")
        }
        return
    }
    val pickApk = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.installLauncher(uri)
    }

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header(
            "Лаунчер",
            "Лёгкий главный экран вместо тормозного",
            "Родной главный экран — часто самое тяжёлое на ТВ. Родной лаунчер не удаляется: " +
                "кнопка «Вернуть прежний» всегда доступна. На некоторых ТВ кнопка «Домой» на пульте " +
                "может всё равно открывать фирменный экран — это ограничение прошивки.",
        )
        AppCard {
            Text("Как поставить", color = Ui.text, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            StepRow(1, "Скачай APK лёгкого лаунчера на телефон")
            StepRow(2, "Нажми «Установить APK» и выбери файл")
            StepRow(3, "Выбери лаунчер в списке и нажми «Сделать основным»")
        }
        PrimaryButton(
            "Установить APK с телефона",
            { pickApk.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream")) },
            Modifier.fillMaxWidth(), enabled = s.busy == null,
        )

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Лаунчеры на ТВ", color = Ui.text, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            GhostButton("Обновить", vm::refreshLaunchers, enabled = s.busy == null)
        }
        if (s.launchers.isEmpty()) {
            Text("Список пуст. Нажми «Обновить».", color = Ui.muted)
        }
        s.launchers.forEach { l ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(l.pkg, color = Ui.text)
                        if (l.isDefault) Tag("сейчас основной", Ui.accent)
                    }
                    if (!l.isDefault) {
                        PrimaryButton("Сделать основным", { vm.setLauncher(l) }, enabled = s.busy == null)
                    }
                }
            }
        }

        if (s.prevLauncher != null) {
            GhostButton("Вернуть прежний лаунчер", vm::restorePrevLauncher, Modifier.fillMaxWidth(), enabled = s.busy == null)
            Text("Прежний: ${s.prevLauncher}", color = Ui.muted, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(72.dp))
    }
}
