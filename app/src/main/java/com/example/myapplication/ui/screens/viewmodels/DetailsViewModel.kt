package com.example.myapplication.ui.screens.viewmodels

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.api.RetrofitClient
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.data.db.RecordingDao
import com.example.myapplication.data.db.RecordingEntity
import com.example.myapplication.data.db.RecordingStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

class DetailsViewModel(
    private val recordingDao: RecordingDao,
    private val recordingId: String
) : ViewModel() {

    private val _recording = MutableStateFlow<RecordingEntity?>(null)
    val recording: StateFlow<RecordingEntity?> = _recording.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    init {
        loadRecording()
    }

    private fun loadRecording() {
        viewModelScope.launch {
            _recording.value = recordingDao.getById(recordingId)
        }
    }

    fun transcribeAudio() {
        val currentRecording = _recording.value ?: return
        if (currentRecording.filePath.isEmpty()) return

        val file = File(currentRecording.filePath)
        if (!file.exists()) {
            updateStatus(RecordingStatus.ERROR, "File not found")
            return
        }

        viewModelScope.launch {
            _isTranscribing.value = true
            updateStatus(RecordingStatus.TRANSCRIBING)

            try {
                val requestFile = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val lang = "ru".toRequestBody("text/plain".toMediaTypeOrNull())

                val response = RetrofitClient.apiService.transcribeAudio(body, lang)

                val updatedRecording = currentRecording.copy(
                    transcript = response.text,
                    status = RecordingStatus.DONE,
                    language = response.language,
                    errorMessage = null
                )
                recordingDao.update(updatedRecording)
                _recording.value = updatedRecording

            } catch (e: Exception) {
                e.printStackTrace()
                updateStatus(RecordingStatus.ERROR, e.localizedMessage ?: "Unknown error")
            } finally {
                _isTranscribing.value = false
            }
        }
    }

    private fun updateStatus(status: RecordingStatus, error: String? = null) {
        viewModelScope.launch {
            _recording.value?.let { rec ->
                val updated = rec.copy(status = status, errorMessage = error)
                recordingDao.update(updated)
                _recording.value = updated
            }
        }
    }

    fun togglePlayback() {
        val currentRecording = _recording.value ?: return
        
        if (mediaPlayer == null) {
            startPlayback(currentRecording.filePath)
        } else {
            if (mediaPlayer?.isPlaying == true) {
                pausePlayback()
            } else {
                resumePlayback()
            }
        }
    }

    private fun startPlayback(filePath: String) {
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(filePath)
                prepare()
                start()
                setOnCompletionListener {
                    stopPlayback()
                }
                _isPlaying.value = true
                startProgressTracker()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun resumePlayback() {
        mediaPlayer?.start()
        _isPlaying.value = true
        startProgressTracker()
    }

    private fun pausePlayback() {
        mediaPlayer?.pause()
        _isPlaying.value = false
        stopProgressTracker()
    }

    private fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
        _playbackProgress.value = 0f
        stopProgressTracker()
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (_isPlaying.value) {
                mediaPlayer?.let { player ->
                    if (player.duration > 0) {
                        val current = player.currentPosition
                        val total = player.duration
                        _playbackProgress.value = current.toFloat() / total.toFloat()
                    }
                }
                delay(100)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

class DetailsViewModelFactory(
    private val context: Context,
    private val recordingId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailsViewModel::class.java)) {
            val db = AppDatabase.getDatabase(context)
            @Suppress("UNCHECKED_CAST")
            return DetailsViewModel(db.recordingDao(), recordingId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
