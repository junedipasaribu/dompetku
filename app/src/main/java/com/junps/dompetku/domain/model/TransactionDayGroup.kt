package com.junps.dompetku.domain.model

data class TransactionDayGroup(
    val dayStart: Long,
    val transactions: List<TransactionWithCategory>,
)
