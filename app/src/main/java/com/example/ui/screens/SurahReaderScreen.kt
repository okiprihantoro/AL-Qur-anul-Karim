package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.AudioPlayerState
import com.example.data.QariCatalog
import com.example.data.local.AyahEntity
import com.example.data.local.UserSettings
import com.example.domain.tajwid.TajwidParser
import com.example.ui.SurahReaderUiState
import com.example.ui.components.AppFooter
import com.example.ui.components.RubElHizbNumberBadge
import com.example.ui.components.TajwidGuideDialog
import com.example.ui.theme.AmiriArabicFamily
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.ScheherazadeArabicFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahReaderScreen(
    readerState: SurahReaderUiState,
    initialAyah: Int,
    bookmarkedIds: Set<Int>,
    settings: UserSettings,
    audioState: AudioPlayerState,
    onBack: () -> Unit,
    onNavigateSurah: (Int) -> Unit,
    onRetryLoad: (Int) -> Unit,
    onPlayFromAyah: (List<AyahEntity>, Int, Boolean) -> Unit,
    onTogglePlayPause: () -> Unit,
    onDownloadSurahAudio: () -> Unit,
    onMarkLastRead: (Int, String, Int) -> Unit,
    onToggleBookmark: (AyahEntity) -> Unit,
    onToggleTajwid: (Boolean) -> Unit,
    onToggleTransliteration: (Boolean) -> Unit,
    onToggleTranslation: (Boolean) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onSelectQari: (String) -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val listState = rememberLazyListState()
    var showTajwidGuide by remember { mutableStateOf(false) }
    var showReadingSettings by remember { mutableStateOf(false) }
    var showQariPicker by remember { mutableStateOf(false) }
    var hasScrolledToTarget by remember(readerState.surahNomor, initialAyah) { mutableStateOf(false) }

    // Scroll to target ayah (e.g. from Last Read or Search) once loaded
    LaunchedEffect(readerState.ayahs, initialAyah) {
        if (!hasScrolledToTarget && initialAyah > 1 && readerState.ayahs.isNotEmpty()) {
            val idx = readerState.ayahs.indexOfFirst { it.nomorAyat == initialAyah }
            if (idx >= 0) {
                listState.scrollToItem(idx + 1) // +1 for Surah Header card
                hasScrolledToTarget = true
            }
        }
    }

    // Auto-scroll to currently playing ayah
    LaunchedEffect(audioState.currentAyahNomor, audioState.surahNomor) {
        if (audioState.isActive && audioState.surahNomor == readerState.surahNomor && readerState.ayahs.isNotEmpty()) {
            val idx = readerState.ayahs.indexOfFirst { it.nomorAyat == audioState.currentAyahNomor }
            if (idx >= 0) {
                listState.animateScrollToItem(idx + 1)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = readerState.surah?.namaLatin ?: "Surat ${readerState.surahNomor}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    readerState.surah?.let { s ->
                        Text(
                            text = "${s.arti} • ${s.jumlahAyat} Ayat",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("reader_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { showTajwidGuide = true },
                    modifier = Modifier.testTag("reader_tajwid_guide_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ColorLens,
                        contentDescription = "Panduan warna tajwid",
                        tint = if (settings.tajwidEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { showReadingSettings = true },
                    modifier = Modifier.testTag("reader_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Pengaturan bacaan"
                    )
                }
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.testTag("reader_dark_mode_toggle")
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = stringResource(R.string.cd_toggle_theme)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (readerState.isLoading && readerState.ayahs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Memuat ayat-ayat Al-Qur'an...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else if (readerState.errorMessage != null && readerState.ayahs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = readerState.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { onRetryLoad(readerState.surahNomor) }) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Coba Unduh Lagi")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Surah Header + Bismillah + Audio Controls
                item {
                    val surah = readerState.surah
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
                                            listOf(Color(0xFF10372C), Color(0xFF0A241D))
                                        } else {
                                            listOf(Color(0xFF0E4D3C), Color(0xFF1A6B54))
                                        }
                                    )
                                )
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = surah?.namaArab ?: "",
                                fontFamily = AmiriArabicFamily,
                                fontSize = 32.sp,
                                color = Color(0xFFFFE7AE)
                            )
                            Text(
                                text = "${surah?.namaLatin ?: ""} (${surah?.arti ?: ""})",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = "${surah?.tempatTurun ?: ""} • ${surah?.jumlahAyat ?: readerState.ayahs.size} Ayat",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )

                            if (readerState.surahNomor != 1 && readerState.surahNomor != 9) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                                Text(
                                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                    fontFamily = ScheherazadeArabicFamily,
                                    fontSize = 26.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isThisSurahPlaying = audioState.isActive &&
                                    audioState.surahNomor == readerState.surahNomor &&
                                    audioState.isPlaying

                                Button(
                                    onClick = {
                                        if (audioState.isActive && audioState.surahNomor == readerState.surahNomor) {
                                            onTogglePlayPause()
                                        } else {
                                            onPlayFromAyah(readerState.ayahs, 1, true)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("play_full_surah_button"),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE7C167),
                                        contentColor = Color(0xFF281B00)
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isThisSurahPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isThisSurahPlaying) "Jeda Murottal" else "Putar Surat")
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (!readerState.isAudioCachedOffline) {
                                            onDownloadSurahAudio()
                                        } else {
                                            Toast.makeText(context, "Audio surat sudah tersimpan offline", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                    modifier = Modifier.testTag("download_surah_audio_button")
                                ) {
                                    Icon(
                                        imageVector = if (readerState.isAudioCachedOffline) Icons.Filled.CheckCircle else Icons.Filled.CloudDownload,
                                        contentDescription = "Unduh audio offline",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (readerState.isAudioCachedOffline) "Offline" else "Simpan MP3",
                                        color = Color.White
                                    )
                                }
                            }

                            if (audioState.isDownloadingAudio) {
                                LinearProgressIndicator(
                                    progress = { audioState.audioDownloadProgress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFFE7C167)
                                )
                            }

                            TextButton(onClick = { showQariPicker = true }) {
                                Text(
                                    text = "Qari: ${QariCatalog.findById(settings.selectedQariId).name} ▾",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFFFE7AE)
                                )
                            }
                        }
                    }
                }

                // Ayahs
                items(readerState.ayahs, key = { it.id }) { ayah ->
                    val isCurrentlyPlaying = audioState.isActive &&
                        audioState.surahNomor == ayah.surahNomor &&
                        audioState.currentAyahNomor == ayah.nomorAyat
                    val isLastRead = settings.lastReadSurah == ayah.surahNomor &&
                        settings.lastReadAyah == ayah.nomorAyat
                    val isBookmarked = ayah.id in bookmarkedIds

                    AyahCard(
                        ayah = ayah,
                        settings = settings,
                        isCurrentlyPlaying = isCurrentlyPlaying,
                        isPlayingAudio = isCurrentlyPlaying && audioState.isPlaying,
                        isLastRead = isLastRead,
                        isBookmarked = isBookmarked,
                        onPlayToggle = {
                            if (isCurrentlyPlaying) {
                                onTogglePlayPause()
                            } else {
                                onPlayFromAyah(readerState.ayahs, ayah.nomorAyat, true)
                            }
                        },
                        onMarkLastRead = {
                            onMarkLastRead(ayah.surahNomor, ayah.surahNamaLatin, ayah.nomorAyat)
                            Toast.makeText(
                                context,
                                "Ditandai bacaan terakhir: ${ayah.surahNamaLatin} ayat ${ayah.nomorAyat}",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onToggleBookmark = { onToggleBookmark(ayah) }
                    )
                }

                // Next / Previous Surah Navigation
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (readerState.surahNomor > 1) {
                            OutlinedButton(
                                onClick = { onNavigateSurah(readerState.surahNomor - 1) },
                                modifier = Modifier.testTag("prev_surah_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Surat Sebelumnya")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        if (readerState.surahNomor < 114) {
                            FilledTonalButton(
                                onClick = { onNavigateSurah(readerState.surahNomor + 1) },
                                modifier = Modifier.testTag("next_surah_button")
                            ) {
                                Text("Surat Berikutnya")
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                item {
                    AppFooter()
                }
            }
        }
    }

    if (showTajwidGuide) {
        TajwidGuideDialog(onDismiss = { showTajwidGuide = false })
    }

    if (showReadingSettings) {
        ModalBottomSheet(onDismissRequest = { showReadingSettings = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Pengaturan Tampilan Mushaf", style = MaterialTheme.typography.titleLarge)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tanda Baca Tajwid Berwarna", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Warnai hukum Ghunnah, Idgham, Iqlab, Ikhfa, Qalqalah, dan Mad",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.tajwidEnabled,
                        onCheckedChange = onToggleTajwid
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tampilkan Teks Latin (Transliterasi)", style = MaterialTheme.typography.titleSmall)
                    Switch(
                        checked = settings.showTransliteration,
                        onCheckedChange = onToggleTransliteration
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tampilkan Terjemahan Indonesia", style = MaterialTheme.typography.titleSmall)
                    Switch(
                        checked = settings.showTranslation,
                        onCheckedChange = onToggleTranslation
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ukuran Huruf Arab", style = MaterialTheme.typography.titleSmall)
                        Text("${settings.arabicFontSize.toInt()} sp", style = MaterialTheme.typography.labelLarge)
                    }
                    Slider(
                        value = settings.arabicFontSize,
                        onValueChange = onFontSizeChange,
                        valueRange = 22f..42f
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showQariPicker) {
        AlertDialog(
            onDismissRequest = { showQariPicker = false },
            title = { Text("Pilih Qari Murottal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    QariCatalog.qaris.forEach { qari ->
                        val selected = qari.id == settings.selectedQariId
                        Surface(
                            onClick = {
                                onSelectQari(qari.id)
                                showQariPicker = false
                            },
                            shape = MaterialTheme.shapes.medium,
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(qari.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    qari.style,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQariPicker = false }) { Text("Tutup") }
            }
        )
    }
}

@Composable
private fun AyahCard(
    ayah: AyahEntity,
    settings: UserSettings,
    isCurrentlyPlaying: Boolean,
    isPlayingAudio: Boolean,
    isLastRead: Boolean,
    isBookmarked: Boolean,
    onPlayToggle: () -> Unit,
    onMarkLastRead: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val defaultArabicColor = MaterialTheme.colorScheme.onSurface

    val tajwidAnnotated = remember(ayah.teksArab, settings.tajwidEnabled, isDark, defaultArabicColor) {
        TajwidParser.parse(
            arabicText = ayah.teksArab,
            enabled = settings.tajwidEnabled,
            isDark = isDark,
            defaultColor = defaultArabicColor
        )
    }

    val containerColor = when {
        isCurrentlyPlaying -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        isLastRead -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        isCurrentlyPlaying -> MaterialTheme.colorScheme.primary
        isLastRead -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ayah_card_${ayah.surahNomor}_${ayah.nomorAyat}"),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isCurrentlyPlaying || isLastRead) 1.5.dp else 1.dp, borderColor),
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RubElHizbNumberBadge(
                        number = ayah.nomorAyat,
                        size = 36.dp
                    )
                    if (isLastRead) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Bacaan Terakhir",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPlayToggle,
                        modifier = Modifier.testTag("ayah_play_btn_${ayah.nomorAyat}")
                    ) {
                        Icon(
                            imageVector = if (isPlayingAudio) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play_audio),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onMarkLastRead,
                        modifier = Modifier.testTag("ayah_last_read_btn_${ayah.nomorAyat}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = stringResource(R.string.cd_last_read),
                            tint = if (isLastRead) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.testTag("ayah_bookmark_btn_${ayah.nomorAyat}")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = stringResource(R.string.cd_bookmark),
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Arabic Uthmani Text with Tajwid colors in RTL direction
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = tajwidAnnotated,
                    fontFamily = ScheherazadeArabicFamily,
                    fontSize = settings.arabicFontSize.sp,
                    lineHeight = (settings.arabicFontSize * 1.85f).sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (settings.showTransliteration && ayah.teksLatin.isNotBlank()) {
                Text(
                    text = ayah.teksLatin,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (settings.showTranslation && ayah.teksIndonesia.isNotBlank()) {
                Text(
                    text = ayah.teksIndonesia,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
