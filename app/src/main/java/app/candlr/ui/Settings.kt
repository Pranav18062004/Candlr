package app.candlr.ui

import android.app.TimePickerDialog
import android.content.Intent
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.candlr.BuildConfig
import app.candlr.R
import app.candlr.data.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    prefs: Preferences,
    busy: Boolean,
    recovery: Boolean,
    update: ((Preferences) -> Preferences) -> Unit,
    enableReminders: (Boolean) -> Unit,
    export: () -> Unit,
    restore: () -> Unit,
    recover: () -> Unit,
) {
    val context = LocalContext.current
    var information by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var allowed by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        allowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    LaunchedEffect(prefs.reminders) {
        allowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            stringResource(R.string.settings),
            Modifier.padding(top = 24.dp, bottom = 32.dp),
            style = MaterialTheme.typography.headlineLarge,
        )
        SectionTitle(stringResource(R.string.appearance))
        Text(
            stringResource(R.string.theme),
            Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to R.string.system, "light" to R.string.light, "dark" to R.string.dark)
                .forEach { (value, label) ->
                    FilterChip(
                        selected = prefs.theme == value,
                        onClick = { update { it.copy(theme = value) } },
                        enabled = !busy,
                        label = { Text(stringResource(label)) },
                    )
                }
        }
        ToggleRow(
            stringResource(R.string.reduce_motion),
            stringResource(R.string.reduce_motion_body),
            prefs.reduceMotion,
            !busy,
        ) { checked ->
            update { it.copy(reduceMotion = checked) }
        }
        DividerSpace()
        SectionTitle(stringResource(R.string.reminders))
        ToggleRow(
            stringResource(R.string.reminders),
            stringResource(R.string.reminders_body),
            prefs.reminders,
            !busy,
            enableReminders,
        )
        if (!allowed) {
            Text(
                stringResource(R.string.notification_blocked),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }
            ) {
                Text(stringResource(R.string.open_settings))
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.reminder_time))
            TextButton(
                enabled = !busy,
                onClick = {
                    TimePickerDialog(
                            context,
                            { _, hour, minute -> update { it.copy(hour = hour, minute = minute) } },
                            prefs.hour,
                            prefs.minute,
                            DateFormat.is24HourFormat(context),
                        )
                        .show()
                },
            ) {
                Text(
                    LocalTime.of(prefs.hour, prefs.minute)
                        .format(
                            DateTimeFormatter.ofPattern(
                                if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
                            )
                        )
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            reminderOffsets.forEachIndexed { index, offset ->
                val bit = 1 shl index
                FilterChip(
                    selected = prefs.reminderMask and bit != 0,
                    onClick = {
                        update {
                            val next = it.reminderMask xor bit
                            if (next == 0) it else it.copy(reminderMask = next)
                        }
                    },
                    enabled = !busy,
                    label = { Text(offsetLabel(offset)) },
                )
            }
        }
        Text(
            stringResource(R.string.leap_day),
            Modifier.padding(top = 24.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(R.string.leap_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !prefs.leapMarch,
                onClick = { update { it.copy(leapMarch = false) } },
                enabled = !busy,
                label = { Text(stringResource(R.string.feb_28)) },
            )
            FilterChip(
                selected = prefs.leapMarch,
                onClick = { update { it.copy(leapMarch = true) } },
                enabled = !busy,
                label = { Text(stringResource(R.string.mar_1)) },
            )
        }
        DividerSpace()
        SectionTitle(stringResource(R.string.backup_heading))
        Text(
            stringResource(R.string.backup_body),
            Modifier.padding(top = 12.dp, bottom = 20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = export,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
        ) {
            BookIcon(Glyph.Upload, Modifier.size(20.dp), MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.create_backup))
        }
        OutlinedButton(
            onClick = restore,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            BookIcon(Glyph.Download, Modifier.size(20.dp), MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.restore_backup))
        }
        Text(
            stringResource(R.string.backup_destination),
            Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.backup_unencrypted),
            Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (recovery)
            TextButton(
                onClick = recover,
                enabled = !busy,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.recovery))
            }
        DividerSpace()
        BookIcon(Glyph.Candle, Modifier.size(32.dp), MaterialTheme.colorScheme.primary)
        Text(
            stringResource(R.string.private_title),
            Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            stringResource(R.string.private_body),
            Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = { information = R.string.privacy_title to R.string.privacy_body }) {
            Text(stringResource(R.string.privacy_title))
        }
        TextButton(
            onClick = { information = R.string.reminder_help_title to R.string.reminder_help_body }
        ) {
            Text(stringResource(R.string.reminder_help_title))
        }
        TextButton(onClick = { information = R.string.licenses_title to R.string.licenses_body }) {
            Text(stringResource(R.string.licenses_title))
        }
        androidx.compose.foundation.text.selection.SelectionContainer {
            Text(
                stringResource(R.string.developer_contact),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            stringResource(R.string.version, BuildConfig.VERSION_NAME),
            Modifier.padding(top = 24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
    information?.let { (title, body) -> InformationDialog(title, body) { information = null } }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun DividerSpace() {
    HorizontalDivider(
        Modifier.padding(vertical = 28.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun ToggleRow(
    title: String,
    body: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}

@Composable
fun RestoreDialog(
    contents: BackupContents,
    current: List<Birthday>,
    busy: Boolean,
    dismiss: () -> Unit,
    restore: (Boolean, Boolean) -> Unit,
) {
    var overwrite by remember { mutableStateOf(false) }
    var confirmReplace by remember { mutableStateOf(false) }
    val conflicts =
        remember(contents, current) {
            val existing = current.associateBy { it.id }
            contents.birthdays.count { existing[it.id] != null && existing[it.id] != it }
        }
    AlertDialog(
        onDismissRequest = dismiss,
        title = {
            Text(
                stringResource(R.string.restore_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(
                        R.string.restore_summary,
                        pluralStringResource(
                            R.plurals.birthday_count,
                            contents.birthdays.size,
                            contents.birthdays.size,
                        ),
                        pluralStringResource(
                            R.plurals.photo_count,
                            contents.photos.size,
                            contents.photos.size,
                        ),
                    )
                )
                Text(
                    stringResource(R.string.restore_merge_body),
                    Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (conflicts > 0) {
                    Text(
                        pluralStringResource(
                            R.plurals.restore_conflicts,
                            conflicts.toInt(),
                            conflicts,
                        ),
                        Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !overwrite,
                            onClick = { overwrite = false },
                            enabled = !busy,
                        )
                        Text(stringResource(R.string.keep_current))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = overwrite,
                            onClick = { overwrite = true },
                            enabled = !busy,
                        )
                        Text(stringResource(R.string.use_backup))
                    }
                }
                TextButton(
                    onClick = { confirmReplace = true },
                    enabled = !busy,
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text(stringResource(R.string.replace), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { restore(false, overwrite) }, enabled = !busy) {
                Text(stringResource(R.string.merge))
            }
        },
        dismissButton = {
            TextButton(onClick = dismiss, enabled = !busy) { Text(stringResource(R.string.cancel)) }
        },
    )
    if (confirmReplace)
        AlertDialog(
            onDismissRequest = { if (!busy) confirmReplace = false },
            title = { Text(stringResource(R.string.replace_title)) },
            text = { Text(stringResource(R.string.replace_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReplace = false
                        restore(true, true)
                    },
                    enabled = !busy,
                ) {
                    Text(stringResource(R.string.replace), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReplace = false }, enabled = !busy) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
}
