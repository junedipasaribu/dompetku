package com.junps.dompetku.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.junps.dompetku.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override fun observeAppLockEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[APP_LOCK_ENABLED] ?: false
        }
    }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[APP_LOCK_ENABLED] = enabled
        }
    }

    companion object {
        private val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    }
}
