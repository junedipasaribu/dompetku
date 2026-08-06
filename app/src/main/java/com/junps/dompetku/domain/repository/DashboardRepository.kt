package com.junps.dompetku.domain.repository

import com.junps.dompetku.domain.model.FinancialSummary
import com.junps.dompetku.domain.model.MonthRange
import com.junps.dompetku.domain.model.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun observeSummary(monthRange: MonthRange): Flow<FinancialSummary>
    fun observeRecentTransactions(limit: Int): Flow<List<TransactionWithCategory>>
}
