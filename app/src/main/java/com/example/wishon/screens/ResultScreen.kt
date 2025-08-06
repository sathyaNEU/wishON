// app/src/main/java/com/example/voicefirstapp/screens/ResultScreen.kt
package com.example.wishon.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wishon.components.AudioWaveAnimation
import kotlinx.coroutines.delay
import java.util.*

@Composable
fun ResultScreen(
    result: String,
    selectedLanguage: SupportedLanguage,
    assistanceType: AssistanceType,
    tts: TextToSpeech?,
    onNavigateToHome: () -> Unit
) {
    var isSpeaking by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current

    // Calculate 75% of screen height for text area
    val screenHeightDp = configuration.screenHeightDp.dp
    val textAreaHeight = screenHeightDp * 0.75f
    val animationAreaHeight = screenHeightDp * 0.25f

    // Map SupportedLanguage to TTS Locale
    fun getTTSLocale(language: SupportedLanguage): Locale {
        return when (language) {
            SupportedLanguage.ENGLISH -> Locale.US
            SupportedLanguage.SPANISH -> Locale("es", "ES")
            SupportedLanguage.FRENCH -> Locale.FRENCH
            SupportedLanguage.GERMAN -> Locale.GERMAN
            SupportedLanguage.HINDI -> Locale("hi", "IN")
            SupportedLanguage.CHINESE -> Locale.CHINESE
        }
    }

    LaunchedEffect(result) {
        // Only speak the result for Vision support, not for Hearing support
        if (assistanceType == AssistanceType.VISION && tts != null) {
            isSpeaking = true

            // Set TTS language to match selected language
            tts.language = getTTSLocale(selectedLanguage)

            // Speak the result using TTS in the selected language
            tts.speak(result, TextToSpeech.QUEUE_FLUSH, null, "result")

            // Estimate TTS duration (rough calculation based on text length)
            delay(20*1000)

            isSpeaking = false
        }

        // Brief pause before returning to home
        delay(10*1000)
        onNavigateToHome()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Result in ${selectedLanguage.displayName}: $result"
            }
    ) {
        // Header with language and assistance type indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language indicator
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Language",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = selectedLanguage.displayName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Assistance type indicator
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (assistanceType) {
                        AssistanceType.VISION -> Icons.Default.Visibility
                        AssistanceType.HEARING -> Icons.Default.Hearing
                    },
                    contentDescription = assistanceType.name,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${assistanceType.name} SUPPORT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // Text Area - Takes up 75% of screen height
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(textAreaHeight)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Result:", // Keep English UI text
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Scrollable text area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = result, // This will be in the selected language from LLM
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
        }

        // Animation Area - Takes up 25% of screen height
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(animationAreaHeight)
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Only show speaking animation for Vision support
            if (assistanceType == AssistanceType.VISION) {
                if (isSpeaking) {
                    Text(
                        text = "Reading result...", // Keep English UI text
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Audio wave animation during TTS playback
                    AudioWaveAnimation(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    )
                } else {
                    Text(
                        text = "Completed", // Keep English UI text
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                // For Hearing support, just show completion
                Text(
                    text = "Completed", // Keep English UI text
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}