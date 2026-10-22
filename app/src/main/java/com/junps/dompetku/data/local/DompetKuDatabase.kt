package com.junps.dompetku.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.junps.dompetku.data.local.dao.AccountDao
import com.junps.dompetku.data.local.dao.CategoryDao
import com.junps.dompetku.data.local.dao.RecurringTransactionDao
import com.junps.dompetku.data.local.dao.TransactionDao
import com.junps.dompetku.data.local.entity.AccountEntity
import com.junps.dompetku.data.local.entity.CategoryEntity
import com.junps.dompetku.data.local.entity.RecurringTransactionEntity
import com.junps.dompetku.data.local.entity.TransactionEntity

@Database(
    entities = [CategoryEntity::class, TransactionEntity::class, AccountEntity::class, RecurringTransactionEntity::class],
    version = 5,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class DompetKuDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `accounts` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `initialBalance` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                val currentTime = System.currentTimeMillis()
                db.execSQL("INSERT INTO `accounts` (`id`, `name`, `initialBalance`, `createdAt`) VALUES ('default_account', 'Dompet Utama', 0, $currentTime)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `transactions_new` (`id` TEXT NOT NULL, `amount` INTEGER NOT NULL, `categoryId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `transactionDate` INTEGER NOT NULL, `note` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
                )
                
                db.execSQL(
                    "INSERT INTO `transactions_new` (`id`, `amount`, `categoryId`, `accountId`, `transactionDate`, `note`, `createdAt`) " +
                    "SELECT `id`, `amount`, `categoryId`, 'default_account', `transactionDate`, `note`, `createdAt` FROM `transactions`"
                )
                
                db.execSQL("DROP TABLE `transactions`")
                db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
                
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_transactionDate` ON `transactions` (`transactionDate`)")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `monthlyBudget` INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recurring_transactions` (`id` TEXT NOT NULL, `amount` INTEGER NOT NULL, `categoryId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `type` TEXT NOT NULL, `note` TEXT NOT NULL, `frequency` TEXT NOT NULL, `nextExecutionTime` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }
    }
}
