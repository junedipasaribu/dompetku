package com.junps.dompetku.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junps.dompetku.core.format.formatRupiah
import com.junps.dompetku.domain.model.TransactionDayGroup
import com.junps.dompetku.domain.model.TransactionType
import com.junps.dompetku.domain.model.TransactionWithCategory
import com.junps.dompetku.ui.components.categoryColor
import com.junps.dompetku.ui.components.categoryIcon
import com.junps.dompetku.ui.components.LoadingScreen
import com.junps.dompetku.ui.components.MessageScreen
import com.junps.dompetku.ui.theme.DompetGreen
import com.junps.dompetku.ui.theme.DompetRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HistoryScreen(
    onTransactionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var transactionToDelete by remember { mutableStateOf<TransactionWithCategory?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                HistoryEvent.Deleted -> {
                    transactionToDelete = null
                    snackbarHostState.showSnackbar("Transaksi berhasil dihapus.")
                }
                is HistoryEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingScreen("Memuat riwayat transaksi", Modifier.padding(innerPadding))
            uiState.errorMessage != null -> HistoryError(
                uiState.errorMessage.orEmpty(),
                Modifier.padding(innerPadding),
            )
            uiState.groups.isEmpty() -> EmptyHistory(Modifier.padding(innerPadding))
            else -> HistoryList(
                groups = uiState.groups,
                onTransactionClick = onTransactionClick,
                onDelete = { transactionToDelete = it },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    transactionToDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Hapus transaksi?") },
            text = {
                Text(
                    "Transaksi ${transaction.note?.takeIf(String::isNotBlank) ?: transaction.categoryName} " +
                        "sebesar ${formatRupiah(transaction.amount)} akan dihapus permanen.",
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(transaction.id) }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun HistoryList(
    groups: List<TransactionDayGroup>,
    onTransactionClick: (String) -> Unit,
    onDelete: (TransactionWithCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Riwayat", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Semua transaksi, diurutkan dari yang terbaru.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        groups.forEach { group ->
            item(key = "header-${group.dayStart}") {
                Text(
                    text = formatDayHeader(group.dayStart),
                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(group.transactions, key = { it.id }) { transaction ->
                HistoryTransactionRow(
                    transaction = transaction,
                    onClick = { onTransactionClick(transaction.id) },
                    onDelete = { onDelete(transaction) },
                )
            }
        }
    }
}

@Composable
private fun HistoryTransactionRow(
    transaction: TransactionWithCategory,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val isIncome = transaction.categoryType == TransactionType.INCOME
    val accentColor = categoryColor(transaction.categoryColorCode)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = .14f)),
            ) {
                Icon(
                    imageVector = categoryIcon(transaction.categoryIconName),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.padding(10.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    transaction.note?.takeIf(String::isNotBlank) ?: transaction.categoryName,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${transaction.categoryName} · ${formatTime(transaction.transactionDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = (if (isIncome) "+ " else "- ") + formatRupiah(transaction.amount),
                color = if (isIncome) DompetGreen else DompetRed,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Hapus transaksi",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Belum ada riwayat",
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        Text(
            "Transaksi yang Anda catat akan dikelompokkan berdasarkan tanggal di sini.",
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistoryError(message: String, modifier: Modifier = Modifier) {
    MessageScreen("Riwayat tidak dapat dimuat", message, modifier)
}

private val IndonesianLocale = Locale.forLanguageTag("id-ID")

private fun formatDayHeader(dayStart: Long): String {
    val today = startOfDay(System.currentTimeMillis())
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = today
        add(Calendar.DAY_OF_MONTH, -1)
    }.timeInMillis
    return when (dayStart) {
        today -> "Hari ini"
        yesterday -> "Kemarin"
        else -> SimpleDateFormat("EEEE, dd MMMM yyyy", IndonesianLocale).format(Date(dayStart))
    }
}

private fun startOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", IndonesianLocale).format(Date(timestamp))
