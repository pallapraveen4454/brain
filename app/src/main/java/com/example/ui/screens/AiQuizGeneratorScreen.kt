package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiQuizGeneratorScreen(
    onStartAiQuiz: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var topicText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val suggestionTopics = listOf(
        "Quantum Physics",
        "World War II History",
        "Marvel Cinematic Universe",
        "Space Exploration & NASA",
        "Cybersecurity & Hacking",
        "Human Anatomy & Brain",
        "90s Rock Music",
        "Culinary Arts & Cooking",
        "Artificial Intelligence",
        "Football World Cup"
    )

    fun handleGenerate() {
        val trimmed = topicText.trim()
        if (trimmed.isNotBlank()) {
            keyboardController?.hide()
            onStartAiQuiz("ai_topic_$trimmed")
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_quiz_generator_screen"),
        containerColor = DarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("ai_generator_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextWhite
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "AI Quiz Generator",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = TextWhite
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Hero Card
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(PrimaryPurple, PrimaryPurpleLight)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini AI",
                            tint = TextWhite,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Create Custom AI Quiz",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = TextWhite,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Powered by Gemini 3.5 Flash • 10 Questions • 15s Timer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryPurpleLight,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Enter any subject, hobby, historical era, or scientific topic to generate a unique 10-question quiz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Topic Input Section
            Text(
                text = "Enter Your Topic",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = topicText,
                onValueChange = { topicText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_topic_input_field"),
                placeholder = {
                    Text(
                        text = "e.g., Quantum Physics, Marvel Movies...",
                        color = TextMuted
                    )
                },
                trailingIcon = {
                    if (topicText.isNotEmpty()) {
                        IconButton(onClick = { topicText = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextMuted
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { handleGenerate() }),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkCardSurface,
                    unfocusedContainerColor = DarkCardSurface,
                    focusedBorderColor = PrimaryPurpleLight,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            GradientButton(
                text = "Generate AI Quiz ✨",
                onClick = { if (topicText.trim().isNotBlank()) handleGenerate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generate_ai_quiz_button")
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Topic Suggestions
            Text(
                text = "Popular Suggestions",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                suggestionTopics.forEach { topic ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                            .clickable {
                                topicText = topic
                                onStartAiQuiz("ai_topic_$topic")
                            }
                            .testTag("suggestion_chip_${topic.lowercase().replace(" ", "_")}"),
                        color = DarkCardSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PrimaryPurpleLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = topic,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = TextWhite
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
