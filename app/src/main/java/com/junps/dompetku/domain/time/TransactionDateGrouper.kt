package com.junps.dompetku.domain.time

import com.junps.dompetku.domain.model.TransactionDayGroup
import com.junps.dompetku.domain.model.TransactionWithCategory
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

class TransactionDateGrouper @Inject constructor() {
    fun group(
        transactions: List<TransactionWithCategory>,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): List<TransactionDayGroup> = transactions
        .sortedByDescending { it.transactionDate }
        .groupBy { transaction -> startOfDay(transaction.transactionDate, timeZone) }
        .map { (dayStart, values) -> TransactionDayGroup(dayStart, values) }
        .sortedByDescending { it.dayStart }

    private fun startOfDay(timestamp: Long, timeZone: TimeZone): Long =
        Calendar.getInstance(timeZone).apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
