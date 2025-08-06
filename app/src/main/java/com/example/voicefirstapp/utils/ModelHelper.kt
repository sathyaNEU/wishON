// app/src/main/java/com/example/voicefirstapp/utils/ModelHelper.kt
package com.example.voicefirstapp.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ModelHelper {
    private const val TAG = "ModelHelper"

    // Model file name in assets
    private const val MODEL_FILENAME = "model.task"

    fun copyModelToInternalStorage(context: Context): String? {
        val modelFile = File(context.filesDir, MODEL_FILENAME)

        // If model already exists, return its path
        if (modelFile.exists()) {
            Log.d(TAG, "Model already exists at: ${modelFile.absolutePath}")
            return modelFile.absolutePath
        }

        return try {
            // Copy model from assets to internal storage
            context.assets.open(MODEL_FILENAME).use { inputStream ->
                FileOutputStream(modelFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d(TAG, "Model copied to: ${modelFile.absolutePath}")
            modelFile.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "Failed to copy model from assets", e)
            null
        }
    }

    fun getModelPath(context: Context): String? {
        // First try to get from internal storage
        val internalPath = File(context.filesDir, MODEL_FILENAME)
        if (internalPath.exists()) {
            return internalPath.absolutePath
        }

        // Try to copy from assets
        return copyModelToInternalStorage(context)
    }

    fun deleteModel(context: Context): Boolean {
        val modelFile = File(context.filesDir, MODEL_FILENAME)
        return if (modelFile.exists()) {
            modelFile.delete()
        } else {
            true
        }
    }

    fun getModelSize(context: Context): Long {
        val modelFile = File(context.filesDir, MODEL_FILENAME)
        return if (modelFile.exists()) {
            modelFile.length()
        } else {
            0L
        }
    }
}