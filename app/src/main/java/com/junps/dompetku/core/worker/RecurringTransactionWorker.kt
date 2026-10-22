package com.junps.dompetku.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.junps.dompetku.data.local.DompetKuDatabase
import com.junps.dompetku.data.local.dao.RecurringTransactionDao
import com.junps.dompetku.data.local.dao.TransactionDao
import com.junps.dompetku.data.local.entity.TransactionEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.UUID

@HiltWorker
class RecurringTransactionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: DompetKuDatabase,
    private val recurringDao: RecurringTransactionDao,
    private val transactionDao: TransactionDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val currentTime = System.currentTimeMillis()
        val dueTransactions = recurringDao.getDueTransactions(currentTime)

        for (recurring in dueTransactions) {
            database.withTransaction {
                // Re-read inside the transaction so duplicate or stale worker runs cannot
                // record the same scheduled occurrence twice.
                val current = recurringDao.getRecurringTransactionById(recurring.id)
                    ?: return@withTransaction
                if (!current.isActive || current.nextExecutionTime > currentTime) return@withTransaction

                transactionDao.insert(
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        amount = current.amount,
                        categoryId = current.categoryId,
                        accountId = current.accountId,
                        transactionDate = current.nextExecutionTime,
                        note = current.note,
                        createdAt = currentTime,
                    ),
                )

                recurringDao.updateRecurringTransaction(
                    current.copy(
                        nextExecutionTime = nextExecutionAfter(
                            current.nextExecutionTime,
                            currentTime,
                            current.frequency,
                        ),
                    ),
                )
            }
        }

        return Result.success()
    }

    private fun calculateNextExecutionTime(currentTime: Long, frequency: String): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime
        when (frequency) {
            "daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "monthly" -> calendar.add(Calendar.MONTH, 1)
            "yearly" -> calendar.add(Calendar.YEAR, 1)
            else -> error("Frekuensi transaksi berulang tidak valid: $frequency")
        }
        return calendar.timeInMillis
    }

    private fun nextExecutionAfter(scheduledTime: Long, now: Long, frequency: String): Long {
        var next = calculateNextExecutionTime(scheduledTime, frequency)
        while (next <= now) next = calculateNextExecutionTime(next, frequency)
        return next
    }
}
