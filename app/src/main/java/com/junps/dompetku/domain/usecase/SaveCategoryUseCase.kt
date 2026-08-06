package com.junps.dompetku.domain.usecase

import com.junps.dompetku.domain.model.Category
import com.junps.dompetku.domain.model.TransactionType
import com.junps.dompetku.domain.repository.CategoryRepository
import java.util.UUID
import javax.inject.Inject

data class SaveCategoryRequest(
    val id: String? = null,
    val name: String,
    val type: TransactionType,
    val iconName: String,
    val colorCode: String,
)

class SaveCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(request: SaveCategoryRequest) {
        val normalizedName = request.name.trim().replace(Regex("\\s+"), " ")
        if (normalizedName.isBlank()) throw CategoryException("Nama kategori wajib diisi.")
        if (normalizedName.length > MAX_NAME_LENGTH) {
            throw CategoryException("Nama kategori maksimal $MAX_NAME_LENGTH karakter.")
        }
        if (!HEX_COLOR.matches(request.colorCode)) {
            throw CategoryException("Warna kategori tidak valid.")
        }

        val existing = request.id?.let { id ->
            repository.getById(id) ?: throw CategoryException("Kategori tidak ditemukan.")
        }
        val type = existing?.type ?: request.type
        val id = existing?.id ?: UUID.randomUUID().toString()

        if (repository.existsByName(type, normalizedName, id)) {
            throw CategoryException("Kategori dengan nama tersebut sudah ada.")
        }

        val category = Category(
            id = id,
            name = normalizedName,
            type = type,
            iconName = request.iconName,
            colorCode = request.colorCode.uppercase(),
            isDefault = existing?.isDefault ?: false,
        )
        if (existing == null) repository.add(category) else repository.update(category)
    }

    companion object {
        const val MAX_NAME_LENGTH = 40
        private val HEX_COLOR = Regex("^#[0-9A-Fa-f]{6}$")
    }
}
