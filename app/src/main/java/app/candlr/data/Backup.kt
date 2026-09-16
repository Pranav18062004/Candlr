package app.candlr.data

import androidx.room.withTransaction
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupManifest(
    val format: String = "candlr",
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
)

data class BackupContents(
    val birthdays: List<Birthday>,
    val preferences: Preferences,
    val photos: Map<String, ByteArray>,
)

data class RestoreResult(val added: Int, val updated: Int, val unchanged: Int)

/** Bounded parser. Entries never determine filesystem paths. No live state is touched here. */
object BackupCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    const val MAX_BYTES = 64 * 1024 * 1024

    fun write(output: OutputStream, data: BackupContents) {
        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            fun entry(name: String, bytes: ByteArray) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
            entry("manifest.json", json.encodeToString(BackupManifest()).toByteArray())
            entry("birthdays.json", json.encodeToString(data.birthdays).toByteArray())
            entry("settings.json", json.encodeToString(data.preferences).toByteArray())
            data.photos.forEach { (name, bytes) -> entry("photos/$name", bytes) }
        }
    }

    fun read(input: InputStream): BackupContents {
        val entries = mutableMapOf<String, ByteArray>()
        var total = 0L
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                require(
                    !entry.isDirectory &&
                        (name in listOf("manifest.json", "birthdays.json", "settings.json") ||
                            name.matches(Regex("photos/[a-f0-9]{64}\\.jpg")))
                ) {
                    "Backup contains an unsupported file."
                }
                require(!entries.containsKey(name) && entries.size < 10003) {
                    "Backup contains duplicate or too many files."
                }
                val limit = if (name.startsWith("photos/")) 1024 * 1024 else 8 * 1024 * 1024
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = zip.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_BYTES && out.size() + count <= limit) {
                        "Backup is too large (64 MB maximum)."
                    }
                    out.write(buffer, 0, count)
                }
                entries[name] = out.toByteArray()
                zip.closeEntry()
            }
        }
        fun text(name: String) =
            entries[name]?.toString(Charsets.UTF_8) ?: error("Backup is missing $name.")
        val manifest = json.decodeFromString<BackupManifest>(text("manifest.json"))
        require(manifest.format == "candlr" && manifest.version == 1) {
            "This backup format is not supported. Try a newer version of Candlr."
        }
        val birthdays = json.decodeFromString<List<Birthday>>(text("birthdays.json"))
        require(birthdays.size <= 10000 && birthdays.map { it.id }.toSet().size == birthdays.size) {
            "Backup has duplicate or too many birthdays."
        }
        birthdays.forEach { it.validate() }
        val preferences =
            json.decodeFromString<Preferences>(text("settings.json")).also { it.validate() }
        val photos =
            entries
                .filterKeys { it.startsWith("photos/") }
                .mapKeys { it.key.removePrefix("photos/") }
        photos.forEach { (name, bytes) ->
            require(PhotoStore.hash(bytes) + ".jpg" == name) { "A backup photo is damaged." }
        }
        require(birthdays.all { it.photo == null || photos.containsKey(it.photo) }) {
            "Backup is missing a photo."
        }
        require(photos.keys == birthdays.mapNotNull { it.photo }.toSet()) {
            "Backup contains unreferenced photos."
        }
        return BackupContents(birthdays, preferences, photos)
    }
}

class BackupService(
    private val db: CandlrDatabase,
    private val photos: PhotoStore,
    private val recovery: File,
) {
    suspend fun snapshot(): BackupContents =
        db.withTransaction {
            val birthdays = db.birthdays().all()
            val images =
                birthdays
                    .mapNotNull { it.photo }
                    .distinct()
                    .associateWith { photos.file(it).readBytes() }
            require(images.values.sumOf { it.size.toLong() } < 56L * 1024 * 1024) {
                "Photo collection is too large for a backup."
            }
            BackupContents(birthdays, db.birthdays().preferences() ?: Preferences(), images)
        }

    fun hasRecovery() = recovery.exists()

    fun readRecovery(): BackupContents = recovery.inputStream().use(BackupCodec::read)

    suspend fun restore(
        contents: BackupContents,
        replace: Boolean,
        overwrite: Boolean,
    ): RestoreResult {
        contents.birthdays.forEach { it.validate() }
        contents.preferences.validate()
        contents.photos.forEach { (_, bytes) ->
            require(
                bytes.size >= 4 &&
                    bytes[0] == 0xFF.toByte() &&
                    bytes[1] == 0xD8.toByte() &&
                    bytes[bytes.lastIndex - 1] == 0xFF.toByte() &&
                    bytes.last() == 0xD9.toByte()
            ) {
                "Backup contains an invalid JPEG photo."
            }
            val bounds =
                android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            require(bounds.outWidth in 1..1024 && bounds.outHeight in 1..1024) {
                "Backup contains an invalid photo."
            }
        }
        // Recovery archive is atomically replaced before committing anything to the database.
        val previous = snapshot()
        val temp = File(recovery.parentFile, "recovery.tmp")
        temp.outputStream().use { BackupCodec.write(it, previous) }
        java.nio.file.Files.move(
            temp.toPath(),
            recovery.toPath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
            java.nio.file.StandardCopyOption.ATOMIC_MOVE,
        )
        // Content-addressed immutable photos: orphaned staged files are harmless after an
        // interruption.
        contents.photos.forEach { (name, bytes) ->
            val destination = photos.file(name)
            if (!destination.exists()) {
                val staged = File(photos.directory, "$name.tmp")
                staged.writeBytes(bytes)
                java.nio.file.Files.move(
                    staged.toPath(),
                    destination.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                )
            }
        }
        return db.withTransaction {
            val dao = db.birthdays()
            if (replace) {
                dao.clear()
                dao.putPreferences(contents.preferences)
            }
            val existing = dao.all().associateBy { it.id }
            val added = contents.birthdays.filter { it.id !in existing }
            val updated =
                contents.birthdays.filter {
                    overwrite && it.id in existing && existing[it.id] != it
                }
            dao.putAll(added + updated)
            RestoreResult(
                added.size,
                updated.size,
                contents.birthdays.size - added.size - updated.size,
            )
        }
    }
}
