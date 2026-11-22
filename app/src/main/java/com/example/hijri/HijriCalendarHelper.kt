package com.example.hijri

import java.util.Calendar
import kotlin.math.floor

data class HijriDate(
    val day: Int,
    val month: Int, // 1..12
    val year: Int,
    val monthName: String
) {
    val formatted: String
        get() = "$day $monthName $year H"
}

data class IslamicEvent(
    val hijriMonth: Int,
    val hijriDay: Int,
    val title: String,
    val category: String
)

object HijriCalendarHelper {

    val hijriMonthNames = listOf(
        "Muharram",
        "Shafar",
        "Rabi'ul Awal",
        "Rabi'ul Akhir",
        "Jumadil Awal",
        "Jumadil Akhir",
        "Rajab",
        "Sya'ban",
        "Ramadhan",
        "Syawal",
        "Dzulqa'dah",
        "Dzulhijjah"
    )

    val indonesianMonthNames = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    val indonesianDayNames = listOf(
        "Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jum'at", "Sabtu"
    )

    val importantIslamicEvents = listOf(
        IslamicEvent(1, 1, "Tahun Baru Islam 1 Muharram", "Hari Besar"),
        IslamicEvent(1, 9, "Puasa Sunnah Tasu'a", "Puasa Sunnah"),
        IslamicEvent(1, 10, "Puasa Sunnah Asyura", "Puasa Sunnah"),
        IslamicEvent(3, 12, "Maulid Nabi Muhammad SAW", "Hari Besar"),
        IslamicEvent(7, 27, "Isra Mi'raj Nabi Muhammad SAW", "Hari Besar"),
        IslamicEvent(8, 15, "Nisfu Sya'ban", "Momen Mulia"),
        IslamicEvent(9, 1, "Awal Ramadhan (Puasa Wajib)", "Ramadhan"),
        IslamicEvent(9, 17, "Nuzulul Qur'an", "Ramadhan"),
        IslamicEvent(10, 1, "Hari Raya Idul Fitri 1 Syawal", "Hari Raya"),
        IslamicEvent(10, 2, "Awal Puasa Sunnah Syawal (6 Hari)", "Puasa Sunnah"),
        IslamicEvent(12, 9, "Puasa Sunnah Arafah", "Puasa Sunnah"),
        IslamicEvent(12, 10, "Hari Raya Idul Adha", "Hari Raya"),
        IslamicEvent(12, 11, "Hari Tasyrik (Haram Berpuasa)", "Peringatan"),
        IslamicEvent(12, 12, "Hari Tasyrik (Haram Berpuasa)", "Peringatan"),
        IslamicEvent(12, 13, "Hari Tasyrik (Haram Berpuasa)", "Peringatan")
    )

    fun fromGregorian(calendar: Calendar, adjustmentDays: Int = 0): HijriDate {
        val adjCal = (calendar.clone() as Calendar).apply {
            if (adjustmentDays != 0) add(Calendar.DAY_OF_MONTH, adjustmentDays)
        }
        val y = adjCal.get(Calendar.YEAR)
        val m = adjCal.get(Calendar.MONTH) + 1
        val d = adjCal.get(Calendar.DAY_OF_MONTH)

        val jd = gregorianToJulianDay(y, m, d)
        return julianDayToHijri(jd)
    }

    fun getEventsForDate(calendar: Calendar, hijri: HijriDate): List<IslamicEvent> {
        val list = mutableListOf<IslamicEvent>()
        list.addAll(importantIslamicEvents.filter { it.hijriMonth == hijri.month && it.hijriDay == hijri.day })

        // Ayyamul Bidh (13, 14, 15 except 13 Dzulhijjah which is Tasyrik)
        if (hijri.day in 13..15 && !(hijri.month == 12 && hijri.day == 13)) {
            list.add(IslamicEvent(hijri.month, hijri.day, "Puasa Sunnah Ayyamul Bidh", "Puasa Sunnah"))
        }

        // Senin - Kamis sunnah (outside Ramadan & Eid / Tasyrik)
        val dow = calendar.get(Calendar.DAY_OF_WEEK)
        val isForbiddenDay = (hijri.month == 10 && hijri.day == 1) ||
            (hijri.month == 12 && hijri.day in 10..13)
        if (!isForbiddenDay && hijri.month != 9) {
            if (dow == Calendar.MONDAY) {
                list.add(IslamicEvent(hijri.month, hijri.day, "Puasa Sunnah Hari Senin", "Puasa Sunnah"))
            } else if (dow == Calendar.THURSDAY) {
                list.add(IslamicEvent(hijri.month, hijri.day, "Puasa Sunnah Hari Kamis", "Puasa Sunnah"))
            }
        }
        return list
    }

    fun formatGregorianIndonesian(calendar: Calendar): String {
        val dayName = indonesianDayNames[calendar.get(Calendar.DAY_OF_WEEK) - 1]
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = indonesianMonthNames[calendar.get(Calendar.MONTH)]
        val year = calendar.get(Calendar.YEAR)
        return "$dayName, $day $month $year"
    }

    private fun gregorianToJulianDay(year: Int, month: Int, day: Int): Int {
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
    }

    private fun julianDayToHijri(jd: Int): HijriDate {
        // Kuwaiti / Astronomical Tabular Islamic Calendar conversion
        val l0 = jd - 1948440 + 10632
        val n = floor((l0 - 1) / 10631.0).toInt()
        val l1 = l0 - 10631 * n + 354
        val j = (floor((10985 - l1) / 5316.0) * floor((50 * l1) / 17719.0) +
            floor(l1 / 5670.0) * floor((43 * l1) / 15238.0)).toInt()
        val l2 = l1 - (floor((30 - j) / 15.0) * floor((17719 * j) / 50.0) +
            floor(j / 16.0) * floor((15238 * j) / 43.0)).toInt() + 29
        val month = floor((24 * l2) / 709.0).toInt().coerceIn(1, 12)
        val day = (l2 - floor((709 * month) / 24.0).toInt()).coerceIn(1, 30)
        val year = 30 * n + j - 30

        return HijriDate(
            day = day,
            month = month,
            year = year,
            monthName = hijriMonthNames[month - 1]
        )
    }
}
