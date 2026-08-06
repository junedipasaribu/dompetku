package com.junps.dompetku.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.junps.dompetku.data.local.DefaultCategories
import com.junps.dompetku.data.local.DompetKuDatabase
import com.junps.dompetku.data.local.dao.CategoryDao
import com.junps.dompetku.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DompetKuDatabase =
        Room.databaseBuilder(context, DompetKuDatabase::class.java, DompetKuDatabase.DATABASE_NAME)
            .addMigrations(DompetKuDatabase.MIGRATION_1_2)
            .addCallback(defaultCategoryCallback)
            .build()

    @Provides
    fun provideCategoryDao(database: DompetKuDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTransactionDao(database: DompetKuDatabase): TransactionDao = database.transactionDao()

    private val defaultCategoryCallback = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            DefaultCategories.all.forEach { category ->
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO categories
                        (id, name, type, iconName, colorCode, isDefault)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any>(
                        category.id,
                        category.name,
                        category.type.name,
                        category.iconName,
                        category.colorCode,
                        if (category.isDefault) 1 else 0,
                    ),
                )
            }
        }
    }
}
