package app.candlr

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.candlr.data.*
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class RestoreTransactionTest {
    private lateinit var db: CandlrDatabase
    private lateinit var service: BackupService
    private val original = Birthday(name = "Original", month = 8, day = 1)

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CandlrDatabase::class.java).build()
        service =
            BackupService(
                db,
                PhotoStore(context),
                File(context.cacheDir, "recovery-${System.nanoTime()}.candlr"),
            )
        db.birthdays().put(original)
        db.birthdays().putPreferences(Preferences(theme = "dark"))
    }

    @After
    fun close() {
        db.close()
    }

    @Test
    fun mergeIsIdempotentAndKeepsCurrentPreferences() = runBlocking {
        val data =
            BackupContents(
                listOf(original, Birthday(name = "New", month = 5, day = 2)),
                Preferences(theme = "light"),
                emptyMap(),
            )
        service.restore(data, false, false)
        service.restore(data, false, false)
        assertEquals(2, db.birthdays().all().size)
        assertEquals("dark", db.birthdays().preferences()!!.theme)
    }

    @Test
    fun replaceAndRecoveryPreserveTheWholeBook() = runBlocking {
        service.restore(
            BackupContents(emptyList(), Preferences(theme = "light"), emptyMap()),
            true,
            true,
        )
        assertTrue(db.birthdays().all().isEmpty())
        assertEquals("light", db.birthdays().preferences()!!.theme)
        val recovery = service.readRecovery()
        assertEquals(listOf(original), recovery.birthdays)
        service.restore(recovery, true, true)
        assertEquals(listOf(original), db.birthdays().all())
        assertEquals("dark", db.birthdays().preferences()!!.theme)
    }

    @Test
    fun conflictPolicyIsExplicit() = runBlocking {
        val changed = original.copy(notes = "Changed")
        val data = BackupContents(listOf(changed), Preferences(), emptyMap())
        service.restore(data, false, false)
        assertEquals("", db.birthdays().all().single().notes)
        service.restore(data, false, true)
        assertEquals("Changed", db.birthdays().all().single().notes)
    }

    @Test
    fun invalidPhotoCannotReplaceCurrentData() = runBlocking {
        val bytes = byteArrayOf(1, 2, 3)
        val name = PhotoStore.hash(bytes) + ".jpg"
        val data =
            BackupContents(
                listOf(original.copy(name = "Changed", photo = name)),
                Preferences(),
                mapOf(name to bytes),
            )
        try {
            service.restore(data, true, true)
            fail("Invalid photo accepted")
        } catch (_: IllegalArgumentException) {}
        assertEquals(listOf(original), db.birthdays().all())
        assertEquals("dark", db.birthdays().preferences()!!.theme)
    }
}
