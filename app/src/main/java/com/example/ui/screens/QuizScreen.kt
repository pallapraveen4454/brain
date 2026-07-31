package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.QuizUiState
import com.example.viewmodel.QuizViewModel
import com.example.ui.components.AchievementUnlockedDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.StatCard
import com.example.ui.theme.AccentCoins
import com.example.ui.theme.AccentCoinsGradientEnd
import com.example.ui.theme.AccentStreak
import com.example.ui.theme.AccentStreakGradientEnd
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = PrimaryPurpleLight,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = categoryTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
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

                Spacer(modifier = Modifier.height(24.dp))

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(scrollState)
    ) {
        // Debug Mode Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("quiz_debug_card"),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            ),
            border = BorderStroke(1.dp, Color(0xFF38BDF8))
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "🛠️ DEBUG MODE",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Previous Install Date: ${uiState.debugPreviousInstallDate}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                Text(text = "Current Install Date: ${uiState.debugInstallDate}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                Text(text = "Today's Date: ${uiState.debugTodayDate}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                Text(text = "Calculated Day Number: Day ${uiState.debugCalculatedDayNumber}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)
                Text(text = "Calculated Offset Value: ${uiState.debugOffsetValue}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFACC15))
                Text(text = "Start Index: ${uiState.debugStartIndex}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFACC15))
                Text(text = "End Index: ${uiState.debugEndIndex}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFACC15))
                Text(text = "Generated IDs Before DB Query: ${uiState.debugGeneratedIds.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = Color.Yellow)
                Text(text = "Current Question ID: ${currentQuestion?.id ?: "N/A"}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)
                Text(text = "Assigned Question IDs (all 10): ${uiState.debugAssignedQuestionIds.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "✅ ${uiState.debugConfirmationMessage}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF22C55E), fontWeight = FontWeight.SemiBold)
            }
        }

        // Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("quiz_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextWhite
                )
            }

            Text(
                text = uiState.categoryTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = TextWhite,
                modifier = Modifier.testTag("quiz_category_title")
            )

            // Score Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(PrimaryPurple.copy(alpha = 0.25f))
                    .border(1.dp, PrimaryPurpleLight.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("quiz_score_pill")
            ) {
                Text(
                    text = "${uiState.score} XP",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = PrimaryPurpleLight
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress & Timer Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Question ${uiState.currentQuestionIndex + 1}/${uiState.questions.size}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = TextSecondary,
                modifier = Modifier.testTag("question_progress_text")
            )

            // Timer Badge
            val timerColor = if (uiState.timeRemaining <= 5) Color(0xFFFF5252) else PrimaryPurpleLight
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("timer_badge")
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Timer",
                    tint = timerColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${uiState.timeRemaining}s",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = timerColor
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Question Progress Bar
        val questionProgressAnimated by animateFloatAsState(
            targetValue = (uiState.currentQuestionIndex + 1) / uiState.questions.size.toFloat(),
            label = "QuestionProgress"
        )
        LinearProgressIndicator(
            progress = { questionProgressAnimated },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .testTag("question_progress_bar"),
            color = PrimaryPurpleLight,
            trackColor = DarkCardSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Timer Countdown Bar
        val timerProgress = uiState.timeRemaining / 15f
        LinearProgressIndicator(
            progress = { timerProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .testTag("timer_progress_bar"),
            color = if (uiState.timeRemaining <= 5) Color(0xFFFF5252) else AccentCoins,
            trackColor = DarkCardBorder
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Question Card
        if (currentQuestion != null) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("question_card"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = currentQuestion.questionText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 19.sp,
                            lineHeight = 26.sp
                        ),
                        color = TextWhite,
                        modifier = Modifier.testTag("question_text")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Option Buttons
            val optionLabels = listOf("A", "B", "C", "D")
            currentQuestion.options.forEachIndexed { index, optionText ->
                val isSelected = (uiState.selectedOptionIndex == index)
                val isCorrectAnswer = currentQuestion.isAnswerCorrect(index)

                // Color calculation
                val containerColor: Color
                val borderColor: Color
                val textColor: Color

                if (uiState.isAnswerSubmitted) {
                    if (isSelected && isCorrectAnswer) {
                        // Selected Correct Answer = Green
                        containerColor = Color(0xFF2ECC71).copy(alpha = 0.25f)
                        borderColor = Color(0xFF2ECC71)
                        textColor = Color(0xFF2ECC71)
                    } else if (isSelected && !isCorrectAnswer) {
                        // Selected Wrong Answer = Red
                        containerColor = Color(0xFFE74C3C).copy(alpha = 0.25f)
                        borderColor = Color(0xFFE74C3C)
                        textColor = Color(0xFFE74C3C)
                    } else if (!isSelected && isCorrectAnswer) {
                        // Unselected Correct Answer = Soft Green highlight
                        containerColor = Color(0xFF2ECC71).copy(alpha = 0.15f)
                        borderColor = Color(0xFF2ECC71).copy(alpha = 0.5f)
                        textColor = Color(0xFF2ECC71)
                    } else {
                        // Unselected options
                        containerColor = DarkCardSurface.copy(alpha = 0.5f)
                        borderColor = DarkCardBorder.copy(alpha = 0.3f)
                        textColor = TextMuted
                    }
                } else {
                    if (isSelected) {
                        containerColor = PrimaryPurple.copy(alpha = 0.3f)
                        borderColor = PrimaryPurpleLight
                        textColor = TextWhite
                    } else {
                        containerColor = DarkCardSurface
                        borderColor = DarkCardBorder
                        textColor = TextWhite
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(containerColor)
                        .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                        .clickable(enabled = !uiState.isAnswerSubmitted) {
                            onSelectOption(index)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
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
                            // Circle with label (A, B, C, D)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(borderColor.copy(alpha = 0.2f))
                                    .border(1.dp, borderColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = optionLabels.getOrElse(index) { "${index + 1}" },
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = borderColor
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                ),
                                color = textColor
                            )
                        }

                        // Right Status Icon
                        if (uiState.isAnswerSubmitted) {
                            if (isCorrectAnswer) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Correct",
                                    tint = Color(0xFF2ECC71),
                                    modifier = Modifier.size(24.dp)
                                )
                            } else if (isSelected && !isCorrectAnswer) {
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

            Spacer(modifier = Modifier.height(16.dp))

            // Feedback Message Banner
            var lastFeedbackText by remember { mutableStateOf("") }
            var lastIsCorrect by remember { mutableStateOf(false) }

            if (uiState.isAnswerSubmitted) {
                val isCorrectState = uiState.isCorrect == true
                val isTimeUpState = uiState.selectedOptionIndex == null && uiState.timeRemaining <= 0
                lastFeedbackText = if (isCorrectState) "Correct Answer" else if (isTimeUpState) "Time's up!" else "Incorrect"
                lastIsCorrect = isCorrectState
            }

            AnimatedVisibility(
                visible = uiState.isAnswerSubmitted,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut()
            ) {
                val feedbackBg = if (lastIsCorrect) Color(0xFF2ECC71).copy(alpha = 0.2f) else Color(0xFFE74C3C).copy(alpha = 0.2f)
                val feedbackBorder = if (lastIsCorrect) Color(0xFF2ECC71) else Color(0xFFE74C3C)
                val feedbackText = lastFeedbackText

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(feedbackBg)
                        .border(1.dp, feedbackBorder, RoundedCornerShape(12.dp))
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                        .testTag("quiz_feedback_banner"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = feedbackText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = feedbackBorder
                    )
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
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Trophy / Star Graphic
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(AccentCoins, AccentCoinsGradientEnd)
                    )
                )
                .border(2.dp, GlassBorder, CircleShape)
                .testTag("complete_trophy_badge"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Trophy",
                tint = TextWhite,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Quiz Complete!",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp
            ),
            color = TextWhite,
            modifier = Modifier.testTag("quiz_complete_title")
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Great job testing your brain in ${uiState.categoryTitle}!",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("quiz_complete_subtitle")
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Final Stats Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Final Score",
                    value = "${uiState.score} XP",
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "XP Earned",
                    value = "+${uiState.xpEarned} XP",
                    icon = Icons.Default.AutoAwesome,
                    accentColors = listOf(AccentXP, AccentXPGradientEnd),
                    modifier = Modifier.weight(1f),
                    testTag = "xp_earned_stat_card"
                )

                StatCard(
                    title = "Coins Earned",
                    value = "+${uiState.coinsEarned}",
                    icon = Icons.Default.MonetizationOn,
                    accentColors = listOf(AccentCoins, AccentCoinsGradientEnd),
                    modifier = Modifier.weight(1f),
                    testTag = "coins_earned_stat_card"
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Action Buttons
        GradientButton(
            text = "Play Again",
            icon = Icons.Default.Refresh,
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth(),
            testTag = "play_again_button"
        )

        Spacer(modifier = Modifier.height(14.dp))

        GradientButton(
            text = "Back to Home",
            onClick = onBackHome,
            isOutlined = true,
            outlineColor = GlassBorder,
            containerColor = DarkCardSurface,
            modifier = Modifier.fillMaxWidth(),
            testTag = "back_to_home_button"
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}
