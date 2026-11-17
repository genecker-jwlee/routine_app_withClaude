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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(tick: Int) {
    // 데이터가 바뀌면 이 화면을 다시 그리도록 변경 번호를 읽습니다.
    val version = Store.version.intValue
    val ctx = LocalContext.current
    val today = LocalDate.now()
    var offset by rememberSaveable { mutableStateOf(0) }
    var selected by rememberSaveable { mutableStateOf(-1) }
    val ym = YearMonth.from(today).plusMonths(offset.toLong())
    val length = ym.lengthOfMonth()
    val lead = ym.atDay(1).dayOfWeek.value % 7

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenTitle("기록")

        CardBox {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { offset -= 1; selected = -1 }) {
                        Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "이전 달", tint = C.text)
                    }
                    Text(
                        "${ym.year}년 ${ym.monthValue}월",
                        color = C.text, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { offset += 1; selected = -1 }) {
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "다음 달", tint = C.text)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    for (name in DAY_NAMES) {
                        Text(
                            name, color = C.sub, fontSize = 12.sp,
                            textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
                        )
                    }
                }
                val rows = (lead + length + 6) / 7
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth()) {
                        for (c in 0..6) {
                            val d = r * 7 + c - lead + 1
                            Box(Modifier.weight(1f).height(42.dp), contentAlignment = Alignment.Center) {
                                if (d in 1..length) {
                                    val date = ym.atDay(d)
                                    val s = Store.summary(ctx, date)
                                    // 0 = 기록 없음, 1 = 안 함, 2 = 일부, 3 = 모두 완료
                                    val state = when {
                                        s == null -> 0
                                        s.missed.isEmpty() -> 3
                                        s.done.isEmpty() && s.counts.values.all { it == 0 } -> 1
                                        else -> 2
                                    }
                                    var m = Modifier.size(34.dp).clip(CircleShape)
                                    m = when (state) {
                                        3 -> m.background(C.accent)
                                        2 -> m.background(C.soft).border(1.5.dp, C.accent, CircleShape)
                                        1 -> m.border(1.5.dp, C.ring, CircleShape)
                                        else -> m
                                    }
                                    if (d == selected) m = m.border(2.5.dp, C.text, CircleShape)
                                    Box(m.clickable { selected = d }, contentAlignment = Alignment.Center) {
                                        Text(
                                            "$d",
                                            fontSize = 14.sp,
                                            color = if (state == 3) Color.White else if (state == 0) C.sub else C.text,
                                            fontWeight = if (date == today || state == 3) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(C.accent))
                    Text(" 모두 완료", color = C.sub, fontSize = 12.sp)
                    Spacer(Modifier.width(14.dp))
                    Box(Modifier.size(12.dp).clip(CircleShape).background(C.soft).border(1.5.dp, C.accent, CircleShape))
                    Text(" 일부", color = C.sub, fontSize = 12.sp)
                    Spacer(Modifier.width(14.dp))
                    Box(Modifier.size(12.dp).border(1.5.dp, C.ring, CircleShape))
                    Text(" 안 함", color = C.sub, fontSize = 12.sp)
                }
            }
        }

        if (selected in 1..length) {
            val date = ym.atDay(selected)
            val s = Store.summary(ctx, date)
            val title = date.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))
            CardBox {
                Column(
                    Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (s == null) {
                        Text(title, color = C.sub, fontSize = 13.sp)
                        Text("기록이 없습니다", color = C.text, fontSize = 15.sp)
                    } else {
                        Text(
                            "$title · ${s.done.size + s.missed.size}개 중 ${s.done.size}개",
                            color = C.sub, fontSize = 13.sp
                        )
                        if (s.done.isNotEmpty()) {
                            Text(
                                "완료: " + s.done.joinToString(", ") { it.name },
                                color = C.text, fontSize = 15.sp
                            )
                        }
                        if (s.missed.isNotEmpty()) {
                            Text(
                                "빠짐: " + s.missed.joinToString(", ") {
                                    if (it.isCount) "${it.name} (${it.goal}회 중 ${s.counts[it.id] ?: 0}회)" else it.name
                                },
                                color = C.text, fontSize = 15.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        val items = Store.activeItems(ctx)
        if (items.isNotEmpty()) {
            CardBox {
                Column(
                    Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("${ym.monthValue}월 항목별 완료한 날", color = C.sub, fontSize = 13.sp)
                    for (item in items) {
                        var total = 0
                        var done = 0
                        for (d in 1..length) {
                            val s = Store.summary(ctx, ym.atDay(d)) ?: continue
                            if (s.done.any { it.id == item.id }) {
                                total += 1
                                done += 1
                            } else if (s.missed.any { it.id == item.id }) {
                                total += 1
                            }
                        }
                        val fraction = if (total == 0) 0f else done.toFloat() / total
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                item.name, color = C.text, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                                maxLines = 1, modifier = Modifier.width(84.dp)
                            )
                            Box(
                                Modifier
                                    .weight(1f)
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
                            Text(
                                "$done/$total", color = C.sub, fontSize = 14.sp,
                                textAlign = TextAlign.End, modifier = Modifier.width(52.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
