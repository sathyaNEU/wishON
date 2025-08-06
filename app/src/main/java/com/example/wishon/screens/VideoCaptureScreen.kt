// app/src/main/java/com/example/voicefirstapp/screens/VideoCaptureScreen.kt
package com.example.wishon.screens

import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wishon.components.CameraPreview
import com.example.wishon.utils.VideoFrameExtractor
import com.example.wishon.utils.ExtractedFrame
import kotlinx.coroutines.delay

@Composable
fun VideoCaptureScreen(
    tts: TextToSpeech?,
    userQuestion: String,
    onNavigateToProcessing: (List<ExtractedFrame>) -> Unit
) {
    val context = LocalContext.current
    var captureStatus by remember { mutableStateOf("Preparing...") }
    var isRecording by remember { mutableStateOf(false) }
    var startRecording by remember { mutableStateOf(false) }
    var recordedVideoUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        // TTS announcement with user's question
        val questionToSpeak = if (userQuestion.isNotEmpty()) {
            "Looking for: $userQuestion. Move your phone to record the video"
        } else {
            "Move your phone to record the video to identify objects"
        }

        tts?.speak(
            questionToSpeak,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "capture_instruction"
        )

        captureStatus = "Move your phone to record"
        delay(2000) // Wait 2 seconds as specified

        // Start actual video recording
        captureStatus = "Recording video..."
        startRecording = true

        // Record for 5 seconds
        delay(5000)

        // Stop recording
        startRecording = false
    }

    // Handle video recording completion
    LaunchedEffect(recordedVideoUri) {
        recordedVideoUri?.let { uri ->
            captureStatus = "Extracting frames..."
            delay(500)

            try {
                // Extract frames from the recorded video
                val frames = VideoFrameExtractor.extractFrames(context, uri, 4)
                onNavigateToProcessing(frames)
            } catch (e: Exception) {
                // Handle extraction error - could navigate with empty frames or show error
                captureStatus = "Error processing video"
                delay(2000)
                onNavigateToProcessing(emptyList())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "$captureStatus for question: $userQuestion"
            }
    ) {
        // Camera preview fills the entire screen
        CameraPreview(
            onVideoRecorded = { uri ->
                recordedVideoUri = uri
            },
            onRecordingStateChanged = { recording ->
                isRecording = recording
            },
            startRecording = startRecording
        )

        // Overlay with status and recording indicator
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // User question display
            if (userQuestion.isNotEmpty()) {
                Card(
                    modifier = Modifier.padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Looking for:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "\"$userQuestion\"",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Status card
            Card(
                modifier = Modifier.padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
            ) {
                Text(
                    text = captureStatus,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Recording indicator in the center-bottom
        if (isRecording) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // Recording indicator with pulsing animation
                val infiniteTransition = rememberInfiniteTransition(label = "recording")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.Red.copy(alpha = alpha), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "REC",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}