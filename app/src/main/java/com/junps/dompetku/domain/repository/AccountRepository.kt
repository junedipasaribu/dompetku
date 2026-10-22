package com.junps.dompetku.domain.repository

import com.junps.dompetku.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAllAccounts(): Flow<List<Account>>
    suspend fun getAccountById(id: String): Account?
    suspend fun saveAccount(account: Account)
    suspend fun deleteAccount(account: Account)
}
