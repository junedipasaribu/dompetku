package com.junps.dompetku.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junps.dompetku.data.local.dao.AccountDao
import com.junps.dompetku.data.local.dao.RecurringTransactionDao
import com.junps.dompetku.domain.model.Account
import com.junps.dompetku.domain.model.AccountWithBalance
import com.junps.dompetku.domain.repository.AccountRepository
import com.junps.dompetku.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    dashboardRepository: DashboardRepository,
    private val accountDao: AccountDao,
    private val recurringTransactionDao: RecurringTransactionDao,
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages

    val accounts: StateFlow<List<AccountWithBalance>> = dashboardRepository.observeAccountsWithBalance()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addAccount(name: String, initialBalance: Long) {
        viewModelScope.launch {
            val newAccount = Account(
                id = UUID.randomUUID().toString(),
                name = name,
                initialBalance = initialBalance,
                createdAt = System.currentTimeMillis()
            )
            accountRepository.saveAccount(newAccount)
        }
    }

    fun deleteAccount(account: AccountWithBalance) {
        viewModelScope.launch {
            val isInUse = accountDao.countTransactions(account.id) > 0 ||
                recurringTransactionDao.countByAccountId(account.id) > 0
            if (isInUse) {
                _messages.emit("Dompet masih digunakan oleh transaksi atau jadwal berulang.")
                return@launch
            }
            runCatching {
                accountRepository.deleteAccount(
                    Account(
                        id = account.id,
                        name = account.name,
                        initialBalance = account.initialBalance,
                        createdAt = account.createdAt,
                    ),
                )
            }.onSuccess {
                _messages.emit("Dompet berhasil dihapus.")
            }.onFailure {
                _messages.emit("Dompet tidak dapat dihapus. Coba lagi.")
            }
        }
    }
}
