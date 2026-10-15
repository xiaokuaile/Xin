package com.xiaokuaile.xin.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun kindToString(kind: ClipKind): String = kind.name

    @TypeConverter
    fun stringToKind(value: String): ClipKind =
        runCatching { ClipKind.valueOf(value) }.getOrDefault(ClipKind.TEXT)
}
