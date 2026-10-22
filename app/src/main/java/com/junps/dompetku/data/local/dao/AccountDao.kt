package com.junps.dompetku.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.junps.dompetku.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY createdAt ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query(
        """
        SELECT 
            accounts.id, 
            accounts.name, 
            accounts.initialBalance, 
            accounts.createdAt,
            accounts.initialBalance + COALESCE(SUM(
                CASE 
                    WHEN categories.type = 'INCOME' THEN transactions.amount 
                    WHEN categories.type = 'EXPENSE' THEN -transactions.amount 
                    ELSE 0 
                END
            ), 0) AS balance
        FROM accounts
        LEFT JOIN transactions ON accounts.id = transactions.accountId
        LEFT JOIN categories ON transactions.categoryId = categories.id
        GROUP BY accounts.id
        ORDER BY accounts.createdAt ASC
        """
    )
    fun observeAccountsWithBalance(): Flow<List<com.junps.dompetku.data.local.model.AccountWithBalanceRow>>


    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: String): AccountEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId")
    suspend fun countTransactions(accountId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)
}
