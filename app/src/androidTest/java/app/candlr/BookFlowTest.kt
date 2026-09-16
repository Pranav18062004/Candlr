package app.candlr

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import app.candlr.data.*
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.*

class BookFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app
        get() = compose.activity.application as CandlrApplication

    @Before
    fun reset() {
        runBlocking {
            app.database.birthdays().clear()
            app.database.birthdays().putPreferences(Preferences())
        }
        compose.waitUntil(10000) {
            compose
                .onAllNodesWithText("A little book of\nyour people.")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun addBirthdaySurvivesRecreationAndCanBeFound() {
        compose.onNodeWithText("Add birthday").performClick()
        compose.onNodeWithText("Name").performTextInput("Maya Rao")
        compose.onNode(hasSetTextAction() and hasText("Day")).performTextInput("20")
        compose.onNodeWithText("Save birthday").performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Maya Rao").fetchSemanticsNodes().isNotEmpty()
        }
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Find someone").performClick()
        compose.onNodeWithText("Find someone").performTextInput("Maya")
        compose.onAllNodesWithText("Maya Rao").onFirst().assertIsDisplayed()
    }

    @Test
    fun capturePaperEveningCalendarAndEditor() {
        val today = LocalDate.now()
        fun person(name: String, offset: Long, year: Int, favorite: Boolean = false): Birthday {
            val date = today.plusDays(offset)
            return Birthday(
                name = name,
                month = date.monthValue,
                day = date.dayOfMonth,
                year = year,
                favorite = favorite,
            )
        }
        runBlocking {
            app.database
                .birthdays()
                .putAll(
                    listOf(
                        person("Maya Rao", 1, 1998, true),
                        person("Arjun Shah", 5, 1995),
                        person("Sara Thomas", 12, 2000, true),
                        person("Aanya Mehta", 25, 1999),
                        person("Rohan Sen", 42, 1994),
                    )
                )
        }
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Maya Rao").fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
        capture("upcoming-paper")
        runBlocking { app.database.birthdays().putPreferences(Preferences(theme = "dark")) }
        compose.waitForIdle()
        Thread.sleep(400)
        capture("upcoming-evening")
        compose.onNodeWithText("Calendar").performClick()
        compose.waitForIdle()
        capture("calendar-evening")
        compose.onNodeWithText("Settings").performClick()
        compose.waitForIdle()
        capture("settings-evening")
        runBlocking { app.database.birthdays().putPreferences(Preferences(theme = "light")) }
        compose.onNodeWithText("Upcoming").performClick()
        compose.waitForIdle()
        capture("upcoming-paper")
        compose.waitUntil(10000) {
            compose
                .onAllNodesWithContentDescription("Add birthday")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithContentDescription("Add birthday").performClick()
        compose.waitForIdle()
        capture("editor-paper")
    }

    private fun capture(name: String) {
        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
        Thread.sleep(300)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val file = File(app.getExternalFilesDir(null), "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        listOf(
                "mkdir -p /sdcard/Download/candlr-qa",
                "cp ${file.absolutePath} /sdcard/Download/candlr-qa/$name.png",
            )
            .forEach { command ->
                android.os.ParcelFileDescriptor.AutoCloseInputStream(
                        InstrumentationRegistry.getInstrumentation()
                            .uiAutomation
                            .executeShellCommand(command)
                    )
                    .use { it.readBytes() }
            }
    }
}
