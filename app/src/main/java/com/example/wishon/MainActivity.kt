package com.example.wishon

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.wishon.ui.theme.VoiceFirstAppTheme
import com.example.wishon.utils.GemmaLLMService
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private var isTtsInitialized by mutableStateOf(false)
    private var isModelReady by mutableStateOf(false)
    private var loadingStatus by mutableStateOf("Checking permissions...")
    private var modelStatus by mutableStateOf("Unknown")
    private var showFilePickerOption by mutableStateOf(false)
    private var showNetworkOptions by mutableStateOf(false)

    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            lifecycleScope.launch {
                loadingStatus = "Importing selected model file..."
                val success = GemmaLLMService.importModelFromUri(this@MainActivity, it)
                if (success) {
                    updateModelStatus()
                    prepareModel()
                } else {
                    loadingStatus = "❌ Failed to import selected file"
                    showFilePickerOption = true
                }
            }
        }
    }

    // Storage permission launcher for MANAGE_EXTERNAL_STORAGE
    private val manageStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                Log.d("MainActivity", "MANAGE_EXTERNAL_STORAGE permission granted")
                loadingStatus = "Storage permission granted, checking model..."
                prepareModel()
            } else {
                Log.w("MainActivity", "MANAGE_EXTERNAL_STORAGE permission denied")
                loadingStatus = "Storage permission denied - will try downloading or file picker"
                showNetworkOptions = true
            }
        }
    }

    // Regular permissions launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val deniedPermissions = mutableListOf<String>()

        permissions.entries.forEach { (permission, isGranted) ->
            if (!isGranted) {
                Log.w("MainActivity", "Permission denied: $permission")
                deniedPermissions.add(permission.split(".").last())
            }
        }

        if (deniedPermissions.isNotEmpty()) {
            loadingStatus = "Some permissions denied: ${deniedPermissions.joinToString(", ")}"
        }

        // After regular permissions, check for MANAGE_EXTERNAL_STORAGE
        checkStoragePermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize TTS
        tts = TextToSpeech(this, this)

        // Set up status callback
        GemmaLLMService.onStatusUpdate = { status ->
            loadingStatus = status
        }

        // Check model status first
        updateModelStatus()

        // Request necessary permissions including network
        requestPermissions()

        setContent {
            VoiceFirstAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isModelReady) {
                        VoiceFirstApp(tts = if (isTtsInitialized) tts else null)
                    } else {
                        ModelLoadingScreen(
                            loadingStatus = loadingStatus,
                            showFilePickerOption = showFilePickerOption,
                            showNetworkOptions = showNetworkOptions,
                            onRetry = {
                                updateModelStatus()
                                checkStoragePermission()
                            },
                            onFilePicker = {
                                filePickerLauncher.launch("*/*")
                            },
                            onRequestStoragePermission = {
                                requestStoragePermission()
                            },
                            onDownloadModel = {
                                lifecycleScope.launch {
                                    prepareModel()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun updateModelStatus() {
        val status = GemmaLLMService.getModelStatus(this)
        modelStatus = when {
            status.isFound && !status.needsImport ->
                "✅ Model ready: ${status.sizeInMB} MB"
            status.isFound && status.needsImport ->
                "📁 Model found in Downloads: ${status.sizeInMB} MB (needs import)"
            else ->
                "📥 ${status.errorMessage}"
        }
    }

    private fun requestPermissions() {
        loadingStatus = "Checking permissions..."

        // Enhanced permissions for vision, audio support, and network access
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.INTERNET // For model downloading
            )
        } else {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET // For model downloading
            )
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            loadingStatus = "Requesting ${permissionsToRequest.size} permissions for vision, audio, and network support..."
            Log.d("MainActivity", "Requesting permissions: ${permissionsToRequest.joinToString()}")
            requestPermissionLauncher.launch(permissionsToRequest)
        } else {
            loadingStatus = "All permissions granted, checking storage access..."
            checkStoragePermission()
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                Log.d("MainActivity", "MANAGE_EXTERNAL_STORAGE already granted")
                loadingStatus = "Storage access granted, preparing AI model..."
                prepareModel()
            } else {
                Log.d("MainActivity", "MANAGE_EXTERNAL_STORAGE not granted")
                loadingStatus = "Need storage permission to save model, or can download directly"
                showNetworkOptions = true
            }
        } else {
            // For older Android versions, use regular READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                loadingStatus = "Storage access granted, preparing AI model..."
                prepareModel()
            } else {
                loadingStatus = "Storage permission needed for model access, or can download directly"
                showNetworkOptions = true
            }
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                manageStoragePermissionLauncher.launch(intent)
            } catch (e: Exception) {
                // Fallback to general manage all files setting
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    manageStoragePermissionLauncher.launch(intent)
                } catch (e2: Exception) {
                    Log.e("MainActivity", "Could not open storage permission settings", e2)
                    showNetworkOptions = true
                }
            }
        }
    }

    private fun prepareModel() {
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Starting AI model initialization...")
                loadingStatus = "Preparing multimodal AI model for vision and audio support..."

                val initSuccess = GemmaLLMService.initializeLLM(this@MainActivity)

                if (initSuccess) {
                    Log.d("MainActivity", "AI model initialized successfully")
                    loadingStatus = "AI model ready for vision and audio assistance!"
                    isModelReady = true
                    showFilePickerOption = false
                    showNetworkOptions = false
                } else {
                    Log.e("MainActivity", "Failed to initialize AI model")
                    loadingStatus = "❌ Failed to initialize AI model"
                    showNetworkOptions = true
                    showFilePickerOption = true
                }

                updateModelStatus()
                Log.d("MainActivity", GemmaLLMService.getModelInfo(this@MainActivity))

            } catch (e: Exception) {
                Log.e("MainActivity", "Error during model initialization", e)
                loadingStatus = "❌ Error: ${e.message}"
                showNetworkOptions = true
                showFilePickerOption = true
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("MainActivity", "TTS language not supported, trying UK")
                tts.setLanguage(Locale.UK)
            }
            isTtsInitialized = true
            Log.d("MainActivity", "TTS initialized successfully for accessibility support")
        } else {
            Log.e("MainActivity", "TTS initialization failed")
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        GemmaLLMService.cleanup()
        super.onDestroy()
    }
}

