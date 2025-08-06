// app/src/main/java/com/example/voicefirstapp/screens/AudioProcessingScreen.kt
package com.example.wishon.screens

import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wishon.components.AudioWaveAnimation
import com.example.wishon.utils.GemmaLLMService
import kotlinx.coroutines.delay

@Composable
fun AudioProcessingScreen(
    userQuestion: String, // This is now the text from voice input
    selectedLanguage: SupportedLanguage, // Add this line
    tts: TextToSpeech?,
    onNavigateToResult: (String) -> Unit
) {
    val context = LocalContext.current
    var isProcessing by remember { mutableStateOf(true) }
    var processingStatus by remember { mutableStateOf("Preparing text analysis...") }
    var hasNavigated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (hasNavigated) return@LaunchedEffect

        // TTS announcement
        val announcementText = if (userQuestion.isNotEmpty()) {
            "Processing your request: $userQuestion"
        } else {
            "Processing your hearing assistance request"
        }

        tts?.speak(
            announcementText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "audio_processing_announcement"
        )

        // Initialize LLM
        processingStatus = "Loading text AI model..."
        delay(1000)

        val isLLMInitialized = try {
            GemmaLLMService.initializeLLM(context)
        } catch (e: Exception) {
            android.util.Log.e("AudioProcessingScreen", "Error initializing LLM", e)
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

        // Process the text request with Gemma LLM
        val result = try {
            processingStatus = "Analyzing your request..."
            delay(1000)

            // Create a prompt based on the user's question for hearing assistance
            val prompt = if (userQuestion.isNotEmpty()) {
                """You are a helpful AI assistant specializing in hearing accessibility support. The user has asked: "$userQuestion"
Provide a concise, clear, and helpful answer that would be useful for someone who may have hearing difficulties or needs audio-related assistance."""
            } else {
                """You are a helpful AI assistant specializing in hearing accessibility support. The user is requesting general hearing assistance.

Please provide helpful information about:
- How AI can assist with hearing-related challenges
- Available text-to-speech services
- Communication accessibility options
- Audio description services
- General hearing assistance features

Keep your response clear, practical, and supportive."""
            }

            // Use the Gemma LLM service to generate response
            GemmaLLMService.generateResponse(prompt, selectedLanguage.llmLanguageName)

        } catch (e: Exception) {
            android.util.Log.e("AudioProcessingScreen", "Error during text analysis", e)
            "Sorry, I encountered an error while processing your request: ${e.message ?: "Unknown error"}"
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
            // No specific cleanup needed for text processing
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .semantics {
                contentDescription = if (userQuestion.isNotEmpty()) {
                    "Processing text request: $userQuestion"
                } else {
                    "Processing hearing assistance request"
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        // Main Header
        Text(
            text = "wishON hear",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "AI-Powered Text & Audio Intelligence",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Two-column layout for better width utilization
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column - User Question/Context
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (userQuestion.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hearing,
                                    contentDescription = "Your request",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Your Request",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            Text(
                                text = "\"$userQuestion\"",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                lineHeight = 22.sp
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hearing,
                                    contentDescription = "Hearing assistance",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Hearing Assistance",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            Text(
                                text = "Providing general hearing accessibility support and text-based assistance",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }

            // Right Column - Gemma 3n Branding
            Column(
                modifier = Modifier.weight(1f)
            ) {
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
                            text = "Google's advanced language model for text understanding and generation",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Processing section
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isProcessing) {
                    val infiniteTransition = rememberInfiniteTransition(label = "gemma_processing")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )

                    Text(
                        text = "🧠",
                        fontSize = 40.sp,
                        modifier = Modifier.padding(12.dp)
                    )

                    Text(
                        text = "Real-time Text Processing",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Text(
                        text = "AI-powered language understanding on your device",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                } else {
                    Text(
                        text = "✅",
                        fontSize = 40.sp,
                        modifier = Modifier.padding(12.dp)
                    )

                    Text(
                        text = "Processing Complete",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Text(
                        text = "Text analysis finished successfully",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (isProcessing) {
            Spacer(modifier = Modifier.height(16.dp))

            // Processing status
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

            Spacer(modifier = Modifier.height(20.dp))

            // Text processing animation
            AudioWaveAnimation(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(horizontal = 8.dp)
            )
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