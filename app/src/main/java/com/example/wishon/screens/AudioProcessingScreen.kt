// app/src/main/java/com/example/voicefirstapp/screens/AudioProcessingScreen.kt
package com.example.wishon.screens

import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.QuestionAnswer
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
    userQuestion: String, // User's specific question
    backgroundAudioText: String, // Background audio captured as text
    selectedLanguage: SupportedLanguage,
    tts: TextToSpeech?,
    onNavigateToResult: (String) -> Unit
) {
    val context = LocalContext.current
    var isProcessing by remember { mutableStateOf(true) }
    var processingStatus by remember { mutableStateOf("Preparing hearing assistance...") }
    var hasNavigated by remember { mutableStateOf(false) }

    // Add logging to debug the data flow
    LaunchedEffect(Unit) {
        Log.d("AudioProcessingScreen", "=== AUDIO PROCESSING DEBUG ===")
        Log.d("AudioProcessingScreen", "User Question: '$userQuestion'")
        Log.d("AudioProcessingScreen", "Background Audio Text: '$backgroundAudioText'")
        Log.d("AudioProcessingScreen", "Background Audio Length: ${backgroundAudioText.length} chars")
        Log.d("AudioProcessingScreen", "Selected Language: ${selectedLanguage.displayName}")
        Log.d("AudioProcessingScreen", "==============================")
    }

    LaunchedEffect(Unit) {
        if (hasNavigated) return@LaunchedEffect

        // TTS announcement
        val announcementText = when {
            userQuestion.isNotEmpty() && backgroundAudioText.isNotEmpty() ->
                "Processing your hearing assistance request: $userQuestion, with background audio analysis"
            userQuestion.isNotEmpty() ->
                "Processing your hearing assistance request: $userQuestion"
            backgroundAudioText.isNotEmpty() ->
                "Processing background audio for hearing assistance"
            else ->
                "Processing general hearing assistance request"
        }

        tts?.speak(
            announcementText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "audio_processing_announcement"
        )

        // Initialize LLM
        processingStatus = "Loading hearing assistance AI model..."
        delay(1000)

        val isLLMInitialized = try {
            GemmaLLMService.initializeLLM(context)
        } catch (e: Exception) {
            Log.e("AudioProcessingScreen", "Error initializing LLM", e)
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

        // Process the hearing assistance request with Gemma LLM
        val result = try {
            processingStatus = "Analyzing your hearing assistance request..."
            delay(1000)

            // Create comprehensive prompt with both user question and background audio
            val prompt = when {
                userQuestion.isNotEmpty() && backgroundAudioText.isNotEmpty() -> {
                    """You are a hearing assistance AI. Help the user with their audio-related request.

User's Question: "$userQuestion"

Background Audio (Speech-to-Text): "$backgroundAudioText"

Based on the user's question and the background audio-text captured, provide helpful hearing assistance.

Provide your response in ${selectedLanguage.llmLanguageName}."""
                }
                userQuestion.isNotEmpty() && backgroundAudioText.isEmpty() -> {
                    """You are a hearing assistance AI. The user asked: "$userQuestion"

No background audio was captured clearly. Provide helpful guidance and suggestions for their hearing assistance request.

Respond in ${selectedLanguage.llmLanguageName}."""
                }
                userQuestion.isEmpty() && backgroundAudioText.isNotEmpty() -> {
                    """You are a hearing assistance AI. The user didn't ask a specific question, but background audio was captured:

Background Audio (Speech-to-Text): "$backgroundAudioText"

Analyze this audio and provide helpful insights about what was captured. Describe what you hear and offer any relevant hearing assistance.

Respond in ${selectedLanguage.llmLanguageName}."""
                }
                else -> {
                    """You are a hearing assistance AI. No specific question was asked and no clear background audio was captured.

Provide general hearing assistance guidance and suggest ways the user can get better help with audio-related tasks.

Respond in ${selectedLanguage.llmLanguageName}."""
                }
            }

            Log.d("AudioProcessingScreen", "=== SENDING TO GEMMA LLM ===")
            Log.d("AudioProcessingScreen", "Final Prompt: $prompt")
            Log.d("AudioProcessingScreen", "============================")

            // Use the Gemma LLM service to generate response
            GemmaLLMService.generateResponse(prompt, selectedLanguage.llmLanguageName)

        } catch (e: Exception) {
            Log.e("AudioProcessingScreen", "Error during hearing assistance processing", e)
            "Sorry, I encountered an error while processing your hearing assistance request: ${e.message ?: "Unknown error"}"
        }

        Log.d("AudioProcessingScreen", "=== LLM RESPONSE ===")
        Log.d("AudioProcessingScreen", "Result: $result")
        Log.d("AudioProcessingScreen", "===================")

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
                contentDescription = when {
                    userQuestion.isNotEmpty() && backgroundAudioText.isNotEmpty() ->
                        "Processing hearing assistance for question: $userQuestion, with background audio"
                    userQuestion.isNotEmpty() ->
                        "Processing hearing assistance for question: $userQuestion"
                    backgroundAudioText.isNotEmpty() ->
                        "Processing background audio for hearing assistance"
                    else ->
                        "Processing general hearing assistance request"
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
            text = "AI-Powered Hearing Assistance",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Question and Audio Context Cards - Fixed Height Row Layout
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

            // Background Audio Card - Fixed size
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
                            imageVector = Icons.Default.Hearing,
                            contentDescription = "Background audio",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Background Audio",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Text(
                        text = "Background audio captured and ready for analysis",
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
                    text = "Advanced AI for hearing accessibility and audio understanding",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Processing section - Smaller text, less padding
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
                text = "AI Inference on pre-processed audio, native audio inference - coming soon!",
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