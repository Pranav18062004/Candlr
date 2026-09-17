package app.candlr

import android.app.Application
import app.candlr.data.*
import app.candlr.reminders.ReminderScheduler
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CandlrApplication : Application() {
    val database by lazy { CandlrDatabase.create(this) }
    val photos by lazy { PhotoStore(this) }
    val backup by lazy {
        BackupService(database, photos, File(noBackupFilesDir, "recovery.candlr"))
    }
    val reminders by lazy { ReminderScheduler(this, database) }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val operations = Mutex()

    override fun onCreate() {
        super.onCreate()
        reminders.ensureChannel()
        scope.launch {
            operations.withLock {
                LaunchTheme.save(
                    this@CandlrApplication,
                    (database.birthdays().preferences() ?: Preferences()).theme,
                )
                photos.prune(database.birthdays().all().mapNotNull { it.photo }.toSet())
                runCatching { reminders.schedule() }
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        photos.clearCache()
    }
}
