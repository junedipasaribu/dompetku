package com.junps.dompetku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.junps.dompetku.ui.navigation.DompetKuApp
import com.junps.dompetku.ui.theme.DompetKuTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DompetKuTheme {
                DompetKuApp()
            }
        }
    }
}
