package com.junps.dompetku.data.local.model

import com.junps.dompetku.domain.model.TransactionType

data class TransactionWithCategoryRow(
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
