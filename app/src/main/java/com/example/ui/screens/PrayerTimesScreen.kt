package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.UserSettings
import com.example.hijri.HijriCalendarHelper
import com.example.prayer.AzanNotificationHelper
import com.example.prayer.CityOption
import com.example.prayer.IndonesianCities
import com.example.prayer.PrayerTimeCalculator
import com.example.prayer.PrayerTimeEntry
import com.example.ui.components.AdSensePlacementCard
import com.example.ui.components.AppFooter
import com.example.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@Composable
fun PrayerTimesScreen(
    settings: UserSettings,
    locationMessage: String?,
    onClearLocationMessage: () -> Unit,
    onDetectLocation: () -> Unit,
    onAutoRefreshLocationIfPermitted: () -> Unit,
    onSelectCity: (CityOption) -> Unit,
    onAzanToggle: (String, Boolean) -> Unit,
    onPreReminderToggle: (Boolean) -> Unit,
    onSoundModeChange: (String) -> Unit,
    onTestAzanNotification: () -> Unit,
    onScheduleTestAlarm: () -> Unit,
    onRescheduleAlarms: () -> Unit,
    onOpenAdSenseSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    var showCityDialog by remember { mutableStateOf(false) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        val hasLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasLocation) {
            onAutoRefreshLocationIfPermitted()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    LaunchedEffect(locationMessage) {
        if (!locationMessage.isNullOrBlank()) {
            Toast.makeText(context, locationMessage, Toast.LENGTH_SHORT).show()
            onClearLocationMessage()
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onDetectLocation()
        } else {
            Toast.makeText(
                context,
                "Izin lokasi ditolak. Anda tetap dapat memilih kota secara manual.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onTestAzanNotification()
        } else {
            Toast.makeText(
                context,
                "Izin notifikasi diperlukan untuk menampilkan pengingat azan.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val calendar = remember(nowMillis / 60_000L) { Calendar.getInstance() }
    val todayTimes = remember(
        calendar.get(Calendar.DAY_OF_YEAR),
        settings.prayerLat,
        settings.prayerLon,
        settings.prayerTimezoneHours
    ) {
        PrayerTimeCalculator.calculateForDate(
            calendar = calendar,
            latitude = settings.prayerLat,
            longitude = settings.prayerLon,
            timezoneHours = settings.prayerTimezoneHours
        )
    }

    val hijriToday = remember(calendar.get(Calendar.DAY_OF_YEAR), settings.hijriAdjustmentDays) {
        HijriCalendarHelper.fromGregorian(calendar, settings.hijriAdjustmentDays)
    }

    // Calculate next obligatory prayer & countdown
    val (nextPrayer, countdownText) = remember(nowMillis, todayTimes) {
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val nextToday = todayTimes.azanEntries.firstOrNull { entry ->
            entry.toCalendarForDate(nowCal).timeInMillis > nowMillis
        }
        val targetCal: Calendar
        val chosen: PrayerTimeEntry
        if (nextToday != null) {
            chosen = nextToday
            targetCal = nextToday.toCalendarForDate(nowCal)
        } else {
            chosen = todayTimes.subuh
            val tomorrow = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
            targetCal = todayTimes.subuh.toCalendarForDate(tomorrow)
        }
        val diffSec = ((targetCal.timeInMillis - nowMillis) / 1000L).coerceAtLeast(0L)
        val hrs = diffSec / 3600L
        val mins = (diffSec % 3600L) / 60L
        val secs = diffSec % 60L
        chosen to String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    }

    val azanToggleMap = mapOf(
        "subuh" to settings.azanSubuh,
        "dzuhur" to settings.azanDzuhur,
        "ashar" to settings.azanAshar,
        "maghrib" to settings.azanMaghrib,
        "isya" to settings.azanIsya
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Next Prayer Countdown Hero Card
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
                                    listOf(Color(0xFF0E3A2F), Color(0xFF07211A))
                                } else {
                                    listOf(Color(0xFF0E4D3C), Color(0xFF1D6F57))
                                }
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = settings.prayerCityName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFFFE7AE)
                            )
                            Text(
                                text = "${hijriToday.formatted} • ${HijriCalendarHelper.formatGregorianIndonesian(calendar)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.82f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.WbSunny,
                            contentDescription = null,
                            tint = Color(0xFFE7C167),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, Color(0xFFE7C167).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Menuju Shalat ${nextPrayer.label}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "Pukul ${nextPrayer.formatted}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color(0xFFFFE7AE)
                                )
                            }
                            Text(
                                text = "-$countdownText",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    onDetectLocation()
                                } else {
                                    locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                }
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE7C167),
                                contentColor = Color(0xFF281B00)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("detect_gps_location_button")
                        ) {
                            Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Deteksi Lokasi")
                        }

                        OutlinedButton(
                            onClick = { showCityDialog = true },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_city_button")
                        ) {
                            Icon(
                                Icons.Filled.LocationCity,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pilih Kota", color = Color.White)
                        }
                    }
                }
            }
        }

        // Daily Prayer Times List + Azan Toggles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Jadwal Shalat & Notifikasi Azan",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) onTestAzanNotification()
                            else notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onTestAzanNotification()
                        }
                    },
                    modifier = Modifier.testTag("test_azan_notification_button")
                ) {
                    Text("Uji Notifikasi")
                }
            }
        }

        items(todayTimes.allEntries, key = { it.key }) { entry ->
            val isNext = entry.key == nextPrayer.key
            val alarmEnabled = azanToggleMap[entry.key] ?: false

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prayer_row_${entry.key}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNext) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    if (isNext) 1.5.dp else 1.dp,
                    if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = entry.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (entry.isObligatory) {
                                if (alarmEnabled) "Notifikasi azan aktif" else "Notifikasi azan nonaktif"
                            } else {
                                "Waktu rujukan harian"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = entry.formatted,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (entry.isObligatory) {
                            Switch(
                                checked = alarmEnabled,
                                onCheckedChange = { checked ->
                                    if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!granted) {
                                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                    onAzanToggle(entry.key, checked)
                                },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (alarmEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.testTag("azan_switch_${entry.key}")
                            )
                        }
                    }
                }
            }
        }

        // Azan Local Alarm System Status & Controls
        item {
            val summary = remember(nowMillis / 60_000L, settings) {
                AzanNotificationHelper.getNextScheduledSummary(context, settings, nowMillis)
            }
            val hasNotifPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("azan_alarm_status_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Filled.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Sistem Alarm Azan Lokal", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if (summary.activeAlarmCount > 0) {
                                        val dayLabel = if (summary.isTomorrow) "besok" else "hari ini"
                                        "Berikutnya: ${summary.nextPrayerLabel} pukul ${summary.nextPrayerTime} ($dayLabel) • ${summary.activeAlarmCount}/5 aktif"
                                    } else {
                                        "Belum ada pengingat shalat yang diaktifkan"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = onRescheduleAlarms) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Jadwalkan ulang alarm azan"
                            )
                        }
                    }

                    if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Izin notifikasi belum aktif agar azan dapat tampil.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }) {
                                    Text("Izinkan")
                                }
                            }
                        }
                    }

                    if (!summary.exactAlarmsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Aktifkan izin Alarm Akurat agar azan berbunyi tepat menitnya.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = {
                                    runCatching {
                                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    }
                                }) {
                                    Text("Aktifkan")
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pengingat 10 Menit Sebelumnya", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Beri notifikasi persiapan wudhu 10 menit sebelum masuk waktu shalat",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.azanPreReminderEnabled,
                            onCheckedChange = onPreReminderToggle,
                            modifier = Modifier.testTag("azan_pre_reminder_switch")
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Mode Suara Pengingat Azan", style = MaterialTheme.typography.titleSmall)
                        val modes = listOf(
                            "alarm" to "Alarm",
                            "notification" to "Notifikasi",
                            "vibrate" to "Getar"
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            modes.forEachIndexed { idx, (key, label) ->
                                SegmentedButton(
                                    selected = settings.azanSoundMode == key,
                                    onClick = { onSoundModeChange(key) },
                                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = modes.size),
                                    modifier = Modifier.testTag("azan_mode_$key")
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onScheduleTestAlarm()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("schedule_test_azan_5s_button")
                        ) {
                            Text("Simulasi Alarm (5 dtk)")
                        }
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onTestAzanNotification()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("trigger_azan_now_button")
                        ) {
                            Text("Uji Sekarang")
                        }
                    }
                }
            }
        }

        // Qibla Compass Card (Real SensorManager)
        item {
            QiblaCompassCard(
                latitude = settings.prayerLat,
                longitude = settings.prayerLon,
                cityName = settings.prayerCityName
            )
        }

        if (settings.adsenseEnabled && settings.adsenseShowOnPrayer) {
            item {
                AdSensePlacementCard(
                    settings = settings,
                    placementName = "prayer",
                    onConfigureClick = onOpenAdSenseSettings
                )
            }
        }

        item {
            AppFooter(onOpenAdSenseSettings = onOpenAdSenseSettings)
        }
    }

    if (showCityDialog) {
        var cityQuery by remember { mutableStateOf("") }
        val filtered = remember(cityQuery) {
            if (cityQuery.isBlank()) IndonesianCities.list
            else IndonesianCities.list.filter { it.name.contains(cityQuery, ignoreCase = true) }
        }
        AlertDialog(
            onDismissRequest = { showCityDialog = false },
            title = { Text("Pilih Kota / Wilayah") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = cityQuery,
                        onValueChange = { cityQuery = it },
                        placeholder = { Text("Cari kota...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn(modifier = Modifier.height(280.dp)) {
                        items(filtered) { city ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectCity(city)
                                        showCityDialog = false
                                    }
                                    .padding(vertical = 4.dp),
                                shape = MaterialTheme.shapes.small,
                                color = if (city.name == settings.prayerCityName) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(city.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "UTC+${city.timezoneHours.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCityDialog = false }) { Text("Tutup") }
            }
        )
    }
}

@Composable
private fun QiblaCompassCard(
    latitude: Double,
    longitude: Double,
    cityName: String
) {
    val context = LocalContext.current
    val qiblaBearing = remember(latitude, longitude) {
        PrayerTimeCalculator.calculateQiblaBearing(latitude, longitude).toFloat()
    }
    var azimuth by remember { mutableFloatStateOf(0f) }
    var sensorAvailable by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (accel == null || magnet == null) {
            sensorAvailable = false
            onDispose { }
        } else {
            val gravity = FloatArray(3)
            val geomagnetic = FloatArray(3)
            val rMatrix = FloatArray(9)
            val iMatrix = FloatArray(9)
            val orientation = FloatArray(3)

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val alpha = 0.85f
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        for (i in 0..2) gravity[i] = alpha * gravity[i] + (1 - alpha) * event.values[i]
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        for (i in 0..2) geomagnetic[i] = alpha * geomagnetic[i] + (1 - alpha) * event.values[i]
                    }
                    if (SensorManager.getRotationMatrix(rMatrix, iMatrix, gravity, geomagnetic)) {
                        SensorManager.getOrientation(rMatrix, orientation)
                        val deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        azimuth = (deg + 360f) % 360f
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magnet, SensorManager.SENSOR_DELAY_UI)
            onDispose {
                sensorManager.unregisterListener(listener)
            }
        }
    }

    val diff = abs(((qiblaBearing - azimuth + 540f) % 360f) - 180f)
    val isAligned = sensorAvailable && diff < 6f
    val primaryColor = MaterialTheme.colorScheme.primary
    val goldColor = MaterialTheme.colorScheme.secondary
    val outlineColor = MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Explore,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Arah Kiblat", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = "${qiblaBearing.toInt()}° dari Utara ($cityName)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = primaryColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (!sensorAvailable) {
                        "Sensor kompas tidak tersedia pada perangkat ini. Arahkan ${qiblaBearing.toInt()}° dari Utara."
                    } else if (isAligned) {
                        "Arah perangkat sudah menghadap Kiblat."
                    } else {
                        "Pegang perangkat secara datar untuk melihat arah jarum emas menuju Ka'bah."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        if (isAligned) primaryColor.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(76.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f
                    drawCircle(
                        color = if (isAligned) primaryColor else outlineColor,
                        radius = radius,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    val needleAngle = if (sensorAvailable) qiblaBearing - azimuth else qiblaBearing
                    rotate(degrees = needleAngle, pivot = center) {
                        val path = Path().apply {
                            moveTo(center.x, center.y - radius * 0.78f)
                            lineTo(center.x - 7.dp.toPx(), center.y)
                            lineTo(center.x + 7.dp.toPx(), center.y)
                            close()
                        }
                        drawPath(path = path, color = if (isAligned) primaryColor else goldColor)
                        drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = center)
                    }
                }
            }
        }
    }
}
