package com.junps.dompetku.data.repository

import com.junps.dompetku.data.local.dao.TransactionDao
import com.junps.dompetku.domain.model.CategoryExpense
import com.junps.dompetku.domain.model.MonthRange
import com.junps.dompetku.domain.model.TransactionWithCategory
import com.junps.dompetku.domain.repository.AnalyticsRepository
import com.junps.dompetku.data.mapper.toDomain
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineAnalyticsRepository @Inject constructor(
    private val transactionDao: TransactionDao,
) : AnalyticsRepository {
    override fun observeExpenses(monthRange: MonthRange): Flow<List<CategoryExpense>> =
        transactionDao.observeExpensesByCategory(
            monthStart = monthRange.startInclusive,
            nextMonthStart = monthRange.endExclusive,
        ).map { rows ->
            rows.map { row ->
                CategoryExpense(
                    categoryId = row.categoryId,
                    categoryName = row.categoryName,
                    categoryIconName = row.categoryIconName,
                    categoryColorCode = row.categoryColorCode,
                    amount = row.totalAmount,
                )
            }
        }

    override fun observeTransactions(monthRange: MonthRange): Flow<List<TransactionWithCategory>> =
        transactionDao.observeWithCategoryInRange(
            monthStart = monthRange.startInclusive,
            nextMonthStart = monthRange.endExclusive,
        ).map { rows -> rows.map { it.toDomain() } }

    override fun observeAllTransactions(): Flow<List<TransactionWithCategory>> =
        transactionDao.observeAllWithCategory().map { rows -> rows.map { it.toDomain() } }
}
