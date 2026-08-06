package com.junps.dompetku.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["transactionDate"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amount: Long,
    val categoryId: String,
    val transactionDate: Long,
    val note: String? = null,
    val createdAt: Long,
)
