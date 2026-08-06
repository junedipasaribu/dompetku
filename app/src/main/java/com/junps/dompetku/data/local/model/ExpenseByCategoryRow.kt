package com.junps.dompetku.data.local.model

data class ExpenseByCategoryRow(
    val categoryId: String,
    val categoryName: String,
    val categoryIconName: String,
    val categoryColorCode: String,
    val totalAmount: Long,
)
