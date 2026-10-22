package com.junps.dompetku.data.repository

import com.junps.dompetku.data.local.dao.AccountDao
import com.junps.dompetku.data.mapper.toAccount
import com.junps.dompetku.data.mapper.toEntity
import com.junps.dompetku.domain.model.Account
import com.junps.dompetku.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineAccountRepository @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {
    override fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts().map { entities ->
            entities.map { it.toAccount() }
        }
    }

    override suspend fun getAccountById(id: String): Account? {
        return accountDao.getAccountById(id)?.toAccount()
    }

    override suspend fun saveAccount(account: Account) {
        accountDao.insertAccount(account.toEntity())
    }

    override suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccount(account.toEntity())
    }
}
