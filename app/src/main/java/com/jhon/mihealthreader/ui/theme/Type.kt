package com.jhon.mihealthreader.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun style(size: Int, weight: FontWeight, line: Int, tracking: Double = 0.0) =
    TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.sp,
    )

val GhTypography = Typography(
    displayLarge = style(56, FontWeight.Bold, 60, -1.0),
    displayMedium = style(44, FontWeight.Bold, 48, -0.8),
    displaySmall = style(36, FontWeight.Bold, 40, -0.6),
    headlineLarge = style(32, FontWeight.Bold, 38, -0.5),
    headlineMedium = style(28, FontWeight.Bold, 34, -0.3),
    headlineSmall = style(24, FontWeight.SemiBold, 30),
    titleLarge = style(22, FontWeight.SemiBold, 28),
    titleMedium = style(18, FontWeight.Medium, 24),
    titleSmall = style(16, FontWeight.Medium, 22),
    bodyLarge = style(16, FontWeight.Normal, 24),
    bodyMedium = style(14, FontWeight.Normal, 20),
    bodySmall = style(12, FontWeight.Normal, 18),
    labelLarge = style(14, FontWeight.Medium, 20),
    labelMedium = style(12, FontWeight.Medium, 16),
    labelSmall = style(11, FontWeight.Medium, 14),
)
