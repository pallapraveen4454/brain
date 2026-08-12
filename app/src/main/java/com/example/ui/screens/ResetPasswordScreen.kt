package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GradientButton
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.viewmodel.AuthViewModel

@Composable
fun ResetPasswordScreen(
    oobCode: String,
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(oobCode) {
        authViewModel.initResetPasswordFlow(oobCode)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("reset_password_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = DarkCardSurface,
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        // 1. Verifying Code State
                        uiState.isVerifyingResetCode -> {
                            Spacer(modifier = Modifier.height(20.dp))
                            CircularProgressIndicator(
                                color = PrimaryPurpleLight,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Verifying reset link...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextWhite,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // 2. Code Error / Expired State
                        !uiState.resetCodeError.isNullOrBlank() -> {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3E1212)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Invalid Reset Link",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = TextWhite,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = uiState.resetCodeError ?: "This password reset link is invalid or has expired. Please request a new reset link.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                ),
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            GradientButton(
                                text = "Request New Link",
                                isLoading = false,
                                onClick = onNavigateToLogin,
                                gradientColors = listOf(PrimaryPurple, PrimaryPurpleLight),
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "request_new_link_button"
                            )
                        }

                        // 3. Password Changed Success State
                        uiState.resetPasswordSuccess -> {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color(0xFF00E676), Color(0xFF1B5E20))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = TextWhite,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Password Changed Successfully",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = TextWhite,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Your BrainQuizAI password has been updated successfully. You can now sign in with your new password.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                ),
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            GradientButton(
                                text = "Continue to Sign In",
                                isLoading = false,
                                onClick = onNavigateToLogin,
                                gradientColors = listOf(Color(0xFF00E676), Color(0xFF1B5E20)),
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "continue_to_signin_button"
                            )
                        }

                        // 4. Form Input State
                        uiState.isResetCodeValid -> {
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
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = "Reset Password",
                                    tint = TextWhite,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Reset Your Password",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = TextWhite,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Create a new password for your BrainQuizAI account.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                ),
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )

                            if (uiState.resetPasswordEmail.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = uiState.resetPasswordEmail,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = PrimaryPurpleLight,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PrimaryPurple.copy(alpha = 0.2f))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // New Password Input
                            OutlinedTextField(
                                value = uiState.newPasswordInput,
                                onValueChange = { authViewModel.onNewPasswordInputChanged(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("new_password_input"),
                                label = { Text("New Password", color = TextMuted) },
                                singleLine = true,
                                enabled = !uiState.isConfirmingResetPassword,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "New Password",
                                        tint = PrimaryPurpleLight
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { authViewModel.toggleNewPasswordVisibility() }) {
                                        Icon(
                                            imageVector = if (uiState.isNewPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password visibility",
                                            tint = TextMuted
                                        )
                                    }
                                },
                                visualTransformation = if (uiState.isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryPurpleLight,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    cursorColor = PrimaryPurpleLight,
                                    focusedContainerColor = DarkBackground.copy(alpha = 0.5f),
                                    unfocusedContainerColor = DarkBackground.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )

                            if (uiState.newPasswordInput.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val strengthProgress = authViewModel.getPasswordStrengthProgress(uiState.newPasswordInput)
                                val strengthLabel = authViewModel.getPasswordStrengthLabel(uiState.newPasswordInput)
                                val strengthColor = when {
                                    strengthProgress <= 0.2f -> Color(0xFFFF5252)
                                    strengthProgress <= 0.4f -> Color(0xFFFF9800)
                                    strengthProgress <= 0.6f -> Color(0xFFFFEB3B)
                                    else -> Color(0xFF00E676)
                                }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Password Strength", fontSize = 11.sp, color = TextMuted)
                                        Text(text = strengthLabel, fontSize = 11.sp, color = strengthColor, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { strengthProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = strengthColor,
                                        trackColor = DarkBackground
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Confirm New Password Input
                            OutlinedTextField(
                                value = uiState.resetConfirmPasswordInput,
                                onValueChange = { authViewModel.onResetConfirmPasswordInputChanged(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("confirm_password_input"),
                                label = { Text("Confirm New Password", color = TextMuted) },
                                singleLine = true,
                                enabled = !uiState.isConfirmingResetPassword,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Confirm Password",
                                        tint = PrimaryPurpleLight
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { authViewModel.toggleResetConfirmPasswordVisibility() }) {
                                        Icon(
                                            imageVector = if (uiState.isResetConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle confirm password visibility",
                                            tint = TextMuted
                                        )
                                    }
                                },
                                visualTransformation = if (uiState.isResetConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryPurpleLight,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    cursorColor = PrimaryPurpleLight,
                                    focusedContainerColor = DarkBackground.copy(alpha = 0.5f),
                                    unfocusedContainerColor = DarkBackground.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )

                            AnimatedVisibility(
                                visible = !uiState.resetPasswordScreenError.isNullOrBlank(),
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF3E1212))
                                            .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = "Error",
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = uiState.resetPasswordScreenError ?: "",
                                            color = Color(0xFFFF8A80),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            GradientButton(
                                text = "Change Password",
                                isLoading = uiState.isConfirmingResetPassword,
                                onClick = { authViewModel.submitNewPassword() },
                                gradientColors = listOf(PrimaryPurple, PrimaryPurpleLight),
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "change_password_button"
                            )
                        }
                    }
                }
            }
        }
    }
}