@Composable
fun ModelLoadingScreen(
    loadingStatus: String,
    showFilePickerOption: Boolean,
    showNetworkOptions: Boolean,
    onRetry: () -> Unit,
    onFilePicker: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    onDownloadModel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            if (!loadingStatus.contains("❌") && !showFilePickerOption && !showNetworkOptions) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(
                text = "AI Model Setup",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "For Vision & Hearing Support",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = loadingStatus,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            // Show note about model picker when model failed or not ready
            if (loadingStatus.contains("❌") || showFilePickerOption || showNetworkOptions) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📝 Note",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "If the model failed to load or is not initialized, please use the model picker option below to choose your AI model file.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (showNetworkOptions) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🌐 AI Model Setup Options",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Option 1: Auto-download
                        Button(
                            onClick = onDownloadModel,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Download Model Automatically")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "OR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Option 2: Grant storage permission
                        OutlinedButton(
                            onClick = onRequestStoragePermission,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Storage Permission")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "OR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Option 3: File picker
                        OutlinedButton(
                            onClick = onFilePicker,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Select Model File Manually")
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "The app will automatically download the multimodal AI model, or you can grant storage permission to check Downloads folder, or manually select the model file.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            } else if (showFilePickerOption) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📁 AI Model Import Options",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Option 1: Grant storage permission
                        Button(
                            onClick = onRequestStoragePermission,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Storage Permission")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "OR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Option 2: File picker
                        OutlinedButton(
                            onClick = onFilePicker,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Select AI Model File")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Use file picker to select your multimodal AI model (.task file) from anywhere on your device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (!showFilePickerOption && !showNetworkOptions && loadingStatus.contains("❌")) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📥 AI Model Setup Instructions",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "The app can automatically download the AI model, or you can:\n\n" +
                                    "1. Download your multimodal AI model file (.task format)\n" +
                                    "2. Place it in your device's Downloads folder\n" +
                                    "3. Rename it to 'model_version.task'\n" +
                                    "4. Grant storage permission or use file picker\n\n" +
                                    "This model will support both vision and audio analysis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}