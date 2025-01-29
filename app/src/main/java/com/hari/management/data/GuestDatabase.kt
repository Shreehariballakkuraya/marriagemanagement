package com.hari.management.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters

@Database(
    entities = [GuestEntity::class, GuestCategory::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GuestDatabase : RoomDatabase() {
    abstract fun guestDao(): GuestDao
    
    companion object {
        @Volatile
        private var INSTANCE: GuestDatabase? = null
        
        fun getDatabase(context: Context): GuestDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GuestDatabase::class.java,
                    "guest_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create categories table
                db.execSQL("""
                    CREATE TABLE guest_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        color INTEGER NOT NULL
                    )
                """)

                // Add categoryId to guests table
                db.execSQL("ALTER TABLE guests ADD COLUMN categoryId INTEGER")
                
                // Create index for categoryId
                db.execSQL("CREATE INDEX index_guests_categoryId ON guests(categoryId)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recreate guest_categories table
                db.execSQL("DROP TABLE IF EXISTS guest_categories")
                db.execSQL("""
                    CREATE TABLE guest_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        color INTEGER NOT NULL
                    )
                """)

                // Create temporary table with new schema
                db.execSQL("""
                    CREATE TABLE guests_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        phoneNumber TEXT NOT NULL,
                        isInvitationVerified INTEGER NOT NULL DEFAULT 0,
                        reminderDate INTEGER,
                        categoryId INTEGER,
                        FOREIGN KEY(categoryId) REFERENCES guest_categories(id) ON DELETE SET NULL
                    )
                """)

                // Copy data from old table to new table (only existing columns)
                db.execSQL("""
                    INSERT INTO guests_new (id, name, phoneNumber, isInvitationVerified, reminderDate)
                    SELECT id, name, phoneNumber, isInvitationVerified, reminderDate
                    FROM guests
                """)

                // Drop old table
                db.execSQL("DROP TABLE guests")

                // Rename new table to guests
                db.execSQL("ALTER TABLE guests_new RENAME TO guests")

                // Recreate the index
                db.execSQL("CREATE INDEX index_guests_categoryId ON guests(categoryId)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create temporary table with new schema
                db.execSQL("""
                    CREATE TABLE guests_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        phoneNumber TEXT NOT NULL,
                        isInvitationVerified INTEGER NOT NULL DEFAULT 0,
                        reminderDate INTEGER,
                        categoryId INTEGER,
                        invitationStatus TEXT NOT NULL DEFAULT 'NOT_INVITED',
                        FOREIGN KEY(categoryId) REFERENCES guest_categories(id) ON DELETE SET NULL
                    )
                """)

                // Copy data from old table to new table and set default status
                db.execSQL("""
                    INSERT INTO guests_new (
                        id, name, phoneNumber, isInvitationVerified, 
                        reminderDate, categoryId, invitationStatus
                    )
                    SELECT 
                        id, name, phoneNumber, isInvitationVerified,
                        reminderDate, categoryId,
                        CASE 
                            WHEN isInvitationVerified = 1 THEN 'INVITED'
                            ELSE 'NOT_INVITED'
                        END
                    FROM guests
                """)

                // Drop old table
                db.execSQL("DROP TABLE guests")

                // Rename new table to guests
                db.execSQL("ALTER TABLE guests_new RENAME TO guests")

                // Recreate the index
                db.execSQL("CREATE INDEX index_guests_categoryId ON guests(categoryId)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add hasInteracted column to guests table
                db.execSQL("ALTER TABLE guests ADD COLUMN hasInteracted INTEGER NOT NULL DEFAULT 0")
            }
        }
    }

    fun updateInvitationStatus(guestId: String, status: String) {
        // Logic to update the invitation status in the database
        // For example, using SQLite or Room database
    }
} 