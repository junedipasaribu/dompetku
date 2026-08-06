package com.junps.dompetku.di

import com.junps.dompetku.data.repository.OfflineCategoryRepository
import com.junps.dompetku.domain.repository.CategoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CategoryRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        implementation: OfflineCategoryRepository,
    ): CategoryRepository
}
