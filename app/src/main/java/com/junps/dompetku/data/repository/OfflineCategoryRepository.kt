package com.junps.dompetku.data.repository

import com.junps.dompetku.data.local.dao.CategoryDao
import com.junps.dompetku.data.mapper.toDomain
import com.junps.dompetku.data.mapper.toEntity
import com.junps.dompetku.domain.model.Category
import com.junps.dompetku.domain.model.TransactionType
import com.junps.dompetku.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineCategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
) : CategoryRepository {
    override fun observeAll(): Flow<List<Category>> = categoryDao.observeAll().map { categories ->
        categories.map { it.toDomain() }
    }

    override fun observeByType(type: TransactionType): Flow<List<Category>> =
        categoryDao.observeByType(type).map { categories -> categories.map { it.toDomain() } }

    override suspend fun getById(id: String): Category? = categoryDao.getById(id)?.toDomain()

    override suspend fun existsByName(type: TransactionType, name: String, excludedId: String): Boolean =
        categoryDao.existsByName(type, name, excludedId)

    override suspend fun transactionCount(categoryId: String): Int = categoryDao.transactionCount(categoryId)

    override suspend fun add(category: Category) = categoryDao.insert(category.toEntity())

    override suspend fun update(category: Category) = categoryDao.update(category.toEntity())

    override suspend fun delete(category: Category) = categoryDao.delete(category.toEntity())
}
