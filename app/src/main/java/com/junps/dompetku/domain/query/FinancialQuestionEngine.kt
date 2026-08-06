package com.junps.dompetku.domain.query

import com.junps.dompetku.core.format.formatRupiah
import com.junps.dompetku.domain.model.TransactionType
import com.junps.dompetku.domain.model.TransactionWithCategory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FinancialQuestionEngine {
    private val locale = Locale.forLanguageTag("id-ID")

    fun answer(
        question: String,
        transactions: List<TransactionWithCategory>,
        now: Long = System.currentTimeMillis(),
    ): String {
        val normalized = question.lowercase(locale).trim()
        if (normalized.isBlank()) return "Tulis pertanyaan tentang keuanganmu terlebih dahulu."

        val range = periodRange(normalized, now)
        val periodLabel = range.label
        val inPeriod = transactions.filter { it.transactionDate in range.start until range.end }
        val asksIncome = normalized.contains("pemasukan") || normalized.contains("pendapatan")
        val asksExpense = normalized.contains("pengeluaran") || normalized.contains("belanja")
        val asksLargest = listOf("terbesar", "tertinggi", "paling besar").any(normalized::contains)
        val asksCategory = normalized.contains("kategori") || normalized.contains("jenis")
        val asksComparison = normalized.contains("banding") ||
            (asksIncome && asksExpense)

        if (asksComparison) {
            val income = inPeriod.filter { it.categoryType == TransactionType.INCOME }.sumOf { it.amount }
            val expense = inPeriod.filter { it.categoryType == TransactionType.EXPENSE }.sumOf { it.amount }
            val difference = income - expense
            return "Pada $periodLabel, pemasukanmu ${formatRupiah(income)} dan pengeluaranmu " +
                "${formatRupiah(expense)}. Selisihnya ${formatRupiah(kotlin.math.abs(difference))} " +
                if (difference >= 0) "lebih banyak pemasukan." else "lebih banyak pengeluaran."
        }

        val type = if (asksIncome) TransactionType.INCOME else TransactionType.EXPENSE
        val typed = inPeriod.filter { it.categoryType == type }
        val typeLabel = if (type == TransactionType.INCOME) "pemasukan" else "pengeluaran"

        if (asksLargest && asksCategory) {
            val largest = typed.groupBy { it.categoryName }
                .mapValues { (_, values) -> values.sumOf { it.amount } }
                .maxByOrNull { it.value }
                ?: return "Belum ada $typeLabel pada $periodLabel."
            return "Kategori $typeLabel terbesar pada $periodLabel adalah ${largest.key}, " +
                "dengan total ${formatRupiah(largest.value)}."
        }

        if (asksLargest) {
            val largest = typed.maxByOrNull { it.amount }
                ?: return "Belum ada $typeLabel pada $periodLabel."
            val time = SimpleDateFormat("dd MMM yyyy, HH:mm", locale).format(Date(largest.transactionDate))
            return "$typeLabel terbesar pada $periodLabel adalah ${formatRupiah(largest.amount)} " +
                "untuk ${largest.categoryName} ($time)."
        }

        if (asksIncome || asksExpense || normalized.contains("total")) {
            val mentionedCategory = transactions.map { it.categoryName }.distinct()
                .firstOrNull { normalized.contains(it.lowercase(locale)) }
            val filtered = if (mentionedCategory == null) typed else typed.filter { it.categoryName == mentionedCategory }
            val categoryText = mentionedCategory?.let { " kategori $it" }.orEmpty()
            return "Total $typeLabel$categoryText pada $periodLabel adalah ${formatRupiah(filtered.sumOf { it.amount })}."
        }

        return "Saya belum memahami pertanyaan itu. Coba: “Apa pengeluaran terbesar hari ini?”, " +
            "“Kategori pengeluaran terbesar bulan ini?”, atau “Bandingkan pemasukan dan pengeluaran minggu ini?”."
    }

    private fun periodRange(question: String, now: Long): QueryPeriod {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        return when {
            question.contains("bulan lalu") -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                startOfDay(calendar)
                val end = calendar.timeInMillis
                calendar.add(Calendar.MONTH, -1)
                QueryPeriod(calendar.timeInMillis, end, "bulan lalu")
            }
            question.contains("minggu") || question.contains("pekan") -> {
                calendar.firstDayOfWeek = Calendar.MONDAY
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                startOfDay(calendar)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, 7)
                QueryPeriod(start, calendar.timeInMillis, "minggu ini")
            }
            question.contains("hari ini") || question.contains("hari") -> {
                startOfDay(calendar)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                QueryPeriod(start, calendar.timeInMillis, "hari ini")
            }
            else -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                startOfDay(calendar)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                QueryPeriod(start, calendar.timeInMillis, "bulan ini")
            }
        }
    }

    private fun startOfDay(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    private data class QueryPeriod(val start: Long, val end: Long, val label: String)
}
