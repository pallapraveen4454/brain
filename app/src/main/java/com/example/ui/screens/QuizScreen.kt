package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AchievementUnlockedDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.StatCard
import com.example.ui.theme.AccentCoins
import com.example.ui.theme.AccentCoinsGradientEnd
import com.example.ui.theme.AccentXP
import com.example.ui.theme.AccentXPGradientEnd
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.utils.RankUtils
import com.example.utils.SoundEffects
import com.example.utils.VibrationUtils
import com.example.utils.bounceClick
import com.example.viewmodel.QuizUiState
import com.example.viewmodel.QuizViewModel
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun QuizScreen(
    categoryId: String,
    viewModel: QuizViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(categoryId) {
        viewModel.loadQuiz(categoryId)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("quiz_screen")
    ) {
        if (uiState.newlyUnlockedAchievements.isNotEmpty()) {
            AchievementUnlockedDialog(
                achievement = uiState.newlyUnlockedAchievements.first(),
                onDismiss = { viewModel.dismissAchievementDialog() }
            )
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = PrimaryPurpleLight,
                    modifier = Modifier.testTag("quiz_loading_indicator")
                )
            }
        } else if (uiState.questions.isEmpty()) {
            EmptyQuestionsView(
                categoryTitle = uiState.categoryTitle,
                onNavigateBack = onNavigateBack
            )
        } else if (uiState.isQuizComplete) {
            QuizCompleteView(
                uiState = uiState,
                onRestart = { viewModel.restartQuiz() },
                onBackHome = onNavigateBack
            )
        } else {
            QuizActiveView(
                uiState = uiState,
                onSelectOption = { viewModel.submitAnswer(it) },
                onNavigateBack = onNavigateBack
            )
        }
    }
}

@Composable
fun EmptyQuestionsView(
    categoryTitle: String,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            backgroundColor = DarkCardSurface,
            borderColor = GlassBorder,
            elevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = PrimaryPurpleLight,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = categoryTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = TextWhite,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "More questions coming soon for this category.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                GradientButton(
                    text = "Back to Home",
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_questions_back_button")
                )
            }
        }
    }
}

