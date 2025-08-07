// app/src/main/java/com/example/voicefirstapp/screens/UserQuestionInputScreen.kt
package com.example.wishon.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.wishon.components.WaveformAnimation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun UserQuestionInputScreen(
    tts: TextToSpeech?,
    onNavigateToBackgroundRecording: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // State variables
    var userQuestion by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var hasNavigated by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var showTextInput by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    // Function to completely stop and cleanup speech recognizer
    fun stopSpeechRecognizer() {
        speechRecognizer?.let { recognizer ->
            try {
                recognizer.stopListening()
                recognizer.cancel()
                recognizer.destroy()
            } catch (e: Exception) {
                Log.w("UserQuestionInputScreen", "Error during cleanup: ${e.message}")
            }
        }
        speechRecognizer = null
        isListening = false
    }

    // Function to navigate with the captured question
    fun proceedWithQuestion(question: String) {
        if (hasNavigated) return

        hasNavigated = true
        isProcessing = true
        stopSpeechRecognizer()

        val finalQuestion = question.trim().ifEmpty { "" }

        tts?.speak(
            if (finalQuestion.isNotEmpty()) "Question recorded: $finalQuestion. Now please record background audio."
            else "No question provided. Now please record background audio.",
            TextToSpeech.QUEUE_FLUSH,
            null,
            "question_recorded"
        )

        coroutineScope.launch {
            delay(3000) // Give user time to hear the announcement
            onNavigateToBackgroundRecording(finalQuestion)
        }
    }

    // Start voice recognition
    fun startVoiceRecognition() {
        if (hasNavigated || permissionDenied || !SpeechRecognizer.isRecognitionAvailable(context)) {
            showTextInput = true
            return
        }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionDenied = true
            showTextInput = true
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
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
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
                    // Show captured text and options
                    userQuestion = recognizedText
                }
            }

            override fun onError(error: Int) {
                if (hasNavigated) return

                isListening = false

                when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        permissionDenied = true
                        showTextInput = true
                        return
                    }
                    SpeechRecognizer.ERROR_CLIENT,
                    SpeechRecognizer.ERROR_SERVER,
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                        showTextInput = true
                        return
                    }
                    else -> {
                        // Other errors - show text option
                        showTextInput = true
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                if (hasNavigated) return

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    recognizedText = matches[0]
                    userQuestion = recognizedText
                }
                isListening = false
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

    // Start with TTS announcement
    LaunchedEffect(Unit) {
        delay(500)
        tts?.speak(
            "First, please ask your question. You can speak or type your question, then we'll record background audio.",
            TextToSpeech.QUEUE_FLUSH,
            null,
            "question_instruction"
        )
        delay(4000)
        if (!hasNavigated && !showTextInput) {
            startVoiceRecognition()
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
            .padding(24.dp)
            .semantics {
                contentDescription = "Ask your question for hearing assistance - speak or type your question"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Hearing,
                contentDescription = "Hearing support",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "HEARING SUPPORT",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Text(
            text = "Step 1: Your Question",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Ask what you need help with - like understanding audio, translating speech, or identifying sounds",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Main content area
        if (isProcessing) {
            // Processing state
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Question recorded! Preparing background audio recording...",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        } else if (showTextInput || permissionDenied) {
            // Text input mode
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Type question",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Type Your Question",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = userQuestion,
                        onValueChange = { userQuestion = it },
                        label = { Text("What would you like help with?") },
                        placeholder = { Text("e.g., What sounds do you hear? Translate this audio...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "Enter your question for hearing assistance"
                            },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                proceedWithQuestion(userQuestion)
                            }
                        ),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Voice recognition button (if not permission denied)
                        if (!permissionDenied) {
                            OutlinedButton(
                                onClick = {
                                    showTextInput = false
                                    startVoiceRecognition()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice input",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Voice")
                            }
                        }

                        // Proceed button
                        Button(
                            onClick = { proceedWithQuestion(userQuestion) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Continue",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Continue")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Skip option
            TextButton(
                onClick = { proceedWithQuestion("") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip question and go to background audio recording")
            }

        } else {
            // Voice recognition mode
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isListening) {
                    Text(
                        text = "I'm listening to your question...",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = "Speak clearly and ask what you need help with",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    WaveformAnimation(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )

                    // Show partial results if available
                    if (recognizedText.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text(
                                text = "\"$recognizedText\"",
                                fontSize = 16.sp,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    // Listening indicator
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

                } else {
                    // Show captured question and options
                    if (userQuestion.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
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
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 4.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    userQuestion = ""
                                    recognizedText = ""
                                    startVoiceRecognition()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Try Again")
                            }

                            Button(
                                onClick = { proceedWithQuestion(userQuestion) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Continue")
                            }
                        }
                    } else {
                        Text(
                            text = "Getting ready to listen...",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Option to switch to text input
                if (!showTextInput && userQuestion.isEmpty()) {
                    TextButton(
                        onClick = { showTextInput = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Type instead",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Type instead")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Footer info
        Text(
            text = "After your question, we'll record background audio to provide better assistance",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )
    }
}