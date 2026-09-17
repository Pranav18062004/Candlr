package app.candlr

import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.candlr.data.*
import app.candlr.reminders.ReminderScheduler
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 28], application = android.app.Application::class)
class ReminderDeliveryTest {
    private lateinit var db: CandlrDatabase
    private lateinit var scheduler: ReminderScheduler
    private lateinit var context: Context
    private val now = ZonedDateTime.parse("2026-09-16T09:01:00+05:30[Asia/Kolkata]")
    private val person = Birthday(name = "Maya", month = 9, day = 16)

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, CandlrDatabase::class.java).build()
        scheduler = ReminderScheduler(context, db)
        db.birthdays().put(person)
        db.birthdays().putPreferences(Preferences(reminders = true))
    }

    @After
    fun close() {
        db.close()
    }

    @Test
    fun channelExistsBeforeAnyReminderAndKeepsUserSettings() = runBlocking {
        db.birthdays().putPreferences(Preferences(reminders = false))
        scheduler.schedule(now)
        val manager = context.getSystemService(NotificationManager::class.java)
        assertNotNull(manager.getNotificationChannel("birthdays"))
        assertEquals(0, manager.activeNotifications.size)
        scheduler.ensureChannel()
        assertEquals(1, manager.notificationChannels.size)
    }

    @Test
    fun deliversOnceAndRecordsOccurrence() = runBlocking {
        scheduler.deliver(now)
        assertEquals(1, db.birthdays().delivered("${person.id}:2026-09-16:0"))
        scheduler.deliver(now.plusMinutes(1))
        assertEquals(
            1,
            context.getSystemService(NotificationManager::class.java).activeNotifications.size,
        )
    }

    @Test
    fun neverDeliversBeforeSelectedTime() = runBlocking {
        scheduler.deliver(now.minusMinutes(2))
        assertEquals(0, db.birthdays().delivered("${person.id}:2026-09-16:0"))
    }

    @Test
    fun disabledRemindersDoNotDeliver() = runBlocking {
        db.birthdays().putPreferences(Preferences(reminders = false))
        scheduler.deliver(now)
        assertEquals(0, db.birthdays().delivered("${person.id}:2026-09-16:0"))
    }

    @Test
    fun advanceNotificationsUseSingularAndPlural() = runBlocking {
        for (offset in listOf(1, 3, 7)) {
            db.birthdays()
                .put(
                    person.copy(
                        day = 16 + offset,
                        reminderMask = 1 shl reminderOffsets.indexOf(offset),
                    )
                )
            scheduler.deliver(now)
            val notification =
                context
                    .getSystemService(NotificationManager::class.java)
                    .activeNotifications
                    .single()
                    .notification
            assertEquals(
                "Maya has a birthday in $offset " + if (offset == 1) "day" else "days",
                notification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString(),
            )
        }
    }
}
