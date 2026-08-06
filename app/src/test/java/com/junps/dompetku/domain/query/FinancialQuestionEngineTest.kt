package com.junps.dompetku.domain.query

import com.junps.dompetku.domain.model.TransactionType
import com.junps.dompetku.domain.model.TransactionWithCategory
import java.util.Calendar
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialQuestionEngineTest {
    private val now = timestamp(2026, Calendar.AUGUST, 5, 14)
    private val transactions = listOf(
        transaction("food", 75_000, TransactionType.EXPENSE, "Makanan", timestamp(2026, Calendar.AUGUST, 5, 9)),
        transaction("shop", 125_000, TransactionType.EXPENSE, "Belanja", timestamp(2026, Calendar.AUGUST, 5, 13)),
        transaction("salary", 1_000_000, TransactionType.INCOME, "Gaji", timestamp(2026, Calendar.AUGUST, 4, 8)),
        transaction("old", 900_000, TransactionType.EXPENSE, "Tagihan", timestamp(2026, Calendar.JULY, 12, 8)),
    )

    @Test
    fun answersLargestExpenseToday() {
        val answer = FinancialQuestionEngine.answer(
            "Apa pengeluaran terbesar saya hari ini?",
            transactions,
            now,
        )

        assertTrue(answer.contains("Rp125.000"))
        assertTrue(answer.contains("Belanja"))
    }

    @Test
    fun answersLargestExpenseCategoryThisMonth() {
        val answer = FinancialQuestionEngine.answer(
            "Apa jenis pengeluaran terbesar bulan ini?",
            transactions,
            now,
        )

        assertTrue(answer.contains("Belanja"))
        assertTrue(answer.contains("Rp125.000"))
    }

    @Test
    fun comparesIncomeAndExpenseThisMonth() {
        val answer = FinancialQuestionEngine.answer(
            "Bandingkan pemasukan dan pengeluaran bulan ini",
            transactions,
            now,
        )

        assertTrue(answer.contains("Rp1.000.000"))
        assertTrue(answer.contains("Rp200.000"))
    }

    private fun transaction(
        id: String,
        amount: Long,
        type: TransactionType,
        category: String,
        date: Long,
    ) = TransactionWithCategory(
        id = id,
        amount = amount,
        transactionDate = date,
        note = null,
        categoryId = category,
        categoryName = category,
        categoryType = type,
        categoryIconName = "wallet",
        categoryColorCode = "#000000",
    )

    private fun timestamp(year: Int, month: Int, day: Int, hour: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
