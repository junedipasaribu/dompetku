package com.junps.dompetku.core.export

import com.junps.dompetku.domain.model.TransactionType
import com.junps.dompetku.domain.model.TransactionWithCategory
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialReportCsvTest {
    @Test
    fun report_containsSummaryAndEscapedTransactions() {
        val monthStart = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val transaction = TransactionWithCategory(
            id = "1",
            amount = 15_000,
            transactionDate = monthStart,
            note = "Makan, \"siang\"",
            categoryId = "food",
            categoryName = "Makanan",
            categoryType = TransactionType.EXPENSE,
            categoryIconName = "restaurant",
            categoryColorCode = "#E65100",
        )

        val csv = buildFinancialReportCsv(monthStart, 100_000, 15_000, listOf(transaction))

        assertTrue(csv.contains("Total pemasukan,100000"))
        assertTrue(csv.contains("Saldo bulan ini,85000"))
        assertTrue(csv.contains("\"Makan, \"\"siang\"\"\""))
        assertEquals("laporan-dompetku-2026-08.csv", reportFileName(monthStart))
    }
}
