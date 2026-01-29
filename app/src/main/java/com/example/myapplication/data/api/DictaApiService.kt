package com.example.myapplication.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface DictaApiService {
    @Multipart
    @POST("transcribe")
    suspend fun transcribeAudio(
        @Part file: MultipartBody.Part,
        @Part("lang") lang: RequestBody
    ): TranscribeResponse
}
