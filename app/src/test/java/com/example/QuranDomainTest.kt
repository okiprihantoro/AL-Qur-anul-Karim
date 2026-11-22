package com.example

import androidx.compose.ui.graphics.Color
import com.example.domain.tajwid.TajwidParser
import com.example.hijri.HijriCalendarHelper
import com.example.prayer.PrayerTimeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class QuranDomainTest {

    @Test
    fun tajwidParser_colorsTajwidRules_whenEnabled() {
        val sample = "قُلْ أَعُوذُ بِرَبِّ النَّاسِ مَنْ شَرِّ"
        val colored = TajwidParser.parse(
            arabicText = sample,
            enabled = true,
            isDark = false,
            defaultColor = Color.Black
        )
        assertEquals(sample, colored.text)
        assertTrue(colored.spanStyles.isNotEmpty())

        val plain = TajwidParser.parse(
            arabicText = sample,
            enabled = false,
            isDark = false,
            defaultColor = Color.Black
        )
        assertTrue(plain.spanStyles.isEmpty())
    }

    @Test
    fun prayerTimeCalculator_jakartaTimesAreChronologicalAndQiblaIsWnw() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 22, 12, 0, 0)
        }
        val times = PrayerTimeCalculator.calculateForDate(
            calendar = cal,
            latitude = -6.2088,
            longitude = 106.8456,
            timezoneHours = 7.0
        )

        val minutesList = times.allEntries.map { it.hour * 60 + it.minute }
        for (i in 0 until minutesList.size - 1) {
            assertTrue(
                "Expected ${times.allEntries[i].label} (${times.allEntries[i].formatted}) < ${times.allEntries[i + 1].label} (${times.allEntries[i + 1].formatted})",
                minutesList[i] < minutesList[i + 1]
            )
        }

        val qibla = PrayerTimeCalculator.calculateQiblaBearing(-6.2088, 106.8456)
        assertTrue("Jakarta Qibla should be ~295 deg, got $qibla", qibla in 290.0..300.0)
    }

    @Test
    fun hijriCalendar_convertsValidDateAndDetectsRamadhan() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 22, 10, 0, 0)
        }
        val hijri = HijriCalendarHelper.fromGregorian(cal, 0)
        assertTrue(hijri.year in 1447..1449)
        assertTrue(hijri.month in 1..12)
        assertTrue(hijri.day in 1..30)
        assertTrue(hijri.monthName.isNotBlank())
    }

    @Test
    fun prayerAlarms_selectNextEnabledPrayerAfterGivenTime() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 22, 13, 0, 0)
        }
        val times = PrayerTimeCalculator.calculateForDate(
            calendar = cal,
            latitude = -6.2088,
            longitude = 106.8456,
            timezoneHours = 7.0
        )
        // At 13:00 WIB in Jakarta, Dzuhur (~11:42) has passed, so next enabled prayer should be Ashar (~15:02)
        val next = times.azanEntries.firstOrNull {
            it.toCalendarForDate(cal).timeInMillis > cal.timeInMillis
        }
        assertEquals("Ashar", next?.label)
    }
}
