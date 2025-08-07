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
    var backgroundAudioText: String by remember { mutableStateOf("") }
    var assistanceType: AssistanceType by remember { mutableStateOf(AssistanceType.VISION) }
    var selectedLanguage: SupportedLanguage by remember { mutableStateOf(SupportedLanguage.ENGLISH) }

    // ADDED: Store the result in state instead of URL parameter
    var analysisResult: String by remember { mutableStateOf("") }

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
                    // Navigate based on assistance type
                    when (assistanceType) {
                        AssistanceType.VISION -> navController.navigate("voice_input")
                        AssistanceType.HEARING -> navController.navigate("user_question_input")
                    }
                }
            )
        }

        // For HEARING support: First collect user question
        composable("user_question_input") {
            UserQuestionInputScreen(
                tts = tts,
                onNavigateToBackgroundRecording = { question ->
                    userQuestion = question
                    navController.navigate("voice_input")
                }
            )
        }

        composable("voice_input") {
            VoiceInputScreen(
                assistanceType = assistanceType,
                onNavigateToVideoCapture = { question ->
                    // For VISION: question is the user question
                    if (assistanceType == AssistanceType.VISION) {
                        userQuestion = question
                        android.util.Log.d("VoiceFirstApp", "Vision: Captured question: '$question'")
                    }
                    navController.navigate("video_capture")
                },
                onNavigateToAudioProcessing = { capturedBackgroundAudio ->
                    // Store the background audio text properly
                    backgroundAudioText = capturedBackgroundAudio
                    android.util.Log.d("VoiceFirstApp", "Hearing: Captured background audio: '$capturedBackgroundAudio'")
                    navController.navigate("audio_processing")
                }
            )
        }

        composable("video_capture") {
            VideoCaptureScreen(
                tts = tts,
                userQuestion = userQuestion,
                onNavigateToProcessing = { frames, question ->
                    extractedFrames = frames
                    userQuestion = question
                    android.util.Log.d("VoiceFirstApp", "Video capture complete. Question: '$question', Frames: ${frames.size}")
                    navController.navigate("processing")
                }
            )
        }

        composable("processing") {
            VideoProcessingScreen(
                frames = extractedFrames,
                userQuestion = userQuestion,
                selectedLanguage = selectedLanguage,
                tts = tts,
                onNavigateToResult = { result ->
                    // FIXED: Store result in state instead of URL parameter
                    analysisResult = result
                    android.util.Log.d("VoiceFirstApp", "Processing complete. Result length: ${result.length}")
                    android.util.Log.d("VoiceFirstApp", "Result preview: ${result.take(100)}...")

                    // Navigate without passing result as URL parameter
                    navController.navigate("result")
                }
            )
        }

        composable("audio_processing") {
            AudioProcessingScreen(
                userQuestion = userQuestion,
                backgroundAudioText = backgroundAudioText,
                selectedLanguage = selectedLanguage,
                tts = tts,
                onNavigateToResult = { result ->
                    // FIXED: Store result in state instead of URL parameter
                    analysisResult = result
                    android.util.Log.d("VoiceFirstApp", "Audio processing complete. Result length: ${result.length}")
                    android.util.Log.d("VoiceFirstApp", "Result preview: ${result.take(100)}...")

                    // Navigate without passing result as URL parameter
                    navController.navigate("result")
                }
            )
        }

        // FIXED: Simple route without parameter
        composable("result") {
            ResultScreen(
                result = analysisResult, // Use state variable instead of URL parameter
                selectedLanguage = selectedLanguage,
                assistanceType = assistanceType,
                tts = tts,
                onNavigateToHome = {
                    // Reset state when going back to customer preference screen
                    extractedFrames = emptyList()
                    userQuestion = ""
                    backgroundAudioText = ""
                    analysisResult = "" // RESET RESULT TOO
                    assistanceType = AssistanceType.VISION
                    selectedLanguage = SupportedLanguage.ENGLISH
                    android.util.Log.d("VoiceFirstApp", "State reset, navigating to home")
                    navController.navigate("customer_preference") {
                        popUpTo("customer_preference") { inclusive = true }
                    }
                }
            )
        }
    }
}