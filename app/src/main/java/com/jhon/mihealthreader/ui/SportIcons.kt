package com.jhon.mihealthreader.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.ui.graphics.vector.ImageVector

object SportIcons {
    fun of(type: String): ImageVector = when {
        type.contains("running") -> Icons.Filled.DirectionsRun
        type.contains("riding") || type.contains("cycling") -> Icons.Filled.DirectionsBike
        type.contains("walking") -> Icons.Filled.DirectionsWalk
        type.contains("strength") || type.contains("training") -> Icons.Filled.FitnessCenter
        type.contains("swimming") -> Icons.Filled.Pool
        type.contains("climbing") -> Icons.Filled.Terrain
        type.contains("rope") || type.contains("skipping") -> Icons.Filled.SportsGymnastics
        type.contains("pingpong") || type.contains("badminton") -> Icons.Filled.SportsTennis
        else -> Icons.Filled.DirectionsRun
    }
}
