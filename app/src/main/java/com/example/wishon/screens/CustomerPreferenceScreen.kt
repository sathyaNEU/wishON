// app/src/main/java/com/example/voicefirstapp/screens/CustomerPreferenceScreen.kt
package com.example.wishon.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.launch
import java.util.*

enum class AssistanceType {
    VISION, HEARING
}

@Composable
fun CustomerPreferenceScreen(
    tts: TextToSpeech?,
    onNavigateToVoiceInput: (AssistanceType) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var recognizedText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<AssistanceType?>(null) }
    var hasNavigated by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    // Function to completely stop and cleanup speech recognizer
    fun stopSpeechRecognizer() {
        speechRecognizer?.let { recognizer ->
            try {
                recognizer.stopListening()
                recognizer.cancel()
                recognizer.destroy()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        speechRecognizer = null
        isListening = false
    }

    // Function to check and navigate based on recognized text
    fun checkAndNavigate(text: String) {
        if (hasNavigated || text.isEmpty()) return

        val lowerText = text.lowercase().trim()

        when {
            lowerText.contains("hear") -> {
                hasNavigated = true
                selectedType = AssistanceType.HEARING
                stopSpeechRecognizer()
                tts?.speak("Hearing support selected", TextToSpeech.QUEUE_FLUSH, null, "hearing_selected")
                coroutineScope.launch {
                    delay(2000) // 2 seconds delay
                    onNavigateToVoiceInput(AssistanceType.HEARING)
                }
            }
            lowerText.contains("vision") || lowerText.contains("see") -> {
                hasNavigated = true
                selectedType = AssistanceType.VISION
                stopSpeechRecognizer()
                tts?.speak("Vision support selected", TextToSpeech.QUEUE_FLUSH, null, "vision_selected")
                coroutineScope.launch {
                    delay(2000) // 2 seconds delay
                    onNavigateToVoiceInput(AssistanceType.VISION)
                }
            }
        }
    }

    // Start listening function with better error handling
    fun startListening() {
        if (hasNavigated || permissionDenied) return

        // Check if speech recognition is available
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return
        }

        // Check microphone permission
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionDenied = true
            return
        }

        // Stop any existing recognizer
        stopSpeechRecognizer()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
            }

            val recognitionListener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (!hasNavigated) {
                        isListening = true
                    }
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    if (!hasNavigated) {
                        isListening = false
                        // Restart listening after a short delay
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (!hasNavigated) {
                                startListening()
                            }
                        }, 1000)
                    }
                }

                override fun onError(error: Int) {
                    if (hasNavigated) return

                    isListening = false

                    // Only restart on certain errors
                    when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            permissionDenied = true
                            return
                        }
                        SpeechRecognizer.ERROR_CLIENT,
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            // Wait longer before retrying on client errors
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                if (!hasNavigated) {
                                    startListening()
                                }
                            }, 2000)
                        }
                        else -> {
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                if (!hasNavigated) {
                                    startListening()
                                }
                            }, 1500)
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    if (hasNavigated) return

                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (matches != null && matches.isNotEmpty()) {
                        recognizedText = matches[0]
                        checkAndNavigate(matches[0])
                    }

                    if (!hasNavigated) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (!hasNavigated) {
                                startListening()
                            }
                        }, 1000)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    if (hasNavigated) return

                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (matches != null && matches.isNotEmpty()) {
                        recognizedText = matches[0]
                        checkAndNavigate(matches[0])
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            }

            speechRecognizer?.setRecognitionListener(recognitionListener)
            speechRecognizer?.startListening(intent)

        } catch (e: Exception) {
            // If we can't start speech recognition, don't show error to user
            // Just continue without voice recognition
            isListening = false
        }
    }

    // Start listening when screen loads
    LaunchedEffect(Unit) {
        delay(500) // Small delay to let screen settle
        startListening()
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
            .semantics {
                contentDescription = "Choose assistance type: Vision support on the left, Hearing support on the right, or say your preference"
            }
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Choose Your Assistance",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Touch left for Vision • Touch right for Hearing\nOr say \"vision\" or \"hearing\"",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Voice feedback display
            if (recognizedText.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "Heard: \"$recognizedText\"",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Listening indicator - only show if actively listening and no permission issues
            if (isListening && !permissionDenied) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = "Listening",
                        tint = Color.Green,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Listening...",
                        fontSize = 12.sp,
                        color = Color.Green,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            if (permissionDenied) {
                Text(
                    text = "Microphone permission needed for voice commands",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Split screen options
        Row(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            // Vision Support (Left Side)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
                    .clickable {
                        if (!hasNavigated) {
                            hasNavigated = true
                            selectedType = AssistanceType.VISION
                            stopSpeechRecognizer()
                            tts?.speak("Vision support selected", TextToSpeech.QUEUE_FLUSH, null, "vision_selected")

                            // Add 2-second delay before navigation
                            coroutineScope.launch {
                                delay(2000) // 2 seconds delay
                                onNavigateToVoiceInput(AssistanceType.VISION)
                            }
                        }
                    }
                    .semantics {
                        contentDescription = "Vision support - Touch to select help with seeing and identifying objects"
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Vision icon",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "VISION\nSUPPORT",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "• Identify objects\n• Read text\n• Describe scenes\n• Navigation help",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )
                }
            }

            // Hearing Support (Right Side)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
                    .clickable {
                        if (!hasNavigated) {
                            hasNavigated = true
                            selectedType = AssistanceType.HEARING
                            stopSpeechRecognizer()
                            tts?.speak("Hearing support selected", TextToSpeech.QUEUE_FLUSH, null, "hearing_selected")

                            // Add 2-second delay before navigation
                            coroutineScope.launch {
                                delay(2000) // 2 seconds delay
                                onNavigateToVoiceInput(AssistanceType.HEARING)
                            }
                        }
                    }
                    .semantics {
                        contentDescription = "Hearing support - Touch to select help with audio and sound recognition"
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = "Hearing icon",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "HEARING\nSUPPORT",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "• Sound identification\n• Audio transcription\n• Voice commands\n• Audio assistance",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Voice visualization at bottom - only show if listening
        if (isListening && !permissionDenied) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WaveformAnimation(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                )
            }
        }
    }
}