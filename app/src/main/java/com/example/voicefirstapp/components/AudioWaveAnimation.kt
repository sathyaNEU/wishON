// app/src/main/java/com/example/voicefirstapp/components/AudioWaveAnimation.kt
package com.example.voicefirstapp.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.sin

@Composable
fun AudioWaveAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "audio_progress"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val barCount = 15
        val barWidth = width / barCount / 3

        for (i in 0 until barCount) {
            val x = i * (width / barCount) + barWidth
            val waveHeight = (sin((animationProgress * 3 * Math.PI + i * 0.8).toFloat()) * 0.6f + 0.4f) * height * 0.7f

            drawLine(
                color = Color.Green,
                start = Offset(x, centerY - waveHeight / 2),
                end = Offset(x, centerY + waveHeight / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}