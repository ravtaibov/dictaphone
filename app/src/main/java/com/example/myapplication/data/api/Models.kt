package com.example.myapplication.data.api

import com.google.gson.annotations.SerializedName

data class TranscribeResponse(
    @SerializedName("text") val text: String,
    @SerializedName("language") val language: String,
    @SerializedName("duration_sec") val durationSec: Double
    // segments omitted for MVP
)
