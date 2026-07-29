package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.ComingSoonDialog
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
import com.example.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Dialog for Reset Password Notice
    if (uiState.showResetPasswordNotice) {
        ComingSoonDialog(
            featureTitle = "Password Reset Sent",
            featureDescription = "A password reset link has been dispatched to ${uiState.emailInput.ifBlank { "your email address" }}. Please check your inbox.",
            onDismiss = { viewModel.dismissResetPasswordNotice() },
            testTag = "reset_password_dialog"
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("login_screen")
            .background(DarkBackground)
    ) {
        // Decorative glowing ambient lights
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PrimaryPurple.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top Logo Header
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryPurple, PrimaryPurpleLight)
                        )
                    )
                    .padding(3.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (uiState.isSignUpMode) "Create Account" else "Welcome Back",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = TextWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("welcome_back_title")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (uiState.isSignUpMode) "Sign up to start your cognitive training" else "Sign in to continue your journey",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("welcome_subtitle")
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Main Auth Form Glass Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Mode Toggle: Login / Create Account
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkBackground.copy(alpha = 0.6f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (!uiState.isSignUpMode) PrimaryPurple else Color.Transparent)
                                .clickable { if (uiState.isSignUpMode) viewModel.toggleAuthMode() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign In",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (!uiState.isSignUpMode) TextWhite else TextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (uiState.isSignUpMode) PrimaryPurple else Color.Transparent)
                                .clickable { if (!uiState.isSignUpMode) viewModel.toggleAuthMode() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Create Account",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (uiState.isSignUpMode) TextWhite else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Name Field (shown in Sign Up mode)
                    if (uiState.isSignUpMode) {
                        OutlinedTextField(
                            value = uiState.nameInput,
                            onValueChange = { viewModel.onNameChanged(it) },
                            label = { Text("Full Name") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Name Icon",
                                    tint = PrimaryPurpleLight
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("name_input_field"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryPurpleLight,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedContainerColor = DarkBackground.copy(alpha = 0.5f),
                                unfocusedContainerColor = DarkBackground.copy(alpha = 0.3f),
                                focusedLabelColor = PrimaryPurpleLight,
                                unfocusedLabelColor = TextMuted,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Email Field
                    OutlinedTextField(
                        value = uiState.emailInput,
                        onValueChange = { viewModel.onEmailChanged(it) },
                        label = { Text("Email Address") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email Icon",
                                tint = PrimaryPurpleLight
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input_field"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurpleLight,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedContainerColor = DarkBackground.copy(alpha = 0.5f),
                            unfocusedContainerColor = DarkBackground.copy(alpha = 0.3f),
                            focusedLabelColor = PrimaryPurpleLight,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Field
                    OutlinedTextField(
                        value = uiState.passwordInput,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password Icon",
                                tint = PrimaryPurpleLight
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                                Icon(
                                    imageVector = if (uiState.isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input_field"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurpleLight,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedContainerColor = DarkBackground.copy(alpha = 0.5f),
                            unfocusedContainerColor = DarkBackground.copy(alpha = 0.3f),
                            focusedLabelColor = PrimaryPurpleLight,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    // Error Message
                    if (uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = uiState.errorMessage!!,
                            color = Color(0xFFFF5252),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        )
                    }

                    // Forgot Password link
                    if (!uiState.isSignUpMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "Forgot Password?",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = PrimaryPurpleLight,
                                modifier = Modifier
                                    .testTag("forgot_password_button")
                                    .clickable { viewModel.onForgotPasswordClicked() }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Auth Action Button
                    GradientButton(
                        text = if (uiState.isSignUpMode) "Create Account" else "Login with Email",
                        isLoading = uiState.isLoading,
                        onClick = {
                            viewModel.submitEmailAuth(onSuccess = onNavigateToHome)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = if (uiState.isSignUpMode) "create_account_submit_btn" else "login_email_submit_btn"
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Divider: OR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = DarkCardBorder
                        )
                        Text(
                            text = "OR",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = DarkCardBorder
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Continue with Google Button
                    GradientButton(
                        text = "Continue with Google",
                        icon = Icons.Default.GTranslate,
                        isLoading = false,
                        onClick = {
                            viewModel.signInWithGoogle(
                                context = context,
                                webClientId = "1047242078803-dummy.apps.googleusercontent.com",
                                onSuccess = onNavigateToHome
                            )
                        },
                        isOutlined = true,
                        outlineColor = GlassBorder,
                        containerColor = DarkCardSurface,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "continue_with_google_button"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Continue as Guest Button
                    GradientButton(
                        text = "Continue as Guest",
                        icon = Icons.Default.PersonOutline,
                        isLoading = false,
                        onClick = {
                            viewModel.signInAsGuest(onSuccess = onNavigateToHome)
                        },
                        gradientColors = listOf(Color(0xFF282E4D), Color(0xFF1D2445)),
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "continue_as_guest_button"
                    )
                }
            }
        }
    }
}
