package com.agiso.routine

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun itemSummary(item: Item): String {
    val parts = ArrayList<String>()
    parts.add(daysText(item.days))
    if (item.isCount) parts.add("${item.goal}회 세기")
    if (item.morning) parts.add("아침 알림")
    if (item.ownAlarm) parts.add("${fmtMin(item.ownMinutes)} 알림")
    if (!item.morning && !item.ownAlarm) parts.add("알림 꺼짐")
    return parts.joinToString(" · ")
}

@Composable
fun ItemsScreen(tick: Int, onEdit: (Int) -> Unit) {
    // 데이터가 바뀌면 이 화면을 다시 그리도록 변경 번호를 읽습니다.
    val version = Store.version.intValue
    val ctx = LocalContext.current
    val items = Store.activeItems(ctx)
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ScreenTitle("항목 관리")
            Text("종을 켠 항목만 아침 알림에 나옵니다", color = C.sub, fontSize = 14.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("새 항목") },
                placeholder = { Text("예: 유산균") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        Store.addItem(ctx, name.trim())
                        name = ""
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Text("추가", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (items.isNotEmpty()) {
            CardBox {
                for ((index, item) in items.withIndex()) {
                    if (index > 0) Divider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEdit(item.id) }
                            .heightIn(min = 68.dp)
                            .padding(start = 4.dp, end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            MoveButton(Icons.Filled.KeyboardArrowUp, "${item.name} 위로", index > 0) {
                                Store.move(ctx, item.id, -1)
                            }
                            MoveButton(Icons.Filled.KeyboardArrowDown, "${item.name} 아래로", index < items.size - 1) {
                                Store.move(ctx, item.id, 1)
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, color = C.text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            Text(itemSummary(item), color = C.sub, fontSize = 13.sp)
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .then(
                                    if (item.morning) Modifier.background(C.soft)
                                    else Modifier.border(1.dp, C.line, RoundedCornerShape(12.dp))
                                )
                                .clickable { Store.update(ctx, item.copy(morning = !item.morning)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = if (item.morning) "${item.name} 아침 알림 켜짐" else "${item.name} 아침 알림 꺼짐",
                                tint = if (item.morning) C.accent else C.ring
                            )
                        }
                    }
                }
            }
            Text(
                "항목을 누르면 편집 화면이 열립니다. 화살표로 순서를 바꿉니다.",
                color = C.sub, fontSize = 13.sp
            )
        }
    }
}

@Composable
fun ItemEditScreen(id: Int, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val original = remember(id) { Store.item(ctx, id) }
    if (original == null) {
        LaunchedEffect(id) { onClose() }
        return
    }
    var name by remember(id) { mutableStateOf(original.name) }
    var isCount by remember(id) { mutableStateOf(original.isCount) }
    var target by remember(id) { mutableStateOf(original.target.coerceIn(1, 30)) }
    var days by remember(id) { mutableStateOf(original.days and 127) }
    var morning by remember(id) { mutableStateOf(original.morning) }
    var ownAlarm by remember(id) { mutableStateOf(original.ownAlarm) }
    var ownMinutes by remember(id) { mutableStateOf(original.ownMinutes) }
    var confirmDelete by remember { mutableStateOf(false) }

    BackHandler(onBack = onClose)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로", tint = C.text)
            }
            Text("항목 편집", color = C.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            label = { Text("이름") },
            modifier = Modifier.fillMaxWidth()
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label("체크 방식")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceButton("한 번 체크", !isCount, Modifier.weight(1f)) { isCount = false }
                ChoiceButton("횟수 세기", isCount, Modifier.weight(1f)) { isCount = true }
            }
            if (isCount) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("하루 목표", color = C.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    StepButton("−") { if (target > 1) target -= 1 }
                    Text(
                        "${target}회", color = C.text, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    StepButton("+") { if (target < 30) target += 1 }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "반복 요일", color = C.sub, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(daysText(days), color = C.sub, fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (i in 0..6) {
                    val on = ((days shr i) and 1) == 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .then(
                                if (on) Modifier.background(C.accent)
                                else Modifier.background(C.card).border(1.dp, C.line, RoundedCornerShape(22.dp))
                            )
                            .clickable { days = days xor (1 shl i) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            DAY_NAMES[i],
                            color = if (on) Color.White else C.text,
                            fontSize = 15.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            Text("끈 요일에는 오늘 화면과 알림에 나오지 않습니다", color = C.sub, fontSize = 13.sp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label("알림")
            CardBox {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("아침 알림에 포함", color = C.text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "${fmtMin(Store.prefs(ctx).morningMin)} 묶음 알림",
                            color = C.sub, fontSize = 13.sp
                        )
                    }
                    Switch(checked = morning, onCheckedChange = { morning = it })
                }
                Divider()
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "따로 알림", color = C.text, fontSize = 17.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    if (ownAlarm) {
                        TimeChip(fmtMin(ownMinutes)) { pickTime(ctx, ownMinutes) { ownMinutes = it } }
                        Spacer(Modifier.width(10.dp))
                    }
                    Switch(checked = ownAlarm, onCheckedChange = { ownAlarm = it })
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val finalName = if (name.isBlank()) original.name else name.trim()
                    Store.update(
                        ctx,
                        original.copy(
                            name = finalName,
                            isCount = isCount,
                            target = if (isCount) target else 1,
                            days = days,
                            morning = morning,
                            ownAlarm = ownAlarm,
                            ownMinutes = ownMinutes,
                        )
                    )
                    onClose()
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("저장", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("항목 삭제", color = C.danger, fontSize = 16.sp)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("항목 삭제") },
            text = { Text("\"${original.name}\" 항목을 목록에서 지웁니다. 지난 기록은 달력에 남습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    Store.delete(ctx, original.id)
                    onClose()
                }) { Text("삭제", color = C.danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("취소") }
            }
        )
    }
}

@Composable
fun ChoiceButton(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (selected) Modifier.background(C.accent)
                else Modifier.background(C.card).border(1.dp, C.line, RoundedCornerShape(12.dp))
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Color.White else C.text,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun StepButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(C.card)
            .border(1.dp, C.line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = C.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MoveButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = if (enabled) C.sub else C.line)
    }
}
