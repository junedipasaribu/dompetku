package com.junps.dompetku.data.repository

import com.junps.dompetku.data.local.dao.TransactionDao
import com.junps.dompetku.data.mapper.toDomain
import com.junps.dompetku.data.mapper.toEntity
import com.junps.dompetku.domain.model.Transaction
import com.junps.dompetku.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineTransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
) : TransactionRepository {
    override fun observeAll(): Flow<List<Transaction>> = transactionDao.observeAll().map { transactions ->
        transactions.map { it.toDomain() }
    }

    override suspend fun getById(id: String): Transaction? = transactionDao.getById(id)?.toDomain()

    override suspend fun add(transaction: Transaction) = transactionDao.insert(transaction.toEntity())

    override suspend fun update(transaction: Transaction) = transactionDao.update(transaction.toEntity())

    override suspend fun delete(transaction: Transaction) = transactionDao.delete(transaction.toEntity())
}
