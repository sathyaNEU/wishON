// app/src/main/java/com/example/voicefirstapp/screens/VoiceInputScreen.kt
package com.example.wishon.screens

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.wishon.components.WaveformAnimation
import kotlinx.coroutines.delay
import java.util.*

@Composable
fun VoiceInputScreen(
    assistanceType: AssistanceType,
    onNavigateToVideoCapture: (String) -> Unit,
    onNavigateToAudioProcessing: (String) -> Unit
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var hasNavigated by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var showManualOption by remember { mutableStateOf(false) }
    var backgroundAudioText by remember { mutableStateOf("") }

    // ADDED: Track if we should navigate immediately when we get results
    var shouldNavigateOnResults by remember { mutableStateOf(false) }

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

    // Function to navigate with the captured question/audio
    fun navigateWithQuestion(question: String = "") {
        if (hasNavigated) return
        hasNavigated = true

        stopSpeechRecognizer()

        when (assistanceType) {
            AssistanceType.VISION -> {
                Log.d("VisionAssistance", "Navigating with question: '$question'")
                onNavigateToVideoCapture(question) // Use the passed question parameter
            }
            AssistanceType.HEARING -> {
                Log.d("HearingAssistance", "Navigating with captured audio: '$backgroundAudioText'")
                Log.d("HearingAssistance", "Audio text length: ${backgroundAudioText.length} characters")
                onNavigateToAudioProcessing(backgroundAudioText)
            }
        }
    }

    // Start continuous background recording
    fun startBackgroundRecording() {
        if (hasNavigated || !SpeechRecognizer.isRecognitionAvailable(context)) {
            showManualOption = true
            return
        }

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
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 8000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 8000)
        }

        val recognitionListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (!hasNavigated) {
                    isListening = true
                    showManualOption = false
                    if (assistanceType == AssistanceType.HEARING) {
                        Log.d("HearingAssistance", "Started listening for background audio")
                    }
                }
            }

            override fun onBeginningOfSpeech() {
                if (assistanceType == AssistanceType.HEARING) {
                    Log.d("HearingAssistance", "Beginning of speech detected")
                }
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                if (assistanceType == AssistanceType.HEARING) {
                    Log.d("HearingAssistance", "End of speech detected")
                }

                if (!hasNavigated && assistanceType == AssistanceType.HEARING) {
                    isListening = false
                }
                // FIXED: For vision, navigate immediately when speech ends if we have text
                else if (!hasNavigated && assistanceType == AssistanceType.VISION) {
                    isListening = false
                    if (recognizedText.isNotEmpty()) {
                        // CHANGED: Navigate immediately with current recognized text
                        navigateWithQuestion(recognizedText)
                    } else {
                        // Only restart if we don't have any text yet
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (!hasNavigated) {
                                startBackgroundRecording()
                            }
                        }, 1000)
                    }
                }
            }

            override fun onError(error: Int) {
                if (hasNavigated) return

                isListening = false

                if (assistanceType == AssistanceType.HEARING) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech matched"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Unknown error ($error)"
                    }
                    Log.d("HearingAssistance", "Speech recognition error: $errorMessage")
                    Log.d("HearingAssistance", "Current captured text: '$backgroundAudioText'")
                    return
                }

                // For vision assistance, handle errors as before
                when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        showManualOption = true
                        return
                    }
                    SpeechRecognizer.ERROR_CLIENT,
                    SpeechRecognizer.ERROR_SERVER,
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                        showManualOption = true
                        return
                    }
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (!hasNavigated) {
                                startBackgroundRecording()
                            }
                        }, 1500)
                    }
                    else -> {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (!hasNavigated) {
                                startBackgroundRecording()
                            }
                        }, 2000)
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                if (hasNavigated) return

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    if (assistanceType == AssistanceType.HEARING) {
                        backgroundAudioText = matches[0]
                        Log.d("HearingAssistance", "Final recognized text: '${matches[0]}'")
                        Log.d("HearingAssistance", "Final background text: '$backgroundAudioText'")
                    } else {
                        recognizedText = matches[0]
                        Log.d("VisionAssistance", "Final recognized text for vision: '$recognizedText'")
                        // CHANGED: Navigate immediately when we get final results
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (!hasNavigated) {
                                navigateWithQuestion(recognizedText)
                            }
                        }, 500) // Shorter delay
                    }
                } else {
                    if (assistanceType == AssistanceType.HEARING) {
                        Log.d("HearingAssistance", "No speech results captured in final results")
                    } else {
                        Log.d("VisionAssistance", "No speech results captured for vision")
                    }
                }

                isListening = false
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (hasNavigated) return

                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    if (assistanceType == AssistanceType.HEARING) {
                        backgroundAudioText = matches[0]
                        Log.d("HearingAssistance", "Partial text captured: '${matches[0]}'")
                    } else {
                        recognizedText = matches[0]
                        Log.d("VisionAssistance", "Partial text for vision: '$recognizedText'")

                        // ADDED: If we get good partial results for vision, we can navigate early
                        if (recognizedText.length > 10) { // If we have substantial text
                            shouldNavigateOnResults = true
                        }
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer?.setRecognitionListener(recognitionListener)
        speechRecognizer?.startListening(intent)
    }

    // Start listening when screen loads
    LaunchedEffect(Unit) {
        delay(500)

        if (assistanceType == AssistanceType.HEARING) {
            Log.d("HearingAssistance", "Initializing hearing assistance mode")
            backgroundAudioText = ""
        } else {
            Log.d("VisionAssistance", "Initializing vision assistance mode")
            recognizedText = ""
        }

        startBackgroundRecording()

        if (assistanceType == AssistanceType.HEARING) {
            delay(8000)
            if (!hasNavigated) {
                Log.d("HearingAssistance", "8-second timeout reached, proceeding with navigation")
                navigateWithQuestion("")
            }
        } else {
            // CHANGED: For vision, wait longer and ensure we navigate with current text
            delay(8000)
            if (!hasNavigated) {
                Log.d("VisionAssistance", "8-second timeout reached, proceeding with question: '$recognizedText'")
                navigateWithQuestion(recognizedText) // Make sure we pass current recognized text
            }
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
                    assistanceType == AssistanceType.HEARING && isListening -> "Recording background audio for hearing assistance"
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

        if (showManualOption && assistanceType == AssistanceType.VISION) {
            // Show manual options only for vision when voice recognition fails
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
                        onClick = {
                            Log.d("VisionAssistance", "Manual continue without question selected")
                            navigateWithQuestion("")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text("Continue without question")
                    }

                    OutlinedButton(
                        onClick = {
                            showManualOption = false
                            startBackgroundRecording()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Try voice recognition again")
                    }
                }
            }

        } else {
            // Normal interface
            if (assistanceType == AssistanceType.HEARING) {
                Text(
                    text = if (isListening) "Recording Background Audio..." else "Preparing to record...",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = if (isListening) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Step 2: Background Audio Recording",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                if (isListening) {
                    Text(
                        text = "Capturing all background sounds and speech for analysis...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }

            } else {
                // Vision assistance: Show normal interface with captured text
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
                        isListening -> Color(0xFF4CAF50)
                        recognizedText.isNotEmpty() -> Color(0xFF2196F3)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                if (isListening) {
                    Text(
                        text = "Ask what you want to identify in the video",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // IMPORTANT: Show recognized text for vision - this should be visible!
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
                                text = "\"$recognizedText\"",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Show waveform animation while listening (for both types)
            if (isListening) {
                WaveformAnimation(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
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
                        text = if (assistanceType == AssistanceType.HEARING) "Recording..." else "Listening...",
                        fontSize = 14.sp,
                        color = Color.Green,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}