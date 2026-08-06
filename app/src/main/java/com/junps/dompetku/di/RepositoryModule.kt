package com.junps.dompetku.di

import com.junps.dompetku.data.repository.OfflineCategoryRepository
import com.junps.dompetku.data.repository.OfflineAnalyticsRepository
import com.junps.dompetku.data.repository.OfflineDashboardRepository
import com.junps.dompetku.data.repository.OfflineHistoryRepository
import com.junps.dompetku.data.repository.OfflineTransactionRepository
import com.junps.dompetku.domain.repository.CategoryRepository
import com.junps.dompetku.domain.repository.AnalyticsRepository
import com.junps.dompetku.domain.repository.DashboardRepository
import com.junps.dompetku.domain.repository.HistoryRepository
import com.junps.dompetku.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(implementation: OfflineAnalyticsRepository): AnalyticsRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(implementation: OfflineCategoryRepository): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(implementation: OfflineTransactionRepository): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(implementation: OfflineDashboardRepository): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(implementation: OfflineHistoryRepository): HistoryRepository
}
