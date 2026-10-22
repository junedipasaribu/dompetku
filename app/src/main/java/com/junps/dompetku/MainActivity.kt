package com.junps.dompetku

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.junps.dompetku.ui.navigation.DompetKuApp
import com.junps.dompetku.ui.theme.DompetKuTheme
import dagger.hilt.android.AndroidEntryPoint

import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.junps.dompetku.core.worker.RecurringTransactionWorker
import com.junps.dompetku.ui.lock.LockScreen
import android.view.WindowManager
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var isUnlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup WorkManager for Recurring Transactions
        val workRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "RecurringTransactionWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        setContent {
            val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()

            SideEffect {
                if (isAppLockEnabled == false) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            DompetKuTheme {
                if (isAppLockEnabled == null) {
                    // Loading state, just show background
                } else if (isAppLockEnabled == true && !isUnlocked) {
                    LockScreen(
                        onUnlockSuccess = { isUnlocked = true }
                    )
                } else {
                    DompetKuApp()
                }
            }
        }
    }

    override fun onStop() {
        isUnlocked = false
        super.onStop()
    }
}
