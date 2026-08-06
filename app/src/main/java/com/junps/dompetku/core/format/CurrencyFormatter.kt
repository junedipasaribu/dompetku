package com.junps.dompetku.core.format

import java.text.NumberFormat
import java.util.Locale

private val indonesianLocale = Locale.forLanguageTag("id-ID")

fun formatRupiah(value: Long): String =
    NumberFormat.getCurrencyInstance(indonesianLocale)
        .format(value)
        .replace(",00", "")

fun formatAmountInput(value: String): String {
    val digits = value.filter(Char::isDigit).trimStart('0')
    if (digits.isEmpty()) return ""
    return NumberFormat.getIntegerInstance(indonesianLocale).format(digits.toLong())
}

fun parseAmountInput(value: String): Long? =
    value.filter(Char::isDigit).takeIf(String::isNotEmpty)?.toLongOrNull()
