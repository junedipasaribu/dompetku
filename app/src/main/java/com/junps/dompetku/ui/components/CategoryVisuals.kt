package com.junps.dompetku.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.toColorInt

fun categoryColor(colorCode: String): Color = runCatching {
    Color(colorCode.toColorInt())
}.getOrDefault(Color.Gray)

fun categoryIcon(iconName: String): ImageVector = when (iconName) {
    "restaurant" -> Icons.Default.Restaurant
    "directions_car" -> Icons.Default.DirectionsCar
    "payments" -> Icons.Default.Payments
    "redeem" -> Icons.Default.Redeem
    "add_card" -> Icons.Default.AddCard
    "attach_money" -> Icons.Default.AttachMoney
    "receipt_long" -> Icons.AutoMirrored.Filled.ReceiptLong
    "shopping_bag" -> Icons.Default.ShoppingBag
    "medical_services" -> Icons.Default.MedicalServices
    "school" -> Icons.Default.School
    else -> Icons.Default.MoreHoriz
}

fun categoryIconLabel(iconName: String): String = when (iconName) {
    "restaurant" -> "Makanan"
    "directions_car" -> "Transportasi"
    "payments" -> "Pembayaran"
    "redeem" -> "Hadiah"
    "add_card" -> "Kartu"
    "attach_money" -> "Uang"
    "receipt_long" -> "Tagihan"
    "shopping_bag" -> "Belanja"
    "medical_services" -> "Kesehatan"
    "school" -> "Pendidikan"
    else -> "Lainnya"
}

fun categoryColorLabel(colorCode: String): String = when (colorCode.uppercase()) {
    "#176B45" -> "Hijau"
    "#1565C0" -> "Biru"
    "#6A1B9A" -> "Ungu"
    "#E65100" -> "Oranye"
    "#C62828" -> "Merah"
    else -> "Warna kategori"
}

val CategoryIconOptions = listOf(
    "restaurant",
    "directions_car",
    "payments",
    "add_card",
    "attach_money",
)

val CategoryColorOptions = listOf("#176B45", "#1565C0", "#6A1B9A", "#E65100", "#C62828")
