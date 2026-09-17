package app.candlr

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.candlr.data.*
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 28], application = android.app.Application::class)
class PhotoLifecycleTest {
    @Test
    fun oversizedPhotoIsRejectedBeforeSnapshotLoadsIt() = runBlocking {
        val name = photo(8)
        java.io.RandomAccessFile(photos.file(name), "rw").use { it.setLength(60L * 1024 * 1024) }
        val db = Room.inMemoryDatabaseBuilder(context, CandlrDatabase::class.java).build()
        try {
            db.birthdays().put(Birthday(name = "Maya", month = 1, day = 1, photo = name))
            val backup = BackupService(db, photos, File(context.cacheDir, "oversized.candlr"))
            val failure = runCatching { backup.snapshot() }.exceptionOrNull()
            assertEquals(BookError.PHOTOS_TOO_LARGE, failure!!.bookError())
            assertEquals(1, db.birthdays().all().size)
        } finally {
            db.close()
            photos.file(name).delete()
        }
    }

    private lateinit var photos: PhotoStore
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        photos = PhotoStore(context)
        photos.pinDraft(null)
        photos.prune(emptySet())
    }

    private fun photo(seed: Int): String {
        val name = PhotoStore.hash(byteArrayOf(seed.toByte())) + ".jpg"
        photos.file(name).writeBytes(byteArrayOf(seed.toByte()))
        return name
    }

    @Test
    fun cleanupPreservesSharedReferencesDraftAndUndo() {
        val saved = photo(1)
        val draft = photo(2)
        val deleted = photo(3)
        val orphan = photo(4)
        photos.pinDraft(draft)
        photos.pinUndo("person", deleted)
        photos.prune(setOf(saved))
        assertTrue(listOf(saved, draft, deleted).all { photos.file(it).exists() })
        assertFalse(photos.file(orphan).exists())
        photos.pinDraft(null)
        photos.releaseUndo("person")
        photos.prune(setOf(saved))
        assertTrue(photos.file(saved).exists())
        assertFalse(photos.file(draft).exists())
        assertFalse(photos.file(deleted).exists())
    }

    @Test
    fun recreationKeepsOnlyLatestDraftAndReclaimsInterruptedStaging() {
        val first = photo(5)
        val second = photo(6)
        photos.pinDraft(first)
        photos.pinDraft(second)
        val temp = File(photos.directory, "${first}.tmp").apply { writeText("interrupted") }
        PhotoStore(context).prune(emptySet())
        assertFalse(photos.file(first).exists())
        assertFalse(temp.exists())
        assertTrue(photos.file(second).exists())
    }

    @Test
    fun cacheReusesFullResolutionBitmapWithoutReadingDiskAgain() {
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val bytes =
            ByteArrayOutputStream()
                .also { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                .toByteArray()
        val name = PhotoStore.hash(bytes) + ".jpg"
        photos.file(name).writeBytes(bytes)
        val first = photos.load(name)!!
        assertEquals(512, first.width)
        photos.file(name).delete()
        assertSame(first, photos.load(name))
        photos.clearCache()
        assertNull(photos.cached(name))
    }

    @Test
    fun replaceReclaimsOldPhotosAndRecoveryRestoresThem() = runBlocking {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        val bytes =
            ByteArrayOutputStream()
                .also { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                .toByteArray()
        val name = PhotoStore.hash(bytes) + ".jpg"
        photos.file(name).writeBytes(bytes)
        val person = Birthday(name = "Maya", month = 2, day = 1, photo = name)
        val db = Room.inMemoryDatabaseBuilder(context, CandlrDatabase::class.java).build()
        try {
            db.birthdays().put(person)
            val backup = BackupService(db, photos, File(context.cacheDir, "photo-recovery.candlr"))
            backup.restore(BackupContents(emptyList(), Preferences(), emptyMap()), true, true)
            assertFalse(photos.file(name).exists())
            backup.restore(backup.readRecovery(), true, true)
            assertEquals(person, db.birthdays().all().single())
            assertArrayEquals(bytes, photos.file(name).readBytes())
        } finally {
            db.close()
        }
    }
}
