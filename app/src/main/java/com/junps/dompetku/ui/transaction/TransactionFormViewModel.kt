package com.junps.dompetku.ui.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junps.dompetku.core.format.formatAmountInput
import com.junps.dompetku.core.format.parseAmountInput
import com.junps.dompetku.domain.model.Category
import com.junps.dompetku.domain.model.TransactionType
import com.junps.dompetku.domain.repository.AccountRepository
import com.junps.dompetku.domain.repository.CategoryRepository
import com.junps.dompetku.domain.repository.TransactionRepository
import com.junps.dompetku.domain.model.Account
import com.junps.dompetku.domain.usecase.DeleteTransactionUseCase
import com.junps.dompetku.domain.usecase.SaveTransactionRequest
import com.junps.dompetku.domain.usecase.SaveTransactionUseCase
import com.junps.dompetku.ui.navigation.TransactionFormDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.junps.dompetku.data.local.dao.RecurringTransactionDao

data class TransactionFormUiState(
    val isLoading: Boolean = true,
    val transactionId: String? = null,
    val amountInput: String = "",
    val categoryId: String? = null,
    val transactionDate: Long = System.currentTimeMillis(),
    val note: String = "",
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val accountId: String? = null,
    val isRecurring: Boolean = false,
    val frequency: String = "monthly", // default monthly
    val loadError: String? = null,
    val isSaving: Boolean = false,
) {
    val isEditMode: Boolean get() = transactionId != null
    val selectedCategory: Category? get() = categories.find { it.id == categoryId }
    val selectedAccount: Account? get() = accounts.find { it.id == accountId }
}

sealed interface TransactionFormEvent {
    data object Saved : TransactionFormEvent
    data object Deleted : TransactionFormEvent
    data class ShowMessage(val message: String) : TransactionFormEvent
}

