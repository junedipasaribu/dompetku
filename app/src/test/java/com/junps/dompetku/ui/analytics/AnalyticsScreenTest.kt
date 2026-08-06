package com.junps.dompetku.ui.analytics

import com.junps.dompetku.domain.model.CategoryExpense
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsScreenTest {
    @Test
    fun formatPercentage_roundsToOneDecimalUsingIndonesianSeparator() {
        assertEquals("33,3%", formatPercentage(1, 3))
        assertEquals("66,7%", formatPercentage(2, 3))
        assertEquals("100,0%", formatPercentage(Long.MAX_VALUE, Long.MAX_VALUE))
    }

    @Test
    fun formatPercentage_returnsZeroForInvalidAmounts() {
        assertEquals("0,0%", formatPercentage(0, 100))
        assertEquals("0,0%", formatPercentage(100, 0))
        assertEquals("0,0%", formatPercentage(-1, 100))
    }

    @Test
    fun chartDescription_containsCategoryPercentages() {
        val expenses = listOf(
            CategoryExpense("food", "Makanan", "restaurant", "#E65100", 75),
            CategoryExpense("transport", "Transportasi", "directions_car", "#1565C0", 25),
        )

        assertEquals(
            "Grafik pengeluaran. Makanan 75,0%; Transportasi 25,0%",
            buildChartDescription(expenses, 100),
        )
    }
}