@Composable
fun QuizActiveView(
    uiState: QuizUiState,
    onSelectOption: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val currentQuestion = uiState.questions.getOrNull(uiState.currentQuestionIndex)
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    val context = LocalContext.current

    // Trigger audio & haptic feedback when an answer is submitted
    LaunchedEffect(uiState.isAnswerSubmitted) {
        if (uiState.isAnswerSubmitted) {
            if (uiState.isCorrect == true) {
                SoundEffects.playCorrectSound(context)
                VibrationUtils.vibrateCorrect(context)
            } else {
                SoundEffects.playWrongSound(context)
                VibrationUtils.vibrateWrong(context)
            }
        }
    }

    // Floating XP Reward Banner Animation
    val floatingXpY = remember { Animatable(0f) }
    val floatingXpAlpha = remember { Animatable(0f) }
    LaunchedEffect(uiState.isAnswerSubmitted) {
        if (uiState.isAnswerSubmitted && uiState.isCorrect == true) {
            floatingXpY.snapTo(0f)
            floatingXpAlpha.snapTo(1f)
            floatingXpY.animateTo(
                targetValue = -35f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
            floatingXpAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .verticalScroll(scrollState)
    ) {
        // 1. Premium Header Bar
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            backgroundColor = DarkCardSurface.copy(alpha = 0.9f),
            borderColor = GlassBorder,
            elevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(DarkBackground.copy(alpha = 0.6f))
                        .bounceClick(scaleDown = 0.92f)
                        .testTag("quiz_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = PrimaryPurpleLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.categoryTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("quiz_category_title")
                    )
                }

                // XP / Score Pill with scale bounce when score increases
                val scorePulse = remember { Animatable(1f) }
                LaunchedEffect(uiState.score) {
                    if (uiState.score > 0) {
                        scorePulse.animateTo(1.15f, spring(stiffness = Spring.StiffnessHigh))
                        scorePulse.animateTo(1f, spring(dampingRatio = 0.6f))
                    }
                }

                Box(
                    modifier = Modifier
                        .scale(scorePulse.value)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(PrimaryPurple, PrimaryPurpleLight)
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("quiz_score_pill"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "XP",
                            tint = TextWhite,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${uiState.score} XP",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            ),
                            color = TextWhite
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Question Progress & Timer Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "QUESTION ${uiState.currentQuestionIndex + 1} OF ${uiState.questions.size}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        fontSize = 12.sp
                    ),
                    color = TextSecondary,
                    modifier = Modifier.testTag("question_progress_text")
                )
            }

            // Circular Countdown Timer & Badge
            val timeRemaining = uiState.timeRemaining
            val targetTimerColor = when {
                timeRemaining <= 3 -> Color(0xFFE74C3C)
                timeRemaining <= 7 -> Color(0xFFF39C12)
                else -> Color(0xFF2ECC71)
            }
            val timerColor by animateColorAsState(
                targetValue = targetTimerColor,
                animationSpec = tween(durationMillis = 300),
                label = "TimerColorAnimation"
            )

            // Pulsing animation for remaining time <= 5s
            val isUrgent = timeRemaining <= 5
            val infiniteTransition = rememberInfiniteTransition(label = "timer_pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (isUrgent) 1.12f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_scale"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .scale(pulseScale)
                    .clip(RoundedCornerShape(16.dp))
                    .background(timerColor.copy(alpha = 0.15f))
                    .border(1.dp, timerColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("timer_badge")
            ) {
                // Circular Timer Arc Indicator with smooth sweep angle animation
                val targetProgress = (timeRemaining / 15f).coerceIn(0f, 1f)
                val animatedTimerProgress by animateFloatAsState(
                    targetValue = targetProgress,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "TimerArcSweep"
                )

                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = timerColor.copy(alpha = 0.25f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawArc(
                            color = timerColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedTimerProgress,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Timer",
                        tint = timerColor,
                        modifier = Modifier.size(11.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${timeRemaining}s",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = timerColor
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Question Progress Bar (Smooth 300ms Material Easing)
        val animatedProgress by animateFloatAsState(
            targetValue = (uiState.currentQuestionIndex + 1) / uiState.questions.size.toFloat(),
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "QuestionProgressAnimated"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DarkCardSurface)
                .testTag("question_progress_bar")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(PrimaryPurple, PrimaryPurpleLight)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Timer Progress Line
        val timerLineProgress by animateFloatAsState(
            targetValue = (uiState.timeRemaining / 15f).coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "TimerLineProgress"
        )
        val timerLineColor = when {
            uiState.timeRemaining <= 3 -> Color(0xFFE74C3C)
            uiState.timeRemaining <= 7 -> Color(0xFFF39C12)
            else -> AccentCoins
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(DarkCardBorder.copy(alpha = 0.4f))
                .testTag("timer_progress_bar")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(timerLineProgress)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(timerLineColor)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Question Content with Animated Smooth Transition
        AnimatedContent(
            targetState = uiState.currentQuestionIndex,
            transitionSpec = {
                (slideInHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { width -> width / 2 } + fadeIn(tween(300)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { width -> -width / 2 } + fadeOut(tween(300)))
            },
            label = "QuestionTransition"
        ) { targetIndex ->
            val question = uiState.questions.getOrNull(targetIndex) ?: currentQuestion
            if (question != null) {
                Column {
                    // Question Card with Soft Ambient Glow
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = PrimaryPurple, spotColor = PrimaryPurple)
                            .testTag("question_card"),
                        shape = RoundedCornerShape(24.dp),
                        backgroundColor = DarkCardSurface,
                        borderColor = PrimaryPurple.copy(alpha = 0.4f),
                        elevation = 8.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            PrimaryPurple.copy(alpha = 0.2f),
                                            DarkCardSurface,
                                            DarkCardSurface
                                        )
                                    )
                                )
                                .padding(22.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PrimaryPurple.copy(alpha = 0.3f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = uiState.categoryTitle.uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 10.sp
                                            ),
                                            color = PrimaryPurpleLight
                                        )
                                    }

                                    // Floating XP Animation Badge above card
                                    if (floatingXpAlpha.value > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .offset { IntOffset(0, floatingXpY.value.dp.roundToPx()) }
                                                .alpha(floatingXpAlpha.value)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF2ECC71))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "+10 XP",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 11.sp
                                                ),
                                                color = TextWhite
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = question.questionText,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        lineHeight = 28.sp
                                    ),
                                    color = TextWhite,
                                    modifier = Modifier.testTag("question_text")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Option Buttons
                    val optionLabels = listOf("A", "B", "C", "D")
                    question.options.forEachIndexed { index, optionText ->
                        val isSelected = (uiState.selectedOptionIndex == index)
                        val isCorrectAnswer = question.isAnswerCorrect(index)

                        QuizOptionItem(
                            index = index,
                            label = optionLabels.getOrElse(index) { "${index + 1}" },
                            text = optionText,
                            isSelected = isSelected,
                            isCorrectAnswer = isCorrectAnswer,
                            isAnswerSubmitted = uiState.isAnswerSubmitted,
                            onClick = { onSelectOption(index) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Success / Error Banner with Slide & Fade Animation
        var lastFeedbackText by remember { mutableStateOf("") }
        var lastIsCorrect by remember { mutableStateOf(false) }

        if (uiState.isAnswerSubmitted) {
            val isCorrectState = uiState.isCorrect == true
            val isTimeUpState = uiState.selectedOptionIndex == null && uiState.timeRemaining <= 0
            lastFeedbackText = if (isCorrectState) "🎉 Correct! +10 XP" else if (isTimeUpState) "⏰ Time's up!" else "❌ Incorrect Answer"
            lastIsCorrect = isCorrectState
        }

        AnimatedVisibility(
            visible = uiState.isAnswerSubmitted,
            enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(tween(250)) + scaleIn(tween(250)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            val feedbackBg = if (lastIsCorrect) Color(0xFF2ECC71).copy(alpha = 0.22f) else Color(0xFFE74C3C).copy(alpha = 0.22f)
            val feedbackBorder = if (lastIsCorrect) Color(0xFF2ECC71) else Color(0xFFE74C3C)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(feedbackBg)
                    .border(1.5.dp, feedbackBorder, RoundedCornerShape(18.dp))
                    .padding(vertical = 14.dp, horizontal = 18.dp)
                    .testTag("quiz_feedback_banner"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (lastIsCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = feedbackBorder,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = lastFeedbackText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        ),
                        color = feedbackBorder
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun QuizOptionItem(
    index: Int,
    label: String,
    text: String,
    isSelected: Boolean,
    isCorrectAnswer: Boolean,
    isAnswerSubmitted: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val targetContainerColor: Color
    val targetBorderColor: Color
    val targetTextColor: Color
    val targetBadgeColor: Color

    if (isAnswerSubmitted) {
        if (isSelected && isCorrectAnswer) {
            targetContainerColor = Color(0xFF2ECC71).copy(alpha = 0.28f)
            targetBorderColor = Color(0xFF2ECC71)
            targetTextColor = Color(0xFF2ECC71)
            targetBadgeColor = Color(0xFF2ECC71)
        } else if (isSelected && !isCorrectAnswer) {
            targetContainerColor = Color(0xFFE74C3C).copy(alpha = 0.28f)
            targetBorderColor = Color(0xFFE74C3C)
            targetTextColor = Color(0xFFE74C3C)
            targetBadgeColor = Color(0xFFE74C3C)
        } else if (!isSelected && isCorrectAnswer) {
            targetContainerColor = Color(0xFF2ECC71).copy(alpha = 0.18f)
            targetBorderColor = Color(0xFF2ECC71).copy(alpha = 0.6f)
            targetTextColor = Color(0xFF2ECC71)
            targetBadgeColor = Color(0xFF2ECC71)
        } else {
            targetContainerColor = DarkCardSurface.copy(alpha = 0.4f)
            targetBorderColor = DarkCardBorder.copy(alpha = 0.25f)
            targetTextColor = TextMuted
            targetBadgeColor = TextMuted
        }
    } else {
        if (isSelected) {
            targetContainerColor = PrimaryPurple.copy(alpha = 0.35f)
            targetBorderColor = PrimaryPurpleLight
            targetTextColor = TextWhite
            targetBadgeColor = PrimaryPurpleLight
        } else {
            targetContainerColor = DarkCardSurface
            targetBorderColor = DarkCardBorder
            targetTextColor = TextWhite
            targetBadgeColor = TextSecondary
        }
    }

    val containerColor by animateColorAsState(targetValue = targetContainerColor, animationSpec = tween(200), label = "OptionBg")
    val borderColor by animateColorAsState(targetValue = targetBorderColor, animationSpec = tween(200), label = "OptionBorder")
    val textColor by animateColorAsState(targetValue = targetTextColor, animationSpec = tween(200), label = "OptionText")
    val badgeColor by animateColorAsState(targetValue = targetBadgeColor, animationSpec = tween(200), label = "OptionBadge")

    // Shake animation for incorrect answer press
    val shakeOffset = remember { Animatable(0f) }
    // Scale pulse animation for correct answer
    val correctPulseScale = remember { Animatable(1f) }

    LaunchedEffect(isAnswerSubmitted) {
        if (isAnswerSubmitted) {
            if (isSelected && !isCorrectAnswer) {
                // Gentle horizontal shake
                val shakeSequence = listOf(-10f, 10f, -6f, 6f, -3f, 3f, 0f)
                for (offset in shakeSequence) {
                    shakeOffset.animateTo(offset, tween(durationMillis = 35))
                }
            } else if (isCorrectAnswer && isSelected) {
                // Subtle scale pulse
                correctPulseScale.animateTo(1.04f, spring(stiffness = Spring.StiffnessHigh))
                correctPulseScale.animateTo(1f, spring(dampingRatio = 0.5f))
            }
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .graphicsLayer {
                translationX = shakeOffset.value
                scaleX = correctPulseScale.value
                scaleY = correctPulseScale.value
            }
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
            .bounceClick(scaleDown = 0.97f) {
                if (!isAnswerSubmitted) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    SoundEffects.playCoinSound()
                    onClick()
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = PrimaryPurpleLight),
                enabled = !isAnswerSubmitted,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 15.dp)
            .testTag("option_button_$index")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Option Label Circle Badge
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.2f))
                        .border(1.dp, badgeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        ),
                        color = badgeColor
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    ),
                    color = textColor
                )
            }

            // Status Indicator Icon with Scale Entrance
            if (isAnswerSubmitted) {
                if (isCorrectAnswer) {
                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow)) + fadeIn()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Correct",
                            tint = Color(0xFF2ECC71),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else if (isSelected && !isCorrectAnswer) {
                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow)) + fadeIn()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Wrong",
                            tint = Color(0xFFE74C3C),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuizCompleteView(
    uiState: QuizUiState,
    onRestart: () -> Unit,
    onBackHome: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        SoundEffects.playCompleteSound(context)
        VibrationUtils.vibrateComplete(context)
    }

    // 1. Calculate accuracy & performance tier
    val totalQuestions = uiState.questions.size.coerceAtLeast(1)
    val accuracyPercentage = ((uiState.correctCount.toFloat() / totalQuestions) * 100).roundToInt()

    val (titleText, subtitleText, performanceTier, heroGradient) = when {
        accuracyPercentage == 100 -> Quadruple(
            "PERFECT SCORE!",
            "Flawless execution! You answered every question correctly in ${uiState.categoryTitle}! 🎯",
            "S+ TIER",
            listOf(Color(0xFFFFD700), Color(0xFFFFA500))
        )
        accuracyPercentage >= 80 -> Quadruple(
            "OUTSTANDING!",
            "Incredible brain power! Outstanding masterclass in ${uiState.categoryTitle}! 🌟",
            "S TIER",
            listOf(AccentCoins, AccentCoinsGradientEnd)
        )
        accuracyPercentage >= 60 -> Quadruple(
            "EXCELLENT JOB!",
            "Great effort! You showed strong knowledge in ${uiState.categoryTitle}! 🧠",
            "A TIER",
            listOf(PrimaryPurple, PrimaryPurpleLight)
        )
        accuracyPercentage >= 40 -> Quadruple(
            "NICE EFFORT!",
            "Good attempt! Keep testing your memory to unlock higher ranks! 💪",
            "B TIER",
            listOf(AccentXP, AccentXPGradientEnd)
        )
        else -> Quadruple(
            "QUIZ FINISHED!",
            "Practice makes perfect! Review your answers and try again to boost your score! 🚀",
            "C TIER",
            listOf(Color(0xFF3498DB), Color(0xFF2980B9))
        )
    }

    // 2. Count-up animations for scores, XP, coins & accuracy
    val animatedScore by animateIntAsState(
        targetValue = uiState.score,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "ScoreCountUp"
    )
    val animatedXpEarned by animateIntAsState(
        targetValue = uiState.xpEarned,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "XpCountUp"
    )
    val animatedCoinsEarned by animateIntAsState(
        targetValue = uiState.coinsEarned,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "CoinsCountUp"
    )
    val animatedAccuracy by animateIntAsState(
        targetValue = accuracyPercentage,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "AccuracyCountUp"
    )

    // 3. XP Level & Rank Progress Calculations
    val totalXp = uiState.totalXp
    val currentRank = RankUtils.getRankForXp(totalXp)
    val (nextRankName, prevRankThreshold, nextRankThreshold) = when {
        totalXp >= 1000 -> Triple("MAX RANK", 1000, 1000)
        totalXp >= 500 -> Triple("Genius", 500, 1000)
        totalXp >= 100 -> Triple("Master", 100, 500)
        else -> Triple("Learner", 0, 100)
    }
    val rankProgressRatio = if (nextRankThreshold == prevRankThreshold) 1f else {
        ((totalXp - prevRankThreshold).toFloat() / (nextRankThreshold - prevRankThreshold)).coerceIn(0f, 1f)
    }
    val animatedRankProgress by animateFloatAsState(
        targetValue = rankProgressRatio,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "RankProgressAnimation"
    )

    // 4. Hero Badge Spring & Pulse Animations
    val trophyScale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        trophyScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "hero_pulse_transition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // High-performance particle canvas background
        ConfettiParticlesCanvas()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // ------------------------------------------------
            // 1. HERO CELEBRATION (Trophy / Medal & Tier Badge)
            // ------------------------------------------------
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                // Ambient Outer Glow Pulse Ring
                Box(
                    modifier = Modifier
                        .scale(trophyScale.value * pulseScale)
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(heroGradient.first().copy(alpha = pulseAlpha * 0.4f))
                )

                // Main Trophy Badge
                Box(
                    modifier = Modifier
                        .scale(trophyScale.value)
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(brush = Brush.linearGradient(heroGradient))
                        .border(3.dp, GlassBorder, CircleShape)
                        .shadow(16.dp, CircleShape, ambientColor = heroGradient.first(), spotColor = heroGradient.last())
                        .testTag("complete_trophy_badge"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (accuracyPercentage >= 80) Icons.Default.EmojiEvents else Icons.Default.Star,
                        contentDescription = "Trophy",
                        tint = TextWhite,
                        modifier = Modifier.size(56.dp)
                    )
                }

                // Tier Pill Badge Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 12.dp)
                        .scale(trophyScale.value)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCardSurface)
                        .border(1.5.dp, heroGradient.first(), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = performanceTier,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        ),
                        color = heroGradient.first()
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ------------------------------------------------
            // 2. RESULT TITLE & SUBTITLE
            // ------------------------------------------------
            Text(
                text = titleText,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    letterSpacing = 0.5.sp
                ),
                color = TextWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("quiz_complete_title")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                ),
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .testTag("quiz_complete_subtitle")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ------------------------------------------------
            // 3. MAIN GLASS SCORE & STATS CARD (2x2 Grid)
            // ------------------------------------------------
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("final_score_card"),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = DarkCardSurface,
                borderColor = PrimaryPurple.copy(alpha = 0.4f),
                elevation = 10.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Banner inside Score Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PERFORMANCE OVERVIEW",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                fontSize = 11.sp
                            ),
                            color = TextSecondary
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (accuracyPercentage >= 70) Color(0xFF2ECC71).copy(alpha = 0.2f)
                                    else Color(0xFFF39C12).copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "$animatedAccuracy% ACCURACY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = if (accuracyPercentage >= 70) Color(0xFF2ECC71) else Color(0xFFF39C12)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2x2 Stat Cards Grid
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                title = "Final Score",
                                value = "$animatedScore XP",
                                icon = Icons.Default.EmojiEvents,
                                accentColors = listOf(PrimaryPurple, PrimaryPurpleLight),
                                modifier = Modifier.weight(1f),
                                testTag = "final_score_stat_card"
                            )

                            StatCard(
                                title = "Accuracy",
                                value = "${uiState.correctCount}/${uiState.questions.size}",
                                icon = Icons.Default.CheckCircle,
                                accentColors = listOf(Color(0xFF2ECC71), Color(0xFF27AE60)),
                                modifier = Modifier.weight(1f),
                                testTag = "accuracy_stat_card"
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                title = "XP Earned",
                                value = "+$animatedXpEarned XP",
                                icon = Icons.Default.AutoAwesome,
                                accentColors = listOf(AccentXP, AccentXPGradientEnd),
                                modifier = Modifier.weight(1f),
                                testTag = "xp_earned_stat_card"
                            )

                            StatCard(
                                title = "Coins Earned",
                                value = "+$animatedCoinsEarned",
                                icon = Icons.Default.MonetizationOn,
                                accentColors = listOf(AccentCoins, AccentCoinsGradientEnd),
                                modifier = Modifier.weight(1f),
                                testTag = "coins_earned_stat_card"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ------------------------------------------------
            // 4. XP & LEVEL PROGRESS CARD
            // ------------------------------------------------
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("xp_level_card"),
                shape = RoundedCornerShape(22.dp),
                backgroundColor = DarkCardSurface,
                borderColor = GlassBorder,
                elevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = AccentXP,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RANK & LEVEL PROGRESS",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.8.sp
                                ),
                                color = TextWhite
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentXP.copy(alpha = 0.2f))
                                .border(1.dp, AccentXP.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = currentRank.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                ),
                                color = AccentXP
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total XP: $totalXp",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextWhite
                        )
                        Text(
                            text = if (nextRankThreshold == prevRankThreshold) "Max Level Reached!" else "Next Rank: $nextRankName",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Smooth XP Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(DarkBackground)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedRankProgress)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(AccentXP, AccentXPGradientEnd)
                                    )
                                )
                        )
                    }

                    if (nextRankThreshold > prevRankThreshold) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${nextRankThreshold - totalXp} XP needed to reach $nextRankName",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ------------------------------------------------
            // 5. EXTENDED METRICS (Best Streak & Performance Rating)
            // ------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = DarkCardSurface,
                    borderColor = GlassBorder,
                    elevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = "Streak",
                            tint = Color(0xFFE67E22),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "BEST STREAK",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = TextSecondary
                            )
                            Text(
                                text = "${uiState.maxStreak} in a row",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                ),
                                color = TextWhite
                            )
                        }
                    }
                }

                GlassCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = DarkCardSurface,
                    borderColor = GlassBorder,
                    elevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coins",
                            tint = AccentCoins,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "REWARD COINS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = TextSecondary
                            )
                            Text(
                                text = "+$animatedCoinsEarned Coins",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                ),
                                color = AccentCoins
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ------------------------------------------------
            // 6. ACHIEVEMENTS CARD / PLACEHOLDER
            // ------------------------------------------------
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("achievements_result_card"),
                shape = RoundedCornerShape(22.dp),
                backgroundColor = DarkCardSurface,
                borderColor = GlassBorder,
                elevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AccentCoins,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ACHIEVEMENTS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = TextWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (uiState.newlyUnlockedAchievements.isNotEmpty()) {
                        uiState.newlyUnlockedAchievements.forEach { achievement ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AccentCoins.copy(alpha = 0.15f))
                                    .border(1.dp, AccentCoins.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = achievement.iconName,
                                    fontSize = 24.sp,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column {
                                    Text(
                                        text = achievement.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = TextWhite
                                    )
                                    Text(
                                        text = achievement.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No new achievements unlocked in this run. Keep playing to earn badges!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ------------------------------------------------
            // 7. SHARE SCORE CARD
            // ------------------------------------------------
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(scaleDown = 0.98f) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "🧠 I scored $animatedScore XP with $accuracyPercentage% accuracy in ${uiState.categoryTitle} on Brain Quiz AI! Can you beat my score?"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share your Brain Quiz Result"))
                    }
                    .testTag("share_score_card"),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = PrimaryPurple.copy(alpha = 0.2f),
                borderColor = PrimaryPurpleLight.copy(alpha = 0.6f),
                elevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = PrimaryPurpleLight,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Share Your Achievement",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = TextWhite,
                                maxLines = 1
                            )
                            Text(
                                text = "Challenge friends with your score!",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryPurpleLight)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "SHARE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            ),
                            color = TextWhite,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ------------------------------------------------
            // 8. ACTION BUTTONS (Play Again & Home)
            // ------------------------------------------------
            GradientButton(
                text = "Play Again",
                icon = Icons.Default.Refresh,
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(scaleDown = 0.97f),
                testTag = "play_again_button"
            )

            Spacer(modifier = Modifier.height(12.dp))

            GradientButton(
                text = "Back to Home",
                onClick = onBackHome,
                isOutlined = true,
                outlineColor = GlassBorder,
                containerColor = DarkCardSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(scaleDown = 0.97f),
                testTag = "back_to_home_button"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
fun ConfettiParticlesCanvas() {
    val particles = remember {
        List(45) {
            ConfettiParticle(
                xPct = Random.nextFloat(),
                yPct = Random.nextFloat() * 0.7f,
                radius = Random.nextFloat() * 4.5f + 2.5f,
                color = listOf(
                    Color(0xFFF1C40F),
                    Color(0xFF2ECC71),
                    Color(0xFF9B59B6),
                    Color(0xFFE74C3C),
                    Color(0xFF3498DB),
                    Color(0xFFE67E22)
                ).random()
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "confetti_float_transition")
    val floatOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "confettiOffset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEachIndexed { idx, particle ->
            val offsetY = (particle.yPct * size.height) + (if (idx % 2 == 0) floatOffset else -floatOffset)
            val offsetX = particle.xPct * size.width
            drawCircle(
                color = particle.color.copy(alpha = 0.8f),
                radius = particle.radius.dp.toPx(),
                center = Offset(offsetX, offsetY)
            )
        }
    }
}

private data class ConfettiParticle(
    val xPct: Float,
    val yPct: Float,
    val radius: Float,
    val color: Color
)
