package com.junps.dompetku.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val amount: Long,
    val categoryId: String,
    val accountId: String,
    val type: String, // "income" or "expense"
    val note: String,
    val frequency: String, // "daily", "weekly", "monthly", "yearly"
    val nextExecutionTime: Long,
    val isActive: Boolean = true
)
