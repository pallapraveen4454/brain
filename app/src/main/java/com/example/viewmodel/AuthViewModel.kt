package com.example.viewmodel

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.QuizResultRepository
import com.example.data.UserProfile
import com.example.utils.GoogleAuthDiagnostics
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

data class AuthUiState(
    val emailInput: String = "",
    val passwordInput: String = "",
    val confirmPasswordInput: String = "",
    val nameInput: String = "",
    val selectedAvatarId: String = "brain",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isSignUpMode: Boolean = false,
    val rememberMe: Boolean = true,
    val isLoading: Boolean = false,
    val isEmailSignInLoading: Boolean = false,
    val isGoogleSignInLoading: Boolean = false,
    val isGuestLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val currentUserProfile: UserProfile? = null,
    val showResetPasswordNotice: Boolean = false,
    val showForgotPasswordDialog: Boolean = false,
    val resetPasswordEmailInput: String = "",
    val isResetPasswordLoading: Boolean = false,
    val resetPasswordError: String? = null,
    val showResetPasswordSuccess: Boolean = false,
    val resetPasswordSuccessEmail: String = "",
    val resetPasswordCode: String = "",
    val resetPasswordEmail: String = "",
    val isVerifyingResetCode: Boolean = false,
    val isResetCodeValid: Boolean = false,
    val resetCodeError: String? = null,
    val newPasswordInput: String = "",
    val resetConfirmPasswordInput: String = "",
    val isNewPasswordVisible: Boolean = false,
    val isResetConfirmPasswordVisible: Boolean = false,
    val isConfirmingResetPassword: Boolean = false,
    val resetPasswordSuccess: Boolean = false,
    val resetPasswordScreenError: String? = null,
    val shakeTrigger: Int = 0
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val quizResultRepository: QuizResultRepository = QuizResultRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var autoLoginJob: kotlinx.coroutines.Job? = null

    init {
        checkAutoLogin()
    }

    private fun checkAutoLogin() {
        try {
            autoLoginJob?.cancel()
            if (authRepository.hasSavedUserSession()) {
                _uiState.update { it.copy(isLoading = true) }
                autoLoginJob = viewModelScope.launch {
                    try {
                        val isGuest = authRepository.isGuestSessionActive()
                        val savedProfile = authRepository.getPersistentGuestProfile()
                        val user = if (isGuest) null else authRepository.currentUser
                        val profileToUse = if (user != null) {
                            val fetched = authRepository.fetchUserProfile(user.uid)
                            fetched ?: UserProfile(
                                uid = user.uid,
                                name = user.displayName ?: user.email?.substringBefore("@") ?: "Player",
                                email = user.email ?: "",
                                avatarId = "brain",
                                xp = 0,
                                level = 1,
                                coins = 0,
                                rank = "Beginner"
                            )
                        } else {
                            val sanitizedName = if (savedProfile.name.isBlank() || savedProfile.name == "Player" || savedProfile.name == "Guest Player") "Guest" else savedProfile.name
                            savedProfile.copy(
                                name = sanitizedName,
                                email = "Guest Account"
                            )
                        }

                        if (authRepository.hasSavedUserSession()) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = true,
                                    currentUserProfile = profileToUse
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = false
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Error in checkAutoLogin inner launch", e)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = false
                            )
                        }
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = false
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error in checkAutoLogin outer", e)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoggedIn = false
                )
            }
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(emailInput = email.trim(), errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(passwordInput = password, errorMessage = null) }
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
        _uiState.update { it.copy(confirmPasswordInput = confirmPassword, errorMessage = null) }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(nameInput = name, errorMessage = null) }
    }

    fun onAvatarSelected(avatarId: String) {
        _uiState.update { it.copy(selectedAvatarId = avatarId) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun toggleRememberMe() {
        _uiState.update { it.copy(rememberMe = !it.rememberMe) }
    }

    fun toggleAuthMode() {
        _uiState.update { 
            it.copy(
                isSignUpMode = !it.isSignUpMode,
                errorMessage = null,
                successMessage = null,
                passwordInput = "",
                confirmPasswordInput = ""
            ) 
        }
    }

    fun onForgotPasswordClicked() {
        openForgotPasswordDialog()
    }

    fun openForgotPasswordDialog() {
        _uiState.update { 
            it.copy(
                showForgotPasswordDialog = true,
                resetPasswordEmailInput = it.emailInput,
                resetPasswordError = null,
                isResetPasswordLoading = false,
                showResetPasswordSuccess = false
            ) 
        }
    }

    fun onResetPasswordEmailChanged(email: String) {
        _uiState.update {
            it.copy(
                resetPasswordEmailInput = email,
                resetPasswordError = null
            )
        }
    }

    fun dismissForgotPasswordDialog() {
        _uiState.update {
            it.copy(
                showForgotPasswordDialog = false,
                isResetPasswordLoading = false,
                resetPasswordError = null
            )
        }
    }

    fun dismissResetPasswordSuccessDialog() {
        _uiState.update {
            it.copy(
                showResetPasswordSuccess = false,
                resetPasswordSuccessEmail = ""
            )
        }
    }

    fun submitPasswordResetRequest(emailToReset: String? = null) {
        val targetEmail = (emailToReset ?: _uiState.value.resetPasswordEmailInput).trim()
        
        if (targetEmail.isBlank()) {
            _uiState.update { it.copy(resetPasswordError = "Please enter your email address.") }
            return
        }
        if (!isEmailValid(targetEmail)) {
            _uiState.update { it.copy(resetPasswordError = "Please enter a valid email address.") }
            return
        }

        _uiState.update {
            it.copy(
                isResetPasswordLoading = true,
                resetPasswordError = null
            )
        }

        viewModelScope.launch {
            try {
                val result = authRepository.sendPasswordResetEmail(targetEmail)
                result.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isResetPasswordLoading = false,
                                showForgotPasswordDialog = false,
                                showResetPasswordSuccess = true,
                                resetPasswordSuccessEmail = targetEmail,
                                resetPasswordError = null
                            )
                        }
                    },
                    onFailure = { error ->
                        val friendlyMsg = getFriendlyErrorMessage(error)
                        _uiState.update {
                            it.copy(
                                isResetPasswordLoading = false,
                                resetPasswordError = friendlyMsg
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                val friendlyMsg = getFriendlyErrorMessage(e)
                _uiState.update {
                    it.copy(
                        isResetPasswordLoading = false,
                        resetPasswordError = friendlyMsg
                    )
                }
            }
        }
    }

    fun dismissResetPasswordNotice() {
        _uiState.update { it.copy(showResetPasswordNotice = false) }
    }

    // Full In-App Password Reset Flow Methods
    fun initResetPasswordFlow(oobCode: String) {
        val trimmedCode = oobCode.trim()
        _uiState.update {
            it.copy(
                resetPasswordCode = trimmedCode,
                isVerifyingResetCode = true,
                isResetCodeValid = false,
                resetCodeError = null,
                newPasswordInput = "",
                resetConfirmPasswordInput = "",
                isNewPasswordVisible = false,
                isResetConfirmPasswordVisible = false,
                isConfirmingResetPassword = false,
                resetPasswordSuccess = false,
                resetPasswordScreenError = null
            )
        }

        if (trimmedCode.isBlank()) {
            _uiState.update {
                it.copy(
                    isVerifyingResetCode = false,
                    resetCodeError = "This password reset link is invalid or has expired. Please request a new reset link."
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                val result = authRepository.verifyPasswordResetCode(trimmedCode)
                result.fold(
                    onSuccess = { email ->
                        _uiState.update {
                            it.copy(
                                isVerifyingResetCode = false,
                                isResetCodeValid = true,
                                resetPasswordEmail = email,
                                resetCodeError = null
                            )
                        }
                    },
                    onFailure = { error ->
                        val friendlyMsg = if (error is com.google.firebase.auth.FirebaseAuthActionCodeException ||
                            error.message?.contains("invalid", ignoreCase = true) == true ||
                            error.message?.contains("expired", ignoreCase = true) == true
                        ) {
                            "This password reset link is invalid or has expired. Please request a new reset link."
                        } else {
                            getFriendlyErrorMessage(error)
                        }

                        _uiState.update {
                            it.copy(
                                isVerifyingResetCode = false,
                                isResetCodeValid = false,
                                resetCodeError = friendlyMsg
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isVerifyingResetCode = false,
                        isResetCodeValid = false,
                        resetCodeError = getFriendlyErrorMessage(e)
                    )
                }
            }
        }
    }

    fun onNewPasswordInputChanged(password: String) {
        _uiState.update { it.copy(newPasswordInput = password, resetPasswordScreenError = null) }
    }

    fun onResetConfirmPasswordInputChanged(password: String) {
        _uiState.update { it.copy(resetConfirmPasswordInput = password, resetPasswordScreenError = null) }
    }

    fun toggleNewPasswordVisibility() {
        _uiState.update { it.copy(isNewPasswordVisible = !it.isNewPasswordVisible) }
    }

    fun toggleResetConfirmPasswordVisibility() {
        _uiState.update { it.copy(isResetConfirmPasswordVisible = !it.isResetConfirmPasswordVisible) }
    }

    fun submitNewPassword() {
        val newPassword = _uiState.value.newPasswordInput
        val confirmPassword = _uiState.value.resetConfirmPasswordInput
        val code = _uiState.value.resetPasswordCode

        if (newPassword.isBlank()) {
            _uiState.update { it.copy(resetPasswordScreenError = "Please enter a new password.") }
            return
        }

        if (!isPasswordMinLength(newPassword)) {
            _uiState.update { it.copy(resetPasswordScreenError = "Password must be at least 6 characters.") }
            return
        }

        if (confirmPassword.isBlank()) {
            _uiState.update { it.copy(resetPasswordScreenError = "Please confirm your password.") }
            return
        }

        if (newPassword != confirmPassword) {
            _uiState.update { it.copy(resetPasswordScreenError = "Passwords do not match.") }
            return
        }

        if (code.isBlank()) {
            _uiState.update { it.copy(resetPasswordScreenError = "This password reset link is invalid or has expired. Please request a new reset link.") }
            return
        }

        _uiState.update {
            it.copy(
                isConfirmingResetPassword = true,
                resetPasswordScreenError = null
            )
        }

        viewModelScope.launch {
            try {
                val result = authRepository.confirmPasswordReset(code, newPassword)
                result.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isConfirmingResetPassword = false,
                                resetPasswordSuccess = true,
                                resetPasswordScreenError = null,
                                resetPasswordCode = ""
                            )
                        }
                    },
                    onFailure = { error ->
                        val friendlyMsg = if (error is com.google.firebase.auth.FirebaseAuthActionCodeException ||
                            error.message?.contains("invalid", ignoreCase = true) == true ||
                            error.message?.contains("expired", ignoreCase = true) == true
                        ) {
                            "This password reset link is invalid or has expired. Please request a new reset link."
                        } else {
                            getFriendlyErrorMessage(error)
                        }

                        _uiState.update {
                            it.copy(
                                isConfirmingResetPassword = false,
                                resetPasswordScreenError = friendlyMsg
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isConfirmingResetPassword = false,
                        resetPasswordScreenError = getFriendlyErrorMessage(e)
                    )
                }
            }
        }
    }

    fun clearResetPasswordState() {
        _uiState.update {
            it.copy(
                resetPasswordCode = "",
                resetPasswordEmail = "",
                isVerifyingResetCode = false,
                isResetCodeValid = false,
                resetCodeError = null,
                newPasswordInput = "",
                resetConfirmPasswordInput = "",
                isNewPasswordVisible = false,
                isResetConfirmPasswordVisible = false,
                isConfirmingResetPassword = false,
                resetPasswordSuccess = false,
                resetPasswordScreenError = null
            )
        }
    }

    private fun triggerError(msg: String) {
        _uiState.update { 
            it.copy(
                errorMessage = msg, 
                isLoading = false,
                isEmailSignInLoading = false,
                isGoogleSignInLoading = false,
                isGuestLoading = false,
                shakeTrigger = it.shakeTrigger + 1
            ) 
        }
    }

    // Validation Functions
    fun isEmailValid(email: String): Boolean {
        return email.isNotBlank() && (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() || (email.contains("@") && email.contains(".")))
    }

    fun isUsernameValid(name: String): Boolean {
        val trimmed = name.trim()
        return trimmed.length in 3..20
    }

    fun isPasswordMinLength(password: String): Boolean = password.length >= 6
    fun hasPasswordUppercase(password: String): Boolean = password.any { it.isUpperCase() }
    fun hasPasswordLowercase(password: String): Boolean = password.any { it.isLowerCase() }
    fun hasPasswordNumber(password: String): Boolean = password.any { it.isDigit() }
    fun hasPasswordSpecialChar(password: String): Boolean = password.any { !it.isLetterOrDigit() }

    fun isPasswordValid(password: String): Boolean {
        return isPasswordMinLength(password)
    }

    fun getPasswordStrengthProgress(password: String): Float {
        if (password.isEmpty()) return 0f
        var score = 0
        if (isPasswordMinLength(password)) score++
        if (hasPasswordUppercase(password)) score++
        if (hasPasswordLowercase(password)) score++
        if (hasPasswordNumber(password)) score++
        if (hasPasswordSpecialChar(password)) score++
        return score / 5f
    }

    fun getPasswordStrengthLabel(password: String): String {
        val progress = getPasswordStrengthProgress(password)
        return when {
            password.isEmpty() -> "Empty"
            progress <= 0.2f -> "Very Weak"
            progress <= 0.4f -> "Weak"
            progress <= 0.6f -> "Fair"
            progress <= 0.8f -> "Good"
            else -> "Strong"
        }
    }

    fun doPasswordsMatch(password: String, confirm: String): Boolean {
        return password.isNotEmpty() && password == confirm
    }

    fun submitEmailAuth(onSuccess: () -> Unit) {
        val state = _uiState.value
        val email = state.emailInput.trim()
        val password = state.passwordInput
        val confirmPassword = state.confirmPasswordInput
        val username = state.nameInput.trim()

        if (state.isSignUpMode) {
            // Validate Create Account
            if (!isUsernameValid(username)) {
                triggerError("Username must be between 3 and 20 characters.")
                return
            }
            if (!isEmailValid(email)) {
                triggerError("Please enter a valid email address.")
                return
            }
            if (!isPasswordValid(password)) {
                triggerError("Password must be at least 6 characters.")
                return
            }
            if (!doPasswordsMatch(password, confirmPassword)) {
                triggerError("Passwords do not match. Please verify your confirm password.")
                return
            }
        } else {
            // Validate Login
            if (!isEmailValid(email)) {
                triggerError("Please enter a valid email address.")
                return
            }
            if (password.isBlank()) {
                triggerError("Please enter your password.")
                return
            }
        }

        _uiState.update { 
            it.copy(
                isEmailSignInLoading = true,
                isGoogleSignInLoading = false,
                isGuestLoading = false,
                isLoading = true,
                errorMessage = null
            ) 
        }

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                withTimeout(10_000L) {
                    if (state.isSignUpMode) {
                        Log.d("AUTH_PERF", "[AuthViewModel] Create Account initiated at $startTime ms for email='$email'")
                        val result = authRepository.signUpWithEmail(email, password, username, state.selectedAvatarId)
                        result.fold(
                            onSuccess = { profile ->
                                val totalMs = System.currentTimeMillis() - startTime
                                Log.d("AUTH_PERF", "[AuthViewModel] Create Account SUCCESS in $totalMs ms. Logging in user automatically.")
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isEmailSignInLoading = false,
                                        isLoggedIn = true,
                                        currentUserProfile = profile,
                                        successMessage = null,
                                        errorMessage = null
                                    )
                                }
                                onSuccess()
                            },
                            onFailure = { error ->
                                val totalMs = System.currentTimeMillis() - startTime
                                Log.e("AUTH_PERF", "[AuthViewModel] Create Account FAILED after $totalMs ms: [${error.javaClass.name}] ${error.message}", error)
                                triggerError(getFriendlyErrorMessage(error))
                            }
                        )
                    } else {
                        Log.d("AUTH_PERF", "[AuthViewModel] Login initiated at $startTime ms for email='$email'")
                        val result = authRepository.signInWithEmail(email, password)
                        result.fold(
                            onSuccess = { user ->
                                val totalMs = System.currentTimeMillis() - startTime
                                Log.d("AUTH_PERF", "[AuthViewModel] Login SUCCESS in $totalMs ms for uid=${user.uid}")
                                val fetchedProfile = authRepository.fetchUserProfile(user.uid)
                                val profile = fetchedProfile ?: UserProfile(
                                    uid = user.uid,
                                    name = user.displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() },
                                    email = user.email ?: email,
                                    avatarId = "brain",
                                    xp = 0,
                                    level = 1,
                                    coins = 0,
                                    streak = 0,
                                    rank = "Beginner"
                                )
                                authRepository.saveUserProfileToFirestore(profile)
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isEmailSignInLoading = false,
                                        isLoggedIn = true,
                                        currentUserProfile = profile,
                                        successMessage = null,
                                        errorMessage = null
                                    )
                                }
                                onSuccess()
                            },
                            onFailure = { error ->
                                val totalMs = System.currentTimeMillis() - startTime
                                Log.e("AUTH_PERF", "[AuthViewModel] Login FAILED after $totalMs ms: [${error.javaClass.name}] ${error.message}", error)
                                triggerError(getFriendlyErrorMessage(error))
                            }
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                val totalMs = System.currentTimeMillis() - startTime
                Log.e("AUTH_PERF", "[AuthViewModel] Create Account/Auth TIMED OUT after 10 seconds (elapsed $totalMs ms)", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEmailSignInLoading = false,
                        errorMessage = "Request timed out. Please try again."
                    )
                }
            } catch (e: Exception) {
                val totalMs = System.currentTimeMillis() - startTime
                Log.e("AUTH_PERF", "[AuthViewModel] Auth operation EXCEPTION after $totalMs ms: [${e.javaClass.name}] ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEmailSignInLoading = false,
                        errorMessage = getFriendlyErrorMessage(e)
                    )
                }
            } finally {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isEmailSignInLoading = false
                    ) 
                }
            }
        }
    }

    fun signInAsGuest(onSuccess: () -> Unit) {
        _uiState.update { 
            it.copy(
                isGuestLoading = true,
                isEmailSignInLoading = false,
                isGoogleSignInLoading = false,
                isLoading = true,
                errorMessage = null
            ) 
        }
        viewModelScope.launch {
            try {
                val result = authRepository.signInAsGuestProfile()
                val rawProfile = result.getOrElse { authRepository.createOrGetGuestProfile() }
                val sanitizedName = if (rawProfile.name.isBlank() || rawProfile.name == "Player" || rawProfile.name == "Guest Player") "Guest" else rawProfile.name
                val guestProfile = rawProfile.copy(
                    name = sanitizedName,
                    email = "Guest Account"
                )
                _uiState.update {
                    it.copy(
                        isGuestLoading = false,
                        isLoading = false,
                        isLoggedIn = true,
                        errorMessage = null,
                        currentUserProfile = guestProfile
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error in signInAsGuest", e)
                _uiState.update {
                    it.copy(
                        isGuestLoading = false,
                        isLoading = false,
                        errorMessage = getFriendlyErrorMessage(e)
                    )
                }
            }
        }
    }

    private fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun setAuthError(message: String?) {
        _uiState.update { 
            it.copy(
                isLoading = false,
                isEmailSignInLoading = false,
                isGoogleSignInLoading = false,
                isGuestLoading = false,
                errorMessage = message
            ) 
        }
    }

    fun signInWithGoogleIdToken(
        idToken: String,
        userEmail: String = "",
        userName: String = "",
        context: Context? = null,
        onSuccess: () -> Unit
    ) {
        _uiState.update { 
            it.copy(
                isGoogleSignInLoading = true,
                isEmailSignInLoading = false,
                isGuestLoading = false,
                isLoading = true,
                errorMessage = null
            ) 
        }
        viewModelScope.launch {
            if (context != null) {
                GoogleAuthDiagnostics.logEvent(
                    context = context,
                    stage = "STAGE_6_FIREBASE_AUTH_BEFORE_SIGN_IN",
                    flowStep = "STEP 10 Firebase signInWithCredential started",
                    additionalInfo = "idTokenLength=${idToken.length}"
                )
            } else {
                Log.d("GOOGLE_AUTH_FLOW", "STEP 10 Firebase signInWithCredential started (idToken length=${idToken.length})")
            }

            val firebaseResult = authRepository.signInWithGoogleCredential(idToken)
            firebaseResult.fold(
                onSuccess = { user ->
                    if (context != null) {
                        GoogleAuthDiagnostics.logEvent(
                            context = context,
                            stage = "STAGE_6_FIREBASE_AUTH_SUCCESS",
                            flowStep = "STEP 11 Firebase auth success",
                            additionalInfo = "uid=${user.uid}"
                        )
                    } else {
                        Log.d("GOOGLE_AUTH_FLOW", "STEP 11 Firebase auth success -> uid=${user.uid}")
                    }

                    val profile = authRepository.fetchUserProfile(user.uid) ?: UserProfile(
                        uid = user.uid,
                        name = user.displayName ?: userName.ifBlank { "Google User" },
                        email = user.email ?: userEmail
                    )
                    Log.d("GOOGLE_AUTH_FLOW", "STEP 13 authenticated profile loaded/created: uid=${profile.uid}")

                    authRepository.setGuestSessionActive(false)
                    authRepository.saveUserProfileToFirestore(profile)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isGoogleSignInLoading = false,
                            isLoggedIn = true,
                            currentUserProfile = profile,
                            errorMessage = null
                        )
                    }
                    Log.d("GOOGLE_AUTH_FLOW", "STEP 14 navigation success")
                    onSuccess()
                },
                onFailure = { error ->
                    val exClass = error.javaClass.name
                    val exMsg = error.message ?: "Firebase authentication failed"
                    if (context != null) {
                        GoogleAuthDiagnostics.logEvent(
                            context = context,
                            stage = "STAGE_6_FIREBASE_AUTH_FAILURE",
                            flowStep = "Firebase auth failed",
                            exception = if (error is Exception) error else Exception(error)
                        )
                    } else {
                        Log.e("GOOGLE_AUTH_FLOW", "Firebase signInWithCredential FAILED: [$exClass] $exMsg", error)
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isGoogleSignInLoading = false,
                            errorMessage = getFriendlyErrorMessage(error)
                        )
                    }
                }
            )
        }
    }

    fun signInWithGoogleEmailFallback(
        email: String,
        name: String,
        onSuccess: () -> Unit
    ) {
        _uiState.update { 
            it.copy(
                isGoogleSignInLoading = true,
                isEmailSignInLoading = false,
                isGuestLoading = false,
                isLoading = true,
                errorMessage = null
            ) 
        }
        viewModelScope.launch {
            Log.d("AUTH_AUDIT", "[LOCAL_GOOGLE_FALLBACK] Logging in with Google Account Email: $email")
            val profile = authRepository.createOrGetLocalEmailProfile(email, name.ifBlank { "Google User" })
            authRepository.setGuestSessionActive(false)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isGoogleSignInLoading = false,
                    isLoggedIn = true,
                    currentUserProfile = profile,
                    errorMessage = null
                )
            }
            onSuccess()
        }
    }

    fun signInWithGoogle(
        context: Context,
        onFallbackToGoogleSignInClient: ((Intent) -> Unit)? = null,
        onSuccess: () -> Unit
    ) {
        Log.d("GOOGLE_AUTH_FLOW", "STEP 1 button clicked")
        _uiState.update { 
            it.copy(
                isGoogleSignInLoading = true,
                isEmailSignInLoading = false,
                isGuestLoading = false,
                isLoading = true,
                errorMessage = null,
                successMessage = null
            ) 
        }

        viewModelScope.launch {
            val activityContext = context.findActivity()
            if (activityContext == null) {
                GoogleAuthDiagnostics.logEvent(
                    context = context,
                    stage = "STAGE_1_ACTIVITY_CONTEXT_RESOLUTION_FAILED",
                    flowStep = "STEP 2 Activity Context Resolution Failed",
                    additionalInfo = "findActivity() returned null"
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isGoogleSignInLoading = false,
                        errorMessage = "Unable to launch Google Sign-In. Please try again."
                    )
                }
                return@launch
            }

            val webClientId = try {
                val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                if (resId != 0) context.getString(resId) else context.getString(com.example.R.string.default_web_client_id)
            } catch (e: Exception) {
                GoogleAuthDiagnostics.EXPECTED_SERVER_CLIENT_ID
            }

            GoogleAuthDiagnostics.logEvent(
                context = context,
                stage = "STAGE_1_INIT",
                flowStep = "STEP 3 Sign-In Initialized",
                serverClientId = webClientId
            )

            fun triggerGoogleSignInClientFallback(causeException: Exception? = null) {
                if (onFallbackToGoogleSignInClient != null) {
                    GoogleAuthDiagnostics.logEvent(
                        context = context,
                        stage = "STAGE_4_TRIGGER_LEGACY_FALLBACK_PREPARE",
                        flowStep = "Triggering GoogleSignInClient intent fallback",
                        exception = causeException,
                        serverClientId = webClientId,
                        additionalInfo = "Trigger Cause: ${causeException?.javaClass?.name ?: "Explicit Fallback"}"
                    )

                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .requestProfile()
                        .build()

                    val googleSignInClient = GoogleSignIn.getClient(activityContext, gso)
                    googleSignInClient.signOut().addOnCompleteListener {
                        try {
                            val signInIntent = googleSignInClient.signInIntent
                            GoogleAuthDiagnostics.logEvent(
                                context = context,
                                stage = "STAGE_4_LEGACY_SIGN_IN_INTENT_LAUNCHING",
                                flowStep = "Launching legacy GoogleSignInClient intent",
                                serverClientId = webClientId
                            )
                            onFallbackToGoogleSignInClient.invoke(signInIntent)
                        } catch (e: Exception) {
                            GoogleAuthDiagnostics.logEvent(
                                context = context,
                                stage = "STAGE_4_LEGACY_SIGN_IN_INTENT_LAUNCH_FAILED",
                                flowStep = "Failed to launch legacy GoogleSignInClient intent",
                                exception = e,
                                serverClientId = webClientId
                            )
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isGoogleSignInLoading = false,
                                    errorMessage = "Google Sign-In launch failed: ${e.message}"
                                )
                            }
                        }
                    }
                } else {
                    GoogleAuthDiagnostics.logEvent(
                        context = context,
                        stage = "STAGE_4_LEGACY_FALLBACK_NO_LAUNCHER_AVAILABLE",
                        flowStep = "No fallback launcher provided",
                        serverClientId = webClientId
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isGoogleSignInLoading = false,
                            errorMessage = "No Google account credentials found on device. Please sign in to Google in device Settings."
                        )
                    }
                }
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            GoogleAuthDiagnostics.logEvent(
                context = context,
                stage = "STAGE_2_CREDENTIAL_MANAGER_BEFORE_GET_CREDENTIAL",
                flowStep = "STEP 5 getCredential starting",
                serverClientId = webClientId
            )

            val credentialManager = CredentialManager.create(activityContext)
            val result = try {
                credentialManager.getCredential(
                    request = request,
                    context = activityContext
                )
            } catch (e: GetCredentialCancellationException) {
                GoogleAuthDiagnostics.logEvent(
                    context = context,
                    stage = "STAGE_2_CREDENTIAL_MANAGER_CANCELLED",
                    flowStep = "User cancelled Google account selection in CredentialManager",
                    exception = e,
                    serverClientId = webClientId
                )
                _uiState.update { it.copy(isLoading = false, isGoogleSignInLoading = false, errorMessage = null) }
                return@launch
            } catch (e: NoCredentialException) {
                GoogleAuthDiagnostics.logEvent(
                    context = context,
                    stage = "STAGE_2_CREDENTIAL_MANAGER_NO_CREDENTIAL",
                    flowStep = "NoCredentialException in CredentialManager -> triggering GoogleSignInClient fallback",
                    exception = e,
                    serverClientId = webClientId
                )
                triggerGoogleSignInClientFallback(e)
                return@launch
            } catch (e: GetCredentialException) {
                GoogleAuthDiagnostics.logEvent(
                    context = context,
                    stage = "STAGE_2_CREDENTIAL_MANAGER_GET_CREDENTIAL_EXCEPTION",
                    flowStep = "GetCredentialException in CredentialManager -> triggering GoogleSignInClient fallback",
                    exception = e,
                    serverClientId = webClientId
                )
                triggerGoogleSignInClientFallback(e)
                return@launch
            } catch (e: Exception) {
                GoogleAuthDiagnostics.logEvent(
                    context = context,
                    stage = "STAGE_2_CREDENTIAL_MANAGER_UNEXPECTED_EXCEPTION",
                    flowStep = "Unexpected Exception in CredentialManager -> triggering GoogleSignInClient fallback",
                    exception = e,
                    serverClientId = webClientId
                )
                triggerGoogleSignInClientFallback(e)
                return@launch
            }

            val credential = result.credential
            GoogleAuthDiagnostics.logEvent(
                context = context,
                stage = "STAGE_2_CREDENTIAL_MANAGER_AFTER_GET_CREDENTIAL_SUCCESS",
                flowStep = "STEP 6 credential returned",
                serverClientId = webClientId,
                additionalInfo = "credentialType=${credential.type}"
            )

            GoogleAuthDiagnostics.logEvent(
                context = context,
                stage = "STAGE_3_CREDENTIAL_EXTRACTION_BEFORE",
                flowStep = "STEP 7 Extracting ID Token",
                serverClientId = webClientId,
                additionalInfo = "credentialType=${credential.type}"
            )

            val (extractedToken, extractedEmail, extractedName) = try {
                val googleIdTokenCred = when {
                    credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                        GoogleIdTokenCredential.createFrom(credential.data)
                    }
                    credential is GoogleIdTokenCredential -> {
                        credential
                    }
                    else -> {
                        throw IllegalArgumentException("Unrecognized Google credential type: ${credential.type}")
                    }
                }
                val token = googleIdTokenCred.idToken
                if (token.isBlank()) {
                    throw IllegalStateException("Extracted Google ID Token is empty or blank")
                }
                Triple(token, googleIdTokenCred.id, googleIdTokenCred.displayName ?: googleIdTokenCred.givenName ?: "")
            } catch (e: Exception) {
                GoogleAuthDiagnostics.logEvent(
                    context = context,
                    stage = "STAGE_3_CREDENTIAL_EXTRACTION_EXCEPTION",
                    flowStep = "Failed to extract Google ID Token -> triggering fallback",
                    exception = e,
                    serverClientId = webClientId
                )
                triggerGoogleSignInClientFallback(e)
                return@launch
            }

            GoogleAuthDiagnostics.logEvent(
                context = context,
                stage = "STAGE_3_CREDENTIAL_EXTRACTION_SUCCESS",
                flowStep = "STEP 8 ID token extracted successfully",
                serverClientId = webClientId,
                additionalInfo = "idTokenLength=${extractedToken.length}"
            )

            signInWithGoogleIdToken(
                idToken = extractedToken,
                userEmail = extractedEmail,
                userName = extractedName,
                context = context,
                onSuccess = onSuccess
            )
        }
    }

    fun signOut(context: Context? = null, onComplete: () -> Unit = {}) {
        autoLoginJob?.cancel()
        autoLoginJob = null
        authRepository.signOut()
        _uiState.value = AuthUiState(isLoggedIn = false)
        if (context != null) {
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                GoogleSignIn.getClient(context, gso).signOut()
            } catch (e: Exception) {
                Log.w("AuthViewModel", "Error signing out Google client: ${e.message}")
            }
        }
        onComplete()
    }

    fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
    }

    fun resetAuthState() {
        autoLoginJob?.cancel()
        autoLoginJob = null
        authRepository.signOut()
        _uiState.value = AuthUiState(isLoggedIn = false)
    }

    private fun getFriendlyErrorMessage(throwable: Throwable): String {
        val exClass = throwable.javaClass.simpleName
        val rawMsg = throwable.message ?: throwable.localizedMessage ?: "Unknown authentication error"
        Log.e("AuthViewModel", "Authentication Exception: [$exClass] $rawMsg", throwable)

        return when {
            throwable is FirebaseNetworkException || 
            throwable is java.net.UnknownHostException || 
            throwable is java.net.ConnectException || 
            throwable is java.net.SocketTimeoutException ||
            throwable is java.io.IOException ||
            rawMsg.contains("network error", ignoreCase = true) ||
            rawMsg.contains("unreachable host", ignoreCase = true) ||
            rawMsg.contains("interrupted connection", ignoreCase = true) ||
            rawMsg.contains("NO_NETWORK", ignoreCase = true) ||
            rawMsg.contains("FirebaseNetworkException", ignoreCase = true) ->
                "No internet connection. Please check your connection and try again."

            throwable is TimeoutCancellationException ||
            rawMsg.contains("timed out", ignoreCase = true) ||
            rawMsg.contains("Timeout", ignoreCase = true) ->
                "Network issue. Please try again later."

            throwable is FirebaseAuthUserCollisionException ||
            rawMsg.contains("email-already-in-use", ignoreCase = true) ||
            rawMsg.contains("already exists", ignoreCase = true) ->
                "An account with this email address already exists. Please sign in instead."

            throwable is FirebaseAuthInvalidCredentialsException -> {
                if (rawMsg.contains("invalid email", ignoreCase = true) || rawMsg.contains("badly formatted", ignoreCase = true)) {
                    "Please enter a valid email address."
                } else {
                    "Invalid email or password. Please verify your login credentials."
                }
            }

            throwable is FirebaseAuthInvalidUserException ||
            rawMsg.contains("user-not-found", ignoreCase = true) ->
                "No account found with this email. Please check your entry or create an account."

            throwable is FirebaseAuthWeakPasswordException ->
                "Password is too weak. Please use at least 6 characters."

            rawMsg.contains("badly formatted", ignoreCase = true) || rawMsg.contains("invalid email", ignoreCase = true) ->
                "Please enter a valid email address."

            rawMsg.contains("wrong-password", ignoreCase = true) ->
                "Incorrect password. Please try again."

            rawMsg.contains("too-many-requests", ignoreCase = true) || rawMsg.contains("TOO_MANY_ATTEMPTS", ignoreCase = true) ->
                "Too many failed login attempts. Please try again later."

            else ->
                "Authentication failed. Please check your connection and try again."
        }
    }
}
