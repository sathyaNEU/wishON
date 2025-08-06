// app/src/main/java/com/example/voicefirstapp/screens/LanguagePreferenceScreen.kt
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Language
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

enum class SupportedLanguage(
    val displayName: String,
    val voiceKeywords: List<String>,
    val llmLanguageName: String
) {
    ENGLISH(
        displayName = "English",
        voiceKeywords = listOf("english", "inglés", "anglais", "englisch"),
        llmLanguageName = "English"
    ),
    SPANISH(
        displayName = "Español",
        voiceKeywords = listOf("spanish", "español", "espagnol", "spanisch"),
        llmLanguageName = "Spanish"
    ),
    FRENCH(
        displayName = "Français",
        voiceKeywords = listOf("french", "francés", "français", "französisch"),
        llmLanguageName = "French"
    ),
    GERMAN(
        displayName = "Deutsch",
        voiceKeywords = listOf("german", "alemán", "allemand", "deutsch"),
        llmLanguageName = "German"
    ),
    HINDI(
        displayName = "हिन्दी",
        voiceKeywords = listOf("hindi", "हिन्दी", "हिंदी", "hindou"),
        llmLanguageName = "Hindi"
    ),
    CHINESE(
        displayName = "中文",
        voiceKeywords = listOf("chinese", "chino", "chinois", "中文", "mandarin"),
        llmLanguageName = "Chinese"
    )
}

@Composable
fun LanguagePreferenceScreen(
    tts: TextToSpeech?,
    onLanguageSelected: (SupportedLanguage) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var recognizedText by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf<SupportedLanguage?>(null) }
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

        // Check each language for matches
        for (language in SupportedLanguage.values()) {
            for (keyword in language.voiceKeywords) {
                if (lowerText.contains(keyword.lowercase())) {
                    hasNavigated = true
                    selectedLanguage = language
                    stopSpeechRecognizer()

                    tts?.speak(
                        "${language.displayName} selected",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "language_selected"
                    )

                    coroutineScope.launch {
                        delay(2000) // 2 seconds delay
                        onLanguageSelected(language)
                    }
                    return
                }
            }
        }
    }

    // Start listening function (same logic as CustomerPreferenceScreen)
    fun startListening() {
        if (hasNavigated || permissionDenied) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return
        }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionDenied = true
            return
        }

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

                    when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            permissionDenied = true
                            return
                        }
                        SpeechRecognizer.ERROR_CLIENT,
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
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
            isListening = false
        }
    }

    // Start listening when screen loads
    LaunchedEffect(Unit) {
        delay(500) // Small delay to let screen settle
        tts?.speak(
            "Choose your language. Touch a language or say the name",
            TextToSpeech.QUEUE_FLUSH,
            null,
            "language_instruction"
        )
        delay(1000)
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
                contentDescription = "Choose your language: Touch a language card or say the language name"
            }
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Language selection",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Choose Language",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Touch a language or say its name",
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

            // Listening indicator
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

        // Language options in a scrollable grid
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SupportedLanguage.values().toList().chunked(2)) { rowLanguages ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowLanguages.forEach { language ->
                        LanguageCard(
                            language = language,
                            isSelected = selectedLanguage == language,
                            onClick = {
                                if (!hasNavigated) {
                                    hasNavigated = true
                                    selectedLanguage = language
                                    stopSpeechRecognizer()

                                    tts?.speak(
                                        "${language.displayName} selected",
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "language_selected"
                                    )

                                    coroutineScope.launch {
                                        delay(2000)
                                        onLanguageSelected(language)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // If odd number of languages, add spacer
                    if (rowLanguages.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Voice visualization at bottom
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

@Composable
fun LanguageCard(
    language: SupportedLanguage,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1.2f)
            .clickable { onClick() }
            .semantics {
                contentDescription = "Select ${language.displayName} language"
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = getLanguageEmoji(language),
                fontSize = 32.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = language.displayName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = getLanguageExample(language),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}

private fun getLanguageEmoji(language: SupportedLanguage): String {
    return when (language) {
        SupportedLanguage.ENGLISH -> "🇺🇸"
        SupportedLanguage.SPANISH -> "🇪🇸"
        SupportedLanguage.FRENCH -> "🇫🇷"
        SupportedLanguage.GERMAN -> "🇩🇪"
        SupportedLanguage.HINDI -> "🇮🇳"
        SupportedLanguage.CHINESE -> "🇨🇳"
    }
}

private fun getLanguageExample(language: SupportedLanguage): String {
    return when (language) {
        SupportedLanguage.ENGLISH -> "Hello"
        SupportedLanguage.SPANISH -> "Hola"
        SupportedLanguage.FRENCH -> "Bonjour"
        SupportedLanguage.GERMAN -> "Hallo"
        SupportedLanguage.HINDI -> "नमस्ते"
        SupportedLanguage.CHINESE -> "你好"
    }
}