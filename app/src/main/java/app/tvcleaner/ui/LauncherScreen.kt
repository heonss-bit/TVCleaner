package app.tvcleaner.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import app.tvcleaner.MainViewModel
import app.tvcleaner.UiState

private data class Suggested(val name: String, val about: String, val url: String)

private val suggested = listOf(
    Suggested("FLauncher", "Простой и лёгкий, открытый код. Хороший первый выбор.", "https://github.com/svrooij/flauncher/releases"),
    Suggested("FLauncher (форк osrosal)", "Тот же FLauncher, обновляется активнее.", "https://github.com/osrosal/FLauncher/releases"),
    Suggested("LTvLauncher", "Минималистичный, мало ест памяти.", "https://github.com/LeanBitLab/LtvLauncher/releases"),
)

@Composable
fun LauncherScreen(s: UiState, vm: MainViewModel, modifier: Modifier = Modifier) {
    if (!s.connected) {
        Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Header("Лаунчер", "Главный экран ТВ")
            EmptyState("Нет подключения", "Сначала подключись к ТВ на вкладке «ТВ».")
        }
        return
    }
    val uri = LocalUriHandler.current
    val pickApk = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) vm.installLauncher(u)
    }

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header(
            "Лаунчер",
            "Главный экран ТВ: тот, что с иконками приложений",
            "Лаунчер — это программа, которая показывает главный экран. Родной на многих ТВ тяжёлый и тормозит. " +
                "Лёгкий лаунчер работает быстрее. Родной не удаляется, его можно вернуть одной кнопкой. " +
                "Этот раздел необязателен: чистка работает и без него.",
        )

        AppCard {
            Text("Как сменить за 3 шага", color = Ui.text, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            StepRow(1, "Скачай на телефон APK лёгкого лаунчера")
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
            { pickApk.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream")) },
            Modifier.fillMaxWidth(), enabled = s.busy == null,
        )

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("3 · Лаунчеры на ТВ", color = Ui.text, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = vm::refreshLaunchers, enabled = s.busy == null) { Text("Обновить", color = Ui.accent) }
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
                        PrimaryButton("Сделать основным", { vm.setLauncher(l) }, enabled = s.busy == null)
                    }
                }
            }
        }

        if (s.prevLauncher != null) {
            GhostButton("Вернуть прежний лаунчер", vm::restorePrevLauncher, Modifier.fillMaxWidth(), enabled = s.busy == null)
        }
        Text(
            "Если после выбора «Домой» всё равно открывает старый экран — на этом ТВ прошивка не даёт сменить лаунчер так. Ничего не сломано.",
            color = Ui.muted, style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(72.dp))
    }
}
