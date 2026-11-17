package com.agiso.routine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import java.time.LocalDate

private fun openAppInfo(ctx: Context) {
    try {
        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName))
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    } catch (e: Exception) {
    }
}

private fun openNotificationSettings(ctx: Context) {
    try {
        val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        i.putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    } catch (e: Exception) {
        openAppInfo(ctx)
    }
}

@Composable
private fun OutlineButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(C.card)
            .border(1.dp, C.accent, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = C.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SettingsScreen(tick: Int) {
    val ctx = LocalContext.current
    val prefs = Store.prefs(ctx)
    val today = LocalDate.now()
    val morningItems = Store.dueItems(ctx, today).filter { it.morning }
    val notifAllowed = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ScreenTitle("설정")

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label("알림")
            CardBox {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "아침 알림", color = C.text, fontSize = 17.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    TimeChip(fmtMin(prefs.morningMin)) {
                        pickTime(ctx, prefs.morningMin) { Store.setPrefs(ctx, Store.prefs(ctx).copy(morningMin = it)) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(
                        checked = prefs.morningOn,
                        onCheckedChange = { Store.setPrefs(ctx, Store.prefs(ctx).copy(morningOn = it)) }
                    )
                }
                Divider()
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("저녁 재알림", color = C.text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        Text("남은 항목만", color = C.sub, fontSize = 13.sp)
                    }
                    TimeChip(fmtMin(prefs.eveningMin)) {
                        pickTime(ctx, prefs.eveningMin) { Store.setPrefs(ctx, Store.prefs(ctx).copy(eveningMin = it)) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(
                        checked = prefs.eveningOn,
                        onCheckedChange = { Store.setPrefs(ctx, Store.prefs(ctx).copy(eveningOn = it)) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label("아침 알림 미리보기")
            CardBox(Modifier.border(1.dp, C.line, RoundedCornerShape(16.dp))) {
                Column(
                    Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("아기소 루틴 · ${fmtMin(prefs.morningMin)}", color = C.sub, fontSize = 13.sp)
                    if (!prefs.morningOn) {
                        Text("아침 알림이 꺼져 있습니다", color = C.text, fontSize = 15.sp)
                    } else if (morningItems.isEmpty()) {
                        Text("오늘은 아침 알림에 포함된 항목이 없습니다", color = C.text, fontSize = 15.sp)
                    } else {
                        Text(
                            "오늘 챙길 것 ${morningItems.size}개",
                            color = C.text, fontSize = 16.sp, fontWeight = FontWeight.Bold
                        )
                        Text(morningItems.joinToString(", ") { it.name }, color = C.text, fontSize = 15.sp)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label("갤럭시 설정")
            CardBox {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("배터리 제한 해제", color = C.text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "앱 정보 > 배터리 > 제한 없음으로 바꾸면 알림이 늦거나 빠지지 않습니다",
                            color = C.sub, fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    OutlineButton("열기") { openAppInfo(ctx) }
                }
                Divider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("알림 권한", color = C.text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        Text(
                            if (notifAllowed) "허용됨" else "꺼져 있어 알림이 오지 않습니다",
                            color = if (notifAllowed) C.sub else C.danger, fontSize = 13.sp
                        )
                    }
                    if (!notifAllowed) {
                        Spacer(Modifier.width(10.dp))
                        OutlineButton("허용") { openNotificationSettings(ctx) }
                    }
                }
            }
        }

        Text("체크는 매일 자정에 자동으로 초기화됩니다.", color = C.sub, fontSize = 13.sp)
    }
}
