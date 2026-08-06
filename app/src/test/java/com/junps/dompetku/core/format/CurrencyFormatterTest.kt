package com.junps.dompetku.core.format

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {
    @Test
    fun formatRupiah_usesIndonesianCurrencyWithoutDecimalFraction() {
        assertEquals("Rp1.250.000", formatRupiah(1_250_000))
    }

    @Test
    fun amountInput_formatsDigitsAndCanBeParsedBack() {
        assertEquals("15.000", formatAmountInput("15000"))
        assertEquals("1.250.000", formatAmountInput("1.250.000"))
        assertEquals(1_250_000L, parseAmountInput("1.250.000"))
        assertEquals("", formatAmountInput("000"))
    }
}
