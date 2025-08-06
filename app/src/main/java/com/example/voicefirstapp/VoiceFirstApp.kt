// app/src/main/java/com/example/voicefirstapp/VoiceFirstApp.kt
package com.example.voicefirstapp

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.voicefirstapp.screens.*
import com.example.voicefirstapp.utils.ExtractedFrame

@Composable
fun VoiceFirstApp(tts: TextToSpeech?) {
    val navController = rememberNavController()

    // Shared state holders
    var extractedFrames: List<ExtractedFrame> by remember { mutableStateOf(emptyList()) }
    var userQuestion: String by remember { mutableStateOf("") }
    var assistanceType: AssistanceType by remember { mutableStateOf(AssistanceType.VISION) }

    NavHost(
        navController = navController,
        startDestination = "customer_preference"
    ) {
        composable("customer_preference") {
            CustomerPreferenceScreen(
                tts = tts,
                onNavigateToVoiceInput = { selectedType ->
                    assistanceType = selectedType
                    navController.navigate("voice_input")
                }
            )
        }

        composable("voice_input") {
            VoiceInputScreen(
                assistanceType = assistanceType,
                onNavigateToVideoCapture = { question ->
                    userQuestion = question
                    navController.navigate("video_capture")
                },
                onNavigateToAudioProcessing = { question ->
                    userQuestion = question
                    navController.navigate("audio_processing")
                }
            )
        }

        composable("video_capture") {
            VideoCaptureScreen(
                tts = tts,
                userQuestion = userQuestion,
                onNavigateToProcessing = { frames ->
                    extractedFrames = frames
                    navController.navigate("processing")
                }
            )
        }

        composable("processing") {
            VideoProcessingScreen(
                frames = extractedFrames,
                userQuestion = userQuestion,
                tts = tts,
                onNavigateToResult = { result ->
                    navController.navigate("result/$result")
                }
            )
        }

        composable("audio_processing") {
            AudioProcessingScreen(
                userQuestion = userQuestion,
                tts = tts,
                onNavigateToResult = { result ->
                    navController.navigate("result/$result")
                }
            )
        }

        composable("result/{result}") { backStackEntry ->
            val result = backStackEntry.arguments?.getString("result") ?: "Sorry, I couldn't analyze the content."
            ResultScreen(
                result = result,
                tts = tts,
                onNavigateToHome = {
                    // Reset state when going back to customer preference screen
                    extractedFrames = emptyList()
                    userQuestion = ""
                    assistanceType = AssistanceType.VISION
                    navController.navigate("customer_preference") {
                        popUpTo("customer_preference") { inclusive = true }
                    }
                }
            )
        }
    }
}