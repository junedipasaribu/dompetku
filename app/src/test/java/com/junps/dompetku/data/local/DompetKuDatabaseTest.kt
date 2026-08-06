package com.junps.dompetku.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.junps.dompetku.data.local.entity.CategoryEntity
import com.junps.dompetku.data.local.entity.TransactionEntity
import com.junps.dompetku.domain.model.TransactionType
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DompetKuDatabaseTest {
    private lateinit var database: DompetKuDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DompetKuDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    @Throws(IOException::class)
    fun closeDatabase() = database.close()

    @Test
    fun categoryDao_canInsertReadUpdateAndDelete() = runBlocking {
        val dao = database.categoryDao()
        val original = CategoryEntity(
            id = "custom_food",
            name = "Jajan",
            type = TransactionType.EXPENSE,
            iconName = "restaurant",
            colorCode = "#E65100",
        )

        dao.insert(original)
        assertEquals(original, dao.getById(original.id))
        assertEquals(
            true,
            dao.existsByName(TransactionType.EXPENSE, "JAJAN", excludedId = "another-id"),
        )
        assertEquals(
            false,
            dao.existsByName(TransactionType.EXPENSE, "Jajan", excludedId = original.id),
        )

        val updated = original.copy(name = "Makan di luar")
        dao.update(updated)
        assertEquals(updated, dao.getById(original.id))

        dao.delete(updated)
        assertEquals(null, dao.getById(original.id))
    }

    @Test
    fun transactionDao_canInsertReadUpdateAndDelete() = runBlocking {
        val category = DefaultCategories.all.first()
        database.categoryDao().insert(category)
        val dao = database.transactionDao()
        val original = TransactionEntity(
            id = "transaction_1",
            amount = 1_000_000,
            categoryId = category.id,
            transactionDate = 1_700_000_000_000,
            note = "Test",
            createdAt = 1_700_000_000_000,
        )

        dao.insert(original)
        assertNotNull(dao.getById(original.id))
        assertEquals(listOf(original), dao.observeAll().first())
        assertEquals(1, database.categoryDao().transactionCount(category.id))

        val updated = original.copy(amount = 1_500_000)
        dao.update(updated)
        assertEquals(updated, dao.getById(original.id))

        dao.delete(updated)
        assertEquals(0, dao.count())
    }

    @Test
    fun defaultCategories_areIdempotent() = runBlocking {
        val dao = database.categoryDao()

        dao.insertAll(DefaultCategories.all)
        dao.insertAll(DefaultCategories.all)

        assertEquals(DefaultCategories.all.size, dao.count())
    }

    @Test
    fun financialSummary_usesAllTimeBalance_andExclusiveMonthRange() = runBlocking {
        val income = DefaultCategories.all.first { it.type == TransactionType.INCOME }
        val expense = DefaultCategories.all.first { it.type == TransactionType.EXPENSE }
        database.categoryDao().insertAll(listOf(income, expense))
        val transactions = listOf(
            TransactionEntity("old-income", 1_000, income.id, 999, null, 1),
            TransactionEntity("month-income", 500, income.id, 1_000, null, 2),
            TransactionEntity("month-expense", 200, expense.id, 1_999, null, 3),
            TransactionEntity("next-month-expense", 50, expense.id, 2_000, null, 4),
        )
        transactions.forEach { database.transactionDao().insert(it) }

        val summary = database.transactionDao().observeFinancialSummary(1_000, 2_000).first()

        assertEquals(1_250, summary.totalBalance)
        assertEquals(500, summary.monthlyIncome)
        assertEquals(200, summary.monthlyExpense)
    }

    @Test
    fun recentTransactions_areJoinedOrderedAndLimited() = runBlocking {
        val category = DefaultCategories.all.first()
        database.categoryDao().insert(category)
        listOf(
            TransactionEntity("old", 100, category.id, 1_000, null, 1),
            TransactionEntity("new", 300, category.id, 3_000, "Terbaru", 3),
            TransactionEntity("middle", 200, category.id, 2_000, null, 2),
        ).forEach { database.transactionDao().insert(it) }

        val recent = database.transactionDao().observeRecentWithCategory(2).first()
        val all = database.transactionDao().observeAllWithCategory().first()

        assertEquals(listOf("new", "middle"), recent.map { it.id })
        assertEquals(listOf("new", "middle", "old"), all.map { it.id })
        assertEquals(category.name, recent.first().categoryName)
        assertEquals(category.type, recent.first().categoryType)
    }

    @Test
    fun expensesByCategory_groupsOnlyExpensesInsideSelectedMonth() = runBlocking {
        val food = CategoryEntity("food", "Makanan", TransactionType.EXPENSE, "restaurant", "#E65100")
        val transport = CategoryEntity("transport", "Transportasi", TransactionType.EXPENSE, "directions_car", "#1565C0")
        val salary = CategoryEntity("salary", "Gaji", TransactionType.INCOME, "payments", "#2E7D32")
        database.categoryDao().insertAll(listOf(food, transport, salary))
        listOf(
            TransactionEntity("food-1", 40_000, food.id, 1_000, null, 1),
            TransactionEntity("food-2", 60_000, food.id, 1_500, null, 2),
            TransactionEntity("transport-1", 75_000, transport.id, 1_999, null, 3),
            TransactionEntity("before-month", 999_000, food.id, 999, null, 4),
            TransactionEntity("next-month", 999_000, transport.id, 2_000, null, 5),
            TransactionEntity("income", 5_000_000, salary.id, 1_250, null, 6),
        ).forEach { database.transactionDao().insert(it) }

        val expenses = database.transactionDao().observeExpensesByCategory(1_000, 2_000).first()

        assertEquals(listOf("food", "transport"), expenses.map { it.categoryId })
        assertEquals(listOf(100_000L, 75_000L), expenses.map { it.totalAmount })
        assertEquals(listOf("Makanan", "Transportasi"), expenses.map { it.categoryName })
    }

    @Test
    fun transactionsInRange_areJoinedOrderedAndRespectExclusiveEnd() = runBlocking {
        val category = DefaultCategories.all.first()
        database.categoryDao().insert(category)
        listOf(
            TransactionEntity("inside-later", 200, category.id, 1_500, null, 2),
            TransactionEntity("inside-first", 100, category.id, 1_000, null, 1),
            TransactionEntity("before", 50, category.id, 999, null, 3),
            TransactionEntity("next", 300, category.id, 2_000, null, 4),
        ).forEach { database.transactionDao().insert(it) }

        val result = database.transactionDao().observeWithCategoryInRange(1_000, 2_000).first()

        assertEquals(listOf("inside-first", "inside-later"), result.map { it.id })
        assertEquals(category.name, result.first().categoryName)
    }
}
