package com.junps.dompetku.di

import com.junps.dompetku.data.repository.OfflineCategoryRepository
import com.junps.dompetku.data.repository.OfflineTransactionRepository
import com.junps.dompetku.domain.repository.CategoryRepository
import com.junps.dompetku.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TransactionRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        implementation: OfflineCategoryRepository,
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        implementation: OfflineTransactionRepository,
    ): TransactionRepository
}
