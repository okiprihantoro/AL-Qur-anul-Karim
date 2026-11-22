package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserSettings
import com.example.hijri.HijriCalendarHelper
import com.example.ui.components.AppFooter
import com.example.ui.theme.LocalIsDarkTheme
import java.util.Calendar

@Composable
fun HijriCalendarScreen(
    settings: UserSettings,
    onAdjustHijri: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val todayCal = remember { Calendar.getInstance() }
    var displayedYear by remember { mutableIntStateOf(todayCal.get(Calendar.YEAR)) }
    var displayedMonth by remember { mutableIntStateOf(todayCal.get(Calendar.MONTH)) }
    var selectedDayOfMonth by remember { mutableIntStateOf(todayCal.get(Calendar.DAY_OF_MONTH)) }

    val todayHijri = remember(settings.hijriAdjustmentDays) {
        HijriCalendarHelper.fromGregorian(todayCal, settings.hijriAdjustmentDays)
    }

    val displayedMonthCal = remember(displayedYear, displayedMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val daysInMonth = displayedMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOffset = displayedMonthCal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY

    val safeSelectedDay = selectedDayOfMonth.coerceIn(1, daysInMonth)
    val selectedCal = remember(displayedYear, displayedMonth, safeSelectedDay) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, safeSelectedDay)
        }
    }
    val selectedHijri = remember(selectedCal, settings.hijriAdjustmentDays) {
        HijriCalendarHelper.fromGregorian(selectedCal, settings.hijriAdjustmentDays)
    }
    val selectedEvents = remember(selectedCal, selectedHijri) {
        HijriCalendarHelper.getEventsForDate(selectedCal, selectedHijri)
    }

    val startHijriOfMonth = remember(displayedYear, displayedMonth, settings.hijriAdjustmentDays) {
        HijriCalendarHelper.fromGregorian(displayedMonthCal, settings.hijriAdjustmentDays)
    }
    val endMonthCal = remember(displayedYear, displayedMonth, daysInMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, daysInMonth)
        }
    }
    val endHijriOfMonth = remember(endMonthCal, settings.hijriAdjustmentDays) {
        HijriCalendarHelper.fromGregorian(endMonthCal, settings.hijriAdjustmentDays)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Today's Hijri Hero Card
        item {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = if (isDark) {
                                    listOf(Color(0xFF10372C), Color(0xFF07201A))
                                } else {
                                    listOf(Color(0xFF0E4D3C), Color(0xFF1D6E56))
                                }
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Kalender Hijriah Hari Ini",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFE7C167)
                            )
                            Text(
                                text = todayHijri.formatted,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = HijriCalendarHelper.formatGregorianIndonesian(todayCal),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFFE7C167),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Kemenag Rukyatul Hilal offset adjustment
                    Surface(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Koreksi Rukyatul Hilal (${if (settings.hijriAdjustmentDays > 0) "+${settings.hijriAdjustmentDays}" else settings.hijriAdjustmentDays} hari)",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onAdjustHijri(settings.hijriAdjustmentDays - 1) },
                                    modifier = Modifier.testTag("hijri_adjust_minus")
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Kurangi 1 hari", tint = Color.White)
                                }
                                IconButton(
                                    onClick = { onAdjustHijri(settings.hijriAdjustmentDays + 1) },
                                    modifier = Modifier.testTag("hijri_adjust_plus")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Tambah 1 hari", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Interactive Monthly Grid Calendar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (displayedMonth == 0) {
                                    displayedMonth = 11
                                    displayedYear -= 1
                                } else {
                                    displayedMonth -= 1
                                }
                            },
                            modifier = Modifier.testTag("hijri_prev_month")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Bulan sebelumnya")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${HijriCalendarHelper.indonesianMonthNames[displayedMonth]} $displayedYear",
                                style = MaterialTheme.typography.titleMedium
                            )
                            val hijriSpan = if (startHijriOfMonth.month == endHijriOfMonth.month) {
                                "${startHijriOfMonth.monthName} ${startHijriOfMonth.year} H"
                            } else {
                                "${startHijriOfMonth.monthName} – ${endHijriOfMonth.monthName} ${endHijriOfMonth.year} H"
                            }
                            Text(
                                text = hijriSpan,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = {
                                if (displayedMonth == 11) {
                                    displayedMonth = 0
                                    displayedYear += 1
                                } else {
                                    displayedMonth += 1
                                }
                            },
                            modifier = Modifier.testTag("hijri_next_month")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Bulan berikutnya")
                        }
                    }

                    // Day-of-week header
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val shortDays = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
                        shortDays.forEachIndexed { index, d ->
                            Text(
                                text = d,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = when (index) {
                                    5 -> MaterialTheme.colorScheme.primary
                                    0 -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Calendar cells (6 rows x 7 columns max)
                    val totalCells = ((firstDayOffset + daysInMonth + 6) / 7) * 7
                    for (rowStart in 0 until totalCells step 7) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (col in 0 until 7) {
                                val cellIndex = rowStart + col
                                val dayNum = cellIndex - firstDayOffset + 1
                                if (dayNum in 1..daysInMonth) {
                                    val cellCal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, displayedYear)
                                        set(Calendar.MONTH, displayedMonth)
                                        set(Calendar.DAY_OF_MONTH, dayNum)
                                    }
                                    val cellHijri = HijriCalendarHelper.fromGregorian(cellCal, settings.hijriAdjustmentDays)
                                    val events = HijriCalendarHelper.getEventsForDate(cellCal, cellHijri)
                                    val isToday = displayedYear == todayCal.get(Calendar.YEAR) &&
                                        displayedMonth == todayCal.get(Calendar.MONTH) &&
                                        dayNum == todayCal.get(Calendar.DAY_OF_MONTH)
                                    val isSelected = dayNum == safeSelectedDay
                                    val isFriday = col == 5

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.88f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                                    isFriday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable { selectedDayOfMonth = dayNum },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    col == 0 -> MaterialTheme.colorScheme.error
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            Text(
                                                text = "${cellHijri.day}",
                                                fontSize = 10.sp,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                                                    else -> MaterialTheme.colorScheme.primary
                                                }
                                            )
                                            if (events.isNotEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) Color(0xFFFFE7AE)
                                                            else MaterialTheme.colorScheme.secondary
                                                        )
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            displayedYear = todayCal.get(Calendar.YEAR)
                            displayedMonth = todayCal.get(Calendar.MONTH)
                            selectedDayOfMonth = todayCal.get(Calendar.DAY_OF_MONTH)
                        }) {
                            Text("Kembali ke Hari Ini")
                        }
                    }
                }
            }
        }

        // Selected Date Detail Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = HijriCalendarHelper.formatGregorianIndonesian(selectedCal),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = selectedHijri.formatted,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (selectedEvents.isEmpty()) {
                        Text(
                            text = "Tidak ada peringatan hari besar atau puasa sunnah khusus pada tanggal ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    } else {
                        selectedEvents.forEach { ev ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.EventAvailable,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(ev.title, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            ev.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Important Islamic Calendar Milestones
        item {
            Text(
                text = "Hari Besar Islam & Momen Puasa",
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(HijriCalendarHelper.importantIslamicEvents) { event ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(event.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${event.hijriDay} ${HijriCalendarHelper.hijriMonthNames[event.hijriMonth - 1]}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = event.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        item {
            AppFooter()
        }
    }
}
