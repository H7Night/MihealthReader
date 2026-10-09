package com.jhon.mihealthreader.ui

import androidx.compose.ui.graphics.Color

object SportColors {
    fun of(type: String): Color = when {
        type.contains("running") -> Color(0xFF00B894)
        type.contains("riding") || type.contains("cycling") -> Color(0xFF0984E3)
        type.contains("walking") -> Color(0xFF00CEC9)
        type.contains("strength") || type.contains("training") -> Color(0xFFFF7043)
        type.contains("swimming") -> Color(0xFF00B8D4)
        type.contains("climbing") -> Color(0xFF8E44AD)
        type.contains("rope") || type.contains("skipping") -> Color(0xFFE17055)
        type.contains("pingpong") || type.contains("badminton") -> Color(0xFF00A8A8)
        else -> Color(0xFF6C5CE7)
    }
}
