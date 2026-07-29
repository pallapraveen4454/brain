package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
    authRepository: com.example.data.AuthRepository = remember { com.example.data.AuthRepository() }
) {
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "glowPulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulseScale"
    )

    LaunchedEffect(Unit) {
        // Animate fade & scale
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(
                durationMillis = 1000,
                easing = LinearOutSlowInEasing
            )
        )
        alpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(
                durationMillis = 800
            )
        )

        // Wait 2 seconds before checking auth state
        delay(2000)
        val isLoggedIn = try {
            authRepository.isUserLoggedIn()
        } catch (e: Exception) {
            false
        }
        if (isLoggedIn) {
            onNavigateToHome()
        } else {
            onNavigateToLogin()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("splash_screen")
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        // Background Glow Effect
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(scale.value * glowScale)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PrimaryPurple.copy(alpha = 0.45f),
                            PrimaryPurpleLight.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
                .padding(24.dp)
        ) {
            // Brain Quiz Logo
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryPurple, PrimaryPurpleLight)
                        )
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.brain_quiz_logo),
                    contentDescription = "BrainQuizAI Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "BrainQuizAI",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 36.sp,
                    letterSpacing = 0.5.sp
                ),
                color = TextWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("splash_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Challenge your Mind",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                ),
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("splash_tagline")
            )
        }
    }
}
