package app.tvcleaner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------- палитра: тёмный минимализм + один акцент ----------
object Ui {
    val bg = Color(0xFF0B0D10)
    val surface = Color(0xFF14181D)
    val surface2 = Color(0xFF1B2128)
    val line = Color(0xFF262D36)
    val text = Color(0xFFF2F5F8)
    val muted = Color(0xFF8B96A3)
    val accent = Color(0xFF6EE7B7)
    val onAccent = Color(0xFF04281B)
    val junk = Color(0xFFFF7A7A)
    val warn = Color(0xFFFFC857)
}

fun formatBytes(b: Long?): String = when {
    b == null -> ""
    b >= 1_048_576L -> "%.1f МБ".format(b / 1_048_576.0)
    b >= 1024L -> "${b / 1024} КБ"
    else -> "$b Б"
}

fun formatDate(ms: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(ms))

// ---------- базовые компоненты ----------

@Composable
fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val base = modifier.fillMaxWidth().clip(shape).background(Ui.surface, shape)
    Column(
        (if (onClick != null) base.clickable(onClick = onClick) else base).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Ui.accent, contentColor = Ui.onAccent,
            disabledContainerColor = Ui.surface2, disabledContentColor = Ui.muted,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier,
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Ui.line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Ui.text, disabledContentColor = Ui.muted),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = modifier,
    ) { Text(text) }
}

@Composable
fun Tag(text: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) { Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
}

@Composable
fun StepRow(n: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(Ui.surface2),
            contentAlignment = Alignment.Center,
        ) { Text("$n", color = Ui.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        Text(text, color = Ui.text, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Ui.surface).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, color = Ui.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Ui.muted, fontSize = 12.sp)
    }
}

/** Заголовок экрана с кнопкой «ⓘ», раскрывающей мини-инструкцию. */
@Composable
fun Header(title: String, subtitle: String, hint: String? = null, key: String = title) {
    var open by rememberSaveable(key) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = Ui.text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Ui.muted, fontSize = 14.sp)
            }
            if (hint != null) {
                IconButton(onClick = { open = !open }) {
                    Icon(Icons.Filled.Info, contentDescription = "Подсказка", tint = if (open) Ui.accent else Ui.muted)
                }
            }
        }
        AnimatedVisibility(
            visible = open && hint != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Ui.accent.copy(alpha = 0.10f)).padding(14.dp)
            ) { Text(hint.orEmpty(), color = Ui.text, fontSize = 13.sp, lineHeight = 19.sp) }
        }
    }
}

@Composable
fun EmptyState(title: String, body: String) {
    AppCard {
        Text(title, color = Ui.text, fontWeight = FontWeight.SemiBold)
        Text(body, color = Ui.muted, fontSize = 14.sp)
    }
}
