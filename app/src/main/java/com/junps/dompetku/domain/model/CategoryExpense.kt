package com.junps.dompetku.domain.model

data class CategoryExpense(
    val categoryId: String,
    val categoryName: String,
    val categoryIconName: String,
    val categoryColorCode: String,
    val amount: Long,
)
