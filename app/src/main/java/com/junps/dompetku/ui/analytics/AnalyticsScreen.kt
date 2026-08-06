package com.junps.dompetku.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junps.dompetku.core.format.formatRupiah
import com.junps.dompetku.domain.model.CategoryExpense
import com.junps.dompetku.domain.model.TransactionType
import com.junps.dompetku.domain.model.TransactionWithCategory
import com.junps.dompetku.domain.query.FinancialQuestionEngine
import com.junps.dompetku.ui.components.categoryColor
import com.junps.dompetku.ui.components.categoryIcon
import com.junps.dompetku.ui.components.LoadingScreen
import com.junps.dompetku.ui.components.MessageScreen
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.pieSeries
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when {
        uiState.isLoading -> LoadingScreen("Memuat laporan pengeluaran", modifier)
        uiState.errorMessage != null -> AnalyticsError(uiState.errorMessage.orEmpty(), modifier)
        else -> AnalyticsContent(
            uiState = uiState,
            onPreviousMonth = viewModel::selectPreviousMonth,
            onNextMonth = viewModel::selectNextMonth,
            modifier = modifier,
        )
    }
}

@Composable
private fun AnalyticsContent(
    uiState: AnalyticsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var chartType by remember { mutableStateOf(ReportChartType.PIE) }
    var comparisonPeriod by remember { mutableStateOf(ComparisonPeriod.DAILY) }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val comparisonSource = if (comparisonPeriod == ComparisonPeriod.MONTHLY) {
        uiState.yearlyTransactions
    } else {
        uiState.transactions
    }
    val comparison = remember(comparisonSource, comparisonPeriod) {
        buildComparison(comparisonSource, comparisonPeriod)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Laporan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Distribusi pengeluaran berdasarkan kategori.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            MonthSelector(
                monthTimestamp = uiState.monthRange?.startInclusive ?: 0,
                canSelectNextMonth = uiState.canSelectNextMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
            )
        }
        item {
            AskDompetKu(
                question = question,
                answer = answer,
                onQuestionChange = {
                    question = it
                    answer = null
                },
                onAsk = {
                    answer = FinancialQuestionEngine.answer(question, uiState.allTransactions)
                    focusManager.clearFocus()
                },
            )
        }
        if (uiState.expenses.isEmpty()) {
            item { EmptyAnalytics() }
        } else {
            item {
                Text("Bentuk diagram", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ReportChartType.entries.forEach { type ->
                        FilterChip(
                            selected = chartType == type,
                            onClick = { chartType = type },
                            label = { Text(type.label) },
                        )
                    }
                }
            }
            item {
                when (chartType) {
                    ReportChartType.PIE -> ExpensePieChart(uiState.expenses, uiState.totalExpense)
                    ReportChartType.BAR -> ExpenseBarChart(uiState.expenses, uiState.totalExpense)
                }
            }
            item {
                Text("Rincian kategori", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            items(uiState.expenses, key = { it.categoryId }) { expense ->
                ExpenseLegendRow(expense, uiState.totalExpense)
            }
        }
        item {
            Text("Pemasukan vs pengeluaran", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ComparisonPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = comparisonPeriod == period,
                        onClick = { comparisonPeriod = period },
                        label = { Text(period.label) },
                    )
                }
            }
        }
        item { ComparisonChart(comparison) }
    }
}

