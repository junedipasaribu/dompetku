package com.junps.dompetku.di

import com.junps.dompetku.data.repository.OfflineAnalyticsRepository
import com.junps.dompetku.domain.repository.AnalyticsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(
        implementation: OfflineAnalyticsRepository,
    ): AnalyticsRepository
}
