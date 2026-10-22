package com.junps.dompetku.domain.model

data class AccountWithBalance(
    val id: String,
    val name: String,
    val initialBalance: Long = 0,
    val createdAt: Long,
    val balance: Long,
)
