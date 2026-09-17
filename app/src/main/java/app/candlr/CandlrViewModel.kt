package app.candlr

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.candlr.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
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
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                BookState(preferences = Preferences(theme = LaunchTheme.read(app))),
            )
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<BookError?>(null)
    val restorePreview = MutableStateFlow<BackupContents?>(null)
    val hasRecovery = MutableStateFlow(app.backup.hasRecovery())

    private data class Work(
        val visible: Boolean,
        val reportError: Boolean,
        val block: suspend () -> Unit,
    )

    private val queue = Channel<Work>(Channel.UNLIMITED)
    private var pendingVisible = 0

    init {
        viewModelScope.launch {
            for (work in queue) {
                try {
                    withContext(Dispatchers.IO) {
                        app.operations.withLock {
                            try {
                                work.block()
                            } finally {
                                app.photos.prune(dao.all().mapNotNull { it.photo }.toSet())
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (work.reportError) error.value = e.bookError()
                } finally {
                    if (work.visible) pendingVisible--
                    busy.value = pendingVisible > 0
                    hasRecovery.value = app.backup.hasRecovery()
                }
            }
        }
    }

    private fun operation(
        visible: Boolean = true,
        reportError: Boolean = true,
        block: suspend () -> Unit,
    ) {
        if (visible) {
            pendingVisible++
            busy.value = true
        }
        check(queue.trySend(Work(visible, reportError, block)).isSuccess)
    }

    fun toggleFavorite(id: String) = operation(visible = false) { dao.toggleFavorite(id) }

    fun pinDraft(photo: String?) = operation(visible = false) { app.photos.pinDraft(photo) }

    fun releaseDraft() = pinDraft(null)

    fun releaseUndo(id: String) = operation(visible = false) { app.photos.releaseUndo(id) }

    fun save(person: Birthday, onSuccess: () -> Unit) = operation {
        person.validate()
        dao.put(person)
        app.reminders.schedule()
        withContext(Dispatchers.Main) { onSuccess() }
    }

    fun delete(person: Birthday, onSuccess: () -> Unit) = operation {
        app.photos.pinUndo(person.id, person.photo)
        dao.delete(person.id)
        app.reminders.schedule()
        withContext(Dispatchers.Main) { onSuccess() }
    }

    fun preferences(update: (Preferences) -> Preferences) =
        operation(visible = false) {
            val next = update(dao.preferences() ?: Preferences()).also { it.validate() }
            dao.putPreferences(next)
            LaunchTheme.save(app, next.theme)
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
                ?: (throw BookException(BookError.OPEN_DESTINATION))
        output.use { BackupCodec.write(it, snapshot) }
        withContext(Dispatchers.Main) { onSuccess() }
    }

    fun preview(uri: Uri) = operation {
        val input =
            app.contentResolver.openInputStream(uri) ?: (throw BookException(BookError.OPEN_BACKUP))
        restorePreview.value = input.use(BackupCodec::read)
    }

    fun restore(replace: Boolean, overwrite: Boolean, onSuccess: (RestoreResult) -> Unit) =
        operation {
            val contents = restorePreview.value ?: (throw BookException(BookError.CHOOSE_BACKUP))
            val result = app.backup.restore(contents, replace, overwrite)
            LaunchTheme.save(app, (dao.preferences() ?: Preferences()).theme)
            restorePreview.value = null
            app.reminders.schedule()
            withContext(Dispatchers.Main) { onSuccess(result) }
        }

    fun previewRecovery() = operation { restorePreview.value = app.backup.readRecovery() }

    fun reschedule() = operation(visible = false, reportError = false) { app.reminders.schedule() }
}
