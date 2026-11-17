package com.agiso.routine

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/** 챙길 항목 하나. days는 요일 비트(0=일요일 … 6=토요일). */
data class Item(
    val id: Int,
    val name: String,
    val isCount: Boolean = false,
    val target: Int = 1,
    val days: Int = 127,
    val morning: Boolean = true,
    val ownAlarm: Boolean = false,
    val ownMinutes: Int = 18 * 60,
    val order: Int = 0,
    val deleted: Boolean = false,
) {
    val goal: Int get() = if (isCount) target.coerceAtLeast(1) else 1

    fun dueOn(date: LocalDate): Boolean {
        val bit = date.dayOfWeek.value % 7
        return !deleted && ((days shr bit) and 1) == 1
    }
}

data class Prefs(
    val morningOn: Boolean = true,
    val morningMin: Int = 7 * 60,
    val eveningOn: Boolean = true,
    val eveningMin: Int = 20 * 60,
)

/** 하루치 기록. due는 그날 챙겨야 했던 항목 번호. */
class DayRecord {
    var due: Set<Int> = emptySet()
    val counts = HashMap<Int, Int>()
    val times = HashMap<Int, Long>()
}

class DaySummary(val done: List<Item>, val missed: List<Item>, val counts: Map<Int, Int>)

/** 모든 데이터는 폰 안의 data.json 한 파일에 저장합니다. */
object Store {
    private val _tick = MutableStateFlow(0)
    val tick: StateFlow<Int> get() = _tick

    private var loaded = false
    private val items = ArrayList<Item>()
    private var nextId = 1
    private var prefs = Prefs()
    private val days = HashMap<String, DayRecord>()

    private fun file(ctx: Context) = File(ctx.applicationContext.filesDir, "data.json")

    @Synchronized
    private fun ensure(ctx: Context) {
        if (loaded) return
        loaded = true
        val f = file(ctx)
        if (!f.exists()) {
            seed()
            return
        }
        try {
            parse(JSONObject(f.readText()))
        } catch (e: Exception) {
            try {
                f.copyTo(File(f.parentFile, "data.bad.json"), overwrite = true)
            } catch (e2: Exception) {
            }
            items.clear()
            days.clear()
            seed()
        }
    }

    private fun seed() {
        items.add(Item(id = 1, name = "비타민D", order = 0))
        items.add(Item(id = 2, name = "유산균", order = 1))
        items.add(Item(id = 3, name = "우유", isCount = true, target = 5, order = 2))
        nextId = 4
    }

