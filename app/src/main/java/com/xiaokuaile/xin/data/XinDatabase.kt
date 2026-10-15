package com.xiaokuaile.xin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Clip::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class XinDatabase : RoomDatabase() {
    abstract fun clipDao(): ClipDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clips ADD COLUMN html TEXT")
            }
        }

        @Volatile
        private var instance: XinDatabase? = null

        fun get(context: Context): XinDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, XinDatabase::class.java, "xin.db")
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}
