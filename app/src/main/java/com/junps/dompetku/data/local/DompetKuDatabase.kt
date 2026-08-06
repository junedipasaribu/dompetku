package com.junps.dompetku.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.junps.dompetku.data.local.dao.CategoryDao
import com.junps.dompetku.data.local.dao.TransactionDao
import com.junps.dompetku.data.local.entity.CategoryEntity
import com.junps.dompetku.data.local.entity.TransactionEntity

@Database(
    entities = [CategoryEntity::class, TransactionEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class DompetKuDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        const val DATABASE_NAME = "dompetku.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_categories_name_type` ON `categories` (`name`, `type`)",
                )
            }
        }
    }
}
