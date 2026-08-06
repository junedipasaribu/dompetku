package com.junps.dompetku.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junps.dompetku.domain.model.TransactionDayGroup
import com.junps.dompetku.domain.repository.HistoryRepository
import com.junps.dompetku.domain.time.TransactionDateGrouper
import com.junps.dompetku.domain.usecase.DeleteTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val isLoading: Boolean = true,
    val groups: List<TransactionDayGroup> = emptyList(),
    val errorMessage: String? = null,
)

sealed interface HistoryEvent {
    data object Deleted : HistoryEvent
    data class ShowMessage(val message: String) : HistoryEvent
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: HistoryRepository,
    dateGrouper: TransactionDateGrouper,
    private val deleteTransaction: DeleteTransactionUseCase,
) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> = repository.observeTransactions()
        .map { transactions ->
            HistoryUiState(
                isLoading = false,
                groups = dateGrouper.group(transactions),
            )
        }
        .catch { error ->
            emit(
                HistoryUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Riwayat transaksi gagal dimuat.",
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(),
        )

    private val _events = MutableSharedFlow<HistoryEvent>()
    val events: SharedFlow<HistoryEvent> = _events

    fun delete(transactionId: String) {
        viewModelScope.launch {
            runCatching { deleteTransaction(transactionId) }
                .onSuccess { _events.emit(HistoryEvent.Deleted) }
                .onFailure { error ->
                    _events.emit(
                        HistoryEvent.ShowMessage(error.message ?: "Transaksi gagal dihapus."),
                    )
                }
        }
    }
}
