package com.junps.dompetku.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junps.dompetku.domain.model.CategoryExpense
import com.junps.dompetku.domain.model.MonthRange
import com.junps.dompetku.domain.model.TransactionType
import com.junps.dompetku.domain.model.TransactionWithCategory
import com.junps.dompetku.domain.repository.AnalyticsRepository
import com.junps.dompetku.domain.time.MonthRangeCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val monthRange: MonthRange? = null,
    val expenses: List<CategoryExpense> = emptyList(),
    val totalExpense: Long = 0,
    val totalIncome: Long = 0,
    val transactions: List<TransactionWithCategory> = emptyList(),
    val yearlyTransactions: List<TransactionWithCategory> = emptyList(),
    val allTransactions: List<TransactionWithCategory> = emptyList(),
    val canSelectNextMonth: Boolean = false,
    val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: AnalyticsRepository,
    private val monthRangeCalculator: MonthRangeCalculator,
) : ViewModel() {
    private val selectedMonth = MutableStateFlow(System.currentTimeMillis())

    val uiState: StateFlow<AnalyticsUiState> = selectedMonth
        .map(monthRangeCalculator::containing)
        .flatMapLatest { monthRange ->
            combine(
                repository.observeExpenses(monthRange),
                repository.observeTransactions(monthRange),
                repository.observeTransactions(yearRange(monthRange.startInclusive)),
                repository.observeAllTransactions(),
            ) { expenses, transactions, yearlyTransactions, allTransactions ->
                AnalyticsUiState(
                    isLoading = false,
                    monthRange = monthRange,
                    expenses = expenses,
                    totalExpense = expenses.sumOf { it.amount },
                    totalIncome = transactions
                        .filter { it.categoryType == TransactionType.INCOME }
                        .sumOf { it.amount },
                    transactions = transactions,
                    yearlyTransactions = yearlyTransactions,
                    allTransactions = allTransactions,
                    canSelectNextMonth = monthRange.startInclusive < currentMonth().startInclusive,
                )
            }
        }
        .catch { error ->
            emit(
                AnalyticsUiState(
                    isLoading = false,
                    monthRange = monthRangeCalculator.containing(selectedMonth.value),
                    errorMessage = error.message ?: "Laporan pengeluaran gagal dimuat.",
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AnalyticsUiState(),
        )

    fun selectPreviousMonth() {
        selectedMonth.value = shiftMonth(selectedMonth.value, -1)
    }

    fun selectNextMonth() {
        val candidate = shiftMonth(selectedMonth.value, 1)
        if (monthRangeCalculator.containing(candidate).startInclusive <= currentMonth().startInclusive) {
            selectedMonth.value = candidate
        }
    }

    private fun currentMonth(): MonthRange = monthRangeCalculator.containing(System.currentTimeMillis())

    private fun yearRange(timestamp: Long): MonthRange {
        val start = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.YEAR, 1) }
        return MonthRange(start.timeInMillis, end.timeInMillis)
    }

    private fun shiftMonth(timestamp: Long, offset: Int): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, offset)
    }.timeInMillis
}
