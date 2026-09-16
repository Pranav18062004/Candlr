package app.candlr

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.candlr.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.withLock

data class BookState(
    val people: List<Birthday> = emptyList(),
    val preferences: Preferences = Preferences(),
    val loaded: Boolean = false,
)

class CandlrViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CandlrApplication
    private val dao = app.database.birthdays()
    val state =
        combine(dao.observe(), dao.observePreferences()) { people, preferences ->
                BookState(people, preferences ?: Preferences(), true)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookState())
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val restorePreview = MutableStateFlow<BackupContents?>(null)
    val hasRecovery = MutableStateFlow(app.backup.hasRecovery())

    private fun operation(block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { app.operations.withLock { block() } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error.value =
                    e.message ?: "Could not finish. Your birthday book is still on this phone."
            } finally {
                busy.value = false
                hasRecovery.value = app.backup.hasRecovery()
            }
        }
    }

    fun save(person: Birthday, onSuccess: () -> Unit) = operation {
        person.validate()
        dao.put(person)
        app.reminders.schedule()
        withContext(Dispatchers.Main) { onSuccess() }
    }

    fun delete(person: Birthday, onSuccess: () -> Unit) = operation {
        dao.delete(person.id)
        app.reminders.schedule()
        withContext(Dispatchers.Main) { onSuccess() }
    }

    fun preferences(update: (Preferences) -> Preferences) = operation {
        val next = update(dao.preferences() ?: Preferences()).also { it.validate() }
        dao.putPreferences(next)
        app.reminders.schedule()
    }

    fun photo(uri: Uri, onSuccess: (String) -> Unit) = operation {
        val name = app.photos.import(uri)
        withContext(Dispatchers.Main) { onSuccess(name) }
    }

    fun export(uri: Uri, onSuccess: () -> Unit) = operation {
        val snapshot = app.backup.snapshot()
        val output =
            app.contentResolver.openOutputStream(uri, "wt")
                ?: error("Could not open the backup destination.")
        output.use { BackupCodec.write(it, snapshot) }
        withContext(Dispatchers.Main) { onSuccess() }
    }

    fun preview(uri: Uri) = operation {
        val input = app.contentResolver.openInputStream(uri) ?: error("Could not open the backup.")
        restorePreview.value = input.use(BackupCodec::read)
    }

    fun restore(replace: Boolean, overwrite: Boolean, onSuccess: (RestoreResult) -> Unit) =
        operation {
            val contents = restorePreview.value ?: error("Choose a backup first.")
            val result = app.backup.restore(contents, replace, overwrite)
            restorePreview.value = null
            app.reminders.schedule()
            withContext(Dispatchers.Main) { onSuccess(result) }
        }

    fun previewRecovery() = operation { restorePreview.value = app.backup.readRecovery() }

    fun reschedule() = operation { app.reminders.schedule() }
}
