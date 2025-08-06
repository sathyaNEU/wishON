// app/src/main/java/com/example/voicefirstapp/screens/VoiceInputScreen.kt
package com.example.voicefirstapp.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Visibility
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
import androidx.core.content.ContextCompat
import com.example.voicefirstapp.components.WaveformAnimation
import kotlinx.coroutines.delay
import java.util.*

@Composable
fun VoiceInputScreen(
    assistanceType: AssistanceType,
    onNavigateToVideoCapture: (String) -> Unit,
    onNavigateToAudioProcessing: (String) -> Unit // Changed from audio capture to processing
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var hasNavigated by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var showManualOption by remember { mutableStateOf(false) }

    // Function to completely stop and cleanup speech recognizer
    fun stopSpeechRecognizer() {
        speechRecognizer?.let { recognizer ->
            try {
                recognizer.stopListening()
                recognizer.cancel()
                recognizer.destroy()
            } catch (e: Exception) {
                Log.w("VoiceInputScreen", "Error during cleanup: ${e.message}")
            }
        }
        speechRecognizer = null
        isListening = false
    }

    // Function to navigate with the captured question
    fun navigateWithQuestion(question: String = "") {
        if (hasNavigated) return
        hasNavigated = true

        stopSpeechRecognizer()

        when (assistanceType) {
            AssistanceType.VISION -> onNavigateToVideoCapture(question)
            AssistanceType.HEARING -> onNavigateToAudioProcessing(question) // Direct to processing
        }
    }

    // Start continuous listening (same pattern as CustomerPreferenceScreen)
    fun startContinuousListening() {
        if (hasNavigated || !SpeechRecognizer.isRecognitionAvailable(context)) return

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showManualOption = true
            return
        }

        stopSpeechRecognizer()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // Force offline to avoid network issues
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
        }

        val recognitionListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (!hasNavigated) {
                    isListening = true
                    showManualOption = false
                }
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                if (!hasNavigated) {
                    isListening = false
                    // If we got some text, navigate with it
                    if (recognizedText.isNotEmpty()) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            navigateWithQuestion(recognizedText)
                        }, 1000)
                    } else {
                        // Restart listening if no text captured yet
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (!hasNavigated) {
                                startContinuousListening()
                            }
                        }, 1000)
                    }
                }
            }

            override fun onError(error: Int) {
                if (hasNavigated) return

                isListening = false

                when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        showManualOption = true
                        return
                    }
                    SpeechRecognizer.ERROR_CLIENT,
                    SpeechRecognizer.ERROR_SERVER,
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                        // Network/service errors - show manual option after a few tries
                        showManualOption = true
                        return
                    }
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        // No speech detected - continue trying or proceed
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (!hasNavigated) {
                                startContinuousListening()
                            }
                        }, 1500)
                    }
                    else -> {
                        // Other errors - retry after delay
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (!hasNavigated) {
                                startContinuousListening()
                            }
                        }, 2000)
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                if (hasNavigated) return

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    recognizedText = matches[0]
                }

                isListening = false

                // Navigate with the recognized text after a brief delay
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    navigateWithQuestion(recognizedText)
                }, 1500)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (hasNavigated) return

                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    recognizedText = matches[0]
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer?.setRecognitionListener(recognitionListener)
        speechRecognizer?.startListening(intent)
    }

    // Start listening when screen loads
    LaunchedEffect(Unit) {
        delay(500) // Brief delay to let screen settle
        startContinuousListening()

        // Timeout after 8 seconds - proceed with whatever we have
        delay(8000)
        if (!hasNavigated) {
            navigateWithQuestion(recognizedText)
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            hasNavigated = true
            stopSpeechRecognizer()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .semantics {
                contentDescription = when {
                    showManualOption -> "Voice recognition unavailable, tap to proceed"
                    isListening -> "Listening for your ${assistanceType.name.lowercase()} question"
                    recognizedText.isNotEmpty() -> "Question captured: $recognizedText"
                    else -> "Getting ready to listen"
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Assistance type indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Icon(
                imageVector = when (assistanceType) {
                    AssistanceType.VISION -> Icons.Default.Visibility
                    AssistanceType.HEARING -> Icons.Default.Hearing
                },
                contentDescription = "${assistanceType.name} support",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "${assistanceType.name} SUPPORT",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (showManualOption) {
            // Show manual options when voice recognition fails
            Text(
                text = "Voice Recognition Unavailable",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Speech service requires internet connection or may be unavailable.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Options:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Button(
                        onClick = { navigateWithQuestion("") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text("Continue without question")
                    }

                    OutlinedButton(
                        onClick = {
                            showManualOption = false
                            startContinuousListening()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Try voice recognition again")
                    }
                }
            }

        } else {
            // Normal voice recognition interface
            Text(
                text = when {
                    isListening -> "I'm listening!"
                    recognizedText.isNotEmpty() -> "Got it!"
                    else -> "Getting ready..."
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
                color = when {
                    isListening -> Color(0xFF4CAF50) // Green when listening
                    recognizedText.isNotEmpty() -> Color(0xFF2196F3) // Blue when captured
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            // Instruction text
            if (isListening) {
                val instructionText = when (assistanceType) {
                    AssistanceType.VISION -> "Ask what you want to identify in the video"
                    AssistanceType.HEARING -> "Describe what you want help with - like translating text, reading content, or getting audio descriptions"
                }
                Text(
                    text = instructionText,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Show waveform animation while listening
            if (isListening) {
                WaveformAnimation(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            // Show recognized text
            if (recognizedText.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Your Question:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = if (recognizedText.isEmpty()) "No specific question" else "\"$recognizedText\"",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Show listening indicator when active
            if (isListening) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = "Listening",
                        tint = Color.Green,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Listening...",
                        fontSize = 14.sp,
                        color = Color.Green,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}