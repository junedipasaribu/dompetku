package com.junps.dompetku.domain.time

import com.junps.dompetku.domain.model.MonthRange
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

class MonthRangeCalculator @Inject constructor() {
    fun containing(
        timestamp: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): MonthRange {
        val start = Calendar.getInstance(timeZone).apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        return MonthRange(start.timeInMillis, end.timeInMillis)
    }
}
