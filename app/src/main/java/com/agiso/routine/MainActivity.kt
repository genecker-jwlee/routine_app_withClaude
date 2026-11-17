package com.agiso.routine

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/** 살구 색상표. */
object C {
    val bg = Color(0xFFFBF6F1)
    val card = Color(0xFFFFFFFF)
    val text = Color(0xFF2A211C)
    val sub = Color(0xFF6B5D54)
    val accent = Color(0xFFB8502F)
    val soft = Color(0xFFF8E6DC)
    val track = Color(0xFFF0E4DA)
    val ring = Color(0xFFA39489)
    val line = Color(0xFFEBDFD5)
    val danger = Color(0xFF8C1D18)
}

/** 설정 화면에 표시하는 빌드 번호. 새 APK가 설치됐는지 확인하는 용도입니다. */
const val BUILD_LABEL = "빌드 4"

val DAY_NAMES = listOf("일", "월", "화", "수", "목", "금", "토")

fun fmtMin(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    val ampm = if (h < 12) "오전" else "오후"
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "$ampm $h12:${m.toString().padStart(2, '0')}"
}

fun daysText(days: Int): String {
    val d = days and 127
    if (d == 127) return "매일"
    if (d == 0) return "요일 없음"
    val names = ArrayList<String>()
    for (i in 0..6) if (((d shr i) and 1) == 1) names.add(DAY_NAMES[i])
    return names.joinToString(" ")
}

fun pickTime(ctx: Context, minutes: Int, onPick: (Int) -> Unit) {
    TimePickerDialog(ctx, { _, h, m -> onPick(h * 60 + m) }, minutes / 60, minutes % 60, false).show()
}

@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(C.card),
        content = content
    )
}

@Composable
fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
}

@Composable
fun TimeChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(C.soft)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = C.accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun ScreenTitle(text: String) {
    Text(text, color = C.text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun Label(text: String) {
    Text(text, color = C.sub, fontSize = 13.sp, fontWeight = FontWeight.Medium)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifier.ensureChannel(this)
        Store.refreshToday(this)
        Alarms.rescheduleAll(this)
        setContent { App() }
    }

    override fun onResume() {
        super.onResume()
        Store.refreshToday(this)
        Store.bump()
        Widgets.refresh(this)
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val tick by Store.tick.collectAsState()
    var tab by rememberSaveable { mutableStateOf(0) }
    var editId by rememberSaveable { mutableStateOf(-1) }

    val askNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        Store.bump()
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val scheme = lightColorScheme(
        primary = C.accent,
        onPrimary = Color.White,
        primaryContainer = C.soft,
        onPrimaryContainer = C.accent,
        secondaryContainer = C.soft,
        onSecondaryContainer = C.accent,
        background = C.bg,
        onBackground = C.text,
        surface = C.card,
        onSurface = C.text,
        surfaceVariant = C.track,
        onSurfaceVariant = C.sub,
        outline = C.ring,
    )

    MaterialTheme(colorScheme = scheme) {
        Column(Modifier.fillMaxSize().background(C.bg)) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (editId >= 0) {
                    ItemEditScreen(editId) { editId = -1 }
                } else {
                    when (tab) {
                        0 -> TodayScreen(tick)
                        1 -> HistoryScreen(tick)
                        2 -> ItemsScreen(tick) { editId = it }
                        else -> SettingsScreen(tick)
                    }
                }
            }
            if (editId < 0) {
                val labels = listOf("오늘", "기록", "항목", "설정")
                val icons = listOf(
                    Icons.Filled.CheckCircle,
                    Icons.Filled.DateRange,
                    Icons.Filled.Edit,
                    Icons.Filled.Settings,
                )
                NavigationBar(containerColor = C.card) {
                    for (i in 0..3) {
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(icons[i], contentDescription = null) },
                            label = { Text(labels[i]) }
                        )
                    }
                }
            }
        }
    }
}
