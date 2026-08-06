package com.junps.dompetku.di

import com.junps.dompetku.data.repository.OfflineDashboardRepository
import com.junps.dompetku.domain.repository.DashboardRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DashboardRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindDashboardRepository(
        implementation: OfflineDashboardRepository,
    ): DashboardRepository
}
