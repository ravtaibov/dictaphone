package com.example.myapplication.ui.screens.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.audio.AudioRecorder
import com.example.myapplication.data.db.RecordingDao
import com.example.myapplication.data.db.RecordingEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date

class RecordViewModel(
    private val audioRecorder: AudioRecorder,
    private val recordingDao: RecordingDao,
    private val filesDir: File
) : ViewModel() {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _timerSeconds = MutableStateFlow(0L)
    val timerSeconds: StateFlow<Long> = _timerSeconds.asStateFlow()

    private var timerJob: Job? = null
    private var currentFile: File? = null
    private var startTime: Long = 0

    fun startRecording() {
        val fileName = "recording_${Date().time}.m4a"
        val file = File(filesDir, fileName)
        currentFile = file
        startTime = System.currentTimeMillis()

        audioRecorder.start(file)
        _isRecording.value = true
        startTimer()
    }

    fun stopRecording() {
        audioRecorder.stop()
        _isRecording.value = false
        stopTimer()

        val file = currentFile
        if (file != null && file.exists()) {
            val duration = _timerSeconds.value.toInt()
            val entity = RecordingEntity(
                title = "Recording ${Date()}",
                filePath = file.absolutePath,
                durationSec = duration,
                createdAt = startTime
            )
            viewModelScope.launch {
                recordingDao.insert(entity)
            }
        }
        
        _timerSeconds.value = 0
        currentFile = null
    }

    private fun startTimer() {
        timerJob?.cancel()
        _timerSeconds.value = 0
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _timerSeconds.value++
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.stop()
    }
}
