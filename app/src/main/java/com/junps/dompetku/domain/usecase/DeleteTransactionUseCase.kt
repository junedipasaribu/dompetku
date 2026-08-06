package com.junps.dompetku.domain.usecase

import com.junps.dompetku.domain.repository.TransactionRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(transactionId: String) {
        val transaction = repository.getById(transactionId)
            ?: throw TransactionException("Transaksi tidak ditemukan.")
        repository.delete(transaction)
    }
}
