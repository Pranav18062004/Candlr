package app.candlr.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.MonthDay
import java.time.Year
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "birthdays")
data class Birthday(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val month: Int,
    val day: Int,
    val year: Int? = null,
    val notes: String = "",
    val favorite: Boolean = false,
    val photo: String? = null,
    // -1 = global, 0 = off, bit 0/1/2/3 = day of / 1 / 3 / 7 days before
    val reminderMask: Int = -1,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    fun validate(today: LocalDate = LocalDate.now()) {
        require(runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)) {
            "Invalid birthday identifier."
        }
        require(name.isNotBlank() && name.length <= 120) { "Enter a name of 1–120 characters." }
        require(notes.length <= 4000) { "Notes must be 4,000 characters or fewer." }
        require(reminderMask in -1..15) { "Invalid reminder setting." }
        require(runCatching { MonthDay.of(month, day) }.isSuccess) { "Choose a valid birthday." }
        val birthYear = year
        if (birthYear != null) {
            require(birthYear in 1..today.year) { "Enter a valid birth year." }
            require(
                runCatching { LocalDate.of(birthYear, month, day) }
                    .getOrNull()
                    ?.let { !it.isAfter(today) } == true
            ) {
                "Birth date must be a real date in the past or today."
            }
        }
        require(photo == null || photo.matches(Regex("[a-f0-9]{64}\\.jpg"))) {
            "Invalid photo reference."
        }
    }
}

@Serializable
@Entity(tableName = "preferences")
data class Preferences(
    @PrimaryKey val id: Int = 1,
    val theme: String = "system",
    val reduceMotion: Boolean = false,
    val reminders: Boolean = false,
    val hour: Int = 9,
    val minute: Int = 0,
    val reminderMask: Int = 1,
    val leapMarch: Boolean = false,
) {
    fun validate() {
        require(
            id == 1 &&
                theme in listOf("system", "light", "dark") &&
                hour in 0..23 &&
                minute in 0..59 &&
                reminderMask in 1..15
        ) {
            "Invalid backup preferences."
        }
    }
}

fun Birthday.occurrence(year: Int, leapMarch: Boolean): LocalDate =
    if (month == 2 && day == 29 && !Year.isLeap(year.toLong())) {
        if (leapMarch) LocalDate.of(year, 3, 1) else LocalDate.of(year, 2, 28)
    } else LocalDate.of(year, month, day)

fun Birthday.nextBirthday(today: LocalDate, leapMarch: Boolean): LocalDate {
    val candidate = occurrence(today.year, leapMarch)
    return if (candidate < today) occurrence(today.year + 1, leapMarch) else candidate
}

fun Birthday.daysAway(today: LocalDate, leapMarch: Boolean): Long =
    ChronoUnit.DAYS.between(today, nextBirthday(today, leapMarch))

fun Birthday.turningAge(today: LocalDate, leapMarch: Boolean): Int? =
    year?.let { nextBirthday(today, leapMarch).year - it }

val reminderOffsets = listOf(0, 1, 3, 7)

@Entity(tableName = "deliveries", primaryKeys = ["key"])
data class Delivery(val key: String, val deliveredDate: String)
