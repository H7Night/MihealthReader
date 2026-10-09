package com.jhon.mihealthreader.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChartsScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("视图", style = MaterialTheme.typography.headlineSmall)
        Text("图表开发中…", modifier = Modifier.padding(top = 12.dp))
    }
}
