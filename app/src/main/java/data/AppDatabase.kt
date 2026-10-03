package com.dentalscan.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ScanEntity::class, ProfileEntity::class], version = 5)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scanDao(): ScanDao
    abstract fun profileDao(): ProfileDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration 1 → 2: adds profiles table
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS profiles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        relationship TEXT NOT NULL,
                        age INTEGER NOT NULL,
                        imagePath TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
            }
        }

        // Migration 2 → 3: wraps existing single imagePath into JSON array
        // e.g. "/data/scan.jpg" becomes ["/data/scan.jpg"]
        // Rows already in JSON format (starting with '[') are safely skipped
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    UPDATE scans
                    SET imagePath = '["' || replace(imagePath, '"', '\"') || '"]'
                    WHERE imagePath NOT LIKE '[%'
                    AND imagePath != ''
                    """.trimIndent()
                )
            }
        }

        // Migration 3 → 4: adds the Action Plan checklist state (nullable, so existing rows stay valid)
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE scans ADD COLUMN checkedSteps TEXT")
            }
        }

        // Migration 4 → 5: adds the per-profile Dental Health Score (existing profiles start at 100)
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE profiles ADD COLUMN healthScore INTEGER NOT NULL DEFAULT 100")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dental_scan_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}