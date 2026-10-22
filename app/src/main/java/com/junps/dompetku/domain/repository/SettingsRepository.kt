package com.junps.dompetku.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeAppLockEnabled(): Flow<Boolean>
    suspend fun setAppLockEnabled(enabled: Boolean)
}
