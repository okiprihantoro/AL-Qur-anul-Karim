package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quran_settings")

data class UserSettings(
    val darkModeSetting: String = "system", // "system", "light", "dark"
    val tajwidEnabled: Boolean = true,
    val showTransliteration: Boolean = true,
    val showTranslation: Boolean = true,
    val arabicFontSize: Float = 28f,
    val selectedQariId: String = "01",
    val lastReadSurah: Int = 1,
    val lastReadSurahName: String = "Al-Fatihah",
    val lastReadAyah: Int = 1,
    val lastReadTimestamp: Long = 0L,
    val prayerCityName: String = "Jakarta, Indonesia",
    val prayerLat: Double = -6.2088,
    val prayerLon: Double = 106.8456,
    val prayerTimezoneHours: Double = 7.0,
    val azanSubuh: Boolean = true,
    val azanDzuhur: Boolean = true,
    val azanAshar: Boolean = true,
    val azanMaghrib: Boolean = true,
    val azanIsya: Boolean = true,
    val azanPreReminderEnabled: Boolean = false,
    val azanSoundMode: String = "alarm", // "alarm", "notification", "vibrate"
    val hijriAdjustmentDays: Int = 0,
    val adsenseEnabled: Boolean = false,
    val adsensePublisherId: String = "",
    val adsenseBannerSlotId: String = "",
    val adsenseInterstitialSlotId: String = "",
    val adsenseLinkUrl: String = "",
    val adsenseShowOnHome: Boolean = true,
    val adsenseShowOnPrayer: Boolean = true,
    val adsenseHideDuringReading: Boolean = true
)

class AppPreferences(private val context: Context) {

    private object Keys {
        val DARK_MODE = stringPreferencesKey("dark_mode_setting")
        val TAJWID_ENABLED = booleanPreferencesKey("tajwid_enabled")
        val SHOW_TRANSLITERATION = booleanPreferencesKey("show_transliteration")
        val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
        val ARABIC_FONT_SIZE = floatPreferencesKey("arabic_font_size")
        val SELECTED_QARI = stringPreferencesKey("selected_qari_id")

        val LAST_READ_SURAH = intPreferencesKey("last_read_surah")
        val LAST_READ_SURAH_NAME = stringPreferencesKey("last_read_surah_name")
        val LAST_READ_AYAH = intPreferencesKey("last_read_ayah")
        val LAST_READ_TIMESTAMP = longPreferencesKey("last_read_timestamp")

        val PRAYER_CITY = stringPreferencesKey("prayer_city_name")
        val PRAYER_LAT = doublePreferencesKey("prayer_lat")
        val PRAYER_LON = doublePreferencesKey("prayer_lon")
        val PRAYER_TZ = doublePreferencesKey("prayer_tz")

        val AZAN_SUBUH = booleanPreferencesKey("azan_subuh")
        val AZAN_DZUHUR = booleanPreferencesKey("azan_dzuhur")
        val AZAN_ASHAR = booleanPreferencesKey("azan_ashar")
        val AZAN_MAGHRIB = booleanPreferencesKey("azan_maghrib")
        val AZAN_ISYA = booleanPreferencesKey("azan_isya")
        val AZAN_PRE_REMINDER = booleanPreferencesKey("azan_pre_reminder")
        val AZAN_SOUND_MODE = stringPreferencesKey("azan_sound_mode")

        val HIJRI_ADJUST = intPreferencesKey("hijri_adjustment_days")

