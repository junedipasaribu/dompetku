package com.junps.dompetku.data.local.model

data class AccountWithBalanceRow(
    val id: String,
    val name: String,
    val initialBalance: Long,
    val createdAt: Long,
    val balance: Long,
)
