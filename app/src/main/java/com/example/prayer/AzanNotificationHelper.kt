package com.example.prayer

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppPreferences
import com.example.data.local.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

data class ScheduledAzanSummary(
    val nextPrayerLabel: String,
    val nextPrayerTime: String,
    val isTomorrow: Boolean,
    val activeAlarmCount: Int,
    val exactAlarmsAllowed: Boolean
)

object AzanNotificationHelper {

    const val CHANNEL_ALARM_ID = "azan_prayer_alarm_v2"
    const val CHANNEL_NOTIF_ID = "azan_prayer_notif_v2"
    const val CHANNEL_VIBRATE_ID = "azan_prayer_vibrate_v2"

    const val EXTRA_PRAYER_KEY = "extra_prayer_key"
    const val EXTRA_PRAYER_LABEL = "extra_prayer_label"
    const val EXTRA_PRAYER_TIME = "extra_prayer_time"
    const val EXTRA_CITY_NAME = "extra_city_name"
    const val EXTRA_IS_PRE_REMINDER = "extra_is_pre_reminder"
    const val EXTRA_SOUND_MODE = "extra_sound_mode"
    const val EXTRA_OPEN_PRAYER_TAB = "extra_open_prayer_tab"
    const val ACTION_STOP_AZAN = "com.example.action.STOP_AZAN"
    private const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    @Volatile
    private var activeRingtone: Ringtone? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val alarmAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM_ID,
                "Azan & Pengingat Shalat (Alarm)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi azan dengan suara alarm saat waktu shalat fardhu tiba."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
                setSound(alarmUri, alarmAttr)
            }

            val notifUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notifAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val notifChannel = NotificationChannel(
                CHANNEL_NOTIF_ID,
                "Pengingat Waktu Shalat (Standar)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat waktu shalat fardhu dengan nada notifikasi standar."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 180, 350)
                setSound(notifUri, notifAttr)
            }

            val vibrateChannel = NotificationChannel(
                CHANNEL_VIBRATE_ID,
                "Pengingat Waktu Shalat (Getar)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat waktu shalat fardhu hanya dengan getaran."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setSound(null, null)
            }

            nm.createNotificationChannel(alarmChannel)
            nm.createNotificationChannel(notifChannel)
            nm.createNotificationChannel(vibrateChannel)
        }
    }

    private fun channelForMode(soundMode: String): String = when (soundMode) {
        "vibrate" -> CHANNEL_VIBRATE_ID
        "notification" -> CHANNEL_NOTIF_ID
        else -> CHANNEL_ALARM_ID
    }

    fun showPrayerNotification(
        context: Context,
        prayerKey: String,
        prayerLabel: String,
        prayerTime: String,
        cityName: String,
        isPreReminder: Boolean = false,
        soundMode: String = "alarm"
    ) {
        ensureNotificationChannel(context)

        val notificationId = 1000 + (if (isPreReminder) "pre_$prayerKey" else prayerKey).hashCode()
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_PRAYER_TAB, true)
        }
        val pendingOpen = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, AzanActionReceiver::class.java).apply {
            action = ACTION_STOP_AZAN
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pendingStop = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isPreReminder) {
            "10 Menit Menuju Shalat $prayerLabel ($prayerTime)"
        } else {
            "Waktu Shalat $prayerLabel telah tiba ($prayerTime)"
        }

        val summaryText = if (isPreReminder) {
            "Wilayah $cityName • Bersiap untuk wudhu dan menunaikan shalat $prayerLabel."
        } else {
            "Wilayah $cityName • Hayya 'alash-shalah, mari tunaikan shalat $prayerLabel."
        }

        val bigText = if (isPreReminder) {
            "10 menit lagi memasuki waktu Shalat $prayerLabel pukul $prayerTime untuk wilayah $cityName. Mari bersiap berwudhu dan menuju tempat shalat."
        } else {
            "Waktu Shalat $prayerLabel pukul $prayerTime untuk wilayah $cityName telah tiba. Mari bergegas menunaikan shalat dan melanjutkan tilawah Al-Qur'an."
        }

        val effectiveMode = if (isPreReminder && soundMode == "alarm") "notification" else soundMode
        val builder = NotificationCompat.Builder(context, channelForMode(effectiveMode))
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(summaryText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(
                if (!isPreReminder && effectiveMode == "alarm") {
                    NotificationCompat.CATEGORY_ALARM
                } else {
                    NotificationCompat.CATEGORY_REMINDER
                }
            )
            .setAutoCancel(true)
            .setContentIntent(pendingOpen)
            .addAction(0, "Buka Jadwal", pendingOpen)

        if (!isPreReminder && effectiveMode == "alarm") {
            builder.addAction(0, "Matikan Suara", pendingStop)
            startAzanAlertTone(context)
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, builder.build())
    }

    fun startAzanAlertTone(context: Context) {
        stopAzanAlertTone()
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ringtone.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                ringtone.play()
                activeRingtone = ringtone
                // Auto-stop after 18 seconds so it does not ring indefinitely
                mainHandler.postDelayed({ stopAzanAlertTone() }, 18_000L)
            }
        }
    }

    fun stopAzanAlertTone() {
        mainHandler.removeCallbacksAndMessages(null)
        runCatching {
            activeRingtone?.let {
                if (it.isPlaying) it.stop()
            }
        }
        activeRingtone = null
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    fun getNextScheduledSummary(context: Context, settings: UserSettings, nowMillis: Long = System.currentTimeMillis()): ScheduledAzanSummary {
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val todayTimes = PrayerTimeCalculator.calculateForDate(
            calendar = now,
            latitude = settings.prayerLat,
            longitude = settings.prayerLon,
            timezoneHours = settings.prayerTimezoneHours
        )
        val toggles = mapOf(
            "subuh" to settings.azanSubuh,
            "dzuhur" to settings.azanDzuhur,
            "ashar" to settings.azanAshar,
            "maghrib" to settings.azanMaghrib,
            "isya" to settings.azanIsya
        )

        val activeCount = toggles.values.count { it }
        val enabledToday = todayTimes.azanEntries.filter { toggles[it.key] == true }
        val nextToday = enabledToday.firstOrNull {
            it.toCalendarForDate(now).timeInMillis > nowMillis + 5_000L
        }

        return if (nextToday != null) {
            ScheduledAzanSummary(
                nextPrayerLabel = nextToday.label,
                nextPrayerTime = nextToday.formatted,
                isTomorrow = false,
                activeAlarmCount = activeCount,
                exactAlarmsAllowed = canScheduleExactAlarms(context)
            )
        } else {
            val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowTimes = PrayerTimeCalculator.calculateForDate(
                calendar = tomorrow,
                latitude = settings.prayerLat,
                longitude = settings.prayerLon,
                timezoneHours = settings.prayerTimezoneHours
            )
            val firstTomorrow = tomorrowTimes.azanEntries.firstOrNull { toggles[it.key] == true }
                ?: tomorrowTimes.subuh
            ScheduledAzanSummary(
                nextPrayerLabel = firstTomorrow.label,
                nextPrayerTime = firstTomorrow.formatted,
                isTomorrow = true,
                activeAlarmCount = activeCount,
                exactAlarmsAllowed = canScheduleExactAlarms(context)
            )
        }
    }

    fun scheduleTestAlarmInSeconds(context: Context, settings: UserSettings, delaySeconds: Int = 5) {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + delaySeconds * 1000L
        val cal = Calendar.getInstance().apply { timeInMillis = triggerAt }
        val formattedTime = String.format(
            Locale.US,
            "%02d:%02d",
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE)
        )
        val intent = Intent(context, AzanAlarmReceiver::class.java).apply {
            putExtra(EXTRA_PRAYER_KEY, "test_scheduled")
            putExtra(EXTRA_PRAYER_LABEL, "Simulasi Azan")
            putExtra(EXTRA_PRAYER_TIME, formattedTime)
            putExtra(EXTRA_CITY_NAME, settings.prayerCityName)
            putExtra(EXTRA_IS_PRE_REMINDER, false)
            putExtra(EXTRA_SOUND_MODE, settings.azanSoundMode)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            799,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleExactOrFallback(alarmManager, canScheduleExactAlarms(context), triggerAt, pi)
    }

    fun scheduleDailyAlarms(context: Context, settings: UserSettings) {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exactAllowed = canScheduleExactAlarms(context)

        val now = Calendar.getInstance()
        val todayTimes = PrayerTimeCalculator.calculateForDate(
            calendar = now,
            latitude = settings.prayerLat,
            longitude = settings.prayerLon,
            timezoneHours = settings.prayerTimezoneHours
        )

        val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowTimes = PrayerTimeCalculator.calculateForDate(
            calendar = tomorrow,
            latitude = settings.prayerLat,
            longitude = settings.prayerLon,
            timezoneHours = settings.prayerTimezoneHours
        )

        val toggles = mapOf(
            "subuh" to settings.azanSubuh,
            "dzuhur" to settings.azanDzuhur,
            "ashar" to settings.azanAshar,
            "maghrib" to settings.azanMaghrib,
            "isya" to settings.azanIsya
        )

        todayTimes.azanEntries.forEachIndexed { idx, entry ->
            val enabled = toggles[entry.key] == true
            val mainRequestCode = 500 + idx
            val preRequestCode = 600 + idx

            // Cancel any previous main & pre-reminder alarms for this slot
            val cancelMainPi = PendingIntent.getBroadcast(
                context,
                mainRequestCode,
                Intent(context, AzanAlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val cancelPrePi = PendingIntent.getBroadcast(
                context,
                preRequestCode,
                Intent(context, AzanAlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(cancelMainPi)
            alarmManager.cancel(cancelPrePi)

            if (!enabled) return@forEachIndexed

            val todayCal = entry.toCalendarForDate(now)
            val targetCal = if (todayCal.timeInMillis > System.currentTimeMillis() + 5_000L) {
                todayCal
            } else {
                val tomEntry = tomorrowTimes.azanEntries[idx]
                tomEntry.toCalendarForDate(tomorrow)
            }

            val formattedPrayerTime = String.format(
                Locale.US,
                "%02d:%02d",
                targetCal.get(Calendar.HOUR_OF_DAY),
                targetCal.get(Calendar.MINUTE)
            )

            val mainIntent = Intent(context, AzanAlarmReceiver::class.java).apply {
                putExtra(EXTRA_PRAYER_KEY, entry.key)
                putExtra(EXTRA_PRAYER_LABEL, entry.label)
                putExtra(EXTRA_PRAYER_TIME, formattedPrayerTime)
                putExtra(EXTRA_CITY_NAME, settings.prayerCityName)
                putExtra(EXTRA_IS_PRE_REMINDER, false)
                putExtra(EXTRA_SOUND_MODE, settings.azanSoundMode)
            }
            val mainPi = PendingIntent.getBroadcast(
                context,
                mainRequestCode,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleExactOrFallback(alarmManager, exactAllowed, targetCal.timeInMillis, mainPi)

            if (settings.azanPreReminderEnabled) {
                val preTriggerAt = targetCal.timeInMillis - 10 * 60_000L
                val actualPreCal = if (preTriggerAt > System.currentTimeMillis() + 5_000L) {
                    preTriggerAt to formattedPrayerTime
                } else {
                    val tomEntry = tomorrowTimes.azanEntries[idx]
                    val tomCal = tomEntry.toCalendarForDate(tomorrow)
                    val tomTime = String.format(
                        Locale.US,
                        "%02d:%02d",
                        tomCal.get(Calendar.HOUR_OF_DAY),
                        tomCal.get(Calendar.MINUTE)
                    )
                    (tomCal.timeInMillis - 10 * 60_000L) to tomTime
                }

                val preIntent = Intent(context, AzanAlarmReceiver::class.java).apply {
                    putExtra(EXTRA_PRAYER_KEY, entry.key)
                    putExtra(EXTRA_PRAYER_LABEL, entry.label)
                    putExtra(EXTRA_PRAYER_TIME, actualPreCal.second)
                    putExtra(EXTRA_CITY_NAME, settings.prayerCityName)
                    putExtra(EXTRA_IS_PRE_REMINDER, true)
                    putExtra(EXTRA_SOUND_MODE, settings.azanSoundMode)
                }
                val prePi = PendingIntent.getBroadcast(
                    context,
                    preRequestCode,
                    preIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                scheduleExactOrFallback(alarmManager, exactAllowed, actualPreCal.first, prePi)
            }
        }
    }

    private fun scheduleExactOrFallback(
        alarmManager: AlarmManager,
        exactAllowed: Boolean,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        if (exactAllowed) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
}

class AzanAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(AzanNotificationHelper.EXTRA_PRAYER_KEY) ?: "shalat"
        val label = intent.getStringExtra(AzanNotificationHelper.EXTRA_PRAYER_LABEL) ?: "Shalat"
        val time = intent.getStringExtra(AzanNotificationHelper.EXTRA_PRAYER_TIME) ?: ""
        val city = intent.getStringExtra(AzanNotificationHelper.EXTRA_CITY_NAME) ?: "Lokasi Anda"
        val isPreReminder = intent.getBooleanExtra(AzanNotificationHelper.EXTRA_IS_PRE_REMINDER, false)
        val soundMode = intent.getStringExtra(AzanNotificationHelper.EXTRA_SOUND_MODE) ?: "alarm"

        AzanNotificationHelper.showPrayerNotification(
            context = context,
            prayerKey = key,
            prayerLabel = label,
            prayerTime = time,
            cityName = city,
            isPreReminder = isPreReminder,
            soundMode = soundMode
        )

        if (key != "test_scheduled") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settings = AppPreferences(context).settingsFlow.first()
                    AzanNotificationHelper.scheduleDailyAlarms(context, settings)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

class AzanActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AzanNotificationHelper.ACTION_STOP_AZAN) {
            AzanNotificationHelper.stopAzanAlertTone()
            val notifId = intent.getIntExtra("extra_notification_id", -1)
            if (notifId != -1) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(notifId)
            }
        }
    }
}

class BootRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settings = AppPreferences(context).settingsFlow.first()
                    AzanNotificationHelper.scheduleDailyAlarms(context, settings)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
