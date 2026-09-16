package app.candlr.ui

import android.Manifest
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.candlr.*
import app.candlr.R
import app.candlr.data.*
import java.time.*
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandlrApp(
    vm: CandlrViewModel,
    book: BookState,
    requestedPerson: String?,
    consumedRequest: () -> Unit,
) {
    val context = LocalContext.current
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val preview by vm.restorePreview.collectAsStateWithLifecycle()
    val recovery by vm.hasRecovery.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var editor by rememberSaveable { mutableStateOf<String?>(null) }
    var today by remember { mutableStateOf(LocalDate.now()) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun message(text: String) {
        scope.launch { snack.showSnackbar(text) }
    }
    LaunchedEffect(requestedPerson, book.loaded) {
        if (requestedPerson != null && book.loaded) {
            selected = requestedPerson
            consumedRequest()
        }
    }
    // Only tick while this composition is active; no background polling or seconds countdown.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        today = LocalDate.now()
        vm.reschedule()
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                today = LocalDate.now()
                delay(60_000)
            }
        }
    }
    val export =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream")
        ) { uri ->
            if (uri != null) vm.export(uri) { message(context.getString(R.string.backup_saved)) }
        }
    val restore =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) vm.preview(uri)
        }
    val notifications =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            vm.preferences { it.copy(reminders = granted) }
        }
    val duration = if (book.preferences.reduceMotion) 0 else 200
    BackHandler(selected != null && editor == null) { selected = null }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (selected == null)
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f)
                    )
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.background,
                        tonalElevation = 0.dp,
                    ) {
                        listOf(
                                R.string.upcoming to Glyph.Book,
                                R.string.calendar to Glyph.Calendar,
                                R.string.settings to Glyph.Settings,
                            )
                            .forEachIndexed { index, (label, icon) ->
                                NavigationBarItem(
                                    selected = tab == index,
                                    onClick = { tab = index },
                                    icon = {
                                        BookIcon(
                                            icon,
                                            Modifier.size(22.dp),
                                            color =
                                                if (tab == index) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                    label = { Text(stringResource(label)) },
                                )
                            }
                    }
                }
        },
        floatingActionButton = {
            if (selected == null && tab != 2 && book.people.isNotEmpty())
                ExtendedFloatingActionButton(
                    modifier =
                        Modifier.semantics {
                            contentDescription = context.getString(R.string.add_birthday)
                        },
                    onClick = { editor = "new" },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = {
                        BookIcon(
                            Glyph.Plus,
                            Modifier.size(20.dp),
                            MaterialTheme.colorScheme.onPrimary,
                        )
                    },
                    text = { Text(stringResource(R.string.add_birthday)) },
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                )
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).widthIn(max = 700.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (!book.loaded) LinearProgressIndicator(Modifier.fillMaxWidth())
            else
                Crossfade(
                    targetState = selected ?: "tab$tab",
                    animationSpec = tween(duration),
                    label = "page",
                ) { page ->
                    val person = book.people.firstOrNull { it.id == page }
                    if (person != null)
                        PersonScreen(
                            person,
                            today,
                            book.preferences,
                            onBack = { selected = null },
                            onEdit = { editor = person.id },
                            onFavorite = { vm.save(person.copy(favorite = !person.favorite)) {} },
                            onDelete = {
                                vm.delete(person) {
                                    selected = null
                                    scope.launch {
                                        val result =
                                            snack.showSnackbar(
                                                context.getString(R.string.deleted),
                                                context.getString(R.string.undo),
                                                duration = SnackbarDuration.Long,
                                            )
                                        if (result == SnackbarResult.ActionPerformed)
                                            vm.save(person) {}
                                    }
                                }
                            },
                        )
                    else
                        when (page) {
                            "tab1" -> CalendarScreen(book, today) { selected = it.id }
                            "tab2" ->
                                SettingsScreen(
                                    book.preferences,
                                    busy,
                                    recovery,
                                    update = vm::preferences,
                                    enableReminders = { enabled ->
                                        if (enabled && Build.VERSION.SDK_INT >= 33)
                                            notifications.launch(
                                                Manifest.permission.POST_NOTIFICATIONS
                                            )
                                        else vm.preferences { it.copy(reminders = enabled) }
                                    },
                                    export = { export.launch("Candlr-${LocalDate.now()}.candlr") },
                                    restore = {
                                        restore.launch(
                                            arrayOf(
                                                "application/zip",
                                                "application/octet-stream",
                                                "*/*",
                                            )
                                        )
                                    },
                                    recover = vm::previewRecovery,
                                )
                            else ->
                                UpcomingScreen(
                                    book,
                                    today,
                                    onAdd = { editor = "new" },
                                    onOpen = { selected = it.id },
                                )
                        }
                }
            if (busy)
                LinearProgressIndicator(
                    Modifier.fillMaxWidth().align(Alignment.TopCenter).semantics {
                        contentDescription = context.getString(R.string.working)
                    }
                )
        }
    }
    if (editor != null) {
        val person = book.people.firstOrNull { it.id == editor }
        BirthdayEditor(
            person,
            busy,
            onDismiss = { if (!busy) editor = null },
            onPhoto = vm::photo,
        ) { saved ->
            vm.save(saved) {
                editor = null
                message(context.getString(R.string.saved))
            }
        }
    }
    if (preview != null)
        RestoreDialog(
            preview!!,
            book.people,
            busy,
            dismiss = { if (!busy) vm.restorePreview.value = null },
        ) { replace, overwrite ->
            vm.restore(replace, overwrite) { result ->
                selected = null
                message(
                    context.getString(
                        R.string.restore_done,
                        result.added,
                        result.updated,
                        result.unchanged,
                    )
                )
            }
        }
    if (error != null)
        AlertDialog(
            onDismissRequest = { vm.error.value = null },
            title = { Text(stringResource(R.string.error_title)) },
            text = { Text(error!!) },
            confirmButton = {
                TextButton(onClick = { vm.error.value = null }) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
}

@Composable
private fun Heading(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            if (subtitle != null)
                Text(
                    subtitle,
                    Modifier.padding(top = 6.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
        }
        trailing?.invoke()
    }
}

@Composable
fun UpcomingScreen(
    book: BookState,
    today: LocalDate,
    onAdd: () -> Unit,
    onOpen: (Birthday) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var favorites by rememberSaveable { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    val sorted =
        remember(book.people, today, book.preferences.leapMarch, query, favorites) {
            book.people
                .filter {
                    (!favorites || it.favorite) && it.name.contains(query, ignoreCase = true)
                }
                .sortedWith(
                    compareBy<Birthday> { it.nextBirthday(today, book.preferences.leapMarch) }
                        .thenBy { it.name.lowercase() }
                )
        }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 104.dp),
    ) {
        item {
            Heading("Candlr", today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))) {
                IconButton(
                    onClick = {
                        searching = !searching
                        if (!searching) query = ""
                    }
                ) {
                    BookIcon(
                        if (searching) Glyph.Close else Glyph.Search,
                        Modifier.size(22.dp),
                        description =
                            stringResource(if (searching) R.string.close else R.string.search),
                    )
                }
            }
            if (searching)
                OutlinedTextField(
                    query,
                    { query = it },
                    Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    label = { Text(stringResource(R.string.search)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
        }
        if (book.people.isEmpty())
            item {
                Column(
                    Modifier.fillMaxWidth().padding(top = 36.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    BookIcon(Glyph.Candle, Modifier.size(64.dp), MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.empty_title),
                        Modifier.padding(top = 28.dp),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        stringResource(R.string.empty_body),
                        Modifier.padding(top = 16.dp, bottom = 28.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onAdd,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    ) {
                        Text(stringResource(R.string.add_birthday))
                    }
                    Text(
                        stringResource(R.string.private_title),
                        Modifier.padding(top = 40.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !favorites,
                        onClick = { favorites = false },
                        label = { Text(stringResource(R.string.everyone)) },
                    )
                    FilterChip(
                        selected = favorites,
                        onClick = { favorites = true },
                        label = { Text(stringResource(R.string.favorites)) },
                        leadingIcon = {
                            BookIcon(Glyph.Bookmark, Modifier.size(16.dp), filled = favorites)
                        },
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
            if (sorted.isNotEmpty() && query.isBlank() && !favorites) {
                val firstDate = sorted.first().nextBirthday(today, book.preferences.leapMarch)
                val nearest =
                    sorted.takeWhile {
                        it.nextBirthday(today, book.preferences.leapMarch) == firstDate
                    }
                items(nearest, key = { "hero-${it.id}" }) { person ->
                    Surface(
                        onClick = { onOpen(person) },
                        shape =
                            RoundedCornerShape(
                                topStart = 8.dp,
                                topEnd = 28.dp,
                                bottomEnd = 8.dp,
                                bottomStart = 8.dp,
                            ),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    stringResource(
                                        if (firstDate == today) R.string.today
                                        else R.string.next_birthday
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                BookIcon(
                                    Glyph.Candle,
                                    Modifier.size(27.dp),
                                    MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                person.name,
                                Modifier.padding(top = 22.dp, bottom = 10.dp),
                                style = MaterialTheme.typography.headlineLarge,
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        awayLabel(person, today, book.preferences),
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    person.turningAge(today, book.preferences.leapMarch)?.let {
                                        Text(
                                            stringResource(R.string.turning_age, it),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                                Avatar(person, 52)
                            }
                        }
                    }
                }
            }
            if (sorted.isEmpty())
                item {
                    Text(
                        stringResource(
                            if (favorites && query.isBlank()) R.string.no_bookmarks
                            else R.string.no_results
                        ),
                        Modifier.padding(top = 24.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        stringResource(
                            if (favorites && query.isBlank()) R.string.no_bookmarks_body
                            else R.string.no_results_body
                        ),
                        Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            sorted
                .groupBy { YearMonth.from(it.nextBirthday(today, book.preferences.leapMarch)) }
                .forEach { (month, people) ->
                    item(key = "month-$month") {
                        Text(
                            month.format(
                                DateTimeFormatter.ofPattern(
                                    if (month.year == today.year) "MMMM" else "MMMM yyyy"
                                )
                            ),
                            Modifier.padding(top = 22.dp, bottom = 12.dp),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    items(people, key = { it.id }) { person ->
                        BirthdayRow(person, today, book.preferences) { onOpen(person) }
                    }
                }
        }
    }
}

@Composable
fun awayLabel(person: Birthday, today: LocalDate, prefs: Preferences): String =
    when (val days = person.daysAway(today, prefs.leapMarch)) {
        0L -> stringResource(R.string.today)
        1L -> stringResource(R.string.tomorrow)
        else -> stringResource(R.string.days_away, days)
    }

@Composable
fun Avatar(person: Birthday, size: Int = 46) {
    val context = LocalContext.current
    val image by
        produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, person.photo) {
            value =
                withContext(Dispatchers.IO) {
                    runCatching {
                            person.photo
                                ?.let {
                                    (context.applicationContext as CandlrApplication)
                                        .photos
                                        .file(it)
                                }
                                ?.let {
                                    BitmapFactory.decodeFile(
                                        it.path,
                                        BitmapFactory.Options().apply { inSampleSize = 2 },
                                    )
                                }
                                ?.asImageBitmap()
                        }
                        .getOrNull()
                }
        }
    Box(
        Modifier.size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null)
            Image(
                image!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        else
            Text(
                person.name
                    .trim()
                    .split(Regex("\\s+"))
                    .take(2)
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .joinToString(""),
                fontFamily = BookSerif,
                fontSize = (size * .35).sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
    }
}

@Composable
fun BirthdayRow(person: Birthday, today: LocalDate, prefs: Preferences, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp).semantics(
            mergeDescendants = true
        ) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(person)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(person.name, style = MaterialTheme.typography.titleMedium)
            Text(
                awayLabel(person, today, prefs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (person.favorite)
            BookIcon(
                Glyph.Bookmark,
                Modifier.padding(end = 10.dp).size(14.dp),
                MaterialTheme.colorScheme.primary,
                description = stringResource(R.string.bookmarked),
                filled = true,
            )
        Text(
            person.nextBirthday(today, prefs.leapMarch).dayOfMonth.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
}

@Composable
fun PersonScreen(
    person: Birthday,
    today: LocalDate,
    prefs: Preferences,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirm by remember { mutableStateOf(false) }
    val bookmarkColor by
        animateColorAsState(
            if (person.favorite) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            tween(if (prefs.reduceMotion) 0 else 140),
            label = "bookmark",
        )
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                BookIcon(
                    Glyph.Back,
                    Modifier.size(24.dp),
                    description = stringResource(R.string.close),
                )
            }
            Row {
                IconButton(onClick = onFavorite) {
                    BookIcon(
                        Glyph.Bookmark,
                        Modifier.size(24.dp),
                        bookmarkColor,
                        stringResource(
                            if (person.favorite) R.string.bookmarked else R.string.bookmark
                        ),
                        person.favorite,
                    )
                }
                IconButton(onClick = onEdit) {
                    BookIcon(
                        Glyph.Edit,
                        Modifier.size(24.dp),
                        description = stringResource(R.string.edit_birthday),
                    )
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Avatar(person, 88)
        Text(
            person.name,
            Modifier.padding(top = 24.dp),
            style = MaterialTheme.typography.displayLarge,
        )
        Text(
            MonthDay.of(person.month, person.day).format(DateTimeFormatter.ofPattern("d MMMM")) +
                (person.year?.let { ", $it" } ?: ""),
            Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            Modifier.fillMaxWidth().padding(vertical = 32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    awayLabel(person, today, prefs),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                person.turningAge(today, prefs.leapMarch)?.let {
                    Text(stringResource(R.string.turning_age, it), Modifier.padding(top = 8.dp))
                }
            }
        }
        if (person.notes.isNotBlank()) {
            Text(stringResource(R.string.notes), style = MaterialTheme.typography.titleLarge)
            Text(
                person.notes,
                Modifier.padding(top = 12.dp, bottom = 28.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Text(stringResource(R.string.reminders), style = MaterialTheme.typography.titleLarge)
        Text(
            when (person.reminderMask) {
                -1 -> stringResource(R.string.default_reminders)
                0 -> stringResource(R.string.off)
                else ->
                    reminderOffsets
                        .mapIndexedNotNull { index, day ->
                            if (person.reminderMask and (1 shl index) != 0) offsetLabel(day)
                            else null
                        }
                        .joinToString(", ")
            },
            Modifier.padding(top = 12.dp, bottom = 32.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.edit_birthday))
        }
        TextButton(
            onClick = { confirm = true },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text(stringResource(R.string.delete))
        }
    }
    if (confirm)
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.delete_title, person.name)) },
            text = { Text(stringResource(R.string.delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirm = false
                        onDelete()
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
}

@Composable
fun offsetLabel(day: Int): String =
    when (day) {
        0 -> stringResource(R.string.on_day)
        1 -> stringResource(R.string.day_before)
        else -> stringResource(R.string.advance_days, day)
    }
