// app/src/main/java/com/example/voicefirstapp/screens/ResultScreen.kt
package com.example.voicefirstapp.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicefirstapp.components.AudioWaveAnimation
import kotlinx.coroutines.delay

@Composable
fun ResultScreen(
    result: String,
    tts: TextToSpeech?,
    onNavigateToHome: () -> Unit
) {
    var isSpeaking by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Calculate 75% of screen height for text area
    val screenHeightDp = configuration.screenHeightDp.dp
    val textAreaHeight = screenHeightDp * 0.75f
    val animationAreaHeight = screenHeightDp * 0.25f

    LaunchedEffect(result) {
        // Speak the result using TTS
        tts?.speak(result, TextToSpeech.QUEUE_FLUSH, null, "result")

        // Estimate TTS duration (rough calculation based on text length)
        val estimatedDuration = (result.length * 100L).coerceAtLeast(3000L)
        delay(estimatedDuration)

        isSpeaking = false

        // Brief pause before returning to home
        delay(1000)
        onNavigateToHome()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Result: $result"
            }
    ) {
        // Text Area - Takes up 75% of screen height
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(textAreaHeight)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Result:",
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
                    text = result,
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
            if (isSpeaking) {
                Text(
                    text = "Reading result...",
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
                    text = "Completed",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}