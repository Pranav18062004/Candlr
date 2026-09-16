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
        scope.launch { operations.withLock { runCatching { reminders.schedule() } } }
    }
}
