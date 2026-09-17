package app.candlr

import app.candlr.data.*
import java.io.*
import java.util.zip.*
import org.junit.Assert.*
import org.junit.Test

class BackupCodecTest {
    @Test
    fun exportRejectsDocumentsThatItsOwnReaderCannotRestore() {
        val records =
            (1..2200).map {
                Birthday(name = "Person $it", month = 1, day = 1, notes = "x".repeat(4000))
            }
        val output = ByteArrayOutputStream()
        val failure =
            runCatching {
                    BackupCodec.write(output, BackupContents(records, Preferences(), emptyMap()))
                }
                .exceptionOrNull()
        assertEquals(BookError.BACKUP_TOO_LARGE, failure!!.bookError())
        assertEquals(0, output.size())
    }

    private val person =
        Birthday(
            name = "Maya Rao",
            month = 2,
            day = 29,
            notes = "A book & a handwritten note",
            favorite = true,
        )

    private fun encoded(data: BackupContents): ByteArray =
        ByteArrayOutputStream().also { BackupCodec.write(it, data) }.toByteArray()

    private fun zip(vararg entries: Pair<String, String>): ByteArray =
        ByteArrayOutputStream()
            .also { out ->
                ZipOutputStream(out).use { zip ->
                    entries.forEach { (name, text) ->
                        zip.putNextEntry(ZipEntry(name))
                        zip.write(text.toByteArray())
                        zip.closeEntry()
                    }
                }
            }
            .toByteArray()

    @Test
    fun roundTripPreservesUnicodeAndSettings() {
        val data =
            BackupContents(
                listOf(person.copy(name = "Zoë · മീര")),
                Preferences(theme = "dark", leapMarch = true, minute = 35),
                emptyMap(),
            )
        val restored = BackupCodec.read(encoded(data).inputStream())
        assertEquals(data, restored)
    }

    @Test
    fun archiveTraversalIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.read(zip("../file" to "bad").inputStream())
        }
    }

    @Test
    fun unsupportedVersionIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.read(
                zip("manifest.json" to "{\"format\":\"candlr\",\"version\":99}").inputStream()
            )
        }
    }

    @Test
    fun truncatedArchiveCannotRestore() {
        val bytes = encoded(BackupContents(listOf(person), Preferences(), emptyMap()))
        assertThrows(Exception::class.java) {
            BackupCodec.read(bytes.copyOf(bytes.size / 2).inputStream())
        }
    }

    @Test
    fun duplicateIdentifiersAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.read(
                encoded(BackupContents(listOf(person, person), Preferences(), emptyMap()))
                    .inputStream()
            )
        }
    }

    @Test
    fun missingPhotoIsRejected() {
        val reference = "a".repeat(64) + ".jpg"
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.read(
                encoded(
                        BackupContents(
                            listOf(person.copy(photo = reference)),
                            Preferences(),
                            emptyMap(),
                        )
                    )
                    .inputStream()
            )
        }
    }

    @Test
    fun damagedPhotoIsRejected() {
        val reference = "a".repeat(64) + ".jpg"
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.read(
                encoded(
                        BackupContents(
                            listOf(person.copy(photo = reference)),
                            Preferences(),
                            mapOf(reference to byteArrayOf(1, 2, 3)),
                        )
                    )
                    .inputStream()
            )
        }
    }

    @Test
    fun oversizedEntryIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.read(zip("manifest.json" to "x".repeat(8 * 1024 * 1024 + 1)).inputStream())
        }
    }

    @Test
    fun sameNameDifferentPeopleSurvive() {
        val data =
            BackupContents(
                listOf(person, Birthday(name = person.name, month = 1, day = 1)),
                Preferences(),
                emptyMap(),
            )
        assertEquals(2, BackupCodec.read(encoded(data).inputStream()).birthdays.size)
    }
}
