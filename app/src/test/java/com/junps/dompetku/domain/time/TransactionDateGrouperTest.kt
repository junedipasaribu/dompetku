package com.junps.dompetku.domain.time

import com.junps.dompetku.domain.model.TransactionType
import com.junps.dompetku.domain.model.TransactionWithCategory
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionDateGrouperTest {
    private val grouper = TransactionDateGrouper()

    @Test
    fun group_usesLocalCalendarDateAndOrdersNewestFirst() {
        val zone = TimeZone.getTimeZone("Asia/Jakarta")
        val lateFourth = timestamp(zone, 2026, Calendar.AUGUST, 4, 23, 30)
        val earlyFifth = timestamp(zone, 2026, Calendar.AUGUST, 5, 0, 30)
        val lateFifth = timestamp(zone, 2026, Calendar.AUGUST, 5, 20, 0)

        val groups = grouper.group(
            listOf(
                transaction("old", lateFourth),
                transaction("newest", lateFifth),
                transaction("middle", earlyFifth),
            ),
            zone,
        )

        assertEquals(2, groups.size)
        assertEquals(listOf("newest", "middle"), groups.first().transactions.map { it.id })
        assertEquals(listOf("old"), groups.last().transactions.map { it.id })
        assertEquals(timestamp(zone, 2026, Calendar.AUGUST, 5, 0, 0), groups.first().dayStart)
    }

    @Test
    fun group_emptyInput_returnsEmptyList() {
        assertEquals(emptyList<Any>(), grouper.group(emptyList()))
    }

    private fun transaction(id: String, date: Long) = TransactionWithCategory(
        id = id,
        amount = 10_000,
        transactionDate = date,
        note = null,
        categoryId = "expense_food",
        categoryName = "Makanan",
        categoryType = TransactionType.EXPENSE,
        categoryIconName = "restaurant",
        categoryColorCode = "#E65100",
    )

    private fun timestamp(
        zone: TimeZone,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long = Calendar.getInstance(zone).apply {
        clear()
        set(year, month, day, hour, minute, 0)
    }.timeInMillis
}
