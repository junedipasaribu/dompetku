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
        var processedOccurrences = 0
        var hasRemainingDueOccurrences = false

        for (recurring in dueTransactions) {
            if (processedOccurrences >= MAX_OCCURRENCES_PER_RUN) {
                hasRemainingDueOccurrences = true
                break
            }

            var currentStillDue = false
            database.withTransaction {
                // Re-read inside the transaction so duplicate or stale worker runs cannot
                // record the same scheduled occurrence twice. Keep each occurrence and its
                // schedule advance atomic so a retry cannot duplicate a posted transaction.
                val current = recurringDao.getRecurringTransactionById(recurring.id)
                    ?: return@withTransaction
                if (!current.isActive || current.nextExecutionTime > currentTime) return@withTransaction

                var nextExecutionTime = current.nextExecutionTime
                while (
                    nextExecutionTime <= currentTime &&
                    processedOccurrences < MAX_OCCURRENCES_PER_RUN
                ) {
                    transactionDao.insert(
                        TransactionEntity(
                            id = UUID.randomUUID().toString(),
                            amount = current.amount,
                            categoryId = current.categoryId,
                            accountId = current.accountId,
                            transactionDate = nextExecutionTime,
                            note = current.note,
                            createdAt = currentTime,
                        ),
                    )
                    processedOccurrences++
                    nextExecutionTime = calculateNextExecutionTime(nextExecutionTime, current.frequency)
                }

                recurringDao.updateRecurringTransaction(
                    current.copy(nextExecutionTime = nextExecutionTime),
                )
                currentStillDue = nextExecutionTime <= currentTime
            }

            if (currentStillDue) {
                hasRemainingDueOccurrences = true
                break
            }
        }

        return if (hasRemainingDueOccurrences) Result.retry() else Result.success()
    }

    private fun calculateNextExecutionTime(scheduledTime: Long, frequency: String): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = scheduledTime
        when (frequency) {
            "daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "monthly" -> calendar.add(Calendar.MONTH, 1)
            "yearly" -> calendar.add(Calendar.YEAR, 1)
            else -> error("Frekuensi transaksi berulang tidak valid: $frequency")
        }
        return calendar.timeInMillis
    }

    private companion object {
        const val MAX_OCCURRENCES_PER_RUN = 500
    }
}
