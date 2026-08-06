package com.junps.dompetku.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.junps.dompetku.data.local.entity.TransactionEntity
import com.junps.dompetku.data.local.model.FinancialSummaryRow
import com.junps.dompetku.data.local.model.ExpenseByCategoryRow
import com.junps.dompetku.data.local.model.TransactionWithCategoryRow
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY transactionDate DESC, createdAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE
                WHEN categories.type = 'INCOME' THEN transactions.amount
                ELSE -transactions.amount
            END), 0) AS totalBalance,
            COALESCE(SUM(CASE
                WHEN categories.type = 'INCOME'
                    AND transactions.transactionDate >= :monthStart
                    AND transactions.transactionDate < :nextMonthStart
                THEN transactions.amount ELSE 0
            END), 0) AS monthlyIncome,
            COALESCE(SUM(CASE
                WHEN categories.type = 'EXPENSE'
                    AND transactions.transactionDate >= :monthStart
                    AND transactions.transactionDate < :nextMonthStart
                THEN transactions.amount ELSE 0
            END), 0) AS monthlyExpense
        FROM transactions
        INNER JOIN categories ON categories.id = transactions.categoryId
        """,
    )
    fun observeFinancialSummary(
        monthStart: Long,
        nextMonthStart: Long,
    ): Flow<FinancialSummaryRow>

    @Query(
        """
        SELECT
            transactions.id AS id,
            transactions.amount AS amount,
            transactions.transactionDate AS transactionDate,
            transactions.note AS note,
            categories.id AS categoryId,
            categories.name AS categoryName,
            categories.type AS categoryType,
            categories.iconName AS categoryIconName,
            categories.colorCode AS categoryColorCode
        FROM transactions
        INNER JOIN categories ON categories.id = transactions.categoryId
        ORDER BY transactions.transactionDate DESC, transactions.createdAt DESC
        LIMIT :limit
        """,
    )
    fun observeRecentWithCategory(limit: Int): Flow<List<TransactionWithCategoryRow>>

    @Query(
        """
        SELECT
            transactions.id AS id,
            transactions.amount AS amount,
            transactions.transactionDate AS transactionDate,
            transactions.note AS note,
            categories.id AS categoryId,
            categories.name AS categoryName,
            categories.type AS categoryType,
            categories.iconName AS categoryIconName,
            categories.colorCode AS categoryColorCode
        FROM transactions
        INNER JOIN categories ON categories.id = transactions.categoryId
        ORDER BY transactions.transactionDate DESC, transactions.createdAt DESC
        """,
    )
    fun observeAllWithCategory(): Flow<List<TransactionWithCategoryRow>>

    @Query(
        """
        SELECT
            transactions.id AS id,
            transactions.amount AS amount,
            transactions.transactionDate AS transactionDate,
            transactions.note AS note,
            categories.id AS categoryId,
            categories.name AS categoryName,
            categories.type AS categoryType,
            categories.iconName AS categoryIconName,
            categories.colorCode AS categoryColorCode
        FROM transactions
        INNER JOIN categories ON categories.id = transactions.categoryId
        WHERE transactions.transactionDate >= :monthStart
          AND transactions.transactionDate < :nextMonthStart
        ORDER BY transactions.transactionDate ASC, transactions.createdAt ASC
        """,
    )
    fun observeWithCategoryInRange(
        monthStart: Long,
        nextMonthStart: Long,
    ): Flow<List<TransactionWithCategoryRow>>

    @Query(
        """
        SELECT
            categories.id AS categoryId,
            categories.name AS categoryName,
            categories.iconName AS categoryIconName,
            categories.colorCode AS categoryColorCode,
            SUM(transactions.amount) AS totalAmount
        FROM transactions
        INNER JOIN categories ON categories.id = transactions.categoryId
        WHERE categories.type = 'EXPENSE'
          AND transactions.transactionDate >= :monthStart
          AND transactions.transactionDate < :nextMonthStart
        GROUP BY categories.id, categories.name, categories.iconName, categories.colorCode
        HAVING SUM(transactions.amount) > 0
        ORDER BY totalAmount DESC, categories.name COLLATE NOCASE ASC
        """,
    )
    fun observeExpensesByCategory(
        monthStart: Long,
        nextMonthStart: Long,
    ): Flow<List<ExpenseByCategoryRow>>

    @Insert
    suspend fun insert(transaction: TransactionEntity)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)
}