@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val recurringDao: RecurringTransactionDao,
    private val saveTransaction: SaveTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
) : ViewModel() {
    private val requestedTransactionId: String? =
        savedStateHandle[TransactionFormDestination.transactionIdArgument]

    private val _uiState = MutableStateFlow(
        TransactionFormUiState(transactionId = requestedTransactionId),
    )
    val uiState: StateFlow<TransactionFormUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TransactionFormEvent>()
    val events: SharedFlow<TransactionFormEvent> = _events

    init {
        viewModelScope.launch {
            val existing = requestedTransactionId?.let { transactionRepository.getById(it) }
            if (requestedTransactionId != null && existing == null) {
                _uiState.update {
                    it.copy(isLoading = false, loadError = "Transaksi tidak ditemukan.")
                }
                return@launch
            }

            var initialized = false
            kotlinx.coroutines.flow.combine(
                categoryRepository.observeAll(),
                accountRepository.getAllAccounts()
            ) { cats, accs -> Pair(cats, accs) }.collect { (categories, accounts) ->
                _uiState.update { state ->
                    if (!initialized) {
                        initialized = true
                        val initialCategory = existing?.categoryId
                            ?: categories.firstOrNull { it.type == TransactionType.EXPENSE }?.id
                            ?: categories.firstOrNull()?.id
                        val initialAccount = existing?.accountId
                            ?: accounts.firstOrNull()?.id
                        state.copy(
                            isLoading = false,
                            amountInput = existing?.amount?.toString()?.let(::formatAmountInput).orEmpty(),
                            categoryId = initialCategory,
                            accountId = initialAccount,
                            transactionDate = existing?.transactionDate ?: state.transactionDate,
                            note = existing?.note.orEmpty(),
                            categories = categories,
                            accounts = accounts,
                        )
                    } else {
                        val selectedCatId = state.categoryId?.takeIf { id -> categories.any { it.id == id } }
                        val selectedAccId = state.accountId?.takeIf { id -> accounts.any { it.id == id } }
                        state.copy(
                            categories = categories,
                            accounts = accounts,
                            categoryId = selectedCatId ?: categories.firstOrNull()?.id,
                            accountId = selectedAccId ?: accounts.firstOrNull()?.id,
                        )
                    }
                }
            }
        }
    }

    fun onAmountChange(value: String) {
        val digits = value.filter(Char::isDigit).take(MAX_AMOUNT_DIGITS)
        _uiState.update { it.copy(amountInput = formatAmountInput(digits)) }
    }

    fun onCategoryChange(categoryId: String) {
        _uiState.update { state ->
            if (state.categories.any { it.id == categoryId }) state.copy(categoryId = categoryId) else state
        }
    }

    fun onAccountChange(accountId: String) {
        _uiState.update { state ->
            if (state.accounts.any { it.id == accountId }) state.copy(accountId = accountId) else state
        }
    }

    fun onTransactionDateChange(value: Long) {
        if (value > 0) _uiState.update { it.copy(transactionDate = value) }
    }

    fun onNoteChange(value: String) {
        _uiState.update { it.copy(note = value.take(SaveTransactionUseCase.MAX_NOTE_LENGTH)) }
    }

    fun onRecurringChange(isRecurring: Boolean) {
        _uiState.update { it.copy(isRecurring = isRecurring) }
    }

    fun onFrequencyChange(frequency: String) {
        _uiState.update { it.copy(frequency = frequency) }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        val amount = parseAmountInput(state.amountInput)
        val categoryId = state.categoryId
        val accountId = state.accountId
        if (amount == null || amount <= 0) {
            emitMessage("Nominal harus lebih besar dari nol.")
            return
        }
        if (categoryId == null) {
            emitMessage("Pilih kategori transaksi.")
            return
        }
        if (accountId == null) {
            emitMessage("Pilih dompet/rekening.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                saveTransaction(
                    SaveTransactionRequest(
                        id = state.transactionId,
                        amount = amount,
                        categoryId = categoryId,
                        accountId = accountId,
                        transactionDate = state.transactionDate,
                        note = state.note,
                    ),
                )
                
                // Add to recurring if enabled and it's a new transaction (not editing)
                if (state.isRecurring && !state.isEditMode) {
                    val nextTime = java.util.Calendar.getInstance().apply {
                        timeInMillis = state.transactionDate
                        when (state.frequency) {
                            "daily" -> add(java.util.Calendar.DAY_OF_YEAR, 1)
                            "weekly" -> add(java.util.Calendar.WEEK_OF_YEAR, 1)
                            "monthly" -> add(java.util.Calendar.MONTH, 1)
                            "yearly" -> add(java.util.Calendar.YEAR, 1)
                        }
                    }.timeInMillis
                    
                    val recurringEntity = com.junps.dompetku.data.local.entity.RecurringTransactionEntity(
                        amount = amount,
                        categoryId = categoryId,
                        accountId = accountId,
                        type = state.selectedCategory?.type?.name ?: "EXPENSE",
                        note = state.note,
                        frequency = state.frequency,
                        nextExecutionTime = nextTime,
                        isActive = true
                    )
                    recurringDao.insertRecurringTransaction(recurringEntity)
                }
            }.onSuccess {
                _events.emit(TransactionFormEvent.Saved)
            }.onFailure { error ->
                _events.emit(
                    TransactionFormEvent.ShowMessage(error.message ?: "Transaksi gagal disimpan."),
                )
            }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun delete() {
        val id = _uiState.value.transactionId ?: return
        viewModelScope.launch {
            runCatching { deleteTransaction(id) }
                .onSuccess { _events.emit(TransactionFormEvent.Deleted) }
                .onFailure { error ->
                    _events.emit(
                        TransactionFormEvent.ShowMessage(error.message ?: "Transaksi gagal dihapus."),
                    )
                }
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch { _events.emit(TransactionFormEvent.ShowMessage(message)) }
    }

    companion object {
        private const val MAX_AMOUNT_DIGITS = 18
    }
}
