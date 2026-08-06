package com.junps.dompetku.data.repository

import com.junps.dompetku.data.local.dao.TransactionDao
import com.junps.dompetku.data.mapper.toDomain
import com.junps.dompetku.domain.model.TransactionWithCategory
import com.junps.dompetku.domain.repository.HistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineHistoryRepository @Inject constructor(
    private val transactionDao: TransactionDao,
) : HistoryRepository {
    override fun observeTransactions(): Flow<List<TransactionWithCategory>> =
        transactionDao.observeAllWithCategory().map { rows ->
            rows.map { it.toDomain() }
        }
}
