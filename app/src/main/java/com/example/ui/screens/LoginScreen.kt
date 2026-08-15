package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.utils.GoogleAuthDiagnostics
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.example.utils.SoundEffects
import com.example.utils.VibrationUtils
import com.example.utils.bounceClick
import com.example.viewmodel.AuthViewModel

@Composable
fun Modifier.shakeOnError(trigger: Int): Modifier {
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            val keyframes = listOf(-18f, 18f, -14f, 14f, -10f, 10f, -5f, 5f, 0f)
            for (valX in keyframes) {
                offsetX.animateTo(
                    targetValue = valX,
                    animationSpec = tween(durationMillis = 35, easing = LinearEasing)
                )
            }
        }
    }
    return this.graphicsLayer {
        translationX = offsetX.value
    }
}

@Composable
fun AnimatedParticleBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "background_particles")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_rotation"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val particleColors = listOf(
            Color(0xFF9D4EDD).copy(alpha = 0.25f),
            Color(0xFF00E5FF).copy(alpha = 0.20f),
            Color(0xFFFF007A).copy(alpha = 0.18f),
            Color(0xFF7B2CBF).copy(alpha = 0.30f)
        )

        val centers = listOf(
            Offset(width * 0.2f, height * 0.15f),
            Offset(width * 0.85f, height * 0.35f),
            Offset(width * 0.15f, height * 0.75f),
            Offset(width * 0.8f, height * 0.85f)
        )

        centers.forEachIndexed { index, center ->
            val angle = Math.toRadians((floatAnim + index * 90).toDouble())
            val dx = Math.cos(angle).toFloat() * 30.dp.toPx()
            val dy = Math.sin(angle).toFloat() * 30.dp.toPx()
            drawCircle(
                color = particleColors[index % particleColors.size],
                radius = (100 + index * 30).dp.toPx(),
                center = Offset(center.x + dx, center.y + dy)
            )
        }
    }
}

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel,
    onNavigateToForgotPassword: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        GoogleAuthDiagnostics.logEvent(
            context = context,
            stage = "STAGE_5_LEGACY_LAUNCHER_RESULT_RECEIVED",
            flowStep = "googleSignInLauncher activity result received",
            additionalInfo = "resultCode=${result.resultCode}, dataNotNull=${result.data != null}"
        )
        if (result.data != null) {
            GoogleAuthDiagnostics.logEvent(
                context = context,
                stage = "STAGE_5_LEGACY_BEFORE_GET_ACCOUNT_FROM_INTENT",
                flowStep = "Calling GoogleSignIn.getSignedInAccountFromIntent(result.data)"
            )
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                val email = account?.email ?: ""
                val displayName = account?.displayName ?: account?.givenName ?: "Google User"

                GoogleAuthDiagnostics.logEvent(
                    context = context,
                    stage = "STAGE_5_LEGACY_GET_ACCOUNT_SUCCESS",
                    flowStep = "GoogleSignInAccount retrieved successfully from intent",
                    additionalInfo = "hasIdToken=${!idToken.isNullOrEmpty()}"
                )

                if (!idToken.isNullOrEmpty()) {
                    viewModel.signInWithGoogleIdToken(
                        idToken = idToken,
                        userEmail = email,
                        userName = displayName,
                        context = context,
                        onSuccess = {
                            VibrationUtils.vibrateCorrect(context)
                            SoundEffects.playCorrectSound(context)
                            onNavigateToHome()
                        }
                    )
                } else {
                    GoogleAuthDiagnostics.logEvent(
                        context = context,
                        stage = "STAGE_5_LEGACY_GET_ACCOUNT_EMPTY_ID_TOKEN",
                        flowStep = "GoogleSignInAccount idToken is empty/null"
                    )
                    viewModel.setAuthError("Google Sign-In failed: Could not retrieve ID token from Google Play Services.")
                }
            } catch (e: ApiException) {
                if (e.statusCode == 12501) {
                    GoogleAuthDiagnostics.logEvent(
                        context = context,
                        stage = "STAGE_5_LEGACY_GET_ACCOUNT_CANCELLED",
                        flowStep = "User cancelled Google Account selection (statusCode=12501)",
                        statusCode = 12501
                    )
                    viewModel.setAuthError(null)
                } else {
                    GoogleAuthDiagnostics.logEvent(
                        context = context,
                        stage = "STAGE_5_LEGACY_GOOGLE_SIGN_IN_CLIENT_ACCOUNT_PICKER_RESULT",
                        flowStep = "GoogleSignInClient failed in getSignedInAccountFromIntent",
                        exception = e,
                        statusCode = e.statusCode
                    )
                    val statusExplanation = when (e.statusCode) {
                        7 -> "No internet connection. Please check your connection and try again."
                        else -> "Google Sign-In failed. Please check your connection and try again."
                    }
                    viewModel.setAuthError(statusExplanation)
                }
            }
        } else {
            GoogleAuthDiagnostics.logEvent(
                context = context,
                stage = "STAGE_5_LEGACY_LAUNCHER_NULL_DATA",
                flowStep = "Google Sign-In activity result data is null",
                additionalInfo = "resultCode=${result.resultCode}"
            )
            viewModel.setAuthError(null)
        }
    }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onNavigateToHome()
        }
    }

    // Trigger error sound and vibration when errorMessage or shakeTrigger updates
    LaunchedEffect(uiState.shakeTrigger) {
        if (uiState.shakeTrigger > 0) {
            VibrationUtils.vibrateWrong(context)
            SoundEffects.playWrongSound(context)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("login_screen")
            .background(DarkBackground)
    ) {
        // Dynamic Glowing Particle Background Canvas
        AnimatedParticleBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Premium Floating Logo Header
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(16.dp, CircleShape, spotColor = PrimaryPurpleLight, ambientColor = PrimaryPurple)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryPurple, PrimaryPurpleLight, Color(0xFF00E5FF))
                        )
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(DarkBackground)
                        .padding(8.dp),
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
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Brain Quiz AI",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = PrimaryPurpleLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

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

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (uiState.isSignUpMode) "Join thousands of players in daily cognitive challenges" else "Sign in to sync your rank, streaks, and achievements",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("welcome_subtitle")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Auth Form Glass Card with Shake animation on error
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .shakeOnError(uiState.shakeTrigger),
                shape = RoundedCornerShape(26.dp),
                elevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Mode Toggle: Sign In / Create Account
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(DarkBackground.copy(alpha = 0.7f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (!uiState.isSignUpMode) 
                                        Brush.horizontalGradient(listOf(PrimaryPurple, PrimaryPurpleLight))
                                    else 
                                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                )
                                .bounceClick(scaleDown = 0.96f) {
                                    if (uiState.isSignUpMode) {
                                        SoundEffects.playClickSound(context)
                                        VibrationUtils.vibrateClick(context)
                                        viewModel.toggleAuthMode()
                                    }
                                }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign In",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (!uiState.isSignUpMode) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (!uiState.isSignUpMode) TextWhite else TextMuted
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (uiState.isSignUpMode) 
                                        Brush.horizontalGradient(listOf(PrimaryPurple, PrimaryPurpleLight))
                                    else 
                                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                )
                                .bounceClick(scaleDown = 0.96f) {
                                    if (!uiState.isSignUpMode) {
                                        SoundEffects.playClickSound(context)
                                        VibrationUtils.vibrateClick(context)
                                        viewModel.toggleAuthMode()
                                    }
                                }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Create Account",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (uiState.isSignUpMode) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (uiState.isSignUpMode) TextWhite else TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Username Field (Sign Up mode)
                    if (uiState.isSignUpMode) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = uiState.nameInput,
                                onValueChange = { viewModel.onNameChanged(it) },
                                label = { Text("Username") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Username Icon",
                                        tint = PrimaryPurpleLight
                                    )
                                },
                                trailingIcon = {
                                    if (uiState.nameInput.isNotBlank()) {
                                        val isValid = viewModel.isUsernameValid(uiState.nameInput)
                                        Icon(
                                            imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = "Username Status",
                                            tint = if (isValid) Color(0xFF00E676) else Color(0xFFFFB74D)
                                        )
                                    }
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
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                )
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "3-20 characters",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "${uiState.nameInput.trim().length}/20",
                                    fontSize = 11.sp,
                                    color = if (uiState.nameInput.trim().length in 3..20) PrimaryPurpleLight else TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
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
                        trailingIcon = {
                            if (uiState.emailInput.isNotBlank()) {
                                val isValid = viewModel.isEmailValid(uiState.emailInput)
                                Icon(
                                    imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = "Email Status",
                                    tint = if (isValid) Color(0xFF00E676) else Color(0xFFFFB74D)
                                )
                            }
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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (uiState.isSignUpMode) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.submitEmailAuth {
                                    VibrationUtils.vibrateCorrect(context)
                                    SoundEffects.playCorrectSound(context)
                                    onNavigateToHome()
                                }
                            }
                        )
                    )

                    // Password Strength Indicator & Real-Time Checklist (Sign Up Mode)
                    if (uiState.isSignUpMode && uiState.passwordInput.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkBackground.copy(alpha = 0.4f))
                                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            val progress = viewModel.getPasswordStrengthProgress(uiState.passwordInput)
                            val label = viewModel.getPasswordStrengthLabel(uiState.passwordInput)
                            val progressColor = when {
                                progress <= 0.2f -> Color(0xFFFF5252)
                                progress <= 0.4f -> Color(0xFFFF9800)
                                progress <= 0.6f -> Color(0xFFFFEB3B)
                                progress <= 0.8f -> Color(0xFF64DD17)
                                else -> Color(0xFF00E676)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Password Strength",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = progressColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = progressColor,
                                trackColor = DarkCardBorder
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Requirements List
                            val reqMinLength = viewModel.isPasswordMinLength(uiState.passwordInput)
                            val reqUpper = viewModel.hasPasswordUppercase(uiState.passwordInput)
                            val reqLower = viewModel.hasPasswordLowercase(uiState.passwordInput)
                            val reqNumber = viewModel.hasPasswordNumber(uiState.passwordInput)
                            val reqSpecial = viewModel.hasPasswordSpecialChar(uiState.passwordInput)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                RequirementItem(label = "8+ Chars", isMet = reqMinLength)
                                RequirementItem(label = "Uppercase", isMet = reqUpper)
                                RequirementItem(label = "Lowercase", isMet = reqLower)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                RequirementItem(label = "Number (0-9)", isMet = reqNumber)
                                RequirementItem(label = "Special (!@#$)", isMet = reqSpecial)
                            }
                        }
                    }

                    // Confirm Password Field (Sign Up mode)
                    if (uiState.isSignUpMode) {
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = uiState.confirmPasswordInput,
                            onValueChange = { viewModel.onConfirmPasswordChanged(it) },
                            label = { Text("Confirm Password") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Confirm Password Icon",
                                    tint = PrimaryPurpleLight
                                )
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (uiState.confirmPasswordInput.isNotEmpty()) {
                                        val isMatch = viewModel.doPasswordsMatch(uiState.passwordInput, uiState.confirmPasswordInput)
                                        Icon(
                                            imageVector = if (isMatch) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = "Match Status",
                                            tint = if (isMatch) Color(0xFF00E676) else Color(0xFFFF5252),
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    }
                                    IconButton(onClick = { viewModel.toggleConfirmPasswordVisibility() }) {
                                        Icon(
                                            imageVector = if (uiState.isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle confirm password visibility",
                                            tint = TextSecondary
                                        )
                                    }
                                }
                            },
                            visualTransformation = if (uiState.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_password_input_field"),
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
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.submitEmailAuth {
                                        VibrationUtils.vibrateCorrect(context)
                                        SoundEffects.playCorrectSound(context)
                                        onNavigateToHome()
                                    }
                                }
                            )
                        )
                    }

                    // Error Banner Notice
                    AnimatedVisibility(
                        visible = uiState.errorMessage != null,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        if (uiState.errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF3B151C))
                                    .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = uiState.errorMessage!!,
                                    color = Color(0xFFFF8A80),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Success Banner Notice
                    AnimatedVisibility(
                        visible = uiState.successMessage != null,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        if (uiState.successMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF153B22))
                                    .border(1.dp, Color(0xFF00E676).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = uiState.successMessage!!,
                                    color = Color(0xFFA7F3D0),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Remember Me Row (Sign In mode) - Forgot Password option temporarily removed for Version 1
                    if (!uiState.isSignUpMode) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { viewModel.toggleRememberMe() }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = uiState.rememberMe,
                                    onCheckedChange = { viewModel.toggleRememberMe() },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = PrimaryPurpleLight,
                                        uncheckedColor = TextMuted,
                                        checkmarkColor = TextWhite
                                    ),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Remember Me",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Auth Action Button
                    GradientButton(
                        text = if (uiState.isSignUpMode) "Create Free Account" else "Sign In with Email",
                        isLoading = uiState.isEmailSignInLoading,
                        onClick = {
                            if (!uiState.isEmailSignInLoading && !uiState.isGoogleSignInLoading && !uiState.isGuestLoading) {
                                SoundEffects.playClickSound(context)
                                VibrationUtils.vibrateClick(context)
                                viewModel.submitEmailAuth {
                                    VibrationUtils.vibrateCorrect(context)
                                    SoundEffects.playCorrectSound(context)
                                    onNavigateToHome()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = if (uiState.isSignUpMode) "create_account_submit_btn" else "login_email_submit_btn"
                    )

                    Spacer(modifier = Modifier.height(18.dp))

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
                            text = "OR CONTINUE WITH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = DarkCardBorder
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Continue with Google Button
                    GradientButton(
                        text = "Continue with Google",
                        icon = Icons.Default.GTranslate,
                        isLoading = uiState.isGoogleSignInLoading,
                        onClick = {
                            if (!uiState.isEmailSignInLoading && !uiState.isGoogleSignInLoading && !uiState.isGuestLoading) {
                                SoundEffects.playClickSound(context)
                                VibrationUtils.vibrateClick(context)
                                viewModel.signInWithGoogle(
                                    context = context,
                                    onFallbackToGoogleSignInClient = { intent ->
                                        googleSignInLauncher.launch(intent)
                                    },
                                    onSuccess = {
                                        VibrationUtils.vibrateCorrect(context)
                                        SoundEffects.playCorrectSound(context)
                                        onNavigateToHome()
                                    }
                                )
                            }
                        },
                        isOutlined = true,
                        outlineColor = GlassBorder,
                        containerColor = DarkCardSurface,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "continue_with_google_button"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Continue as Guest Button
                    GradientButton(
                        text = "Play as Guest",
                        icon = Icons.Default.PersonOutline,
                        isLoading = uiState.isGuestLoading,
                        onClick = {
                            if (!uiState.isEmailSignInLoading && !uiState.isGoogleSignInLoading && !uiState.isGuestLoading) {
                                SoundEffects.playClickSound(context)
                                VibrationUtils.vibrateClick(context)
                                viewModel.signInAsGuest {
                                    VibrationUtils.vibrateCorrect(context)
                                    SoundEffects.playCorrectSound(context)
                                    onNavigateToHome()
                                }
                            }
                        },
                        gradientColors = listOf(Color(0xFF232A46), Color(0xFF181E36)),
                        isOutlined = true,
                        outlineColor = Color(0xFF333B60),
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "continue_as_guest_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "By continuing, you agree to BrainQuizAI Terms of Service and Privacy Policy.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun RequirementItem(label: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = label,
            tint = if (isMet) Color(0xFF00E676) else TextMuted,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isMet) TextWhite else TextMuted,
            fontWeight = if (isMet) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun ResetPasswordDialog(
    email: String,
    onEmailChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reset_password_dialog"),
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
                Box(
                    modifier = Modifier
                        .size(56.dp)
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
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Reset Password",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = TextWhite,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter your email address and we'll send you a password reset link.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_password_email_input"),
                    label = { Text("Email Address", color = TextMuted) },
                    singleLine = true,
                    enabled = !isLoading,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = PrimaryPurpleLight
                        )
                    },
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

                if (!errorMessage.isNullOrBlank()) {
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
                            text = errorMessage,
                            color = Color(0xFFFF8A80),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Cancel",
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    GradientButton(
                        text = "Send Reset Link",
                        isLoading = isLoading,
                        onClick = onSubmit,
                        gradientColors = listOf(PrimaryPurple, PrimaryPurpleLight),
                        modifier = Modifier.weight(1.5f),
                        testTag = "send_reset_link_button"
                    )
                }
            }
        }
    }
}

@Composable
fun ResetPasswordSuccessDialog(
    email: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reset_password_success_dialog"),
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
                Box(
                    modifier = Modifier
                        .size(60.dp)
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
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Password Reset Link Sent",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = TextWhite,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "We've sent a password reset link to $email. Please check your inbox and spam folder.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    ),
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                GradientButton(
                    text = "Got It!",
                    isLoading = false,
                    onClick = onDismiss,
                    gradientColors = listOf(Color(0xFF00E676), Color(0xFF1B5E20)),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "reset_password_got_it_button"
                )
            }
        }
    }
}

