package com.junps.dompetku.data.mapper

import com.junps.dompetku.data.local.entity.AccountEntity
import com.junps.dompetku.domain.model.Account

fun AccountEntity.toAccount(): Account = Account(
    id = id,
    name = name,
    initialBalance = initialBalance,
    createdAt = createdAt,
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    initialBalance = initialBalance,
    createdAt = createdAt,
)
