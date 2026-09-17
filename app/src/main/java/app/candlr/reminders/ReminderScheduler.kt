package app.candlr.reminders

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.candlr.*
import app.candlr.data.*
import java.time.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

data class ReminderOccurrence(
    val person: Birthday,
    val birthday: LocalDate,
    val date: LocalDate,
    val offset: Int,
) {
    val key
        get() = "${person.id}:$birthday:$offset"
}

fun occurrences(
    people: List<Birthday>,
    preferences: Preferences,
    from: LocalDate,
): List<ReminderOccurrence> = buildList {
    if (!preferences.reminders) return@buildList
    for (person in people) {
        val mask = if (person.reminderMask < 0) preferences.reminderMask else person.reminderMask
        for (year in from.year..from.year + 1) {
            val birthday = person.occurrence(year, preferences.leapMarch)
            reminderOffsets.forEachIndexed { index, offset ->
                val date = birthday.minusDays(offset.toLong())
                if (mask and (1 shl index) != 0 && date >= from)
                    add(ReminderOccurrence(person, birthday, date, offset))
            }
        }
    }
}

class ReminderScheduler(private val context: Context, private val db: CandlrDatabase) {
    private val manager
        get() = context.getSystemService(AlarmManager::class.java)

    private val alarmIntent
        get() =
            PendingIntent.getBroadcast(
                context,
                10,
                Intent(context, ReminderReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

    suspend fun schedule(now: ZonedDateTime = ZonedDateTime.now()) {
        manager.cancel(alarmIntent)
        val preferences = db.birthdays().preferences() ?: Preferences()
        if (
            !preferences.reminders ||
                !NotificationManagerCompat.from(context).areNotificationsEnabled()
        )
            return
        val next =
            occurrences(db.birthdays().all(), preferences, now.toLocalDate())
                .sortedBy { it.date }
                .firstOrNull { db.birthdays().delivered(it.key) == 0 } ?: return
        val planned = next.date.atTime(preferences.hour, preferences.minute).atZone(now.zone)
        val timestamp = if (planned <= now) now.plusSeconds(5) else planned
        manager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timestamp.toInstant().toEpochMilli(),
            alarmIntent,
        )
    }

    suspend fun deliver(now: ZonedDateTime = ZonedDateTime.now()) {
        val dao = db.birthdays()
        val preferences = dao.preferences() ?: Preferences()
        val notifications = NotificationManagerCompat.from(context)
        if (!notifications.areNotificationsEnabled()) return
        if (
            Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
        )
            return
        val channel =
            NotificationChannel(
                "birthdays",
                context.getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val due =
            occurrences(dao.all(), preferences, now.toLocalDate()).filter {
                it.date == now.toLocalDate() &&
                    !it.date
                        .atTime(preferences.hour, preferences.minute)
                        .atZone(now.zone)
                        .isAfter(now) &&
                    dao.delivered(it.key) == 0
            }
        if (due.isNotEmpty()) {
            val target =
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    if (due.size == 1) putExtra("personId", due.first().person.id)
                }
            val pending =
                PendingIntent.getActivity(
                    context,
                    20,
                    target,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val lines =
                due.map {
                    if (it.offset == 0)
                        context.getString(R.string.notification_today, it.person.name)
                    else
                        context.resources.getQuantityString(
                            R.plurals.notification_advance,
                            it.offset,
                            it.person.name,
                            it.offset,
                        )
                }
            val notification =
                NotificationCompat.Builder(context, "birthdays")
                    .setSmallIcon(R.drawable.ic_candle)
                    .setContentTitle(context.getString(R.string.notification_title))
                    .setContentText(lines.first())
                    .setStyle(
                        NotificationCompat.InboxStyle().also { style ->
                            lines.take(6).forEach(style::addLine)
                        }
                    )
                    .setContentIntent(pending)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .build()
            notifications.notify(30, notification)
            due.forEach { dao.mark(Delivery(it.key, now.toLocalDate().toString())) }
        }
        dao.pruneDeliveries(now.toLocalDate().minusDays(400).toString())
        schedule(now)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as CandlrApplication
        app.scope.launch {
            try {
                app.operations.withLock { app.reminders.deliver() }
            } catch (_: Exception) {
                android.util.Log.w(
                    "Candlr",
                    "Reminder delivery could not finish; rescheduling will retry on next app open.",
                )
            } finally {
                pending.finish()
            }
        }
    }
}

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action !in
                setOf(
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                )
        )
            return
        val pending = goAsync()
        val app = context.applicationContext as CandlrApplication
        app.scope.launch {
            try {
                app.operations.withLock { app.reminders.schedule() }
            } catch (_: Exception) {
                android.util.Log.w(
                    "Candlr",
                    "Reminder rescheduling could not finish; next app open will retry.",
                )
            } finally {
                pending.finish()
            }
        }
    }
}
