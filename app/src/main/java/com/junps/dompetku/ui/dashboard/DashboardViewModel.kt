package com.junps.dompetku.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junps.dompetku.domain.model.FinancialSummary
import com.junps.dompetku.domain.model.MonthRange
import com.junps.dompetku.domain.model.TransactionWithCategory
import com.junps.dompetku.domain.repository.DashboardRepository
import com.junps.dompetku.domain.time.MonthRangeCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val monthRange: MonthRange? = null,
    val summary: FinancialSummary = FinancialSummary(),
    val recentTransactions: List<TransactionWithCategory> = emptyList(),
    val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val monthRangeCalculator: MonthRangeCalculator,
) : ViewModel() {
    private val currentTime = MutableStateFlow(System.currentTimeMillis())

    private val monthRanges = currentTime
        .map(monthRangeCalculator::containing)
        .distinctUntilChanged()

    val uiState: StateFlow<DashboardUiState> = monthRanges
        .flatMapLatest { monthRange ->
            combine(
                repository.observeSummary(monthRange),
                repository.observeRecentTransactions(RECENT_TRANSACTION_LIMIT),
            ) { summary, recentTransactions ->
                DashboardUiState(
                    isLoading = false,
                    monthRange = monthRange,
                    summary = summary,
                    recentTransactions = recentTransactions,
                )
            }
        }
        .catch { error ->
            emit(
                DashboardUiState(
                    isLoading = false,
                    monthRange = monthRangeCalculator.containing(System.currentTimeMillis()),
                    errorMessage = error.message ?: "Ringkasan keuangan gagal dimuat.",
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(),
        )

    init {
        viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val range = monthRangeCalculator.containing(now)
                delay((range.endExclusive - now).coerceAtLeast(MIN_REFRESH_DELAY_MS))
                currentTime.value = System.currentTimeMillis()
            }
        }
    }

    fun refreshPeriod() {
        currentTime.value = System.currentTimeMillis()
    }

    companion object {
        private const val RECENT_TRANSACTION_LIMIT = 5
        private const val MIN_REFRESH_DELAY_MS = 1_000L
    }
}
