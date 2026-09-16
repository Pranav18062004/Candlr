package app.candlr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.candlr.BookState
import app.candlr.R
import app.candlr.data.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun CalendarScreen(book: BookState, today: LocalDate, onOpen: (Birthday) -> Unit) {
    var monthValue by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var selectedValue by rememberSaveable { mutableStateOf(today.toString()) }
    val month = YearMonth.parse(monthValue)
    val selected = LocalDate.parse(selectedValue)
    val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val leading = (month.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
    val grouped =
        remember(book.people, month, book.preferences.leapMarch) {
            book.people.groupBy { it.occurrence(month.year, book.preferences.leapMarch) }
        }
    val people = grouped[selected].orEmpty()
    fun move(delta: Long) {
        val next = month.plusMonths(delta)
        monthValue = next.toString()
        selectedValue = next.atDay(1).toString()
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 104.dp)) {
        item {
            Column(Modifier.padding(horizontal = 24.dp)) {
                Text(
                    stringResource(R.string.calendar),
                    Modifier.padding(top = 24.dp),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    IconButton(onClick = { move(-1) }) {
                        BookIcon(
                            Glyph.Back,
                            Modifier.size(20.dp),
                            description = stringResource(R.string.previous_month),
                        )
                    }
                    IconButton(onClick = { move(1) }) {
                        BookIcon(
                            Glyph.Next,
                            Modifier.size(20.dp),
                            description = stringResource(R.string.next_month),
                        )
                    }
                }
            }
            Column(Modifier.padding(horizontal = 12.dp)) {
                Row {
                    repeat(7) { index ->
                        Text(
                            firstDay
                                .plus(index.toLong())
                                .getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            Modifier.weight(1f).padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                repeat((leading + month.lengthOfMonth() + 6) / 7) { week ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { column ->
                            val number = week * 7 + column - leading + 1
                            if (number !in 1..month.lengthOfMonth())
                                Spacer(Modifier.weight(1f).height(52.dp))
                            else {
                                val date = month.atDay(number)
                                val count = grouped[date].orEmpty().size
                                val isSelected = date == selected
                                val label =
                                    date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")) +
                                        ", " +
                                        stringResource(R.string.birthday_count, count)
                                Box(
                                    Modifier.weight(1f)
                                        .height(52.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.background
                                        )
                                        .clickable { selectedValue = date.toString() }
                                        .semantics {
                                            contentDescription = label
                                            this.selected = isSelected
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            number.toString(),
                                            fontWeight =
                                                if (date == today || isSelected) FontWeight.Bold
                                                else FontWeight.Normal,
                                            color =
                                                if (isSelected)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(Modifier.height(3.dp))
                                        Box(
                                            Modifier.size(4.dp)
                                                .background(
                                                    if (count > 0) MaterialTheme.colorScheme.primary
                                                    else
                                                        androidx.compose.ui.graphics.Color
                                                            .Transparent,
                                                    CircleShape,
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Column(Modifier.padding(horizontal = 24.dp)) {
                TextButton(
                    onClick = {
                        monthValue = YearMonth.from(today).toString()
                        selectedValue = today.toString()
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.this_month))
                }
                HorizontalDivider(
                    Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Text(
                    selected.format(DateTimeFormatter.ofPattern("d MMMM")),
                    style = MaterialTheme.typography.titleLarge,
                )
                if (people.isEmpty())
                    Text(
                        stringResource(R.string.no_day_birthdays),
                        Modifier.padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
            }
        }
        items(people, key = { it.id }) { person ->
            Box(Modifier.padding(horizontal = 24.dp)) {
                BirthdayRow(person, today, book.preferences) { onOpen(person) }
            }
        }
    }
}
