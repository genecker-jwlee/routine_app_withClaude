package com.agiso.routine

import android.content.BroadcastReceiver
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

object Widgets {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 위젯을 다시 그립니다. 리시버에서 부를 때는 goAsync() 결과를 넘깁니다. */
    fun refresh(ctx: Context, pending: BroadcastReceiver.PendingResult? = null) {
        val app = ctx.applicationContext
        scope.launch {
            try {
                RoutineWidget().updateAll(app)
            } catch (e: Exception) {
            } finally {
                pending?.finish()
            }
        }
    }
}

class RoutineWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext
        Store.refreshToday(app)
        provideContent {
            val tick by Store.tick.collectAsState()
            WidgetContent(app, tick)
        }
    }
}

@Composable
private fun WidgetContent(ctx: Context, tick: Int) {
    // 데이터가 바뀌면 이 화면을 다시 그리도록 변경 번호를 읽습니다.
    val version = Store.tick.collectAsState().value
    val today = LocalDate.now()
    val due = Store.dueItems(ctx, today)
    val doneCount = due.count { Store.isDone(ctx, it, today) }
    val textColor = ColorProvider(Color(0xFF2A211C))
    val subColor = ColorProvider(Color(0xFF6B5D54))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF))
            .cornerRadius(22.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "오늘의 루틴",
                style = TextStyle(color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = "$doneCount/${due.size} 완료",
                style = TextStyle(color = subColor, fontSize = 13.sp)
            )
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        if (due.isEmpty()) {
            Text(
                text = "오늘 챙길 항목이 없습니다",
                style = TextStyle(color = subColor, fontSize = 14.sp)
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                items(due) { item ->
                    val count = Store.count(ctx, today, item.id)
                    val done = count >= item.goal
                    val icon = if (done) R.drawable.w_done else if (item.isCount) R.drawable.w_count else R.drawable.w_todo
                    val label = if (item.isCount) "${item.name} $count/${item.goal}" else item.name
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clickable(
                                actionRunCallback<TapAction>(
                                    actionParametersOf(TapAction.KEY to item.id)
                                )
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(icon),
                            contentDescription = if (done) "완료" else "미완료",
                            modifier = GlanceModifier.size(24.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(10.dp))
                        Text(
                            text = label,
                            style = TextStyle(color = if (done) subColor else textColor, fontSize = 15.sp),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

class TapAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[KEY] ?: return
        Store.tap(context.applicationContext, id)
        RoutineWidget().updateAll(context.applicationContext)
    }

    companion object {
        val KEY = ActionParameters.Key<Int>("itemId")
    }
}

class RoutineWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RoutineWidget()
}
