package com.jhon.mihealthreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.jhon.mihealthreader.ui.AppNav
import com.jhon.mihealthreader.ui.ExtractionViewModel
import com.jhon.mihealthreader.ui.theme.MiHealthTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val extractionViewModel: ExtractionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiHealthTheme {
                AppNav(extractionViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        extractionViewModel.onForeground()
    }
}
