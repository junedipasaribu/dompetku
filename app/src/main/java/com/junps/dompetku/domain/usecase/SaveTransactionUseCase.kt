package com.junps.dompetku.domain.usecase

import com.junps.dompetku.domain.model.Transaction
import com.junps.dompetku.domain.repository.CategoryRepository
import com.junps.dompetku.domain.repository.TransactionRepository
import java.util.UUID
import javax.inject.Inject

data class SaveTransactionRequest(
    val id: String? = null,
    val amount: Long,
    val categoryId: String,
    val transactionDate: Long,
    val note: String,
)

class SaveTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(request: SaveTransactionRequest) {
        if (request.amount <= 0) throw TransactionException("Nominal harus lebih besar dari nol.")
        if (request.transactionDate <= 0) throw TransactionException("Tanggal transaksi tidak valid.")
        if (request.note.length > MAX_NOTE_LENGTH) {
            throw TransactionException("Catatan maksimal $MAX_NOTE_LENGTH karakter.")
        }
        if (categoryRepository.getById(request.categoryId) == null) {
            throw TransactionException("Pilih kategori yang valid.")
        }

        val existing = request.id?.let { id ->
            transactionRepository.getById(id)
                ?: throw TransactionException("Transaksi tidak ditemukan.")
        }
        val now = System.currentTimeMillis()
        val transaction = Transaction(
            id = existing?.id ?: UUID.randomUUID().toString(),
            amount = request.amount,
            categoryId = request.categoryId,
            transactionDate = request.transactionDate,
            note = request.note.trim().ifEmpty { null },
            createdAt = existing?.createdAt ?: now,
        )
        if (existing == null) transactionRepository.add(transaction)
        else transactionRepository.update(transaction)
    }

    companion object {
        const val MAX_NOTE_LENGTH = 200
    }
}
