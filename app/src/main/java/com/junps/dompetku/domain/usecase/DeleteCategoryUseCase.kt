package com.junps.dompetku.domain.usecase

import com.junps.dompetku.domain.repository.CategoryRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(categoryId: String) {
        val category = repository.getById(categoryId)
            ?: throw CategoryException("Kategori tidak ditemukan.")
        if (category.isDefault) {
            throw CategoryException("Kategori bawaan tidak dapat dihapus.")
        }
        if (repository.transactionCount(categoryId) > 0) {
            throw CategoryException("Kategori masih digunakan oleh transaksi dan tidak dapat dihapus.")
        }
        repository.delete(category)
    }
}