@Composable
private fun AskDompetKu(
    question: String,
    answer: String?,
    onQuestionChange: (String) -> Unit,
    onAsk: () -> Unit,
) {
    val examples = listOf(
        "Pengeluaran terbesar hari ini?",
        "Kategori terbesar bulan ini?",
        "Bandingkan keuangan minggu ini",
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("Tanya DompetKu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Tanyakan ringkasan dari catatan keuanganmu.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = question,
                onValueChange = onQuestionChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                placeholder = { Text("Apa pengeluaran terbesar saya hari ini?") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onAsk() }),
                singleLine = false,
                maxLines = 3,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(onClick = onAsk, enabled = question.isNotBlank()) { Text("Tanyakan") }
            }
            if (answer == null && question.isBlank()) {
                Text(
                    "Contoh pertanyaan",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    examples.forEach { example ->
                        AssistChip(
                            onClick = { onQuestionChange(example) },
                            label = { Text(example) },
                        )
                    }
                }
            }
            answer?.let {
                Text(
                    it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

private enum class ReportChartType(val label: String) {
    PIE("Lingkaran"),
    BAR("Batang"),
}

internal enum class ComparisonPeriod(val label: String) {
    DAILY("Harian"),
    WEEKLY("Mingguan"),
    MONTHLY("Bulanan"),
}

internal data class PeriodComparison(val label: String, val income: Long, val expense: Long)

internal fun buildComparison(
    transactions: List<TransactionWithCategory>,
    period: ComparisonPeriod,
): List<PeriodComparison> {
    val formatter = when (period) {
        ComparisonPeriod.DAILY -> SimpleDateFormat("dd MMM", IndonesianLocale)
        ComparisonPeriod.WEEKLY -> null
        ComparisonPeriod.MONTHLY -> SimpleDateFormat("MMM yyyy", IndonesianLocale)
    }
    return transactions
        .groupBy { transaction ->
            when (period) {
                ComparisonPeriod.DAILY -> formatter!!.format(Date(transaction.transactionDate))
                ComparisonPeriod.WEEKLY -> {
                    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = transaction.transactionDate }
                    "Pekan ${((calendar.get(java.util.Calendar.DAY_OF_MONTH) - 1) / 7) + 1}"
                }
                ComparisonPeriod.MONTHLY -> formatter!!.format(Date(transaction.transactionDate))
            }
        }
        .map { (label, values) ->
            PeriodComparison(
                label = label,
                income = values.filter { it.categoryType == TransactionType.INCOME }.sumOf { it.amount },
                expense = values.filter { it.categoryType == TransactionType.EXPENSE }.sumOf { it.amount },
            )
        }
        .takeLast(12)
}

@Composable
private fun ExpenseBarChart(expenses: List<CategoryExpense>, totalExpense: Long) {
    val maximum = expenses.maxOfOrNull { it.amount }?.coerceAtLeast(1) ?: 1
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Pengeluaran per kategori", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatRupiah(totalExpense), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Column(
                modifier = Modifier.padding(top = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                expenses.forEach { expense ->
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(expense.categoryName, style = MaterialTheme.typography.bodySmall)
                            Text(formatRupiah(expense.amount), style = MaterialTheme.typography.bodySmall)
                        }
                        Box(
                            Modifier
                                .padding(top = 5.dp)
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(expense.amount.toFloat() / maximum.toFloat())
                                    .height(8.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonChart(values: List<PeriodComparison>) {
    val incomeColor = MaterialTheme.colorScheme.primary
    val expenseColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val maximum = values.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1) ?: 1
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ChartKey(incomeColor, "Pemasukan")
                ChartKey(expenseColor, "Pengeluaran")
            }
            if (values.isEmpty()) {
                Text("Belum ada data pada periode ini.", modifier = Modifier.padding(top = 20.dp))
            } else {
                values.forEach { value ->
                    Column(Modifier.padding(top = 14.dp)) {
                        Text(value.label, style = MaterialTheme.typography.labelMedium)
                        ComparisonBar(value.income, maximum, incomeColor, trackColor)
                        ComparisonBar(value.expense, maximum, expenseColor, trackColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonBar(value: Long, maximum: Long, color: Color, track: Color) {
    Row(
        modifier = Modifier.padding(top = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(7.dp)
                .background(track, CircleShape),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(value.toFloat() / maximum.toFloat())
                    .height(7.dp)
                    .background(color, CircleShape),
            )
        }
        Text(formatRupiah(value), modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ChartKey(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, modifier = Modifier.padding(start = 5.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MonthSelector(
    monthTimestamp: Long,
    canSelectNextMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Bulan sebelumnya")
            }
            Text(
                formatMonth(monthTimestamp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onNextMonth, enabled = canSelectNextMonth) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Bulan berikutnya")
            }
        }
    }
}

@Composable
private fun ExpensePieChart(expenses: List<CategoryExpense>, totalExpense: Long) {
    val modelProducer = remember { PieChartModelProducer() }
    LaunchedEffect(expenses) {
        modelProducer.runTransaction {
            pieSeries { series(expenses.map { it.amount }) }
        }
    }
    val slices = expenses.map { expense ->
        PieChart.Slice(fill = Fill(categoryColor(expense.categoryColorCode)))
    }
    val chart = rememberPieChart(
        sliceProvider = PieChart.SliceProvider.series(slices),
        spacing = 2.dp,
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Total pengeluaran", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                formatRupiah(totalExpense),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            PieChartHost(
                chart = chart,
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(top = 16.dp)
                    .semantics {
                        contentDescription = buildChartDescription(expenses, totalExpense)
                    },
            )
        }
    }
}

@Composable
private fun ExpenseLegendRow(expense: CategoryExpense, totalExpense: Long) {
    val color = categoryColor(expense.categoryColorCode)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = categoryIcon(expense.categoryIconName),
                    contentDescription = null,
                    tint = color,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            ) {
                Text(expense.categoryName, fontWeight = FontWeight.SemiBold)
                Text(
                    formatPercentage(expense.amount, totalExpense),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                formatRupiah(expense.amount),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyAnalytics() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.PieChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Belum ada pengeluaran",
                modifier = Modifier.padding(top = 14.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Grafik akan tampil setelah ada transaksi pengeluaran pada bulan ini.",
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AnalyticsError(message: String, modifier: Modifier = Modifier) {
    MessageScreen("Laporan tidak dapat dimuat", message, modifier)
}

private val IndonesianLocale = Locale.forLanguageTag("id-ID")

private fun formatMonth(timestamp: Long): String =
    SimpleDateFormat("MMMM yyyy", IndonesianLocale).format(Date(timestamp))

internal fun formatPercentage(amount: Long, total: Long): String {
    if (amount <= 0 || total <= 0) return "0,0%"
    val percentage = BigDecimal.valueOf(amount)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)
    return "${percentage.toPlainString().replace('.', ',')}%"
}

internal fun buildChartDescription(expenses: List<CategoryExpense>, total: Long): String =
    expenses.joinToString(
        prefix = "Grafik pengeluaran. ",
        separator = "; ",
    ) { expense ->
        "${expense.categoryName} ${formatPercentage(expense.amount, total)}"
    }
