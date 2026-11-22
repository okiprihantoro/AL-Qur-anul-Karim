package com.example.prayer

import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

data class DailyPrayerTimes(
    val imsak: PrayerTimeEntry,
    val subuh: PrayerTimeEntry,
    val terbit: PrayerTimeEntry,
    val dzuhur: PrayerTimeEntry,
    val ashar: PrayerTimeEntry,
    val maghrib: PrayerTimeEntry,
    val isya: PrayerTimeEntry
) {
    val allEntries: List<PrayerTimeEntry>
        get() = listOf(imsak, subuh, terbit, dzuhur, ashar, maghrib, isya)

    val azanEntries: List<PrayerTimeEntry>
        get() = listOf(subuh, dzuhur, ashar, maghrib, isya)
}

data class PrayerTimeEntry(
    val key: String,
    val label: String,
    val hour: Int,
    val minute: Int,
    val isObligatory: Boolean
) {
    val formatted: String
        get() = String.format(Locale.US, "%02d:%02d", hour, minute)

    fun toCalendarForDate(base: Calendar): Calendar {
        return (base.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}

/**
 * Pure Kotlin Kemenag RI astronomical prayer time & Qibla calculator.
 * Parameters: Fajr 20.0°, Isha 18.0°, Shafi'i Asr (shadow factor = 1),
 * standard Ihtiyat (+2 minutes safety margin on obligatory prayers).
 */
object PrayerTimeCalculator {

    private const val FAJR_ANGLE = 20.0
    private const val ISHA_ANGLE = 18.0
    private const val SUNRISE_ANGLE = 0.8333
    private const val KAABA_LAT = 21.422487
    private const val KAABA_LON = 39.826206

    fun calculateForDate(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        timezoneHours: Double
    ): DailyPrayerTimes {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val jd = julianDate(year, month, day) - longitude / (15.0 * 24.0)

        var fajr = 5.0
        var sunrise = 6.0
        var dhuhr = 12.0
        var asr = 15.0
        var sunset = 18.0
        var isha = 19.0

        // Two-pass solar refinement for sub-minute accuracy
        repeat(2) {
            dhuhr = computeMidDay(jd + dhuhr / 24.0)
            fajr = computeSunAngleTime(jd + fajr / 24.0, FAJR_ANGLE, latitude, dhuhr, isBeforeNoon = true)
            sunrise = computeSunAngleTime(jd + sunrise / 24.0, SUNRISE_ANGLE, latitude, dhuhr, isBeforeNoon = true)
            asr = computeAsrTime(jd + asr / 24.0, 1.0, latitude, dhuhr)
            sunset = computeSunAngleTime(jd + sunset / 24.0, SUNRISE_ANGLE, latitude, dhuhr, isBeforeNoon = false)
            isha = computeSunAngleTime(jd + isha / 24.0, ISHA_ANGLE, latitude, dhuhr, isBeforeNoon = false)
        }

        val offset = timezoneHours - longitude / 15.0

        val subuhMin = toMinutes(fajr + offset) + 2
        val imsakMin = subuhMin - 10
        val terbitMin = toMinutes(sunrise + offset)
        val dzuhurMin = toMinutes(dhuhr + offset) + 2
        val asharMin = toMinutes(asr + offset) + 2
        val maghribMin = toMinutes(sunset + offset) + 2
        val isyaMin = toMinutes(isha + offset) + 2

        return DailyPrayerTimes(
            imsak = entry("imsak", "Imsak", imsakMin, isObligatory = false),
            subuh = entry("subuh", "Subuh", subuhMin, isObligatory = true),
            terbit = entry("terbit", "Terbit", terbitMin, isObligatory = false),
            dzuhur = entry("dzuhur", "Dzuhur", dzuhurMin, isObligatory = true),
            ashar = entry("ashar", "Ashar", asharMin, isObligatory = true),
            maghrib = entry("maghrib", "Maghrib", maghribMin, isObligatory = true),
            isya = entry("isya", "Isya", isyaMin, isObligatory = true)
        )
    }

    fun calculateQiblaBearing(latitude: Double, longitude: Double): Double {
        val phiK = Math.toRadians(KAABA_LAT)
        val lambdaK = Math.toRadians(KAABA_LON)
        val phi = Math.toRadians(latitude)
        val lambda = Math.toRadians(longitude)
        val deltaL = lambdaK - lambda
        val y = sin(deltaL)
        val x = cos(phi) * tan(phiK) - sin(phi) * cos(deltaL)
        val bearing = Math.toDegrees(atan2(y, x))
        return (bearing + 360.0) % 360.0
    }

    private fun entry(key: String, label: String, totalMinutes: Int, isObligatory: Boolean): PrayerTimeEntry {
        val wrapped = ((totalMinutes % 1440) + 1440) % 1440
        return PrayerTimeEntry(
            key = key,
            label = label,
            hour = wrapped / 60,
            minute = wrapped % 60,
            isObligatory = isObligatory
        )
    }

    private fun toMinutes(hours: Double): Int {
        return kotlin.math.round(hours * 60.0).toInt()
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15.0
        val eqt = q / 15.0 - fixHour(ra)
        val decl = darcsin(dsin(e) * dsin(l))
        return decl to eqt
    }

    private fun computeMidDay(jd: Double): Double {
        val (_, eqt) = sunPosition(jd)
        return fixHour(12.0 - eqt)
    }

    private fun computeSunAngleTime(
        jd: Double,
        angle: Double,
        latitude: Double,
        midDay: Double,
        isBeforeNoon: Boolean
    ): Double {
        val (decl, _) = sunPosition(jd)
        val numerator = -dsin(angle) - dsin(decl) * dsin(latitude)
        val denominator = dcos(decl) * dcos(latitude)
        val cosT = (numerator / denominator).coerceIn(-1.0, 1.0)
        val t = darccos(cosT) / 15.0
        return midDay + if (isBeforeNoon) -t else t
    }

    private fun computeAsrTime(
        jd: Double,
        factor: Double,
        latitude: Double,
        midDay: Double
    ): Double {
        val (decl, _) = sunPosition(jd)
        val angle = -darccot(factor + dtan(abs(latitude - decl)))
        return computeSunAngleTime(jd, angle, latitude, midDay, isBeforeNoon = false)
    }

    private fun dsin(d: Double) = sin(d * PI / 180.0)
    private fun dcos(d: Double) = cos(d * PI / 180.0)
    private fun dtan(d: Double) = tan(d * PI / 180.0)
    private fun darcsin(x: Double) = asin(x.coerceIn(-1.0, 1.0)) * 180.0 / PI
    private fun darccos(x: Double) = acos(x.coerceIn(-1.0, 1.0)) * 180.0 / PI
    private fun darccot(x: Double) = atan(1.0 / x) * 180.0 / PI
    private fun darctan2(y: Double, x: Double) = atan2(y, x) * 180.0 / PI

    private fun fixAngle(a: Double): Double {
        val r = a - 360.0 * floor(a / 360.0)
        return if (r < 0) r + 360.0 else r
    }

    private fun fixHour(a: Double): Double {
        val r = a - 24.0 * floor(a / 24.0)
        return if (r < 0) r + 24.0 else r
    }
}
