package com.rgremote.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DeviceEntity::class, PairingCredentialEntity::class],
    version = 2,
    exportSchema = false
)
abstract class RGRemoteDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao

    companion object {
        @Volatile
        private var instance: RGRemoteDatabase? = null

        fun get(context: Context): RGRemoteDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RGRemoteDatabase::class.java,
                    "rgremote.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pairing_credentials ADD COLUMN serverCertificateSha256 TEXT")
            }
        }
    }
}
