package com.junps.dompetku.data.repository

import com.junps.dompetku.data.local.dao.TransactionDao
import com.junps.dompetku.data.mapper.toDomain
import com.junps.dompetku.domain.model.FinancialSummary
import com.junps.dompetku.domain.model.MonthRange
import com.junps.dompetku.domain.model.TransactionWithCategory
import com.junps.dompetku.domain.repository.DashboardRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineDashboardRepository @Inject constructor(
    private val transactionDao: TransactionDao,
) : DashboardRepository {
    override fun observeSummary(monthRange: MonthRange): Flow<FinancialSummary> =
        transactionDao.observeFinancialSummary(
            monthStart = monthRange.startInclusive,
            nextMonthStart = monthRange.endExclusive,
        ).map { row ->
            FinancialSummary(
                totalBalance = row.totalBalance,
                monthlyIncome = row.monthlyIncome,
                monthlyExpense = row.monthlyExpense,
            )
        }

    override fun observeRecentTransactions(limit: Int): Flow<List<TransactionWithCategory>> {
        require(limit > 0) { "Limit transaksi harus lebih besar dari nol." }
        return transactionDao.observeRecentWithCategory(limit).map { rows ->
            rows.map { it.toDomain() }
        }
    }
}
