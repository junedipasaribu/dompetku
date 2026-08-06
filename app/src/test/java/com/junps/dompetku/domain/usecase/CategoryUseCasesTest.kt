package com.junps.dompetku.domain.usecase

import com.junps.dompetku.domain.model.Category
import com.junps.dompetku.domain.model.TransactionType
import com.junps.dompetku.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CategoryUseCasesTest {
    @Test
    fun save_normalizesName_andAddsCustomCategory() = runBlocking {
        val repository = FakeCategoryRepository()

        SaveCategoryUseCase(repository)(
            SaveCategoryRequest(
                name = "  Makan   malam  ",
                type = TransactionType.EXPENSE,
                iconName = "restaurant",
                colorCode = "#e65100",
            ),
        )

        assertEquals("Makan malam", repository.categories.value.single().name)
        assertEquals("#E65100", repository.categories.value.single().colorCode)
        assertEquals(false, repository.categories.value.single().isDefault)
    }

    @Test
    fun save_rejectsDuplicateNameIgnoringCase() {
        val repository = FakeCategoryRepository(listOf(category(name = "Makanan")))

        val error = assertThrows(CategoryException::class.java) {
            runBlocking {
                SaveCategoryUseCase(repository)(
                    SaveCategoryRequest(
                        name = "makanan",
                        type = TransactionType.EXPENSE,
                        iconName = "restaurant",
                        colorCode = "#E65100",
                    ),
                )
            }
        }

        assertEquals("Kategori dengan nama tersebut sudah ada.", error.message)
    }

    @Test
    fun delete_rejectsDefaultCategory() {
        val repository = FakeCategoryRepository(listOf(category(isDefault = true)))

        val error = assertThrows(CategoryException::class.java) {
            runBlocking { DeleteCategoryUseCase(repository)("category-1") }
        }

        assertEquals("Kategori bawaan tidak dapat dihapus.", error.message)
    }

    @Test
    fun delete_rejectsCategoryUsedByTransaction() {
        val repository = FakeCategoryRepository(listOf(category())).apply {
            transactionCounts["category-1"] = 1
        }

        val error = assertThrows(CategoryException::class.java) {
            runBlocking { DeleteCategoryUseCase(repository)("category-1") }
        }

        assertEquals("Kategori masih digunakan oleh transaksi dan tidak dapat dihapus.", error.message)
    }

    @Test
    fun delete_removesUnusedCustomCategory() = runBlocking {
        val repository = FakeCategoryRepository(listOf(category()))

        DeleteCategoryUseCase(repository)("category-1")

        assertEquals(emptyList<Category>(), repository.categories.value)
    }

    private fun category(
        name: String = "Makanan",
        isDefault: Boolean = false,
    ) = Category(
        id = "category-1",
        name = name,
        type = TransactionType.EXPENSE,
        iconName = "restaurant",
        colorCode = "#E65100",
        isDefault = isDefault,
    )
}

private class FakeCategoryRepository(initial: List<Category> = emptyList()) : CategoryRepository {
    val categories = MutableStateFlow(initial)
    val transactionCounts = mutableMapOf<String, Int>()

    override fun observeAll(): Flow<List<Category>> = categories

    override fun observeByType(type: TransactionType): Flow<List<Category>> =
        categories.map { values -> values.filter { it.type == type } }

    override suspend fun getById(id: String): Category? = categories.value.find { it.id == id }

    override suspend fun existsByName(type: TransactionType, name: String, excludedId: String): Boolean =
        categories.value.any { it.id != excludedId && it.type == type && it.name.equals(name, ignoreCase = true) }

    override suspend fun transactionCount(categoryId: String): Int = transactionCounts[categoryId] ?: 0

    override suspend fun add(category: Category) {
        categories.value += category
    }

    override suspend fun update(category: Category) {
        categories.value = categories.value.map { if (it.id == category.id) category else it }
    }

    override suspend fun delete(category: Category) {
        categories.value = categories.value.filterNot { it.id == category.id }
    }
}
