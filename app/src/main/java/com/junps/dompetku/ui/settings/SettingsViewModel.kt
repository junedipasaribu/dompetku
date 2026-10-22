package com.junps.dompetku.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.junps.dompetku.core.backup.BackupManager
import com.junps.dompetku.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager,
) : ViewModel() {

    val isAppLockEnabled: StateFlow<Boolean> = settingsRepository.observeAppLockEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAppLockEnabled(enabled)
        }
    }

    suspend fun backupDatabase(destination: Uri, password: CharArray): Boolean =
        backupManager.backupDatabase(destination, password)

    suspend fun restoreDatabase(source: Uri, password: CharArray): Boolean =
        backupManager.restoreDatabase(source, password)
}
