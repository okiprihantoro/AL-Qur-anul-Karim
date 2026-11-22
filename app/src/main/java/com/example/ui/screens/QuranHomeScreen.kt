package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.OfflineSyncProgress
import com.example.data.local.BookmarkEntity
import com.example.data.local.SurahEntity
import com.example.data.local.UserSettings
import com.example.hijri.HijriCalendarHelper
import com.example.prayer.PrayerTimeCalculator
import com.example.ui.components.AdSensePlacementCard
import com.example.ui.components.AppFooter
import com.example.ui.components.RubElHizbNumberBadge
import com.example.ui.theme.AmiriArabicFamily
import com.example.ui.theme.LocalIsDarkTheme
import java.util.Calendar

@Composable
fun QuranHomeScreen(
    surahs: List<SurahEntity>,
    bookmarks: List<BookmarkEntity>,
    downloadedCount: Int,
    syncProgress: OfflineSyncProgress,
    settings: UserSettings,
    onOpenSurah: (surahNomor: Int, targetAyah: Int) -> Unit,
    onStartFullDownload: () -> Unit,
    onCancelFullDownload: () -> Unit,
    onRemoveBookmark: (Int) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPrayerTimes: () -> Unit,
    onOpenAdSenseSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Semua, 1=Juz Amma, 2=Offline, 3=Penanda
    var quickFilter by remember { mutableStateOf("") }

    val now = remember { Calendar.getInstance() }
    val todayHijri = remember(settings.hijriAdjustmentDays) {
        HijriCalendarHelper.fromGregorian(now, settings.hijriAdjustmentDays)
    }
    val todayGregorian = remember { HijriCalendarHelper.formatGregorianIndonesian(now) }
    val nextPrayerLabel = remember(settings.prayerLat, settings.prayerLon, settings.prayerTimezoneHours) {
        val times = PrayerTimeCalculator.calculateForDate(
            now,
            settings.prayerLat,
            settings.prayerLon,
            settings.prayerTimezoneHours
        )
        val curMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val next = times.azanEntries.firstOrNull { it.hour * 60 + it.minute > curMins }
            ?: times.subuh
        "${next.label} ${next.formatted}"
    }

    val filteredSurahs = remember(surahs, selectedTab, quickFilter) {
        val base = when (selectedTab) {
            1 -> surahs.filter { it.nomor >= 78 }
            2 -> surahs.filter { it.isDownloaded }
            else -> surahs
        }
        if (quickFilter.isBlank()) base
        else {
            val q = quickFilter.trim().lowercase()
            base.filter {
                it.namaLatin.lowercase().contains(q) ||
                    it.arti.lowercase().contains(q) ||
                    it.nomor.toString() == q
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Banner Card
        item {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_quran_hero_1795354020338),
                        contentDescription = "Latar arsitektur Islami AL-Quranul Karim",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xB8082920),
                                        Color(0xE006211A)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Assalamu'alaikum",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFFE7C167)
                                )
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "${todayHijri.formatted} • $todayGregorian",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = onOpenSearch,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.14f))
                                        .testTag("home_search_icon_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Cari ayat offline",
                                        tint = Color.White
                                    )
                                }
                                IconButton(
                                    onClick = onToggleTheme,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.14f))
                                        .testTag("home_dark_mode_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                        contentDescription = stringResource(R.string.cd_toggle_theme),
                                        tint = Color(0xFFE7C167)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, Color(0xFFE7C167).copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .clickable { onOpenPrayerTimes() }
                                    .testTag("home_next_prayer_pill")
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(
                                        text = settings.prayerCityName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Shalat Berikutnya: $nextPrayerLabel",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFFFFE7AE)
                                    )
                                }
                            }

                            Text(
                                text = "القرآن الكريم",
                                fontFamily = AmiriArabicFamily,
                                fontSize = 26.sp,
                                color = Color(0xFFE7C167)
                            )
                        }
                    }
                }
            }
        }

        // Last Read Card (Penanda Bacaan Terakhir)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpenSurah(settings.lastReadSurah, settings.lastReadAyah)
                    }
                    .testTag("last_read_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Penanda Bacaan Terakhir",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "QS. ${settings.lastReadSurahName} • Ayat ${settings.lastReadAyah}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Button(
                        onClick = { onOpenSurah(settings.lastReadSurah, settings.lastReadAyah) },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("continue_reading_button")
                    ) {
                        Text("Lanjut")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Offline Sync Banner (Unduh 30 Juz Lengkap)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (downloadedCount >= 114) Icons.Filled.OfflinePin else Icons.Filled.CloudDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (downloadedCount >= 114) {
                                        "Mushaf 30 Juz Tersedia Offline"
                                    } else {
                                        "Penyimpanan Offline ($downloadedCount / 114 Surat)"
                                    },
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = if (syncProgress.isSyncing) {
                                        "Mengunduh ${syncProgress.currentSurahName} (${syncProgress.completedCount}/114)..."
                                    } else if (downloadedCount >= 114) {
                                        "Seluruh 114 surat & pencarian ayat siap digunakan tanpa internet."
                                    } else {
                                        "Unduh seluruh 114 surat agar bacaan & pencarian ayat 100% offline."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (syncProgress.isSyncing) {
                            OutlinedButton(
                                onClick = onCancelFullDownload,
                                modifier = Modifier.testTag("cancel_offline_sync_button")
                            ) {
                                Text("Henti")
                            }
                        } else if (downloadedCount < 114) {
                            Button(
                                onClick = onStartFullDownload,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                modifier = Modifier.testTag("download_all_quran_button")
                            ) {
                                Text("Unduh")
                            }
                        }
                    }

                    AnimatedVisibility(visible = syncProgress.isSyncing || (downloadedCount in 1..113)) {
                        val progress = if (syncProgress.isSyncing) {
                            syncProgress.completedCount / 114f
                        } else {
                            downloadedCount / 114f
                        }
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }

                    if (!syncProgress.errorMessage.isNullOrBlank()) {
                        Text(
                            text = syncProgress.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // AdSense Banner (if configured by owner for Home)
        if (settings.adsenseEnabled && settings.adsenseShowOnHome) {
            item {
                AdSensePlacementCard(
                    settings = settings,
                    placementName = "home",
                    onConfigureClick = onOpenAdSenseSettings
                )
            }
        }

        // Quick Filter & Tabs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val tabs = listOf(
                        "Semua (114)" to 0,
                        "Juz 'Amma" to 1,
                        "Offline ($downloadedCount)" to 2,
                        "Penanda (${bookmarks.size})" to 3
                    )
                    items(tabs) { (label, idx) ->
                        FilterChip(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("surah_tab_$idx")
                        )
                    }
                }

                if (selectedTab != 3) {
                    OutlinedTextField(
                        value = quickFilter,
                        onValueChange = { quickFilter = it },
                        placeholder = { Text("Filter nama surat atau nomor (cth: Al-Mulk, 36)...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (quickFilter.isNotEmpty()) {
                                IconButton(onClick = { quickFilter = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Hapus filter")
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("surah_quick_filter_input")
                    )
                }
            }
        }

        // List Content: Bookmarks or Surahs
        if (selectedTab == 3) {
            if (bookmarks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Belum Ada Penanda Ayat",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Ketuk ikon penanda pada ayat saat membaca untuk menyimpannya di sini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(bookmarks, key = { it.id }) { bm ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSurah(bm.surahNomor, bm.nomorAyat) }
                            .testTag("bookmark_item_${bm.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "QS. ${bm.surahNamaLatin} : Ayat ${bm.nomorAyat}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { onRemoveBookmark(bm.id) }) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Hapus penanda",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Text(
                                text = bm.teksArab,
                                fontFamily = AmiriArabicFamily,
                                fontSize = 20.sp,
                                lineHeight = 34.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = bm.teksIndonesia,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else {
            items(filteredSurahs, key = { it.nomor }) { surah ->
                SurahListCard(
                    surah = surah,
                    onClick = { onOpenSurah(surah.nomor, 1) }
                )
            }
        }

        // App Footer
        item {
            AppFooter(onOpenAdSenseSettings = onOpenAdSenseSettings)
        }
    }
}

@Composable
private fun SurahListCard(
    surah: SurahEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("surah_card_${surah.nomor}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RubElHizbNumberBadge(
                number = surah.nomor,
                size = 42.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = surah.namaLatin,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (surah.isDownloaded) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Tersedia offline",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Text(
                    text = "${surah.arti} • ${surah.tempatTurun} • ${surah.jumlahAyat} Ayat",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = surah.namaArab,
                fontFamily = AmiriArabicFamily,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
