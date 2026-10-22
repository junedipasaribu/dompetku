package com.junps.dompetku.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.junps.dompetku.data.local.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextExecutionTime <= :currentTime")
    suspend fun getDueTransactions(currentTime: Long): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id LIMIT 1")
    suspend fun getRecurringTransactionById(id: String): RecurringTransactionEntity?

    @Query("SELECT COUNT(*) FROM recurring_transactions WHERE accountId = :accountId")
    suspend fun countByAccountId(accountId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(transaction: RecurringTransactionEntity)

    @Update
    suspend fun updateRecurringTransaction(transaction: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteRecurringTransaction(id: String)
}
