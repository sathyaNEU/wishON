// app/src/main/java/com/example/voicefirstapp/utils/GemmaLLMService.kt
package com.example.wishon.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ModelStatus(
    val isFound: Boolean,
    val location: String,
    val sizeInMB: Long,
    val isInitialized: Boolean,
    val errorMessage: String? = null,
    val needsImport: Boolean = false,
    val supportsVision: Boolean = false
)

object GemmaLLMService {
    private var llmInference: LlmInference? = null
    private const val TAG = "GemmaLLMService"
    private const val MODEL_FILENAME = "model_version.task"
    private const val IMPORTS_DIR = "imports"

    // Add callback for UI updates
    var onStatusUpdate: ((String) -> Unit)? = null

    private fun getAppPrivateModelPath(context: Context): String {
        return File(context.getExternalFilesDir(null), "$IMPORTS_DIR/$MODEL_FILENAME").absolutePath
    }

    private fun findModelInPublicStorage(): String? {
        val searchPaths = listOf(
            "/sdcard/Download/$MODEL_FILENAME",
            "/storage/emulated/0/Download/$MODEL_FILENAME",
            "/storage/emulated/0/Downloads/$MODEL_FILENAME",
            "/sdcard/Downloads/$MODEL_FILENAME"
        )

        for (path in searchPaths) {
            try {
                val file = File(path)
                if (file.exists() && file.length() > 0) {
                    Log.d(TAG, "Found model in public storage at: $path")
                    return path
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cannot access path $path: ${e.message}")
            }
        }

        return null
    }

    suspend fun importModelFromUri(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val privateModelPath = getAppPrivateModelPath(context)
                val privateModelFile = File(privateModelPath)

                // Create imports directory if it doesn't exist
                val importsDir = File(context.getExternalFilesDir(null), IMPORTS_DIR)
                if (!importsDir.exists()) {
                    importsDir.mkdirs()
                }

                // Get file size from URI
                var fileSize = 0L
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val sizeIndex = cursor.getColumnIndex("_size")
                            if (sizeIndex != -1) {
                                fileSize = cursor.getLong(sizeIndex)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not get file size from URI: ${e.message}")
                }

                val modelSizeMB = fileSize / (1024 * 1024)
                onStatusUpdate?.invoke("Importing model from selected file (${modelSizeMB} MB)...")
                Log.d(TAG, "Importing model from URI to $privateModelPath")

                // Copy from URI to private storage
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(privateModelFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytes = 0L
                        var lastProgressUpdate = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead

                            // Update progress every 200ms
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastProgressUpdate > 200) {
                                if (fileSize > 0) {
                                    val progress = (totalBytes * 100 / fileSize).toInt()
                                    onStatusUpdate?.invoke("Importing: $progress% (${totalBytes / (1024 * 1024)} MB)")
                                } else {
                                    onStatusUpdate?.invoke("Importing: ${totalBytes / (1024 * 1024)} MB")
                                }
                                lastProgressUpdate = currentTime
                            }
                        }
                    }
                }

                // Verify the copy was successful
                if (privateModelFile.exists() && privateModelFile.length() > 0) {
                    onStatusUpdate?.invoke("Model imported successfully!")
                    Log.d(TAG, "Model imported successfully to $privateModelPath")
                    return@withContext true
                } else {
                    onStatusUpdate?.invoke("Model import failed - file verification failed")
                    return@withContext false
                }

            } catch (e: Exception) {
                val errorMsg = "Failed to import model from URI: ${e.message}"
                Log.e(TAG, errorMsg, e)
                onStatusUpdate?.invoke(errorMsg)
                return@withContext false
            }
        }
    }

    suspend fun importModelIfNeeded(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val privateModelPath = getAppPrivateModelPath(context)
                val privateModelFile = File(privateModelPath)

                // Check if model already exists in private storage
                if (privateModelFile.exists() && privateModelFile.length() > 0) {
                    Log.d(TAG, "Model already exists in private storage")
                    return@withContext true
                }

                // Find model in public storage
                val publicModelPath = findModelInPublicStorage()
                if (publicModelPath == null) {
                    onStatusUpdate?.invoke("Model file not found in Downloads folder")
                    return@withContext false
                }

                val publicModelFile = File(publicModelPath)
                val modelSizeMB = publicModelFile.length() / (1024 * 1024)

                onStatusUpdate?.invoke("Copying model to app storage (${modelSizeMB} MB)...")
                Log.d(TAG, "Importing model from $publicModelPath to $privateModelPath")

                // Create imports directory if it doesn't exist
                val importsDir = File(context.getExternalFilesDir(null), IMPORTS_DIR)
                if (!importsDir.exists()) {
                    importsDir.mkdirs()
                }

                // Copy the model file
                publicModelFile.inputStream().use { input ->
                    FileOutputStream(privateModelFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytes = 0L
                        val fileSize = publicModelFile.length()
                        var lastProgressUpdate = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead

                            // Update progress every 200ms
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastProgressUpdate > 200) {
                                val progress = (totalBytes * 100 / fileSize).toInt()
                                onStatusUpdate?.invoke("Copying model: $progress% (${totalBytes / (1024 * 1024)} MB / ${modelSizeMB} MB)")
                                lastProgressUpdate = currentTime
                            }
                        }
                    }
                }

                // Verify the copy was successful
                if (privateModelFile.exists() && privateModelFile.length() == publicModelFile.length()) {
                    onStatusUpdate?.invoke("Model imported successfully!")
                    Log.d(TAG, "Model imported successfully to $privateModelPath")
                    return@withContext true
                } else {
                    onStatusUpdate?.invoke("Model import failed - file verification failed")
                    return@withContext false
                }

            } catch (e: Exception) {
                val errorMsg = "Failed to import model: ${e.message}"
                Log.e(TAG, errorMsg, e)
                onStatusUpdate?.invoke(errorMsg)
                return@withContext false
            }
        }
    }

    suspend fun initializeLLM(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                onStatusUpdate?.invoke("Checking model availability...")

                // First, import model if needed
                val importSuccess = importModelIfNeeded(context)
                if (!importSuccess) {
                    return@withContext false
                }

                val modelPath = getAppPrivateModelPath(context)
                val modelFile = File(modelPath)

                if (!modelFile.exists()) {
                    onStatusUpdate?.invoke("Model file not found after import")
                    return@withContext false
                }

                val sizeInMB = modelFile.length() / (1024 * 1024)
                onStatusUpdate?.invoke("Loading AI model (${sizeInMB} MB)...")
                Log.d(TAG, "Using model at: $modelPath")

                // Create LLM inference with multimodal support
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(512)  // Increased for better descriptions
                    .setMaxTopK(64)
                    .setMaxNumImages(1)  // Support for one image per session
                    .build()

                try {
                    llmInference = LlmInference.createFromOptions(context, options)
                } catch (e: UnsatisfiedLinkError) {
                    val errorMsg = "Model compatibility error: This model requires a newer MediaPipe version"
                    Log.e(TAG, errorMsg, e)
                    onStatusUpdate?.invoke(errorMsg)
                    return@withContext false
                } catch (e: IllegalArgumentException) {
                    val errorMsg = "Invalid model format: Please check if this is a valid MediaPipe .task file"
                    Log.e(TAG, errorMsg, e)
                    onStatusUpdate?.invoke(errorMsg)
                    return@withContext false
                } catch (e: RuntimeException) {
                    val errorMsg = "Model loading failed: ${e.message} - Check model version compatibility"
                    Log.e(TAG, errorMsg, e)
                    onStatusUpdate?.invoke(errorMsg)
                    return@withContext false
                }
                val successMsg = "AI model initialized successfully!"
                Log.d(TAG, successMsg)
                onStatusUpdate?.invoke(successMsg)
                return@withContext true

            } catch (e: OutOfMemoryError) {
                val errorMsg = "Not enough memory to load model (${e.message})"
                Log.e(TAG, errorMsg, e)
                onStatusUpdate?.invoke(errorMsg)
                return@withContext false
            } catch (e: Exception) {
                val errorMsg = "Failed to initialize model: ${e.message}"
                Log.e(TAG, errorMsg, e)
                onStatusUpdate?.invoke(errorMsg)
                return@withContext false
            }
        }
    }

    fun getModelStatus(context: Context): ModelStatus {
        val privateModelPath = getAppPrivateModelPath(context)
        val privateModelFile = File(privateModelPath)

        // Check if model exists in private storage (ready to use)
        if (privateModelFile.exists() && privateModelFile.length() > 0) {
            val sizeInMB = privateModelFile.length() / (1024 * 1024)
            return ModelStatus(
                isFound = true,
                location = privateModelPath,
                sizeInMB = sizeInMB,
                isInitialized = isInitialized(),
                needsImport = false,
                supportsVision = true
            )
        }

        // Check if model exists in public storage (needs import)
        val publicModelPath = findModelInPublicStorage()
        if (publicModelPath != null) {
            val publicModelFile = File(publicModelPath)
            val sizeInMB = publicModelFile.length() / (1024 * 1024)
            val supportsVision = true
            return ModelStatus(
                isFound = true,
                location = publicModelPath,
                sizeInMB = sizeInMB,
                isInitialized = false,
                needsImport = true,
                supportsVision = supportsVision
            )
        }

        return ModelStatus(
            isFound = false,
            location = "Not found",
            sizeInMB = 0,
            isInitialized = false,
            errorMessage = "Model file '$MODEL_FILENAME' not found in Downloads folder",
            needsImport = false,
            supportsVision = false
        )
    }

    suspend fun generateResponse(prompt: String, language: String = "English"): String {
        return withContext(Dispatchers.IO) {
            val currentLlm = llmInference
            if (currentLlm == null) {
                Log.e(TAG, "LLM not initialized")
                return@withContext "AI model not available."
            }

            try {
                Log.d(TAG, "=== GEMMA MODEL INPUT (Text Only - $language) ===")
                Log.d(TAG, "Prompt: $prompt")
                Log.d(TAG, "====================================")

                // Add language instruction to prompt
                val languagePrompt = """$prompt

Please respond in: $language"""

                // Use session for consistency with multimodal approach
                val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTopK(40)
                    .setTemperature(0.7f)
                    .build()

                currentLlm.use { llm ->
                    LlmInferenceSession.createFromOptions(llm, sessionOptions).use { session ->
                        session.addQueryChunk(languagePrompt)
                        val response = session.generateResponse().trim()
                        return@withContext response
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating response", e)
                return@withContext "Error generating response: ${e.message}"
            }
        }
    }

    suspend fun analyzeImageForBlindUser(
        bitmap: Bitmap,
        userQuestion: String? = null,
        language: String = "English"
    ): String {
        return withContext(Dispatchers.IO) {
            val currentLlm = llmInference
            if (currentLlm == null) {
                Log.e(TAG, "LLM not initialized")
                return@withContext "AI model not available. Please restart the app and ensure the model file is accessible."
            }

            try {
                Log.d(TAG, "Analyzing image with multimodal capabilities in $language")

                // Convert bitmap to MPImage
                val mpImage = BitmapImageBuilder(bitmap).build()

                val basePrompt = if (userQuestion != null) {
                    """
You are assisting a blind user. Analyze the provided image and answer their question clearly and helpfully.
Question: $userQuestion

Respond in: $language
""".trimIndent()
                } else {
                    """
You are describing an image to a blind user. Mention the main scene, people or objects, layout, colors, any visible text, and anything important or potentially unsafe. Be clear, concise, and fast.

Respond in: $language
""".trimIndent()
                }

                // Create session with vision modality enabled
                val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTopK(40)
                    .setTemperature(0.3f)  // Lower temperature for more consistent descriptions
                    .setGraphOptions(
                        GraphOptions.builder()
                            .setEnableVisionModality(true)
                            .build()
                    )
                    .build()

                currentLlm.use { llm ->
                    LlmInferenceSession.createFromOptions(llm, sessionOptions).use { session ->
                        // Add text prompt first (recommended for better results)
                        session.addQueryChunk(basePrompt)
                        // Add the image
                        session.addImage(mpImage)

                        val response = session.generateResponse().trim()
                        return@withContext response
                    }
                }

            } catch (e: Exception) {
                val errorMsg = "Error analyzing image: ${e.message}"
                Log.e(TAG, errorMsg, e)

                return@withContext "Multimodal capabilities are currently down"
            }
        }
    }

    suspend fun analyzeFrameWithUserQuestion(
        frame: ExtractedFrame,
        userQuestion: String,
        language: String = "English"
    ): String {
        return analyzeImageForBlindUser(frame.bitmap, userQuestion, language)
    }

    fun cleanup() {
        try {
            llmInference?.close()
            llmInference = null
            Log.d(TAG, "Gemma LLM service cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    fun isInitialized(): Boolean {
        return llmInference != null
    }

    // Utility functions for debugging
    fun getModelInfo(context: Context): String {
        val status = getModelStatus(context)
        return if (status.isFound) {
            "Model found at ${status.location}: ${status.sizeInMB} MB (Initialized: ${status.isInitialized}, Needs Import: ${status.needsImport}, Vision Support: ${status.supportsVision})"
        } else {
            status.errorMessage ?: "Model status unknown"
        }
    }
}