// app/src/main/java/com/example/voicefirstapp/screens/VideoProcessingScreen.kt
package com.example.wishon.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wishon.components.AudioWaveAnimation
import com.example.wishon.utils.GemmaLLMService
import com.example.wishon.utils.ExtractedFrame
import kotlinx.coroutines.delay

@Composable
fun VideoProcessingScreen(
    frames: List<ExtractedFrame>,
    userQuestion: String,
    selectedLanguage: SupportedLanguage,
    tts: TextToSpeech?,
    onNavigateToResult: (String) -> Unit
) {
    val context = LocalContext.current
    var showFrame by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(true) }
    var processingStatus by remember { mutableStateOf("Preparing AI analysis...") }
    var hasNavigated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (hasNavigated) return@LaunchedEffect

        // Show frame immediately
        showFrame = true

        // Start background TTS for processing
        tts?.speak(
            "Processing video frame to analyze your question",
            TextToSpeech.QUEUE_FLUSH,
            null,
            "processing_announcement"
        )

        // Initialize Gemma LLM
        processingStatus = "Loading AI model..."
        delay(1000)

        val isLLMInitialized = try {
            GemmaLLMService.initializeLLM(context)
        } catch (e: Exception) {
            android.util.Log.e("ProcessingScreen", "Error initializing LLM", e)
            false
        }

        if (!isLLMInitialized) {
            processingStatus = "Failed to load AI model"
            delay(2000)
            if (!hasNavigated) {
                hasNavigated = true
                onNavigateToResult("Sorry, the AI model couldn't be loaded. Please check if the model file is properly installed.")
            }
            return@LaunchedEffect
        }

        processingStatus = "AI model loaded successfully"
        delay(500)

        // Process with Gemma LLM
        val result = if (frames.isNotEmpty()) {
            try {
                processingStatus = "Analyzing frame..."
                delay(500)

                // Process single frame with real-time logging
                GemmaLLMService.analyzeFrameWithUserQuestion(frames[0], userQuestion,selectedLanguage.llmLanguageName)

            } catch (e: Exception) {
                android.util.Log.e("ProcessingScreen", "Error during analysis", e)
                "Sorry, I encountered an error while analyzing the video: ${e.message ?: "Unknown error"}"
            }
        } else {
            "No video frames were available for analysis. Please try recording again."
        }

        isProcessing = false
        processingStatus = "Analysis complete"

        // Brief pause before navigation
        delay(1000)

        if (!hasNavigated) {
            hasNavigated = true
            onNavigateToResult(result)
        }
    }

    // Cleanup when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            GemmaLLMService.cleanup()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics {
                contentDescription = "Processing video frame for question: $userQuestion"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        Text(
            text = "AI Analysis",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Show user's question
        if (userQuestion.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Your Question:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "\"$userQuestion\"",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        if (showFrame && frames.isNotEmpty()) {
            Text(
                text = "Extracted Frame:",
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Display single extracted frame
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                FramePreview(
                    frame = frames[0],
                    isProcessing = isProcessing
                )
            }
        }

        if (frames.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = "No frames were extracted from the video. This might be due to a video recording issue.",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (isProcessing) {
            Spacer(modifier = Modifier.height(16.dp))

            // Processing status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    text = processingStatus,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Audio wave animation during processing
            AudioWaveAnimation(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
        }
    }
}

@Composable
fun FramePreview(
    frame: ExtractedFrame,
    isProcessing: Boolean = false
) {
    // Validate bitmap outside of composable
    val isBitmapValid = remember(frame.bitmap) {
        try {
            !frame.bitmap.isRecycled && frame.bitmap.width > 0 && frame.bitmap.height > 0
        } catch (e: Exception) {
            false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(200.dp)
            .semantics {
                contentDescription = "Video frame at ${frame.timestampMs}ms" +
                        if (isProcessing) " - Being analyzed by AI" else ""
            }
    ) {
        // Display frame with highlight if currently processing
        Card(
            modifier = Modifier
                .size(180.dp)
                .border(
                    width = if (isProcessing) 3.dp else 2.dp,
                    color = if (isProcessing) Color.Green else MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box {
                if (isBitmapValid) {
                    Image(
                        bitmap = frame.bitmap.asImageBitmap(),
                        contentDescription = "Extracted video frame",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback if bitmap is invalid
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Frame\nUnavailable",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Processing overlay
                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Green.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AI\nAnalyzing",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Text(
            text = "Video Frame",
            fontSize = 14.sp,
            fontWeight = if (isProcessing) FontWeight.Bold else FontWeight.Normal,
            color = if (isProcessing) Color.Green else Color.Unspecified,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = "${frame.timestampMs}ms",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}