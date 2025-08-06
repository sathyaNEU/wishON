// app/src/main/java/com/example/voicefirstapp/SplashActivity.kt
package com.example.voicefirstapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.voicefirstapp.ui.theme.VoiceFirstAppTheme
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {

    companion object {
        private const val SPLASH_DISPLAY_LENGTH = 4000L // 4 seconds for animations
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make splash screen full screen and hide status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContent {
            VoiceFirstAppTheme {
                SplashScreen()
            }
        }

        // Navigate to MainActivity after splash duration
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
            // Add smooth transition animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, SPLASH_DISPLAY_LENGTH)
    }
}

@Composable
fun SplashScreen() {
    // Animation states
    var startAnimations by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (startAnimations) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimations) 1f else 0f,
        animationSpec = tween(durationMillis = 1500, delayMillis = 800),
        label = "contentAlpha"
    )

    LaunchedEffect(Unit) {
        startAnimations = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black,         // Pure black at top
                        Color(0xFF0D0D0D),  // Slightly lighter black
                        Color(0xFF1A1A1A)   // Dark gray at bottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated Logo
            Image(
                painter = painterResource(id = R.drawable.wishon_logo),
                contentDescription = "wishON Logo",
                modifier = Modifier
                    .size(240.dp)
                    .scale(logoScale)
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Powered by Gemma badge
            Card(
                modifier = Modifier
                    .alpha(contentAlpha),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF333333).copy(alpha = 0.8f)
                )
            ) {
                Text(
                    text = "🚀 Powered by Gemma 3n",
                    color = Color(0xFFE0E0E0),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main tagline with gradient text effect
            Text(
                text = "First Offline AI for Hearing\n& Vision Disability Support",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                modifier = Modifier
                    .alpha(contentAlpha)
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Feature highlights with staggered animation
            FeatureHighlights(contentAlpha)
        }

        // Subtle loading indicator at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { index ->
                    val dotAlpha by animateFloatAsState(
                        targetValue = if (startAnimations) 0.7f else 0.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .alpha(dotAlpha)
                            .background(Color(0xFFCCCCCC), shape = RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureHighlights(alpha: Float) {
    val features = listOf(
        "⚡ Optimized Performance" to "Lightning-fast on-device processing",
        "🔒 Privacy-First" to "Secure offline-ready intelligence",
        "🎯 Multimodal AI" to "Audio, text, images & video support",
        "🧠 Smart Resources" to "Dynamic 4B memory optimization"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.alpha(alpha)
    ) {
        features.forEachIndexed { index, (title, description) ->
            FeatureCard(title, description, index)
        }
    }
}

@Composable
fun FeatureCard(title: String, description: String, index: Int) {
    var isVisible by remember { mutableStateOf(false) }

    val cardAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 500,
            delayMillis = 1200 + (index * 150)
        ),
        label = "card$index"
    )

    val cardOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 50f,
        animationSpec = tween(
            durationMillis = 500,
            delayMillis = 1200 + (index * 150)
        ),
        label = "offset$index"
    )

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .offset(y = cardOffset.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E).copy(alpha = 0.9f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFF333333)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 16.sp
                )
            }
        }
    }
}