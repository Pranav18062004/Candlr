package app.candlr.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.candlr.R
import app.candlr.data.*
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BirthdayEditor(
    person: Birthday?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onPhoto: (Uri, (String) -> Unit) -> Unit,
    onDraftPhoto: (String?) -> Unit,
    onSave: (Birthday) -> Unit,
) {
    val key = person?.id ?: "new"
    var id by rememberSaveable(key) { mutableStateOf(person?.id ?: UUID.randomUUID().toString()) }
    var name by rememberSaveable(key) { mutableStateOf(person?.name ?: "") }
    var month by
        rememberSaveable(key) { mutableIntStateOf(person?.month ?: LocalDate.now().monthValue) }
    var day by rememberSaveable(key) { mutableStateOf(person?.day?.toString() ?: "") }
    var year by rememberSaveable(key) { mutableStateOf(person?.year?.toString() ?: "") }
    var notes by rememberSaveable(key) { mutableStateOf(person?.notes ?: "") }
    var favorite by rememberSaveable(key) { mutableStateOf(person?.favorite ?: false) }
    var photo by rememberSaveable(key) { mutableStateOf(person?.photo) }
    var reminderMask by rememberSaveable(key) { mutableIntStateOf(person?.reminderMask ?: -1) }
    var monthMenu by remember { mutableStateOf(false) }
    var validation by rememberSaveable(key) { mutableStateOf<BookError?>(null) }
    val initialMonth = rememberSaveable(key) { person?.month ?: LocalDate.now().monthValue }
    val dirty =
        name != (person?.name ?: "") ||
            month != initialMonth ||
            day != (person?.day?.toString() ?: "") ||
            year != (person?.year?.toString() ?: "") ||
            notes != (person?.notes ?: "") ||
            favorite != (person?.favorite ?: false) ||
            photo != person?.photo ||
            reminderMask != (person?.reminderMask ?: -1)
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    // SheetState retains its confirmation callback. Read current state inside that callback.
    val currentDirty by rememberUpdatedState(dirty)
    val currentBusy by rememberUpdatedState(busy)
    val currentDismiss by rememberUpdatedState(onDismiss)
    fun requestDismiss() {
        if (!currentBusy) {
            if (currentDirty) confirmDiscard = true else currentDismiss()
        }
    }
    LaunchedEffect(photo) { onDraftPhoto(photo) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) onPhoto(uri) { photo = it }
        }
    ModalBottomSheet(
        onDismissRequest = ::requestDismiss,
        sheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { target ->
                    if (target == SheetValue.Hidden && (currentBusy || currentDirty)) {
                        if (!currentBusy) confirmDiscard = true
                        false
                    } else true
                },
            ),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                stringResource(
                    if (person == null) R.string.add_birthday else R.string.edit_birthday
                ),
                style = MaterialTheme.typography.headlineMedium,
            )
            Row(
                Modifier.fillMaxWidth().padding(vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(
                    Birthday(
                        id = id,
                        name = name.ifBlank { "?" },
                        month = month,
                        day = 1,
                        photo = photo,
                    ),
                    64,
                )
                Column(Modifier.padding(start = 12.dp)) {
                    TextButton(enabled = !busy, onClick = { picker.launch("image/*") }) {
                        Text(stringResource(R.string.photo))
                    }
                    if (photo != null)
                        TextButton(enabled = !busy, onClick = { photo = null }) {
                            Text(stringResource(R.string.remove_photo))
                        }
                }
            }
            OutlinedTextField(
                name,
                { name = it.take(120) },
                Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                enabled = !busy,
                shape = RoundedCornerShape(12.dp),
            )
            Text(
                stringResource(R.string.birthday),
                Modifier.padding(top = 24.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = monthMenu,
                    onExpandedChange = { if (!busy) monthMenu = it },
                    modifier = Modifier.weight(1.8f),
                ) {
                    OutlinedTextField(
                        value = Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault()),
                        onValueChange = {},
                        readOnly = true,
                        enabled = !busy,
                        label = { Text(stringResource(R.string.month)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(monthMenu) },
                        modifier =
                            Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                    ExposedDropdownMenu(
                        expanded = monthMenu,
                        onDismissRequest = { monthMenu = false },
                    ) {
                        Month.entries.forEach { m ->
                            DropdownMenuItem(
                                text = {
                                    Text(m.getDisplayName(TextStyle.FULL, Locale.getDefault()))
                                },
                                onClick = {
                                    month = m.value
                                    monthMenu = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    day,
                    { day = it.filter(Char::isDigit).take(2) },
                    Modifier.weight(1f),
                    label = { Text(stringResource(R.string.day)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !busy,
                    shape = RoundedCornerShape(12.dp),
                )
            }
            OutlinedTextField(
                year,
                { year = it.filter(Char::isDigit).take(4) },
                Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text(stringResource(R.string.year_optional)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !busy,
                shape = RoundedCornerShape(12.dp),
            )
            OutlinedTextField(
                notes,
                { notes = it.take(4000) },
                Modifier.fillMaxWidth().padding(top = 20.dp),
                label = { Text(stringResource(R.string.notes)) },
                placeholder = { Text(stringResource(R.string.notes_hint)) },
                minLines = 3,
                maxLines = 6,
                enabled = !busy,
                shape = RoundedCornerShape(12.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.bookmark))
                val bookmarkLabel = stringResource(R.string.bookmark)
                Switch(
                    checked = favorite,
                    onCheckedChange = { favorite = it },
                    enabled = !busy,
                    modifier = Modifier.semantics { contentDescription = bookmarkLabel },
                )
            }
            Text(stringResource(R.string.reminders), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = reminderMask == -1,
                    onClick = { reminderMask = -1 },
                    enabled = !busy,
                    label = { Text(stringResource(R.string.default_reminders)) },
                )
                FilterChip(
                    selected = reminderMask == 0,
                    onClick = { reminderMask = 0 },
                    enabled = !busy,
                    label = { Text(stringResource(R.string.off)) },
                )
                reminderOffsets.forEachIndexed { index, offset ->
                    val bit = 1 shl index
                    FilterChip(
                        selected = reminderMask > 0 && reminderMask and bit != 0,
                        onClick = {
                            reminderMask = if (reminderMask < 0) bit else reminderMask xor bit
                        },
                        enabled = !busy,
                        label = { Text(offsetLabel(offset)) },
                    )
                }
            }
            if (validation != null)
                Text(
                    stringResource(validation!!.resource),
                    Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            Button(
                enabled = !busy,
                onClick = {
                    val birthday =
                        Birthday(
                            id,
                            name.trim(),
                            month,
                            day.toIntOrNull() ?: 0,
                            if (year.isBlank()) null else year.toIntOrNull() ?: 0,
                            notes.trim(),
                            favorite,
                            photo,
                            reminderMask,
                        )
                    try {
                        birthday.validate()
                        validation = null
                        onSave(birthday)
                    } catch (e: IllegalArgumentException) {
                        validation = e.bookError()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                contentPadding = PaddingValues(16.dp),
            ) {
                Text(stringResource(R.string.save))
            }
            TextButton(
                onClick = ::requestDismiss,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
    if (confirmDiscard)
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDiscard = false
                        onDismiss()
                    },
                    enabled = !busy,
                ) {
                    Text(stringResource(R.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(stringResource(R.string.keep_editing))
                }
            },
        )
}
