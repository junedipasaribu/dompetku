package com.junps.dompetku.domain.repository

import com.junps.dompetku.domain.model.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observeTransactions(): Flow<List<TransactionWithCategory>>
}
