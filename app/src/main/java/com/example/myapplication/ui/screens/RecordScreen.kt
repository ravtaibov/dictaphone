package com.example.myapplication.ui.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.audio.AudioRecorder
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.ui.screens.viewmodels.RecordViewModel

@Composable
fun RecordScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: RecordViewModel = viewModel(
        factory = RecordViewModelFactory(context)
    )

    val isRecording by viewModel.isRecording.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isRecording) "Recording..." else "Ready",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = formatSeconds(timerSeconds),
                style = MaterialTheme.typography.displayLarge
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { 
                    if (isRecording) {
                        viewModel.stopRecording()
                        navController.popBackStack()
                    } else {
                        // Check permission
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.size(100.dp)
            ) {
                Text(text = if (isRecording) "STOP" else "START")
            }
        }
    }
}

fun formatSeconds(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

class RecordViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecordViewModel::class.java)) {
            val audioRecorder = AudioRecorder(context)
            val db = AppDatabase.getDatabase(context)
            @Suppress("UNCHECKED_CAST")
            return RecordViewModel(audioRecorder, db.recordingDao(), context.filesDir) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
