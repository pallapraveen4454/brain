package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Global Non-Dismissible No-Internet Blocking Screen.
 * Enforces the strict online-only requirement for BrainQuizAI.
 */
@Composable
fun NoInternetOverlay(
    isOnline: Boolean,
    onRetry: () -> Boolean,
    modifier: Modifier = Modifier
) {
    // Intercept hardware and gesture back navigation so user cannot bypass
    BackHandler(enabled = !isOnline) {
        // Explicitly consume back press when offline
    }

    AnimatedVisibility(
        visible = !isOnline,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        var isRetrying by remember { mutableStateOf(false) }
        var retryErrorMessage by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        val infiniteTransition = rememberInfiniteTransition(label = "pulse_wifi")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale_anim"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground.copy(alpha = 0.98f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
                .testTag("global_no_internet_overlay"),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                backgroundColor = DarkCardSurface,
                borderColor = GlassBorder,
                elevation = 16.dp,
                testTag = "no_internet_card"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Pulsing WifiOff Icon with glowing background ring
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        ErrorRed.copy(alpha = 0.25f),
                                        PrimaryPurple.copy(alpha = 0.10f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(2.dp, ErrorRed.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "No Internet Icon",
                            tint = ErrorRed,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "No Internet Connection",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = TextWhite,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "BrainQuizAI requires an active internet connection to continue.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = PrimaryPurpleLight,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Please enable Wi-Fi or mobile data to access quizzes, leaderboard, and rewards.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 20.sp
                        ),
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    if (retryErrorMessage != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = retryErrorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = ErrorRed,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    GradientButton(
                        text = "Try Again",
                        icon = Icons.Default.Refresh,
                        isLoading = isRetrying,
                        onClick = {
                            if (!isRetrying) {
                                isRetrying = true
                                retryErrorMessage = null
                                scope.launch {
                                    delay(500) // Brief debounce to allow network capability refresh
                                    val nowOnline = onRetry()
                                    isRetrying = false
                                    if (!nowOnline) {
                                        retryErrorMessage = "Still offline. Please check your connection."
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "no_internet_retry_button"
                    )
                }
            }
        }
    }
}
