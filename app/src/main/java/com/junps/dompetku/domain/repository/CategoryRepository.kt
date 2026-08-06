package com.junps.dompetku.domain.repository

import com.junps.dompetku.domain.model.Category
import com.junps.dompetku.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    fun observeByType(type: TransactionType): Flow<List<Category>>
    suspend fun getById(id: String): Category?
    suspend fun existsByName(type: TransactionType, name: String, excludedId: String = ""): Boolean
    suspend fun transactionCount(categoryId: String): Int
    suspend fun add(category: Category)
    suspend fun update(category: Category)
    suspend fun delete(category: Category)
}
