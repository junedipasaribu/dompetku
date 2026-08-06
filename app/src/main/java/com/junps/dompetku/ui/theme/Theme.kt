package com.junps.dompetku.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val DompetGreen = Color(0xFF245C4F)
val DompetPaleGreen = Color(0xFFE8EEEB)
val DompetRed = Color(0xFF9B4545)
val DompetBackground = Color(0xFFF7F7F4)

private val DompetKuColorScheme = lightColorScheme(
    primary = DompetGreen,
    background = DompetBackground,
    surface = Color.White,
    surfaceVariant = Color(0xFFECEDE9),
    onSurface = Color(0xFF202220),
    onSurfaceVariant = Color(0xFF666A66),
    error = DompetRed,
)

private val DompetKuShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

@Composable
fun DompetKuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DompetKuColorScheme,
        shapes = DompetKuShapes,
        content = content,
    )
}
