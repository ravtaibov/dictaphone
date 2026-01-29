package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.db.RecordingStatus
import com.example.myapplication.ui.screens.viewmodels.DetailsViewModel
import com.example.myapplication.ui.screens.viewmodels.DetailsViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(navController: NavController, recordingId: String?) {
    if (recordingId == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: Recording ID is missing")
            Button(onClick = { navController.popBackStack() }) {
                Text("Go Back")
            }
        }
        return
    }

    val context = LocalContext.current
    val viewModel: DetailsViewModel = viewModel(
        factory = DetailsViewModelFactory(context, recordingId)
    )

    val recording by viewModel.recording.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()
    val isTranscribing by viewModel.isTranscribing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recording Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val currentRecording = recording
            if (currentRecording != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentRecording.title,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Status: ${currentRecording.status}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (currentRecording.status == RecordingStatus.ERROR) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.secondary
                    )
                    
                    if (currentRecording.errorMessage != null) {
                        Text(
                            text = currentRecording.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Player Controls
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { viewModel.togglePlayback() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isPlaying) "PAUSE" else "PLAY AUDIO")
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { viewModel.transcribeAudio() },
                            modifier = Modifier.weight(1f),
                            enabled = !isTranscribing && currentRecording.status != RecordingStatus.TRANSCRIBING
                        ) {
                            if (isTranscribing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("TRANSCRIBE")
                            }
                        }
                        OutlinedButton(
                            onClick = { /* TODO: Share */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SHARE")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Transcript Section
                    Text(
                        text = "Transcript",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = currentRecording.transcript ?: "No transcript available yet. Press 'Transcribe' to start.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
