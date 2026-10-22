package com.junps.dompetku.domain.model

data class Account(
    val id: String,
    val name: String,
    val initialBalance: Long = 0,
    val createdAt: Long,
)
