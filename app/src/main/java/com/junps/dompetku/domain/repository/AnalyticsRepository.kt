package com.junps.dompetku.domain.repository

import com.junps.dompetku.domain.model.CategoryExpense
import com.junps.dompetku.domain.model.MonthRange
import com.junps.dompetku.domain.model.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

interface AnalyticsRepository {
    fun observeExpenses(monthRange: MonthRange): Flow<List<CategoryExpense>>
    fun observeTransactions(monthRange: MonthRange): Flow<List<TransactionWithCategory>>
    fun observeAllTransactions(): Flow<List<TransactionWithCategory>>
}
