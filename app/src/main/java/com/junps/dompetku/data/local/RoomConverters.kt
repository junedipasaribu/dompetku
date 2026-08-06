package com.junps.dompetku.data.local

import androidx.room.TypeConverter
import com.junps.dompetku.domain.model.TransactionType

class RoomConverters {
    @TypeConverter
    fun transactionTypeToString(value: TransactionType): String = value.name

    @TypeConverter
    fun stringToTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}
