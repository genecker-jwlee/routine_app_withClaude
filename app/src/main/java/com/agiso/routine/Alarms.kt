package com.agiso.routine

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import java.time.LocalDate
import java.util.Calendar

/** 알림 예약. 매일 반복은 알람이 울릴 때마다 다음 날 것을 다시 예약하는 방식입니다. */
object Alarms {
    const val T_MORNING = "morning"
    const val T_EVENING = "evening"
    const val T_ITEM = "item"
    const val T_MIDNIGHT = "midnight"

    fun rescheduleAll(ctx: Context) {
        val app = ctx.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val p = Store.prefs(app)
        schedule(app, am, 1, T_MORNING, 0, if (p.morningOn) p.morningMin else null, 0)
        schedule(app, am, 2, T_EVENING, 0, if (p.eveningOn) p.eveningMin else null, 0)
        // 자정 직후: 위젯과 오늘 기록을 새 날짜로 넘깁니다.
        schedule(app, am, 3, T_MIDNIGHT, 0, 0, 5)
        for (item in Store.everyItem(app)) {
            val minutes = if (!item.deleted && item.ownAlarm) item.ownMinutes else null
            schedule(app, am, 1000 + item.id, T_ITEM, item.id, minutes, 0)
        }
    }

    private fun pending(ctx: Context, code: Int, type: String, itemId: Int): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java)
        i.action = "com.agiso.routine.ALARM"
        i.data = Uri.parse("agiso://alarm/$code")
        i.putExtra("type", type)
        i.putExtra("itemId", itemId)
        return PendingIntent.getBroadcast(
            ctx, code, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun schedule(
        ctx: Context, am: AlarmManager, code: Int, type: String, itemId: Int,
        minutes: Int?, second: Int,
    ) {
        val pi = pending(ctx, code, type, itemId)
        am.cancel(pi)
        if (minutes == null) return
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, minutes / 60)
        cal.set(Calendar.MINUTE, minutes % 60)
        cal.set(Calendar.SECOND, second)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
        val at = cal.timeInMillis
        try {
            val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
            if (exact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }
}

object Notifier {
    private const val CHANNEL = "routine"
    const val ID_MORNING = 1
    const val ID_EVENING = 2

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(CHANNEL, "루틴 알림", NotificationManager.IMPORTANCE_HIGH)
        ch.description = "아침 알림, 항목별 알림, 저녁 재알림"
        nm.createNotificationChannel(ch)
    }

    fun label(ctx: Context, item: Item, date: LocalDate): String {
        if (!item.isCount) return item.name
        return "${item.name} (${item.goal}회 중 ${Store.count(ctx, date, item.id)}회)"
    }

    fun show(ctx: Context, notifId: Int, title: String, text: String, ids: List<Int>, doneLabel: String) {
        ensureChannel(ctx)
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openIntent = Intent(ctx, MainActivity::class.java)
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val open = PendingIntent.getActivity(
            ctx, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val doneIntent = Intent(ctx, ActionReceiver::class.java)
        doneIntent.action = "com.agiso.routine.DONE"
        doneIntent.data = Uri.parse("agiso://done/$notifId")
        doneIntent.putExtra("ids", ids.toIntArray())
        doneIntent.putExtra("notifId", notifId)
        val done = PendingIntent.getBroadcast(
            ctx, notifId, doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setColor(0xFFB8502F.toInt())
            .setContentTitle(title)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, doneLabel, done)
        if (text.isNotEmpty()) {
            b.setContentText(text)
            b.setStyle(NotificationCompat.BigTextStyle().bigText(text))
        }
        if (ids.size > 1) b.addAction(0, "열기", open)
        try {
            nm.notify(notifId, b.build())
        } catch (e: SecurityException) {
        }
    }

    fun cancel(ctx: Context, notifId: Int) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notifId)
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ctx = context.applicationContext
        val type = intent.getStringExtra("type") ?: return
        val today = LocalDate.now()
        Store.refreshToday(ctx)
        val prefs = Store.prefs(ctx)
        when (type) {
            Alarms.T_MORNING -> {
                val list = Store.dueItems(ctx, today).filter { it.morning && !Store.isDone(ctx, it, today) }
                if (prefs.morningOn && list.isNotEmpty()) {
                    Notifier.show(
                        ctx, Notifier.ID_MORNING, "오늘 챙길 것 ${list.size}개",
                        list.joinToString(", ") { Notifier.label(ctx, it, today) },
                        list.map { it.id }, if (list.size > 1) "모두 완료" else "완료"
                    )
                }
            }
            Alarms.T_EVENING -> {
                val list = Store.dueItems(ctx, today).filter { !Store.isDone(ctx, it, today) }
                if (prefs.eveningOn && list.isNotEmpty()) {
                    Notifier.show(
                        ctx, Notifier.ID_EVENING, "아직 안 한 것 ${list.size}개",
                        list.joinToString(", ") { Notifier.label(ctx, it, today) },
                        list.map { it.id }, if (list.size > 1) "모두 완료" else "완료"
                    )
                }
            }
            Alarms.T_ITEM -> {
                val item = Store.item(ctx, intent.getIntExtra("itemId", -1))
                if (item != null && item.ownAlarm && item.dueOn(today) && !Store.isDone(ctx, item, today)) {
                    val text = if (item.isCount) "${item.goal}회 중 ${Store.count(ctx, today, item.id)}회" else ""
                    Notifier.show(ctx, 1000 + item.id, "${item.name} 챙길 시간", text, listOf(item.id), "완료")
                }
            }
            Alarms.T_MIDNIGHT -> {
                Notifier.cancel(ctx, Notifier.ID_MORNING)
                Notifier.cancel(ctx, Notifier.ID_EVENING)
                Store.bump()
            }
        }
        Alarms.rescheduleAll(ctx)
        Widgets.refresh(ctx, goAsync())
    }
}

/** 알림의 "완료" / "모두 완료" 버튼. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ctx = context.applicationContext
        val ids = intent.getIntArrayExtra("ids") ?: IntArray(0)
        val notifId = intent.getIntExtra("notifId", 0)
        Store.complete(ctx, ids.toList())
        Notifier.cancel(ctx, notifId)
        Widgets.refresh(ctx, goAsync())
    }
}

/** 재부팅, 앱 업데이트, 시간 변경 뒤 알림 예약을 다시 등록합니다. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ctx = context.applicationContext
        Notifier.ensureChannel(ctx)
        Store.refreshToday(ctx)
        Alarms.rescheduleAll(ctx)
        Widgets.refresh(ctx, goAsync())
    }
}
