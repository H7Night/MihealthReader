package com.jhon.mihealthreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.jhon.mihealthreader.ui.AppNav
import com.jhon.mihealthreader.ui.ExtractionViewModel
import com.jhon.mihealthreader.ui.theme.GhTheme
import com.jhon.mihealthreader.ui.theme.GhWindowEffects
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val extractionViewModel: ExtractionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            GhTheme {
                GhWindowEffects()
                AppNav(extractionViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        extractionViewModel.onForeground()
    }
}
