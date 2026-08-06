package com.junps.dompetku.domain.usecase

import com.junps.dompetku.domain.model.Category
import com.junps.dompetku.domain.model.Transaction
import com.junps.dompetku.domain.model.TransactionType
import com.junps.dompetku.domain.repository.CategoryRepository
import com.junps.dompetku.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TransactionUseCasesTest {
    @Test
    fun save_addsValidTransaction_andTrimsBlankNoteToNull() = runBlocking {
        val categories = TransactionFakeCategoryRepository(listOf(testCategory))
        val transactions = FakeTransactionRepository()

        SaveTransactionUseCase(transactions, categories)(
            SaveTransactionRequest(
                amount = 25_000,
                categoryId = testCategory.id,
                transactionDate = 1_700_000_000_000,
                note = "   ",
            ),
        )

        val saved = transactions.values.value.single()
        assertEquals(25_000, saved.amount)
        assertEquals(null, saved.note)
    }

    @Test
    fun save_updatesExistingTransaction_andPreservesCreatedAt() = runBlocking {
        val original = testTransaction
        val categories = TransactionFakeCategoryRepository(listOf(testCategory))
        val transactions = FakeTransactionRepository(listOf(original))

        SaveTransactionUseCase(transactions, categories)(
            SaveTransactionRequest(
                id = original.id,
                amount = 75_000,
                categoryId = testCategory.id,
                transactionDate = original.transactionDate,
                note = "Diubah",
            ),
        )

        assertEquals(75_000, transactions.values.value.single().amount)
        assertEquals(original.createdAt, transactions.values.value.single().createdAt)
    }

    @Test
    fun save_rejectsZeroAmount() {
        val useCase = SaveTransactionUseCase(
            FakeTransactionRepository(),
            TransactionFakeCategoryRepository(listOf(testCategory)),
        )

        val error = assertThrows(TransactionException::class.java) {
            runBlocking {
                useCase(SaveTransactionRequest(amount = 0, categoryId = testCategory.id, transactionDate = 1, note = ""))
            }
        }

        assertEquals("Nominal harus lebih besar dari nol.", error.message)
    }

    @Test
    fun save_rejectsUnknownCategory() {
        val useCase = SaveTransactionUseCase(
            FakeTransactionRepository(),
            TransactionFakeCategoryRepository(),
        )

        val error = assertThrows(TransactionException::class.java) {
            runBlocking {
                useCase(SaveTransactionRequest(amount = 10_000, categoryId = "missing", transactionDate = 1, note = ""))
            }
        }

        assertEquals("Pilih kategori yang valid.", error.message)
    }

    @Test
    fun delete_removesExistingTransaction() = runBlocking {
        val repository = FakeTransactionRepository(listOf(testTransaction))

        DeleteTransactionUseCase(repository)(testTransaction.id)

        assertEquals(emptyList<Transaction>(), repository.values.value)
    }

    private companion object {
        val testCategory = Category(
            id = "expense_food",
            name = "Makanan",
            type = TransactionType.EXPENSE,
            iconName = "restaurant",
            colorCode = "#E65100",
            isDefault = true,
        )
        val testTransaction = Transaction(
            id = "transaction-1",
            amount = 50_000,
            categoryId = testCategory.id,
            transactionDate = 1_700_000_000_000,
            note = null,
            createdAt = 1_699_000_000_000,
        )
    }
}

private class FakeTransactionRepository(initial: List<Transaction> = emptyList()) : TransactionRepository {
    val values = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<Transaction>> = values

    override suspend fun getById(id: String): Transaction? = values.value.find { it.id == id }

    override suspend fun add(transaction: Transaction) {
        values.value += transaction
    }

    override suspend fun update(transaction: Transaction) {
        values.value = values.value.map { if (it.id == transaction.id) transaction else it }
    }

    override suspend fun delete(transaction: Transaction) {
        values.value = values.value.filterNot { it.id == transaction.id }
    }
}

private class TransactionFakeCategoryRepository(initial: List<Category> = emptyList()) : CategoryRepository {
    private val values = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<Category>> = values
    override fun observeByType(type: TransactionType): Flow<List<Category>> =
        values.map { categories -> categories.filter { it.type == type } }
    override suspend fun getById(id: String): Category? = values.value.find { it.id == id }
    override suspend fun existsByName(type: TransactionType, name: String, excludedId: String): Boolean = false
    override suspend fun transactionCount(categoryId: String): Int = 0
    override suspend fun add(category: Category) { values.value += category }
    override suspend fun update(category: Category) {
        values.value = values.value.map { if (it.id == category.id) category else it }
    }
    override suspend fun delete(category: Category) { values.value = values.value.filterNot { it.id == category.id } }
}
