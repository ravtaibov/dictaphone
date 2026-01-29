package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class RecordingStatus {
    RECORDED, TRANSCRIBING, DONE, ERROR
}

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val durationSec: Int? = null,
    val status: RecordingStatus = RecordingStatus.RECORDED,
    val language: String = "ru",
    val transcript: String? = null,
    val summary: String? = null,
    val errorMessage: String? = null
)
