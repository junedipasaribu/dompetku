package com.junps.dompetku.domain.model

data class TransactionWithCategory(
    val id: String,
    val amount: Long,
    val transactionDate: Long,
    val note: String?,
    val categoryId: String,
    val categoryName: String,
    val categoryType: TransactionType,
    val categoryIconName: String,
    val categoryColorCode: String,
)
