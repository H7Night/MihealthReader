package com.jhon.mihealthreader.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.R

@Composable
fun GhMetricTile(
    title: String,
    value: String,
    unit: String?,
    status: String?,
    tone: GhTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = MaterialTheme.typography.displaySmall,
    chart: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = ghToneContainer(scheme, tone),
        contentColor = ghToneContent(scheme, tone),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            Modifier.padding(16.dp).heightIn(min = 200.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = valueStyle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                unit?.let {
                    Text(
                        " $it",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(56.dp), content = chart)
            val pill = status ?: if (value == "—") stringResource(R.string.label_no_data) else null
            pill?.let {
                GhStatusPill(
                    it,
                    container = scheme.surfaceContainerLowest,
                    contentColor = scheme.onSurfaceVariant,
                )
            }
        }
    }
}


