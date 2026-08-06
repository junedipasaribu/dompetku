package com.junps.dompetku.data.local

import com.junps.dompetku.data.local.entity.CategoryEntity
import com.junps.dompetku.domain.model.TransactionType

object DefaultCategories {
    val all = listOf(
        CategoryEntity("income_salary", "Gaji", TransactionType.INCOME, "payments", "#176B45", true),
        CategoryEntity("income_bonus", "Bonus", TransactionType.INCOME, "redeem", "#2E7D32", true),
        CategoryEntity("income_other", "Pemasukan Lain", TransactionType.INCOME, "add_card", "#00897B", true),
        CategoryEntity("expense_food", "Makanan", TransactionType.EXPENSE, "restaurant", "#E65100", true),
        CategoryEntity("expense_transport", "Transportasi", TransactionType.EXPENSE, "directions_car", "#1565C0", true),
        CategoryEntity("expense_bills", "Tagihan", TransactionType.EXPENSE, "receipt_long", "#6A1B9A", true),
        CategoryEntity("expense_shopping", "Belanja", TransactionType.EXPENSE, "shopping_bag", "#C2185B", true),
        CategoryEntity("expense_health", "Kesehatan", TransactionType.EXPENSE, "medical_services", "#C62828", true),
        CategoryEntity("expense_education", "Pendidikan", TransactionType.EXPENSE, "school", "#4527A0", true),
        CategoryEntity("expense_other", "Pengeluaran Lain", TransactionType.EXPENSE, "more_horiz", "#546E7A", true),
    )
}
