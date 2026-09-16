package app.candlr

import app.candlr.data.*
import app.candlr.reminders.occurrences
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class BirthdayRulesTest {
    private val today = LocalDate.of(2026, 9, 16)

    @Test
    fun birthdayTodayIsNotNextYear() {
        val person = Birthday(name = "Maya", month = 9, day = 16, year = 1998)
        assertEquals(today, person.nextBirthday(today, false))
        assertEquals(0L, person.daysAway(today, false))
        assertEquals(28, person.turningAge(today, false))
    }

    @Test
    fun passedBirthdayRollsForward() {
        assertEquals(
            LocalDate.of(2027, 9, 15),
            Birthday(name = "Maya", month = 9, day = 15).nextBirthday(today, false),
        )
    }

    @Test
    fun unknownYearNeverInventsAnAge() {
        assertNull(Birthday(name = "Maya", month = 1, day = 1).turningAge(today, false))
    }

    @Test
    fun leapDayUsesSelectedPolicy() {
        val person = Birthday(name = "Leap", month = 2, day = 29, year = 2000)
        assertEquals(LocalDate.of(2027, 2, 28), person.nextBirthday(today, false))
        assertEquals(LocalDate.of(2027, 3, 1), person.nextBirthday(today, true))
        assertEquals(LocalDate.of(2028, 2, 29), person.nextBirthday(LocalDate.of(2028, 2, 1), true))
    }

    @Test
    fun decemberRolloverIsOneDayAway() {
        assertEquals(
            1L,
            Birthday(name = "New year", month = 1, day = 1)
                .daysAway(LocalDate.of(2026, 12, 31), false),
        )
    }

    @Test
    fun advanceReminderCrossesTheYearBoundary() {
        val person = Birthday(name = "New year", month = 1, day = 1)
        val result =
            occurrences(
                listOf(person),
                Preferences(reminders = true, reminderMask = 8),
                LocalDate.of(2026, 12, 24),
            )
        assertTrue(
            result.any {
                it.date == LocalDate.of(2026, 12, 25) && it.birthday == LocalDate.of(2027, 1, 1)
            }
        )
    }

    @Test
    fun globalOffOverridesPersonalReminders() {
        assertTrue(
            occurrences(
                    listOf(Birthday(name = "Maya", month = 9, day = 16, reminderMask = 15)),
                    Preferences(),
                    today,
                )
                .isEmpty()
        )
    }

    @Test
    fun personalOffDoesNotSchedule() {
        assertTrue(
            occurrences(
                    listOf(Birthday(name = "Maya", month = 9, day = 16, reminderMask = 0)),
                    Preferences(reminders = true),
                    today,
                )
                .isEmpty()
        )
    }

    @Test
    fun personalMaskOverridesGlobalMask() {
        val result =
            occurrences(
                listOf(Birthday(name = "Maya", month = 9, day = 23, reminderMask = 8)),
                Preferences(reminders = true, reminderMask = 1),
                today,
            )
        assertTrue(result.all { it.offset == 7 })
    }

    @Test
    fun invalidDatesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Birthday(name = "Maya", month = 4, day = 31).validate(today)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Birthday(name = "Maya", month = 2, day = 29, year = 2001).validate(today)
        }
    }

    @Test
    fun unknownYearFebruary29IsValid() {
        Birthday(name = "Leap", month = 2, day = 29).validate(today)
    }

    @Test
    fun futureBirthDateIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Birthday(name = "Maya", month = 12, day = 1, year = 2026).validate(today)
        }
    }

    @Test
    fun blankNameAndUnsafePhotoAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Birthday(name = " ", month = 1, day = 1).validate(today)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Birthday(name = "Maya", month = 1, day = 1, photo = "../private").validate(today)
        }
    }
}
