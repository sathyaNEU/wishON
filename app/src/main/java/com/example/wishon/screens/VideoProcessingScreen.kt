// app/src/main/java/com/example/voicefirstapp/screens/VideoProcessingScreen.kt
package com.example.wishon.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.VideoFile
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
import androidx.compose.ui.text.style.TextAlign
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
    var processingStatus by remember { mutableStateOf("Preparing video analysis...") }
    var hasNavigated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (hasNavigated) return@LaunchedEffect

        // Show frame immediately
        showFrame = true

        // TTS announcement
        val announcementText = when {
            userQuestion.isNotEmpty() && frames.isNotEmpty() ->
                "Processing your video assistance request: $userQuestion, with video frame analysis"
            userQuestion.isNotEmpty() ->
                "Processing your video assistance request: $userQuestion"
            frames.isNotEmpty() ->
                "Processing video frame for visual assistance"
            else ->
                "Processing general video assistance request"
        }

        tts?.speak(
            announcementText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "video_processing_announcement"
        )

        // Initialize LLM
        processingStatus = "Loading video assistance AI model..."
        delay(1000)

        val isLLMInitialized = try {
            GemmaLLMService.initializeLLM(context)
        } catch (e: Exception) {
            android.util.Log.e("VideoProcessingScreen", "Error initializing LLM", e)
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
                processingStatus = "Analyzing video frame..."
                delay(1000)

                // Process single frame with real-time logging
                GemmaLLMService.analyzeFrameWithUserQuestion(frames[0], userQuestion, selectedLanguage.llmLanguageName)

            } catch (e: Exception) {
                android.util.Log.e("VideoProcessingScreen", "Error during analysis", e)
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
            .padding(20.dp)
            .semantics {
                contentDescription = when {
                    userQuestion.isNotEmpty() && frames.isNotEmpty() ->
                        "Processing video assistance for question: $userQuestion, with video frame"
                    userQuestion.isNotEmpty() ->
                        "Processing video assistance for question: $userQuestion"
                    frames.isNotEmpty() ->
                        "Processing video frame for visual assistance"
                    else ->
                        "Processing general video assistance request"
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        // Main Header
        Text(
            text = "wishON vision",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "AI-Powered Visual Assistance",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Question and Video Context Cards - Fixed Height Row Layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp), // Fixed height for both cards
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Question Card - Fixed size
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = if (userQuestion.isNotEmpty())
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QuestionAnswer,
                            contentDescription = "Your question",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Your Question",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (userQuestion.isNotEmpty())
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Text(
                        text = if (userQuestion.isNotEmpty()) "\"$userQuestion\"" else "No specific question provided",
                        fontSize = 13.sp,
                        color = if (userQuestion.isNotEmpty())
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        lineHeight = 18.sp,
                        maxLines = 4 // Increased to 4 lines for more text visibility
                    )
                }
            }

            // Video Frame Card - Fixed size
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoFile,
                            contentDescription = "Video frame",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Video Frame",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Text(
                        text = if (frames.isNotEmpty()) "Video frame captured and ready for analysis" else "No video frame available",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Gemma AI Branding - Full Width
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Powered by",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "Gemma",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                Text(
                    text = "3n",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Advanced AI for visual accessibility and video understanding",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Coming Soon section - Smaller text, less padding
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isProcessing)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Video native (audio embedded) inference - coming soon!",
                fontSize = 14.sp, // Reduced from 18.sp
                fontWeight = FontWeight.Medium, // Reduced from Bold
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp), // Reduced from 20.dp
                textAlign = TextAlign.Center
            )
        }

        if (isProcessing) {
            Spacer(modifier = Modifier.height(12.dp)) // Reduced spacing

            // Processing status - moved up
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = processingStatus,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp)) // Reduced spacing

            // Audio processing wave animation - now more prominent
            AudioWaveAnimation(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(horizontal = 8.dp)
            )
        }

        // Show video frame if available - positioned after processing animation
        if (showFrame && frames.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Captured Frame",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    FramePreview(
                        frame = frames[0],
                        isProcessing = isProcessing
                    )
                }
            }
        }

        if (frames.isEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "No frames were extracted from the video. This might be due to a video recording issue.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Footer with privacy message
        Text(
            text = "🔒 All processing happens locally on your device",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )
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
            .width(180.dp)
            .semantics {
                contentDescription = "Video frame at ${frame.timestampMs}ms" +
                        if (isProcessing) " - Being analyzed by AI" else ""
            }
    ) {
        // Display frame with highlight if currently processing
        Card(
            modifier = Modifier
                .size(160.dp)
                .border(
                    width = if (isProcessing) 3.dp else 2.dp,
                    color = if (isProcessing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(8.dp)
                ),
            shape = RoundedCornerShape(8.dp)
        ) {
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Text(
            text = "Frame: ${frame.timestampMs}ms",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}