        val ADSENSE_ENABLED = booleanPreferencesKey("adsense_enabled")
        val ADSENSE_PUB_ID = stringPreferencesKey("adsense_pub_id")
        val ADSENSE_BANNER_SLOT = stringPreferencesKey("adsense_banner_slot")
        val ADSENSE_INTERSTITIAL_SLOT = stringPreferencesKey("adsense_interstitial_slot")
        val ADSENSE_LINK_URL = stringPreferencesKey("adsense_link_url")
        val ADSENSE_SHOW_HOME = booleanPreferencesKey("adsense_show_home")
        val ADSENSE_SHOW_PRAYER = booleanPreferencesKey("adsense_show_prayer")
        val ADSENSE_HIDE_READING = booleanPreferencesKey("adsense_hide_reading")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            darkModeSetting = prefs[Keys.DARK_MODE] ?: "system",
            tajwidEnabled = prefs[Keys.TAJWID_ENABLED] ?: true,
            showTransliteration = prefs[Keys.SHOW_TRANSLITERATION] ?: true,
            showTranslation = prefs[Keys.SHOW_TRANSLATION] ?: true,
            arabicFontSize = prefs[Keys.ARABIC_FONT_SIZE] ?: 28f,
            selectedQariId = prefs[Keys.SELECTED_QARI] ?: "01",
            lastReadSurah = prefs[Keys.LAST_READ_SURAH] ?: 1,
            lastReadSurahName = prefs[Keys.LAST_READ_SURAH_NAME] ?: "Al-Fatihah",
            lastReadAyah = prefs[Keys.LAST_READ_AYAH] ?: 1,
            lastReadTimestamp = prefs[Keys.LAST_READ_TIMESTAMP] ?: 0L,
            prayerCityName = prefs[Keys.PRAYER_CITY] ?: "Jakarta, Indonesia",
            prayerLat = prefs[Keys.PRAYER_LAT] ?: -6.2088,
            prayerLon = prefs[Keys.PRAYER_LON] ?: 106.8456,
            prayerTimezoneHours = prefs[Keys.PRAYER_TZ] ?: 7.0,
            azanSubuh = prefs[Keys.AZAN_SUBUH] ?: true,
            azanDzuhur = prefs[Keys.AZAN_DZUHUR] ?: true,
            azanAshar = prefs[Keys.AZAN_ASHAR] ?: true,
            azanMaghrib = prefs[Keys.AZAN_MAGHRIB] ?: true,
            azanIsya = prefs[Keys.AZAN_ISYA] ?: true,
            azanPreReminderEnabled = prefs[Keys.AZAN_PRE_REMINDER] ?: false,
            azanSoundMode = prefs[Keys.AZAN_SOUND_MODE] ?: "alarm",
            hijriAdjustmentDays = prefs[Keys.HIJRI_ADJUST] ?: 0,
            adsenseEnabled = prefs[Keys.ADSENSE_ENABLED] ?: false,
            adsensePublisherId = prefs[Keys.ADSENSE_PUB_ID] ?: "",
            adsenseBannerSlotId = prefs[Keys.ADSENSE_BANNER_SLOT] ?: "",
            adsenseInterstitialSlotId = prefs[Keys.ADSENSE_INTERSTITIAL_SLOT] ?: "",
            adsenseLinkUrl = prefs[Keys.ADSENSE_LINK_URL] ?: "",
            adsenseShowOnHome = prefs[Keys.ADSENSE_SHOW_HOME] ?: true,
            adsenseShowOnPrayer = prefs[Keys.ADSENSE_SHOW_PRAYER] ?: true,
            adsenseHideDuringReading = prefs[Keys.ADSENSE_HIDE_READING] ?: true
        )
    }

    suspend fun setDarkMode(mode: String) {
        context.dataStore.edit { it[Keys.DARK_MODE] = mode }
    }

    suspend fun setTajwidEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TAJWID_ENABLED] = enabled }
    }

    suspend fun setShowTransliteration(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_TRANSLITERATION] = show }
    }

    suspend fun setShowTranslation(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_TRANSLATION] = show }
    }

    suspend fun setArabicFontSize(size: Float) {
        context.dataStore.edit { it[Keys.ARABIC_FONT_SIZE] = size.coerceIn(20f, 44f) }
    }

    suspend fun setSelectedQari(qariId: String) {
        context.dataStore.edit { it[Keys.SELECTED_QARI] = qariId }
    }

    suspend fun updateLastRead(surahNomor: Int, surahName: String, ayahNomor: Int) {
        context.dataStore.edit {
            it[Keys.LAST_READ_SURAH] = surahNomor
            it[Keys.LAST_READ_SURAH_NAME] = surahName
            it[Keys.LAST_READ_AYAH] = ayahNomor
            it[Keys.LAST_READ_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun updatePrayerLocation(cityName: String, lat: Double, lon: Double, timezoneHours: Double) {
        context.dataStore.edit {
            it[Keys.PRAYER_CITY] = cityName
            it[Keys.PRAYER_LAT] = lat
            it[Keys.PRAYER_LON] = lon
            it[Keys.PRAYER_TZ] = timezoneHours
        }
    }

    suspend fun setAzanToggle(prayerKey: String, enabled: Boolean) {
        context.dataStore.edit {
            when (prayerKey) {
                "subuh" -> it[Keys.AZAN_SUBUH] = enabled
                "dzuhur" -> it[Keys.AZAN_DZUHUR] = enabled
                "ashar" -> it[Keys.AZAN_ASHAR] = enabled
                "maghrib" -> it[Keys.AZAN_MAGHRIB] = enabled
                "isya" -> it[Keys.AZAN_ISYA] = enabled
            }
        }
    }

    suspend fun setAzanPreReminder(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AZAN_PRE_REMINDER] = enabled }
    }

    suspend fun setAzanSoundMode(mode: String) {
        context.dataStore.edit { it[Keys.AZAN_SOUND_MODE] = mode }
    }

    suspend fun setHijriAdjustment(days: Int) {
        context.dataStore.edit { it[Keys.HIJRI_ADJUST] = days.coerceIn(-2, 2) }
    }

    suspend fun updateAdSenseConfig(
        enabled: Boolean,
        publisherId: String,
        bannerSlotId: String,
        interstitialSlotId: String,
        linkUrl: String,
        showOnHome: Boolean,
        showOnPrayer: Boolean,
        hideDuringReading: Boolean
    ) {
        context.dataStore.edit {
            it[Keys.ADSENSE_ENABLED] = enabled
            it[Keys.ADSENSE_PUB_ID] = publisherId.trim()
            it[Keys.ADSENSE_BANNER_SLOT] = bannerSlotId.trim()
            it[Keys.ADSENSE_INTERSTITIAL_SLOT] = interstitialSlotId.trim()
            it[Keys.ADSENSE_LINK_URL] = linkUrl.trim()
            it[Keys.ADSENSE_SHOW_HOME] = showOnHome
            it[Keys.ADSENSE_SHOW_PRAYER] = showOnPrayer
            it[Keys.ADSENSE_HIDE_READING] = hideDuringReading
        }
    }
}