    private fun parse(root: JSONObject) {
        nextId = root.optInt("nextId", 1)
        val p = root.optJSONObject("prefs")
        if (p != null) {
            prefs = Prefs(
                morningOn = p.optBoolean("morningOn", true),
                morningMin = p.optInt("morningMin", 7 * 60),
                eveningOn = p.optBoolean("eveningOn", true),
                eveningMin = p.optInt("eveningMin", 20 * 60),
            )
        }
        val arr = root.optJSONArray("items") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            items.add(
                Item(
                    id = o.getInt("id"),
                    name = o.optString("name", ""),
                    isCount = o.optBoolean("isCount", false),
                    target = o.optInt("target", 1),
                    days = o.optInt("days", 127),
                    morning = o.optBoolean("morning", true),
                    ownAlarm = o.optBoolean("ownAlarm", false),
                    ownMinutes = o.optInt("ownMinutes", 18 * 60),
                    order = o.optInt("order", i),
                    deleted = o.optBoolean("deleted", false),
                )
            )
        }
        val d = root.optJSONObject("days") ?: JSONObject()
        val keys = d.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val o = d.getJSONObject(key)
            val rec = DayRecord()
            val due = o.optJSONArray("due") ?: JSONArray()
            val set = HashSet<Int>()
            for (i in 0 until due.length()) set.add(due.getInt(i))
            rec.due = set
            val c = o.optJSONObject("c") ?: JSONObject()
            val ck = c.keys()
            while (ck.hasNext()) {
                val k = ck.next()
                rec.counts[k.toInt()] = c.getInt(k)
            }
            val t = o.optJSONObject("t") ?: JSONObject()
            val tk = t.keys()
            while (tk.hasNext()) {
                val k = tk.next()
                rec.times[k.toInt()] = t.getLong(k)
            }
            days[key] = rec
        }
    }

    private fun save(ctx: Context) {
        val root = JSONObject()
        root.put("nextId", nextId)
        root.put(
            "prefs",
            JSONObject()
                .put("morningOn", prefs.morningOn)
                .put("morningMin", prefs.morningMin)
                .put("eveningOn", prefs.eveningOn)
                .put("eveningMin", prefs.eveningMin)
        )
        val arr = JSONArray()
        for (it in items) {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("isCount", it.isCount)
                    .put("target", it.target)
                    .put("days", it.days)
                    .put("morning", it.morning)
                    .put("ownAlarm", it.ownAlarm)
                    .put("ownMinutes", it.ownMinutes)
                    .put("order", it.order)
                    .put("deleted", it.deleted)
            )
        }
        root.put("items", arr)
        val d = JSONObject()
        for ((key, rec) in days) {
            val o = JSONObject()
            val due = JSONArray()
            for (id in rec.due) due.put(id)
            o.put("due", due)
            val c = JSONObject()
            for ((k, v) in rec.counts) c.put(k.toString(), v)
            o.put("c", c)
            val t = JSONObject()
            for ((k, v) in rec.times) t.put(k.toString(), v)
            o.put("t", t)
            d.put(key, o)
        }
        root.put("days", d)
        try {
            val f = file(ctx)
            val tmp = File(f.parentFile, "data.tmp")
            tmp.writeText(root.toString())
            if (!tmp.renameTo(f)) {
                f.writeText(root.toString())
                tmp.delete()
            }
        } catch (e: Exception) {
        }
    }

    /** 오늘 챙길 항목 목록을 오늘 기록에 남깁니다. 바뀌었으면 true. */
    private fun snapshotToday(): Boolean {
        val today = LocalDate.now()
        val rec = days.getOrPut(today.toString()) { DayRecord() }
        val now = HashSet<Int>()
        for (it in items) if (it.dueOn(today)) now.add(it.id)
        if (rec.due == now) return false
        rec.due = now
        return true
    }

    private fun persist(ctx: Context) {
        snapshotToday()
        save(ctx)
        _tick.value = _tick.value + 1
        Widgets.refresh(ctx)
    }

    fun bump() {
        _tick.value = _tick.value + 1
    }

    @Synchronized
    fun refreshToday(ctx: Context) {
        ensure(ctx)
        if (snapshotToday()) {
            save(ctx)
            _tick.value = _tick.value + 1
        }
    }

    @Synchronized
    fun prefs(ctx: Context): Prefs {
        ensure(ctx)
        return prefs
    }

    @Synchronized
    fun setPrefs(ctx: Context, p: Prefs) {
        ensure(ctx)
        prefs = p
        persist(ctx)
        Alarms.rescheduleAll(ctx)
    }

    @Synchronized
    fun activeItems(ctx: Context): List<Item> {
        ensure(ctx)
        return items.filter { !it.deleted }.sortedBy { it.order }
    }

    @Synchronized
    fun everyItem(ctx: Context): List<Item> {
        ensure(ctx)
        return items.toList()
    }

    @Synchronized
    fun item(ctx: Context, id: Int): Item? {
        ensure(ctx)
        return items.firstOrNull { it.id == id }
    }

    @Synchronized
    fun dueItems(ctx: Context, date: LocalDate): List<Item> {
        return activeItems(ctx).filter { it.dueOn(date) }
    }

    @Synchronized
    fun count(ctx: Context, date: LocalDate, id: Int): Int {
        ensure(ctx)
        return days[date.toString()]?.counts?.get(id) ?: 0
    }

    @Synchronized
    fun doneAt(ctx: Context, date: LocalDate, id: Int): Long? {
        ensure(ctx)
        return days[date.toString()]?.times?.get(id)
    }

    @Synchronized
    fun isDone(ctx: Context, item: Item, date: LocalDate): Boolean {
        return count(ctx, date, item.id) >= item.goal
    }

    private fun setCountInternal(item: Item, value: Int) {
        val rec = days.getOrPut(LocalDate.now().toString()) { DayRecord() }
        val v = value.coerceIn(0, item.goal)
        rec.counts[item.id] = v
        if (v >= item.goal) rec.times[item.id] = System.currentTimeMillis() else rec.times.remove(item.id)
    }

    /** 한 번 체크 항목: 완료와 해제를 오갑니다. */
    @Synchronized
    fun toggle(ctx: Context, id: Int) {
        val item = item(ctx, id) ?: return
        val cur = count(ctx, LocalDate.now(), id)
        setCountInternal(item, if (cur >= item.goal) 0 else item.goal)
        persist(ctx)
    }

    /** 횟수 항목: delta만큼 더하거나 뺍니다. */
    @Synchronized
    fun add(ctx: Context, id: Int, delta: Int) {
        val item = item(ctx, id) ?: return
        val cur = count(ctx, LocalDate.now(), id)
        setCountInternal(item, cur + delta)
        persist(ctx)
    }

    /** 위젯에서 탭: 체크 항목은 토글, 횟수 항목은 +1(목표에 닿은 뒤 다시 누르면 0). */
    @Synchronized
    fun tap(ctx: Context, id: Int) {
        val item = item(ctx, id) ?: return
        val cur = count(ctx, LocalDate.now(), id)
        if (item.isCount) {
            setCountInternal(item, if (cur >= item.goal) 0 else cur + 1)
        } else {
            setCountInternal(item, if (cur >= item.goal) 0 else item.goal)
        }
        persist(ctx)
    }

    /** 알림의 완료 버튼: 받은 항목을 모두 완료로 표시합니다. */
    @Synchronized
    fun complete(ctx: Context, ids: List<Int>) {
        ensure(ctx)
        for (id in ids) {
            val item = items.firstOrNull { it.id == id } ?: continue
            if (item.deleted) continue
            setCountInternal(item, item.goal)
        }
        persist(ctx)
    }

    @Synchronized
    fun addItem(ctx: Context, name: String): Item {
        ensure(ctx)
        val maxOrder = items.maxOfOrNull { it.order } ?: -1
        val item = Item(id = nextId, name = name, order = maxOrder + 1)
        nextId += 1
        items.add(item)
        persist(ctx)
        Alarms.rescheduleAll(ctx)
        return item
    }

    @Synchronized
    fun update(ctx: Context, item: Item) {
        ensure(ctx)
        val i = items.indexOfFirst { it.id == item.id }
        if (i < 0) return
        items[i] = item
        persist(ctx)
        Alarms.rescheduleAll(ctx)
    }

    /** 목록에서만 지웁니다. 지난 기록은 달력에 남습니다. */
    @Synchronized
    fun delete(ctx: Context, id: Int) {
        ensure(ctx)
        val i = items.indexOfFirst { it.id == id }
        if (i < 0) return
        items[i] = items[i].copy(deleted = true)
        persist(ctx)
        Alarms.rescheduleAll(ctx)
    }

    /** dir = -1 위로, +1 아래로. */
    @Synchronized
    fun move(ctx: Context, id: Int, dir: Int) {
        val list = activeItems(ctx)
        val i = list.indexOfFirst { it.id == id }
        val j = i + dir
        if (i < 0 || j < 0 || j >= list.size) return
        val reordered = list.toMutableList()
        val moved = reordered.removeAt(i)
        reordered.add(j, moved)
        for ((n, it) in reordered.withIndex()) {
            val k = items.indexOfFirst { x -> x.id == it.id }
            if (k >= 0) items[k] = items[k].copy(order = n)
        }
        persist(ctx)
    }

    /** 기록 달력용. 기록이 없는 날은 null. */
    @Synchronized
    fun summary(ctx: Context, date: LocalDate): DaySummary? {
        ensure(ctx)
        val rec = days[date.toString()] ?: return null
        if (rec.due.isEmpty()) return null
        val done = ArrayList<Item>()
        val missed = ArrayList<Item>()
        val all = items.filter { rec.due.contains(it.id) }.sortedBy { it.order }
        for (it in all) {
            val c = rec.counts[it.id] ?: 0
            if (c >= it.goal) done.add(it) else missed.add(it)
        }
        return DaySummary(done, missed, HashMap(rec.counts))
    }
}
