package com.agiso.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@Composable
fun TodayScreen(tick: Int) {
    val ctx = LocalContext.current
    val today = LocalDate.now()
    val due = Store.dueItems(ctx, today)
    val doneCount = due.count { Store.isDone(ctx, it, today) }
    val fraction = if (due.isEmpty()) 0f else doneCount.toFloat() / due.size
    val timeFormat = SimpleDateFormat("a h:mm", Locale.KOREAN)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                today.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)),
                color = C.sub, fontSize = 14.sp
            )
            ScreenTitle("오늘의 루틴")
        }

        CardBox {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${due.size}개 중 ${doneCount}개 완료",
                        color = C.text, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text("자정에 초기화", color = C.sub, fontSize = 13.sp)
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(C.track)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(C.accent)
                    )
                }
            }
        }

        if (due.isEmpty()) {
            Text(
                "오늘 챙길 항목이 없습니다. 아래 \"항목\" 탭에서 추가하세요.",
                color = C.sub, fontSize = 15.sp
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (item in due) {
                val count = Store.count(ctx, today, item.id)
                val done = count >= item.goal
                val doneAt = Store.doneAt(ctx, today, item.id)
                if (item.isCount) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(C.card)
                            .heightIn(min = 64.dp)
                            .padding(start = 18.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = if (done) {
                                Modifier.size(28.dp).clip(CircleShape).background(C.accent)
                            } else {
                                Modifier.size(28.dp).border(2.dp, C.accent, CircleShape)
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$count",
                                color = if (done) Color.White else C.accent,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, color = C.text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            Text("${item.goal}회 중 ${count}회", color = C.sub, fontSize = 13.sp)
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, C.line, RoundedCornerShape(12.dp))
                                .clickable { Store.add(ctx, item.id, -1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("−", color = C.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(C.accent)
                                .clickable { Store.add(ctx, item.id, 1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "${item.name} 횟수 늘리기", tint = Color.White)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(C.card)
                            .clickable { Store.toggle(ctx, item.id) }
                            .heightIn(min = 64.dp)
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (done) {
                            Box(
                                Modifier.size(28.dp).clip(CircleShape).background(C.accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Check, contentDescription = "완료",
                                    tint = Color.White, modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Box(Modifier.size(28.dp).border(2.dp, C.ring, CircleShape))
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            item.name,
                            color = if (done) C.sub else C.text,
                            fontSize = 17.sp,
                            fontWeight = if (done) FontWeight.Normal else FontWeight.Medium,
                            textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                            modifier = Modifier.weight(1f)
                        )
                        if (done && doneAt != null) {
                            Text(timeFormat.format(Date(doneAt)), color = C.sub, fontSize = 13.sp)
                        } else if (!done && item.ownAlarm) {
                            Text("${fmtMin(item.ownMinutes)} 알림", color = C.sub, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
