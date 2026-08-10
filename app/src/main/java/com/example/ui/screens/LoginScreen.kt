package com.example.ui.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("GOOGLE_AUTH_FLOW", "googleSignInLauncher activity result received: resultCode=${result.resultCode}, dataNotNull=${result.data != null}")
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                val email = account?.email ?: ""
                val displayName = account?.displayName ?: account?.givenName ?: "Google User"

                Log.d("GOOGLE_AUTH_FLOW", "STEP 6 (Fallback Launcher) GoogleSignInAccount returned: email=$email, hasIdToken=${!idToken.isNullOrEmpty()}")

                if (!idToken.isNullOrEmpty()) {
                    Log.d("GOOGLE_AUTH_FLOW", "STEP 8 ID token extracted from GoogleSignInAccount (length=${idToken.length})")
                    viewModel.signInWithGoogleIdToken(
                        idToken = idToken,
                        userEmail = email,
                        userName = displayName,
                        onSuccess = {
                            Log.d("GOOGLE_AUTH_FLOW", "STEP 14 navigation success (via fallback launcher callback)")
                            VibrationUtils.vibrateCorrect(context)
                            SoundEffects.playCorrectSound(context)
                            onNavigateToHome()
                        }
                    )
                } else {
                    Log.e("GOOGLE_AUTH_FLOW", "GoogleSignInAccount idToken is empty/null for email=$email")
                    viewModel.setAuthError("Google Sign-In failed: Could not retrieve ID token from Google Play Services.")
                }
            } catch (e: ApiException) {
                if (e.statusCode == 12501) {
                    Log.d("GOOGLE_AUTH_FLOW", "User cancelled Google Account selection (statusCode=12501)")
                    viewModel.setAuthError(null)
                } else {
                    val statusExplanation = when (e.statusCode) {
                        10 -> "Developer error / SHA-1 or OAuth Client ID mismatch (Code 10)"
                        12500 -> "Sign-in failed (Code 12500)"
                        7 -> "Network error (Code 7)"
                        else -> "Status code ${e.statusCode}"
                    }
                    Log.e("GOOGLE_AUTH_FLOW", "GoogleSignInClient failed statusCode=${e.statusCode} ($statusExplanation): ${e.message}", e)
                    viewModel.setAuthError("Google Sign-In failed: $statusExplanation. ${e.message ?: ""}")
                }
            }
        } else {
            Log.w("GOOGLE_AUTH_FLOW", "Google Sign-In activity result not RESULT_OK: resultCode=${result.resultCode}. Remaining on login screen.")
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

    // Dialog for Reset Password Notice
    if (uiState.showResetPasswordNotice) {
        ComingSoonDialog(
            featureTitle = "Password Reset Link Dispatched",
            featureDescription = if (uiState.emailInput.isNotBlank()) 
                "We have dispatched a password reset email to ${uiState.emailInput}. Please check your inbox and spam folder."
            else 
                "Please enter your email address in the field above to receive a password reset link.",
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

                    // Remember Me & Forgot Password Row (Sign In mode)
                    if (!uiState.isSignUpMode) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
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

                            Text(
                                text = "Forgot Password?",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = PrimaryPurpleLight,
                                modifier = Modifier
                                    .testTag("forgot_password_button")
                                    .clickable {
                                        SoundEffects.playClickSound(context)
                                        VibrationUtils.vibrateClick(context)
                                        viewModel.onForgotPasswordClicked()
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Auth Action Button
                    GradientButton(
                        text = if (uiState.isSignUpMode) "Create Free Account" else "Sign In with Email",
                        isLoading = uiState.isLoading,
                        onClick = {
                            SoundEffects.playClickSound(context)
                            VibrationUtils.vibrateClick(context)
                            viewModel.submitEmailAuth {
                                VibrationUtils.vibrateCorrect(context)
                                SoundEffects.playCorrectSound(context)
                                onNavigateToHome()
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
                        isLoading = uiState.isLoading,
                        onClick = {
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
                        isLoading = false,
                        onClick = {
                            SoundEffects.playClickSound(context)
                            VibrationUtils.vibrateClick(context)
                            viewModel.signInAsGuest {
                                VibrationUtils.vibrateCorrect(context)
                                SoundEffects.playCorrectSound(context)
                                onNavigateToHome()
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

