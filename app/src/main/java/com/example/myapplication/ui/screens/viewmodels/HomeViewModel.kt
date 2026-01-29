package com.example.myapplication.ui.screens.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.data.db.RecordingDao
import com.example.myapplication.data.db.RecordingEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val recordingDao: RecordingDao
) : ViewModel() {

    val recordings: StateFlow<List<RecordingEntity>> = recordingDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}

class HomeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val db = AppDatabase.getDatabase(context)
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(db.recordingDao()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
