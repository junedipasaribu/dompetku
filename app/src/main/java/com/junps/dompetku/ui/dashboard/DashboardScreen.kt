package com.junps.dompetku.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junps.dompetku.core.format.formatRupiah
import com.junps.dompetku.domain.model.TransactionType
import com.junps.dompetku.domain.model.TransactionWithCategory
import com.junps.dompetku.ui.components.categoryIcon
import com.junps.dompetku.ui.components.LoadingScreen
import com.junps.dompetku.ui.components.MessageScreen
import com.junps.dompetku.ui.theme.DompetBackground
import com.junps.dompetku.ui.theme.DompetGreen
import com.junps.dompetku.ui.theme.DompetRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onTransactionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> LoadingScreen("Memuat ringkasan keuangan", modifier)
        uiState.errorMessage != null -> DashboardError(uiState.errorMessage.orEmpty(), modifier)
        else -> DashboardContent(uiState, onTransactionClick, modifier)
    }
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onTransactionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DompetBackground),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Ringkasan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                uiState.monthRange?.startInclusive?.let(::formatMonth).orEmpty(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            BalanceCard(
                balance = uiState.summary.totalBalance,
                monthLabel = uiState.monthRange?.startInclusive?.let(::formatMonth).orEmpty(),
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryCard("Pemasukan", uiState.summary.monthlyIncome, Icons.Default.ArrowDownward, DompetGreen, Modifier.weight(1f))
                SummaryCard("Pengeluaran", uiState.summary.monthlyExpense, Icons.Default.ArrowUpward, DompetRed, Modifier.weight(1f))
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            Text("Transaksi terbaru", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        if (uiState.recentTransactions.isEmpty()) {
            item { EmptyRecentTransactions() }
        } else {
            items(uiState.recentTransactions, key = { it.id }) { transaction ->
                TransactionRow(transaction, onClick = { onTransactionClick(transaction.id) })
            }
        }
    }
}

@Composable
private fun BalanceCard(balance: Long, monthLabel: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Saldo saat ini", color = Color.White.copy(alpha = .75f))
            Spacer(Modifier.height(8.dp))
            Text(
                formatRupiah(balance),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(14.dp))
            Text(monthLabel, color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: Long,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(10.dp))
            Text(title, color = Color.Gray, style = MaterialTheme.typography.labelMedium)
            Text(
                formatRupiah(amount),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TransactionRow(transaction: TransactionWithCategory, onClick: () -> Unit) {
    val isIncome = transaction.categoryType == TransactionType.INCOME
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Icon(
                    imageVector = categoryIcon(transaction.categoryIconName),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp),
                )
            }
            Column(
                Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            ) {
                Text(
                    transaction.note?.takeIf { it.isNotBlank() } ?: transaction.categoryName,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${transaction.categoryName} · ${formatTransactionDate(transaction.transactionDate)}",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = (if (isIncome) "+ " else "- ") + formatRupiah(transaction.amount),
                color = if (isIncome) DompetGreen else DompetRed,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyRecentTransactions() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Belum ada transaksi",
                modifier = Modifier.padding(top = 12.dp),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Tekan tombol + untuk mencatat transaksi pertama.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun DashboardError(message: String, modifier: Modifier = Modifier) {
    MessageScreen("Ringkasan tidak dapat dimuat", message, modifier)
}

private val IndonesianLocale = Locale.forLanguageTag("id-ID")

private fun formatMonth(timestamp: Long): String =
    SimpleDateFormat("MMMM yyyy", IndonesianLocale).format(Date(timestamp))

private fun formatTransactionDate(timestamp: Long): String =
    SimpleDateFormat("dd MMM, HH:mm", IndonesianLocale).format(Date(timestamp))
