package com.junps.dompetku.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Dashboard : AppDestination("dashboard", "Beranda", Icons.Default.Home)
    data object History : AppDestination("history", "Riwayat", Icons.AutoMirrored.Filled.ReceiptLong)
    data object Analytics : AppDestination("analytics", "Laporan", Icons.Default.BarChart)
    data object Categories : AppDestination("categories", "Kategori", Icons.Default.Category)

    companion object {
        val bottomBarItems = listOf(Dashboard, History, Analytics, Categories)
    }
}

object TransactionFormDestination {
    const val transactionIdArgument = "transactionId"
    const val route = "transaction_form?$transactionIdArgument={$transactionIdArgument}"

    fun createRoute(transactionId: String? = null): String =
        if (transactionId == null) "transaction_form" else "transaction_form?$transactionIdArgument=$transactionId"
}
