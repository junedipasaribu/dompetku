package com.junps.dompetku.domain.model

data class Transaction(
    val id: String,
    val amount: Long,
    val categoryId: String,
    val transactionDate: Long,
    val note: String?,
    val createdAt: Long,
)
