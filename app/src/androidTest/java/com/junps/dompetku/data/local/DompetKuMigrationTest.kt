package com.junps.dompetku.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DompetKuMigrationTest {
    private val databaseName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DompetKuDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2_preservesDataAndAddsCategoryUniqueness() {
        helper.createDatabase(databaseName, 1).apply {
            execSQL(
                """
                INSERT INTO categories (id, name, type, iconName, colorCode, isDefault)
                VALUES ('salary', 'Gaji', 'INCOME', 'payments', '#2E7D32', 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO transactions
                    (id, amount, categoryId, transactionDate, note, createdAt)
                VALUES ('tx-1', 5000000, 'salary', 1700000000000, 'November', 1700000000000)
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            databaseName,
            2,
            true,
            DompetKuDatabase.MIGRATION_1_2,
        ).use { database ->
            database.query("SELECT name FROM categories WHERE id = 'salary'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Gaji", cursor.getString(0))
            }
            database.query("SELECT amount FROM transactions WHERE id = 'tx-1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(5_000_000L, cursor.getLong(0))
            }
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'index' " +
                    "AND name = 'index_categories_name_type'",
            ).use { cursor ->
                assertEquals(1, cursor.count)
            }
        }
    }
}
