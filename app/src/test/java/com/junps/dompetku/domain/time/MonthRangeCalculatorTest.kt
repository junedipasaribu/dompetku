package com.junps.dompetku.domain.time

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthRangeCalculatorTest {
    private val calculator = MonthRangeCalculator()

    @Test
    fun containing_returnsLocalMonthBoundariesForJakarta() {
        val zone = TimeZone.getTimeZone("Asia/Jakarta")
        val timestamp = calendar(zone, 2026, Calendar.AUGUST, 15, 13, 45).timeInMillis

        val range = calculator.containing(timestamp, zone)

        assertEquals(calendar(zone, 2026, Calendar.AUGUST, 1, 0, 0).timeInMillis, range.startInclusive)
        assertEquals(calendar(zone, 2026, Calendar.SEPTEMBER, 1, 0, 0).timeInMillis, range.endExclusive)
    }

    @Test
    fun containing_respectsDaylightSavingBoundaries() {
        val zone = TimeZone.getTimeZone("America/New_York")
        val timestamp = calendar(zone, 2026, Calendar.MARCH, 20, 12, 0).timeInMillis

        val range = calculator.containing(timestamp, zone)

        assertEquals(calendar(zone, 2026, Calendar.MARCH, 1, 0, 0).timeInMillis, range.startInclusive)
        assertEquals(calendar(zone, 2026, Calendar.APRIL, 1, 0, 0).timeInMillis, range.endExclusive)
    }

    private fun calendar(
        zone: TimeZone,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Calendar = Calendar.getInstance(zone).apply {
        clear()
        set(year, month, day, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
