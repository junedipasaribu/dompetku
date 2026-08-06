package com.junps.dompetku.di

import com.junps.dompetku.data.repository.OfflineHistoryRepository
import com.junps.dompetku.data.repository.OfflineTransactionRepository
import com.junps.dompetku.domain.repository.HistoryRepository
import com.junps.dompetku.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HistoryRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindHistoryRepository(
        implementation: OfflineHistoryRepository,
    ): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        implementation: OfflineTransactionRepository,
    ): TransactionRepository
}
