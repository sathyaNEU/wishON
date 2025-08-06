// app/src/main/java/com/example/voicefirstapp/components/WaveformAnimation.kt
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
fun WaveformAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_progress"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val barCount = 20
        val barWidth = width / barCount / 2

        for (i in 0 until barCount) {
            val x = i * (width / barCount) + barWidth / 2
            val waveHeight = (sin((animationProgress * 2 * Math.PI + i * 0.5).toFloat()) * 0.5f + 0.5f) * height * 0.8f

            drawLine(
                color = Color.Blue,
                start = Offset(x, centerY - waveHeight / 2),
                end = Offset(x, centerY + waveHeight / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}