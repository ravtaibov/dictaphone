package com.example.myapplication.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStatus(status: RecordingStatus): String {
        return status.name
    }

    @TypeConverter
    fun toStatus(value: String): RecordingStatus {
        return try {
            RecordingStatus.valueOf(value)
        } catch (e: Exception) {
            RecordingStatus.ERROR
        }
    }
}
