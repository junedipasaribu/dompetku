package com.junps.dompetku.data.mapper

import com.junps.dompetku.data.local.entity.CategoryEntity
import com.junps.dompetku.domain.model.Category

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    type = type,
    iconName = iconName,
    colorCode = colorCode,
    isDefault = isDefault,
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    type = type,
    iconName = iconName,
    colorCode = colorCode,
    isDefault = isDefault,
)
