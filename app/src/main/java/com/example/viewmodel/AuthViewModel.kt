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
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val currentUserProfile: UserProfile? = null,
    val showResetPasswordNotice: Boolean = false,
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
                        val savedProfile = authRepository.getPersistentGuestProfile()
                        val user = authRepository.currentUser
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
                            savedProfile
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
        val email = _uiState.value.emailInput
        if (email.isNotBlank() && isEmailValid(email)) {
            viewModelScope.launch {
                authRepository.sendPasswordResetEmail(email)
            }
        }
        _uiState.update { it.copy(showResetPasswordNotice = true) }
    }

    fun dismissResetPasswordNotice() {
        _uiState.update { it.copy(showResetPasswordNotice = false) }
    }

    private fun triggerError(msg: String) {
        _uiState.update { 
            it.copy(
                errorMessage = msg, 
                isLoading = false,
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

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

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
                        errorMessage = "Request timed out. Please try again."
                    )
                }
            } catch (e: Exception) {
                val totalMs = System.currentTimeMillis() - startTime
                Log.e("AUTH_PERF", "[AuthViewModel] Auth operation EXCEPTION after $totalMs ms: [${e.javaClass.name}] ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = getFriendlyErrorMessage(e)
                    )
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun signInAsGuest(onSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.signInAsGuestProfile()
            val guestProfile = result.getOrElse { authRepository.createOrGetGuestProfile() }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    errorMessage = null,
                    currentUserProfile = guestProfile
                )
            }
            onSuccess()
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
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    fun signInWithGoogleIdToken(
        idToken: String,
        userEmail: String = "",
        userName: String = "",
        onSuccess: () -> Unit
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            Log.d("GOOGLE_AUTH_FLOW", "STEP 9 Firebase credential created from Google ID Token (length=${idToken.length})")
            Log.d("GOOGLE_AUTH_FLOW", "STEP 10 Firebase signInWithCredential started")

            val firebaseResult = authRepository.signInWithGoogleCredential(idToken)
            firebaseResult.fold(
                onSuccess = { user ->
                    Log.d("GOOGLE_AUTH_FLOW", "STEP 11 Firebase authentication success -> email=${user.email}")
                    Log.d("GOOGLE_AUTH_FLOW", "STEP 12 Firebase UID: ${user.uid}")

                    val profile = authRepository.fetchUserProfile(user.uid) ?: UserProfile(
                        uid = user.uid,
                        name = user.displayName ?: userName.ifBlank { "Google User" },
                        email = user.email ?: userEmail
                    )
                    Log.d("GOOGLE_AUTH_FLOW", "STEP 13 authenticated profile loaded/created: uid=${profile.uid}, name=${profile.name}, xp=${profile.xp}, coins=${profile.coins}")

                    authRepository.setGuestSessionActive(false)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            currentUserProfile = profile,
                            errorMessage = null
                        )
                    }
                    Log.d("GOOGLE_AUTH_FLOW", "STEP 14 navigation success")
                    onSuccess()
                },
                onFailure = { error ->
                    val exClass = error.javaClass.simpleName
                    val exMsg = error.message ?: "Firebase authentication failed"
                    Log.e("GOOGLE_AUTH_FLOW", "Firebase signInWithCredential FAILED: [$exClass] $exMsg", error)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Firebase Authentication Failed [$exClass]: $exMsg"
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
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            Log.d("AUTH_AUDIT", "[LOCAL_GOOGLE_FALLBACK] Logging in with Google Account Email: $email")
            val profile = authRepository.createOrGetLocalEmailProfile(email, name.ifBlank { "Google User" })
            authRepository.setGuestSessionActive(false)
            _uiState.update {
                it.copy(
                    isLoading = false,
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
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val activityContext = context.findActivity()
            if (activityContext == null) {
                Log.e("GOOGLE_AUTH_FLOW", "STEP 2 activity context resolution failed")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to launch Google Sign-In. Please try again."
                    )
                }
                return@launch
            }
            Log.d("GOOGLE_AUTH_FLOW", "STEP 2 activity context resolved")

            val webClientId = try {
                val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                if (resId != 0) context.getString(resId) else context.getString(com.example.R.string.default_web_client_id)
            } catch (e: Exception) {
                "106236832575-nv10u3crcpl0dh353k88c8hkfidh448e.apps.googleusercontent.com"
            }
            Log.d("GOOGLE_AUTH_FLOW", "STEP 3 web client ID resolved: ${webClientId.take(20)}...")
            Log.d("GOOGLE_AUTH_RUNTIME", "package=${context.packageName}, firebaseProjectId=brainquiz-ai-app, firebaseAppId=1:106236832575:android:8bb30cbfcabc48ffdfc18a, serverClientId=${webClientId.take(20)}..., stage=INIT")

            fun triggerGoogleSignInClientFallback() {
                if (onFallbackToGoogleSignInClient != null) {
                    Log.d("GOOGLE_AUTH_FLOW", "Triggering GoogleSignInClient intent fallback with webClientId")
                    Log.d("GOOGLE_AUTH_RUNTIME", "package=${context.packageName}, firebaseProjectId=brainquiz-ai-app, firebaseAppId=1:106236832575:android:8bb30cbfcabc48ffdfc18a, serverClientId=${webClientId.take(20)}..., stage=TRIGGER_LEGACY_GOOGLE_SIGN_IN_CLIENT_FALLBACK")
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .requestProfile()
                        .build()
                    val googleSignInClient = GoogleSignIn.getClient(activityContext, gso)
                    googleSignInClient.signOut().addOnCompleteListener {
                        try {
                            val signInIntent = googleSignInClient.signInIntent
                            onFallbackToGoogleSignInClient.invoke(signInIntent)
                        } catch (e: Exception) {
                            Log.e("GOOGLE_AUTH_FLOW", "Failed to launch GoogleSignInClient intent fallback: ${e.message}", e)
                            Log.e("GOOGLE_AUTH_RUNTIME", "package=${context.packageName}, firebaseProjectId=brainquiz-ai-app, firebaseAppId=1:106236832575:android:8bb30cbfcabc48ffdfc18a, serverClientId=${webClientId.take(20)}..., stage=A_CREDENTIAL_MANAGER_FAILURE, exception=${e.javaClass.simpleName}: ${e.message}. Note: Firebase Auth is NOT responsible for Google Sign-In intent launch failure.")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Google Sign-In launch failed: ${e.message}"
                                )
                            }
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
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
            Log.d("GOOGLE_AUTH_FLOW", "STEP 4 credential request created")

            val credentialManager = CredentialManager.create(activityContext)
            val result = try {
                Log.d("GOOGLE_AUTH_FLOW", "STEP 5 getCredential started")
                credentialManager.getCredential(
                    request = request,
                    context = activityContext
                )
            } catch (e: GetCredentialCancellationException) {
                Log.d("GOOGLE_AUTH_FLOW", "User cancelled Google account selection in CredentialManager")
                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                return@launch
            } catch (e: NoCredentialException) {
                Log.d("GOOGLE_AUTH_FLOW", "NoCredentialException in CredentialManager [${e.javaClass.simpleName}]: ${e.message}. Attempting GoogleSignInClient fallback...")
                triggerGoogleSignInClientFallback()
                return@launch
            } catch (e: GetCredentialException) {
                Log.d("GOOGLE_AUTH_FLOW", "GetCredentialException in CredentialManager [${e.javaClass.simpleName}]: ${e.message}. Attempting GoogleSignInClient fallback...")
                triggerGoogleSignInClientFallback()
                return@launch
            } catch (e: Exception) {
                Log.d("GOOGLE_AUTH_FLOW", "Unexpected Exception in CredentialManager [${e.javaClass.simpleName}]: ${e.message}. Attempting GoogleSignInClient fallback...")
                triggerGoogleSignInClientFallback()
                return@launch
            }

            val credential = result.credential
            Log.d("GOOGLE_AUTH_FLOW", "STEP 6 credential returned")
            Log.d("GOOGLE_AUTH_FLOW", "STEP 7 credential type: ${credential.type}")

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
                Log.e("GOOGLE_AUTH_FLOW", "Failed to extract Google ID Token from CredentialManager result: [${e.javaClass.simpleName}] ${e.message}", e)
                triggerGoogleSignInClientFallback()
                return@launch
            }

            Log.d("GOOGLE_AUTH_FLOW", "STEP 8 ID token extracted (length=${extractedToken.length}, email=$extractedEmail)")
            signInWithGoogleIdToken(
                idToken = extractedToken,
                userEmail = extractedEmail,
                userName = extractedName,
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

        return when (throwable) {
            is TimeoutCancellationException ->
                "Request timed out. Please try again."
            is FirebaseAuthUserCollisionException ->
                "An account with this email address already exists. Please sign in instead."
            is FirebaseAuthInvalidCredentialsException ->
                "Invalid email or password. Please verify your login credentials."
            is FirebaseAuthInvalidUserException ->
                "No account found with this email. Please check your entry or create an account."
            is FirebaseAuthWeakPasswordException ->
                "Password is too weak. Please use at least 6 characters."
            is FirebaseNetworkException ->
                "No internet connection. Please verify your network connection and try again."
            else -> {
                when {
                    rawMsg.contains("timed out", ignoreCase = true) || rawMsg.contains("Timeout", ignoreCase = true) ->
                        "Request timed out. Please try again."
                    rawMsg.contains("badly formatted", ignoreCase = true) || rawMsg.contains("invalid email", ignoreCase = true) ->
                        "Invalid email format ($rawMsg)"
                    rawMsg.contains("user-not-found", ignoreCase = true) ->
                        "No account found matching this email address ($rawMsg)"
                    rawMsg.contains("wrong-password", ignoreCase = true) ->
                        "Incorrect password ($rawMsg)"
                    rawMsg.contains("too-many-requests", ignoreCase = true) || rawMsg.contains("TOO_MANY_ATTEMPTS", ignoreCase = true) ->
                        "Too many failed login attempts ($rawMsg)"
                    rawMsg.contains("blocked", ignoreCase = true) || rawMsg.contains("identitytoolkit", ignoreCase = true) ->
                        "Requests to Identity Toolkit API are blocked in GCP project. Please enable Identity Toolkit API and check API key restrictions."
                    rawMsg.contains("API key", ignoreCase = true) || rawMsg.contains("restricted", ignoreCase = true) ->
                        "Firebase Auth API key restriction error: Please check API key restrictions in Google Cloud Console."
                    else -> "[$exClass] $rawMsg"
                }
            }
        }
    }
}
