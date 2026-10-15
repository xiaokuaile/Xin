package com.xiaokuaile.xin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Clip::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class XinDatabase : RoomDatabase() {
    abstract fun clipDao(): ClipDao

    companion object {
        @Volatile
        private var instance: XinDatabase? = null

        fun get(context: Context): XinDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, XinDatabase::class.java, "xin.db")
                .build()
                .also { instance = it }
        }
    }
}
