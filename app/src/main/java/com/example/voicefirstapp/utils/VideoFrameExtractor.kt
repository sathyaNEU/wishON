// app/src/main/java/com/example/voicefirstapp/utils/VideoFrameExtractor.kt
package com.example.voicefirstapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExtractedFrame(
    val bitmap: Bitmap,
    val timestampMs: Long
)

object VideoFrameExtractor {
    suspend fun extractFrames(context: Context, videoUri: Uri, frameCount: Int = 1): List<ExtractedFrame> {
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            val frames = mutableListOf<ExtractedFrame>()

            try {
                retriever.setDataSource(context, videoUri)

                val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durationMs = durationString?.toLongOrNull() ?: 5000L // Default 5 seconds

                // Extract one frame from the middle of the video
                val timestampMs = durationMs / 2
                val bitmap = retriever.getFrameAtTime(
                    timestampMs * 1000, // Convert to microseconds
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )

                bitmap?.let {
                    frames.add(ExtractedFrame(it, timestampMs))
                }
            } catch (e: Exception) {
                // Handle extraction error
                e.printStackTrace()
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            frames
        }
    }
}