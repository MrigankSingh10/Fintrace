package com.fintrace.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.fintrace.app.data.local.converter.Converters
import com.fintrace.app.data.local.dao.CategoryDao
import com.fintrace.app.data.local.dao.CardMappingDao
import com.fintrace.app.data.local.dao.MonthlyBudgetDao
import com.fintrace.app.data.local.dao.PaymentModeDao
import com.fintrace.app.data.local.dao.TransactionDao
import com.fintrace.app.data.local.entity.CardMappingEntity
import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.local.entity.MonthlyBudgetSalaryEntity
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.local.entity.TransactionSplitEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        PaymentModeEntity::class,
        TransactionEntity::class,
        TransactionSplitEntity::class,
        MonthlyBudgetSalaryEntity::class,
        CardMappingEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun paymentModeDao(): PaymentModeDao
    abstract fun transactionDao(): TransactionDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
    abstract fun cardMappingDao(): CardMappingDao

    companion object {
        private const val DATABASE_NAME = "finance_tracker_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN parseConfidence TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN cardLastFour TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS card_mappings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        cardLastFour TEXT NOT NULL,
                        paymentModeId INTEGER NOT NULL,
                        label TEXT,
                        FOREIGN KEY (paymentModeId) REFERENCES payment_modes(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_card_mappings_cardLastFour ON card_mappings(cardLastFour)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_card_mappings_paymentModeId ON card_mappings(paymentModeId)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN currency TEXT NOT NULL DEFAULT 'INR'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE monthly_budgets ADD COLUMN salary_mode TEXT NOT NULL DEFAULT 'OVERRIDE'")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Only seed on first install (DB creation).
                            // onOpen is intentionally NOT overridden — we never want to
                            // re-seed on subsequent launches, which would resurrect deleted items.
                            INSTANCE?.let { database ->
                                scope.launch {
                                    DatabaseSeeder.seedDatabaseIfEmpty(
                                        database.categoryDao(),
                                        database.paymentModeDao()
                                    )
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
