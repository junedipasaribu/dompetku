package com.junps.dompetku.domain.model

data class Category(
    val id: String,
    val name: String,
    val type: TransactionType,
    val iconName: String,
    val colorCode: String,
    val isDefault: Boolean,
)
