package com.junps.dompetku.core.export

import com.junps.dompetku.domain.model.TransactionType
import com.junps.dompetku.domain.model.TransactionWithCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val IndonesianLocale = Locale.forLanguageTag("id-ID")

fun buildFinancialReportCsv(
    monthStart: Long,
    totalIncome: Long,
    totalExpense: Long,
    transactions: List<TransactionWithCategory>,
): String = buildString {
    appendLine("Laporan Keuangan DompetKu")
    appendLine("Periode,${csv(formatMonth(monthStart))}")
    appendLine("Total pemasukan,$totalIncome")
    appendLine("Total pengeluaran,$totalExpense")
    appendLine("Saldo bulan ini,${totalIncome - totalExpense}")
    appendLine()
    appendLine("Tanggal,Waktu,Tipe,Kategori,Catatan,Nominal")
    transactions.forEach { transaction ->
        append(csv(formatDate(transaction.transactionDate))).append(',')
        append(csv(formatTime(transaction.transactionDate))).append(',')
        append(if (transaction.categoryType == TransactionType.INCOME) "Pemasukan" else "Pengeluaran").append(',')
        append(csv(transaction.categoryName)).append(',')
        append(csv(transaction.note.orEmpty())).append(',')
        appendLine(transaction.amount)
    }
}

fun reportFileName(monthStart: Long): String =
    "laporan-dompetku-${SimpleDateFormat("yyyy-MM", IndonesianLocale).format(Date(monthStart))}.csv"

private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""
private fun formatMonth(timestamp: Long) = SimpleDateFormat("MMMM yyyy", IndonesianLocale).format(Date(timestamp))
private fun formatDate(timestamp: Long) = SimpleDateFormat("dd/MM/yyyy", IndonesianLocale).format(Date(timestamp))
private fun formatTime(timestamp: Long) = SimpleDateFormat("HH:mm", IndonesianLocale).format(Date(timestamp))
