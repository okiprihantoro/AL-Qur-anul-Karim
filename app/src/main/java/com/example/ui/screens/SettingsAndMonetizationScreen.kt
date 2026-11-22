package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OfflineSyncProgress
import com.example.data.QariCatalog
import com.example.data.local.UserSettings
import com.example.ui.components.AppFooter
import com.example.ui.components.TajwidGuideDialog
import com.example.ui.theme.ScheherazadeArabicFamily

@Composable
fun SettingsAndMonetizationScreen(
    settings: UserSettings,
    downloadedSurahCount: Int,
    syncProgress: OfflineSyncProgress,
    onSetDarkMode: (String) -> Unit,
    onSetTajwidEnabled: (Boolean) -> Unit,
    onSetShowTransliteration: (Boolean) -> Unit,
    onSetShowTranslation: (Boolean) -> Unit,
    onSetArabicFontSize: (Float) -> Unit,
    onSelectQari: (String) -> Unit,
    onDownloadAllOffline: () -> Unit,
    onCancelDownload: () -> Unit,
    onSaveAdSense: (
        enabled: Boolean,
        publisherId: String,
        bannerSlotId: String,
        interstitialSlotId: String,
        linkUrl: String,
        showOnHome: Boolean,
        showOnPrayer: Boolean,
        hideDuringReading: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showTajwidGuide by remember { mutableStateOf(false) }

    // Local editable state for Google AdSense monetization form
    var adEnabled by remember(settings.adsenseEnabled) { mutableStateOf(settings.adsenseEnabled) }
    var pubId by remember(settings.adsensePublisherId) { mutableStateOf(settings.adsensePublisherId) }
    var bannerSlot by remember(settings.adsenseBannerSlotId) { mutableStateOf(settings.adsenseBannerSlotId) }
    var interstitialSlot by remember(settings.adsenseInterstitialSlotId) { mutableStateOf(settings.adsenseInterstitialSlotId) }
    var adLinkUrl by remember(settings.adsenseLinkUrl) { mutableStateOf(settings.adsenseLinkUrl) }
    var showOnHome by remember(settings.adsenseShowOnHome) { mutableStateOf(settings.adsenseShowOnHome) }
    var showOnPrayer by remember(settings.adsenseShowOnPrayer) { mutableStateOf(settings.adsenseShowOnPrayer) }
    var hideDuringReading by remember(settings.adsenseHideDuringReading) { mutableStateOf(settings.adsenseHideDuringReading) }

    LaunchedEffect(settings) {
        adEnabled = settings.adsenseEnabled
        pubId = settings.adsensePublisherId
        bannerSlot = settings.adsenseBannerSlotId
        interstitialSlot = settings.adsenseInterstitialSlotId
        adLinkUrl = settings.adsenseLinkUrl
        showOnHome = settings.adsenseShowOnHome
        showOnPrayer = settings.adsenseShowOnPrayer
        hideDuringReading = settings.adsenseHideDuringReading
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Pengaturan & Monetisasi",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        // 1. Dark Mode Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tema & Mode Gelap Malam", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "Gunakan Mode Gelap agar mata tetap nyaman saat membaca Al-Qur'an di malam hari.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val options = listOf("system" to "Sistem", "light" to "Terang", "dark" to "Gelap")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        options.forEachIndexed { idx, (key, label) ->
                            SegmentedButton(
                                selected = settings.darkModeSetting == key,
                                onClick = { onSetDarkMode(key) },
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = options.size),
                                modifier = Modifier.testTag("theme_mode_$key")
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }
        }

        // 2. Mushaf & Tajwid Preferences
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tampilan Mushaf & Tajwid", style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(onClick = { showTajwidGuide = true }) {
                            Text("Panduan")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tajwid Berwarna", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Tandai hukum tajwid dengan warna berbeda",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.tajwidEnabled,
                            onCheckedChange = onSetTajwidEnabled,
                            modifier = Modifier.testTag("settings_tajwid_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tampilkan Teks Latin", style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = settings.showTransliteration,
                            onCheckedChange = onSetShowTransliteration
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
                            onCheckedChange = onSetShowTranslation
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
                            onValueChange = onSetArabicFontSize,
                            valueRange = 22f..42f
                        )
                        Text(
                            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                            fontFamily = ScheherazadeArabicFamily,
                            fontSize = settings.arabicFontSize.sp,
                            lineHeight = (settings.arabicFontSize * 1.6f).sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 3. Qari Selector + Offline Database Sync
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pilihan Qari Audio Murottal", style = MaterialTheme.typography.titleMedium)
                    }
                    QariCatalog.qaris.forEach { qari ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.selectedQariId == qari.id,
                                onClick = { onSelectQari(qari.id) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(qari.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    qari.style,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (downloadedSurahCount >= 114) Icons.Filled.OfflinePin else Icons.Filled.CloudDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Data Offline: $downloadedSurahCount / 114 Surat",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        if (syncProgress.isSyncing) {
                            OutlinedButton(onClick = onCancelDownload) { Text("Henti") }
                        } else if (downloadedSurahCount < 114) {
                            Button(onClick = onDownloadAllOffline) { Text("Unduh Semua") }
                        }
                    }
                    if (syncProgress.isSyncing) {
                        LinearProgressIndicator(
                            progress = { syncProgress.completedCount / 114f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 4. Dedicated Google AdSense Monetization Configuration Page/Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("adsense_config_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Campaign,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Monetisasi & Tautan Google AdSense",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Atur Publisher ID, tautan Google AdSense, dan penempatan iklan aplikasi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Aktifkan Slot Monetisasi AdSense", style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = adEnabled,
                            onCheckedChange = { adEnabled = it },
                            modifier = Modifier.testTag("adsense_enable_switch")
                        )
                    }

                    OutlinedTextField(
                        value = pubId,
                        onValueChange = { pubId = it },
                        label = { Text("Google AdSense Publisher ID") },
                        placeholder = { Text("ca-pub-xxxxxxxxxxxxxxxx") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("adsense_pub_id_input")
                    )

                    OutlinedTextField(
                        value = adLinkUrl,
                        onValueChange = { adLinkUrl = it },
                        label = { Text("Link Google AdSense / Tautan Monetisasi") },
                        placeholder = { Text("https://pagead2.googlesyndication.com/...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("adsense_link_url_input")
                    )

                    OutlinedTextField(
                        value = bannerSlot,
                        onValueChange = { bannerSlot = it },
                        label = { Text("ID Unit Iklan Banner (data-ad-slot)") },
                        placeholder = { Text("1234567890") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = interstitialSlot,
                        onValueChange = { interstitialSlot = it },
                        label = { Text("ID Unit Iklan Native / Interstitial (Opsional)") },
                        placeholder = { Text("0987654321") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tampilkan di Beranda", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = showOnHome, onCheckedChange = { showOnHome = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tampilkan di Jadwal Shalat", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = showOnPrayer, onCheckedChange = { showOnPrayer = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bebas iklan saat membaca ayat suci", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = hideDuringReading, onCheckedChange = { hideDuringReading = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveAdSense(
                                    adEnabled,
                                    pubId,
                                    bannerSlot,
                                    interstitialSlot,
                                    adLinkUrl,
                                    showOnHome,
                                    showOnPrayer,
                                    hideDuringReading
                                )
                                Toast.makeText(context, "Konfigurasi Google AdSense disimpan", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_adsense_button")
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan AdSense")
                        }

                        if (adLinkUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        val url = if (adLinkUrl.startsWith("http")) adLinkUrl else "https://$adLinkUrl"
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }.onFailure {
                                        Toast.makeText(context, "Format tautan URL tidak valid", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("open_adsense_link_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Uji Link")
                            }
                        }
                    }
                }
            }
        }

        // 5. Footer with Developer Name, Privacy Policy, and BCA Donation
        item {
            AppFooter()
        }
    }

    if (showTajwidGuide) {
        TajwidGuideDialog(onDismiss = { showTajwidGuide = false })
    }
}
