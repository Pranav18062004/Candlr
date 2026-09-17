package app.candlr

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.candlr.data.*
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
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
    fun emptyCalendarCanAddAndDirtyEditorRejectsOutsideDismissal() {
        compose.onNodeWithText("Calendar").performClick()
        compose.onNodeWithContentDescription("Add birthday").performClick()
        compose.onNodeWithText("Name").performTextInput("Unfinished draft")
        // Trigger the accessible scrim click; it shares the pointer-dismiss callback.
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Close sheet").performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.OnClick
        ) {
            it()
        }
        compose.onNodeWithText("Discard your changes?").assertIsDisplayed()
        compose.onNodeWithText("Keep editing").performClick()
        compose.onNodeWithText("Unfinished draft").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .performGlobalAction(
                android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
            )
        compose.waitForIdle()
        Thread.sleep(300)
        if (compose.onAllNodesWithText("Discard your changes?").fetchSemanticsNodes().isEmpty()) {
            // The first Back may only close the keyboard.
            InstrumentationRegistry.getInstrumentation()
                .uiAutomation
                .performGlobalAction(
                    android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
                )
        }
        compose.waitUntil(5000) {
            compose.onAllNodesWithText("Discard your changes?").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Keep editing").performClick()
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.onNodeWithText("Discard changes").performClick()
        compose.onNodeWithContentDescription("Add birthday").assertIsDisplayed()
        Assert.assertTrue(runBlocking { app.database.birthdays().all().isEmpty() })
    }

    @Test
    fun tabStateAndCalendarSelectionSurviveSwitchesAndRecreation() {
        runBlocking {
            app.database
                .birthdays()
                .put(Birthday(name = "Maya", month = 9, day = 18, favorite = true))
        }
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Maya").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Bookmarks").performClick()
        compose.onNodeWithContentDescription("Find someone").performClick()
        compose.onNodeWithText("Find someone").performTextInput("May")
        compose.onNodeWithText("Calendar").performClick()
        compose.onNodeWithContentDescription("Next month").performClick()
        val next = YearMonth.now().plusMonths(1)
        val date = next.atDay(12)
        val label = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")) + ", 0 birthdays"
        compose.onNodeWithContentDescription(label).performClick()
        compose.onNodeWithText("Upcoming").performClick()
        compose.onNodeWithText("May").assertIsDisplayed()
        compose.onNodeWithText("Bookmarks").assertIsSelected()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithText("May").assertIsDisplayed()
        compose.onNodeWithText("Calendar").performClick()
        compose
            .onNodeWithText(next.format(DateTimeFormatter.ofPattern("MMMM yyyy")))
            .assertIsDisplayed()
        compose.onNodeWithContentDescription(label).assertIsSelected()
        val bounds = compose.onNodeWithContentDescription(label).fetchSemanticsNode().boundsInRoot
        Assert.assertEquals(bounds.width, bounds.height, 1f)
    }

    @Test
    fun queuedRapidBookmarkTapsAreBothAppliedAndResumeIsSilent() {
        val person = Birthday(name = "Maya", month = 9, day = 18)
        runBlocking {
            app.database.birthdays().put(person)
            app.operations.lock()
        }
        val done = AtomicBoolean(false)
        lateinit var vm: CandlrViewModel
        try {
            compose.runOnIdle {
                vm = ViewModelProvider(compose.activity)[CandlrViewModel::class.java]
                vm.reschedule()
                Assert.assertFalse(vm.busy.value)
                vm.toggleFavorite(person.id)
                vm.toggleFavorite(person.id)
                vm.save(Birthday(name = "Queue barrier", month = 1, day = 1)) { done.set(true) }
            }
        } finally {
            app.operations.unlock()
        }
        compose.waitUntil(10000) { done.get() }
        Assert.assertFalse(
            runBlocking { app.database.birthdays().all().first { it.id == person.id }.favorite }
        )
        compose.waitUntil(10000) { !vm.busy.value }
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        compose.onNodeWithContentDescription("Working on your birthday book…").assertDoesNotExist()
    }

    @Test
    fun editorDraftSurvivesRecreationAndBackRequiresDiscard() {
        compose.onNodeWithText("Add birthday").performClick()
        compose.onNodeWithText("Name").performTextInput("Draft survives")
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithText("Draft survives").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.onNodeWithText("Discard your changes?").assertIsDisplayed()
        compose.onNodeWithText("Keep editing").performClick()
        compose.onNodeWithText("Name").performScrollTo().assertIsDisplayed()
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
        val another = today.withDayOfMonth(if (today.dayOfMonth == 1) 2 else 1)
        val count = runBlocking {
            app.database.birthdays().all().count { it.occurrence(today.year, false) == another }
        }
        val label =
            another.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")) +
                ", " +
                app.resources.getQuantityString(R.plurals.birthday_count, count, count)
        compose.onNodeWithContentDescription(label).performClick()
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
        compose.onNodeWithText("Name").assertIsDisplayed()
        capture("editor-paper")
    }

    @Test
    fun contentIsCenteredAndCapped() {
        val content = compose.onNodeWithTag("page-content").fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val density = compose.activity.resources.displayMetrics.density
        Assert.assertEquals(minOf(root.width, 700f * density), content.width, 1f)
        Assert.assertEquals(root.center.x, content.center.x, 1f)
    }

    @Test
    fun privacyAndReminderHelpAreAvailableOffline() {
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Privacy policy").performScrollTo().performClick()
        compose
            .onNodeWithText("Candlr is developed by Pranav Nambiar", substring = true)
            .assertIsDisplayed()
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Keeping reminders reliable").performScrollTo().performClick()
        compose
            .onNodeWithText("Candlr schedules reminders with Android", substring = true)
            .assertIsDisplayed()
        compose.onNodeWithText("OK").performClick()
        compose
            .onNodeWithText("Developed by Pranav Nambiar", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun capture(baseName: String) {
        val configuration = compose.activity.resources.configuration
        val name =
            baseName +
                when {
                    configuration.fontScale > 1.3f -> "-large-text"
                    configuration.screenWidthDp >= 700 -> "-tablet"
                    else -> ""
                }
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
        Thread.sleep(800)
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
