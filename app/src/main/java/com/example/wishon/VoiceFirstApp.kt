// app/src/main/java/com/example/voicefirstapp/VoiceFirstApp.kt
package com.example.wishon

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.wishon.screens.*
import com.example.wishon.utils.ExtractedFrame

@Composable
fun VoiceFirstApp(tts: TextToSpeech?) {
    val navController = rememberNavController()

    // Shared state holders
    var extractedFrames: List<ExtractedFrame> by remember { mutableStateOf(emptyList()) }
    var userQuestion: String by remember { mutableStateOf("") }
    var assistanceType: AssistanceType by remember { mutableStateOf(AssistanceType.VISION) }
    var selectedLanguage: SupportedLanguage by remember { mutableStateOf(SupportedLanguage.ENGLISH) }

    NavHost(
        navController = navController,
        startDestination = "customer_preference"
    ) {
        composable("customer_preference") {
            CustomerPreferenceScreen(
                tts = tts,
                onNavigateToVoiceInput = { selectedType ->
                    assistanceType = selectedType
                    navController.navigate("language_preference")
                }
            )
        }

        composable("language_preference") {
            LanguagePreferenceScreen(
                tts = tts,
                onLanguageSelected = { language ->
                    selectedLanguage = language
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
                selectedLanguage = selectedLanguage, // Pass language to processing
                tts = tts,
                onNavigateToResult = { result ->
                    navController.navigate("result/$result")
                }
            )
        }

        composable("audio_processing") {
            AudioProcessingScreen(
                userQuestion = userQuestion,
                selectedLanguage = selectedLanguage, // Pass language to processing
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
                selectedLanguage = selectedLanguage, // Pass language to result screen
                assistanceType = assistanceType, // Pass assistance type to result screen
                tts = tts,
                onNavigateToHome = {
                    // Reset state when going back to customer preference screen
                    extractedFrames = emptyList()
                    userQuestion = ""
                    assistanceType = AssistanceType.VISION
                    selectedLanguage = SupportedLanguage.ENGLISH
                    navController.navigate("customer_preference") {
                        popUpTo("customer_preference") { inclusive = true }
                    }
                }
            )
        }
    }
}