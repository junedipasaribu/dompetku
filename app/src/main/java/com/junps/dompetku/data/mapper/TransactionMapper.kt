package com.junps.dompetku.data.mapper

import com.junps.dompetku.data.local.entity.TransactionEntity
import com.junps.dompetku.data.local.model.TransactionWithCategoryRow
import com.junps.dompetku.domain.model.Transaction
import com.junps.dompetku.domain.model.TransactionWithCategory

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    amount = amount,
    categoryId = categoryId,
    transactionDate = transactionDate,
    note = note,
    createdAt = createdAt,
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amount = amount,
    categoryId = categoryId,
    transactionDate = transactionDate,
    note = note,
    createdAt = createdAt,
)

fun TransactionWithCategoryRow.toDomain(): TransactionWithCategory = TransactionWithCategory(
    id = id,
    amount = amount,
    transactionDate = transactionDate,
    note = note,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryType = categoryType,
    categoryIconName = categoryIconName,
    categoryColorCode = categoryColorCode,
)
