package com.example.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.QuranApplication
import com.example.audio.AudioPlayerState
import com.example.data.OfflineSyncProgress
import com.example.data.local.AyahEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.SurahEntity
import com.example.data.local.UserSettings
import com.example.prayer.AzanNotificationHelper
import com.example.prayer.CityOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.math.roundToInt

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val surahResults: List<SurahEntity> = emptyList(),
    val ayahResults: List<AyahEntity> = emptyList()
)

data class SurahReaderUiState(
    val surahNomor: Int = 1,
    val surah: SurahEntity? = null,
    val ayahs: List<AyahEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAudioCachedOffline: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as QuranApplication
    private val repository = app.repository
    private val preferences = app.preferences
    private val audioManager = app.audioManager

    val surahs: StateFlow<List<SurahEntity>> = repository.allSurahs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedSurahCount: StateFlow<Int> = repository.downloadedSurahCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalAyahCount: StateFlow<Int> = repository.totalAyahCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<UserSettings> = preferences.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val syncProgress: StateFlow<OfflineSyncProgress> = repository.syncProgress

    val audioState: StateFlow<AudioPlayerState> = audioManager.state

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _readerState = MutableStateFlow(SurahReaderUiState())
    val readerState: StateFlow<SurahReaderUiState> = _readerState.asStateFlow()

    private val _locationDetectMessage = MutableStateFlow<String?>(null)
    val locationDetectMessage: StateFlow<String?> = _locationDetectMessage.asStateFlow()

    private var surahObserverJob: Job? = null
    private var ayahsObserverJob: Job? = null

    fun openSurah(surahNomor: Int) {
        surahObserverJob?.cancel()
        ayahsObserverJob?.cancel()

        _readerState.value = SurahReaderUiState(
            surahNomor = surahNomor,
            isLoading = true
        )

        surahObserverJob = viewModelScope.launch {
            repository.observeSurah(surahNomor).collectLatest { surah ->
                val cached = if (surah != null) {
                    audioManager.isSurahAudioCachedOffline(
                        settings.value.selectedQariId,
                        surah.nomor,
                        surah.jumlahAyat
                    )
                } else false
                _readerState.value = _readerState.value.copy(
                    surah = surah,
                    isAudioCachedOffline = cached
                )
            }
        }

        ayahsObserverJob = viewModelScope.launch {
            repository.observeAyahs(surahNomor).collectLatest { ayahs ->
                _readerState.value = _readerState.value.copy(
                    ayahs = ayahs,
                    isLoading = if (ayahs.isNotEmpty()) false else _readerState.value.isLoading
                )
            }
        }

        viewModelScope.launch {
            val result = repository.ensureSurahLoaded(surahNomor)
            _readerState.value = _readerState.value.copy(
                isLoading = false,
                errorMessage = if (result.isFailure && _readerState.value.ayahs.isEmpty()) {
                    "Surat belum tersimpan offline. Hubungkan ke internet untuk mengunduh surat ini."
                } else null
            )
        }
    }

    fun retryLoadSurah(surahNomor: Int) {
        openSurah(surahNomor)
    }

    fun downloadAllSurahsOffline() {
        viewModelScope.launch {
            repository.downloadAllRemainingSurahs()
        }
    }

    fun cancelFullDownload() {
        repository.cancelFullDownload()
    }

    fun updateSearchQuery(query: String) {
        _searchState.value = _searchState.value.copy(query = query, isSearching = query.isNotBlank())
        viewModelScope.launch {
            val (surahRes, ayahRes) = repository.searchOffline(query)
            _searchState.value = _searchState.value.copy(
                isSearching = false,
                surahResults = surahRes,
                ayahResults = ayahRes
            )
        }
    }

    fun markLastRead(surahNomor: Int, surahName: String, ayahNomor: Int) {
        viewModelScope.launch {
            preferences.updateLastRead(surahNomor, surahName, ayahNomor)
        }
    }

    fun toggleBookmark(ayah: AyahEntity) {
        viewModelScope.launch {
            repository.toggleBookmark(ayah)
        }
    }

    fun removeBookmark(id: Int) {
        viewModelScope.launch {
            repository.removeBookmark(id)
        }
    }

    fun playSurahFrom(ayahs: List<AyahEntity>, startAyahNomor: Int, continuous: Boolean = true) {
        audioManager.playSurahFrom(ayahs, startAyahNomor, settings.value.selectedQariId, continuous)
    }

    fun togglePlayPause() = audioManager.togglePlayPause()
    fun nextAyah() = audioManager.nextAyah()
    fun previousAyah() = audioManager.previousAyah()
    fun toggleRepeatAyah() = audioManager.toggleRepeatAyah()
    fun cyclePlaybackSpeed() = audioManager.cyclePlaybackSpeed()
    fun stopAudio() = audioManager.stop()
    fun clearAudioError() = audioManager.clearError()

    fun downloadCurrentSurahAudio() {
        val state = _readerState.value
        val ayahs = state.ayahs
        if (ayahs.isEmpty()) return
        val qariId = settings.value.selectedQariId
        audioManager.downloadSurahAudioOffline(ayahs, qariId) { ok ->
            if (ok) {
                _readerState.value = _readerState.value.copy(isAudioCachedOffline = true)
            }
        }
    }

    fun setDarkMode(mode: String) {
        viewModelScope.launch { preferences.setDarkMode(mode) }
    }

    fun toggleQuickDarkMode(currentlyDark: Boolean) {
        viewModelScope.launch {
            preferences.setDarkMode(if (currentlyDark) "light" else "dark")
        }
    }

    fun setTajwidEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setTajwidEnabled(enabled) }
    }

    fun setShowTransliteration(show: Boolean) {
        viewModelScope.launch { preferences.setShowTransliteration(show) }
    }

    fun setShowTranslation(show: Boolean) {
        viewModelScope.launch { preferences.setShowTranslation(show) }
    }

    fun setArabicFontSize(size: Float) {
        viewModelScope.launch { preferences.setArabicFontSize(size) }
    }

    fun setSelectedQari(qariId: String) {
        viewModelScope.launch {
            preferences.setSelectedQari(qariId)
            audioManager.updateQari(qariId)
            val surah = _readerState.value.surah
            if (surah != null) {
                val cached = audioManager.isSurahAudioCachedOffline(qariId, surah.nomor, surah.jumlahAyat)
                _readerState.value = _readerState.value.copy(isAudioCachedOffline = cached)
            }
        }
    }

    fun selectCity(city: CityOption) {
        viewModelScope.launch {
            preferences.updatePrayerLocation(city.name, city.latitude, city.longitude, city.timezoneHours)
            val updated = settings.value.copy(
                prayerCityName = city.name,
                prayerLat = city.latitude,
                prayerLon = city.longitude,
                prayerTimezoneHours = city.timezoneHours
            )
            AzanNotificationHelper.scheduleDailyAlarms(app, updated)
            _locationDetectMessage.value = "Lokasi diatur ke ${city.name}"
        }
    }

    @SuppressLint("MissingPermission")
    fun detectLocationFromDevice(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _locationDetectMessage.value = "Mendeteksi lokasi perangkat..."
            }
            val detected = withContext(Dispatchers.IO) {
                try {
                    val lm = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val providers = lm.getProviders(true)
                    var bestLoc: Location? = null
                    for (provider in providers) {
                        val loc = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
                        if (bestLoc == null || loc.accuracy < bestLoc.accuracy) {
                            bestLoc = loc
                        }
                    }

                    // If no cached fix or fix is older than 15 minutes, actively request a fresh location
                    val isStale = bestLoc == null || (System.currentTimeMillis() - bestLoc.time) > 15 * 60_000L
                    if (isStale && providers.isNotEmpty()) {
                        val freshLoc = requestFreshSingleLocation(lm, providers)
                        if (freshLoc != null) {
                            bestLoc = freshLoc
                        }
                    }

                    bestLoc?.let { loc ->
                        val tzHours = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3600000.0
                        val cityName = resolveCityName(loc.latitude, loc.longitude)
                        CityOption(cityName, loc.latitude, loc.longitude, tzHours)
                    }
                } catch (_: Exception) {
                    null
                }
            }

            if (detected != null) {
                preferences.updatePrayerLocation(
                    detected.name,
                    detected.latitude,
                    detected.longitude,
                    detected.timezoneHours
                )
                val updated = settings.value.copy(
                    prayerCityName = detected.name,
                    prayerLat = detected.latitude,
                    prayerLon = detected.longitude,
                    prayerTimezoneHours = detected.timezoneHours
                )
                AzanNotificationHelper.scheduleDailyAlarms(app, updated)
                if (!silent) {
                    _locationDetectMessage.value = "Lokasi terdeteksi: ${detected.name} & alarm azan diperbarui"
                }
            } else if (!silent) {
                _locationDetectMessage.value = "Koordinat GPS belum tersedia. Pilih kota secara manual."
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshSingleLocation(
        lm: LocationManager,
        providers: List<String>
    ): Location? = withTimeoutOrNull(4500L) {
        val preferredProvider = when {
            providers.contains(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            providers.contains(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> providers.first()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            suspendCancellableCoroutine { cont ->
                val signal = CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                runCatching {
                    lm.getCurrentLocation(
                        preferredProvider,
                        signal,
                        Executors.newSingleThreadExecutor()
                    ) { location ->
                        if (cont.isActive) cont.resume(location)
                    }
                }.onFailure {
                    if (cont.isActive) cont.resume(null)
                }
            }
        } else {
            suspendCancellableCoroutine { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        runCatching { lm.removeUpdates(this) }
                        if (cont.isActive) cont.resume(location)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                cont.invokeOnCancellation {
                    runCatching { lm.removeUpdates(listener) }
                }
                runCatching {
                    lm.requestLocationUpdates(
                        preferredProvider,
                        0L,
                        0f,
                        listener,
                        Looper.getMainLooper()
                    )
                }.onFailure {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }

    private fun resolveCityName(lat: Double, lon: Double): String {
        return try {
            @Suppress("DEPRECATION")
            val list = Geocoder(app, Locale("id", "ID")).getFromLocation(lat, lon, 1)
            val addr = list?.firstOrNull()
            val city = addr?.subAdminArea ?: addr?.locality ?: addr?.adminArea
            val country = addr?.countryName
            if (!city.isNullOrBlank()) {
                if (!country.isNullOrBlank()) "$city, $country" else city
            } else {
                "Lokasi (${(lat * 100).roundToInt() / 100.0}, ${(lon * 100).roundToInt() / 100.0})"
            }
        } catch (_: Exception) {
            "Lokasi (${(lat * 100).roundToInt() / 100.0}, ${(lon * 100).roundToInt() / 100.0})"
        }
    }

    fun clearLocationMessage() {
        _locationDetectMessage.value = null
    }

    fun setAzanToggle(prayerKey: String, enabled: Boolean) {
        viewModelScope.launch {
            preferences.setAzanToggle(prayerKey, enabled)
            val s = settings.value
            val updated = when (prayerKey) {
                "subuh" -> s.copy(azanSubuh = enabled)
                "dzuhur" -> s.copy(azanDzuhur = enabled)
                "ashar" -> s.copy(azanAshar = enabled)
                "maghrib" -> s.copy(azanMaghrib = enabled)
                "isya" -> s.copy(azanIsya = enabled)
                else -> s
            }
            AzanNotificationHelper.scheduleDailyAlarms(app, updated)
        }
    }

    fun setAzanPreReminder(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setAzanPreReminder(enabled)
            val updated = settings.value.copy(azanPreReminderEnabled = enabled)
            AzanNotificationHelper.scheduleDailyAlarms(app, updated)
        }
    }

    fun setAzanSoundMode(mode: String) {
        viewModelScope.launch {
            preferences.setAzanSoundMode(mode)
            val updated = settings.value.copy(azanSoundMode = mode)
            AzanNotificationHelper.scheduleDailyAlarms(app, updated)
        }
    }

    fun rescheduleAzanAlarms() {
        AzanNotificationHelper.scheduleDailyAlarms(app, settings.value)
        _locationDetectMessage.value = "Seluruh alarm waktu shalat dijadwalkan ulang"
    }

    fun sendTestAzanNotification() {
        val s = settings.value
        AzanNotificationHelper.showPrayerNotification(
            context = app,
            prayerKey = "test_azan",
            prayerLabel = "Uji Notifikasi Azan",
            prayerTime = "Aktif",
            cityName = s.prayerCityName,
            isPreReminder = false,
            soundMode = s.azanSoundMode
        )
    }

    fun scheduleTestAzanAlarmIn5Seconds() {
        AzanNotificationHelper.scheduleTestAlarmInSeconds(app, settings.value, delaySeconds = 5)
        _locationDetectMessage.value = "Alarm azan simulasi akan berbunyi dalam 5 detik"
    }

    fun setHijriAdjustment(days: Int) {
        viewModelScope.launch { preferences.setHijriAdjustment(days) }
    }

    fun saveAdSenseConfig(
        enabled: Boolean,
        publisherId: String,
        bannerSlotId: String,
        interstitialSlotId: String,
        linkUrl: String,
        showOnHome: Boolean,
        showOnPrayer: Boolean,
        hideDuringReading: Boolean
    ) {
        viewModelScope.launch {
            preferences.updateAdSenseConfig(
                enabled = enabled,
                publisherId = publisherId,
                bannerSlotId = bannerSlotId,
                interstitialSlotId = interstitialSlotId,
                linkUrl = linkUrl,
                showOnHome = showOnHome,
                showOnPrayer = showOnPrayer,
                hideDuringReading = hideDuringReading
            )
        }
    }
}
