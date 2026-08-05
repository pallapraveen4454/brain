package com.example.viewmodel

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
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

    init {
        checkAutoLogin()
    }

    private fun checkAutoLogin() {
        try {
            if (authRepository.hasSavedUserSession()) {
                _uiState.update { it.copy(isLoading = true) }
                viewModelScope.launch {
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

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                currentUserProfile = profileToUse
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Error in checkAutoLogin", e)
                        val savedProfile = authRepository.getPersistentGuestProfile()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                currentUserProfile = savedProfile
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error in checkAutoLogin", e)
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
                                Log.d("AUTH_PERF", "[AuthViewModel] Create Account SUCCESS in $totalMs ms. Switching to Login screen with success message.")
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isLoggedIn = false,
                                        isSignUpMode = false,
                                        successMessage = "✅ Account created successfully.",
                                        errorMessage = null,
                                        passwordInput = "",
                                        confirmPasswordInput = ""
                                    )
                                }
                                // Do NOT auto-login or navigate to Home
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

    fun signInWithGoogle(
        context: Context,
        webClientId: String = "106236832575-nv10u3crcpl0dh353k88c8hkfidh448e.apps.googleusercontent.com",
        onSuccess: () -> Unit
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val packageName = context.packageName
            val resolvedWebClientId = try {
                val resId = context.resources.getIdentifier("default_web_client_id", "string", packageName)
                if (resId != 0) context.getString(resId) else webClientId
            } catch (e: Exception) {
                webClientId
            }

            Log.d("AUTH_AUDIT", "[START] Google Sign-In initiated. Package=$packageName, ClientId=$resolvedWebClientId")

            val activityContext = context.findActivity() ?: context
            val credentialManager = CredentialManager.create(activityContext)
            var idToken: String? = null
            var lastException: Exception? = null

            // Step 1: Official Interactive Google Identity Services flow (GetSignInWithGoogleOption)
            try {
                Log.d("AUTH_AUDIT", "[STEP 1] Executing GetSignInWithGoogleOption with clientId=$resolvedWebClientId")
                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(resolvedWebClientId)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = activityContext
                )

                val credential = result.credential
                Log.d("AUTH_AUDIT", "[STEP 1] Credential returned: type=${credential.type}, class=${credential.javaClass.name}")

                idToken = when {
                    credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                        try {
                            val googleIdTokenCred = GoogleIdTokenCredential.createFrom(credential.data)
                            Log.d("AUTH_AUDIT", "[STEP 1] GoogleIdTokenCredential.createFrom SUCCESS. Email=${googleIdTokenCred.id}, Token Length=${googleIdTokenCred.idToken.length}")
                            googleIdTokenCred.idToken
                        } catch (e: Exception) {
                            Log.e("AUTH_AUDIT", "[STEP 1] GoogleIdTokenCredential.createFrom FAILED: ${e.message}", e)
                            lastException = e
                            null
                        }
                    }
                    credential is GoogleIdTokenCredential -> {
                        Log.d("AUTH_AUDIT", "[STEP 1] GoogleIdTokenCredential direct object received. Email=${credential.id}, Token Length=${credential.idToken.length}")
                        credential.idToken
                    }
                    else -> {
                        Log.w("AUTH_AUDIT", "[STEP 1] Unrecognized credential type: ${credential.type}")
                        null
                    }
                }
            } catch (e: GetCredentialCancellationException) {
                val msg = e.message ?: e.localizedMessage ?: "Cancellation exception"
                Log.d("AUTH_AUDIT", "[STEP 1] GetCredentialCancellationException: type=${e.type}, message=$msg, cause=${e.cause}")
                lastException = e
            } catch (e: GetCredentialException) {
                val msg = e.message ?: e.localizedMessage ?: "GetCredentialException"
                Log.d("AUTH_AUDIT", "[STEP 1] GetCredentialException (${e.type}): $msg. Will attempt Step 2 fallback...")
                lastException = e
            } catch (e: Exception) {
                Log.e("AUTH_AUDIT", "[STEP 1] Unexpected exception: [${e.javaClass.name}] ${e.message}", e)
                lastException = e
            }

            // Step 2: Fallback A - GetGoogleIdOption (filterByAuthorizedAccounts = false)
            if (idToken == null) {
                try {
                    Log.d("AUTH_AUDIT", "[STEP 2] Executing GetGoogleIdOption fallback (filterByAuthorizedAccounts=false)...")
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(resolvedWebClientId)
                        .setAutoSelectEnabled(false)
                        .build()

                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    val result = credentialManager.getCredential(
                        request = fallbackRequest,
                        context = activityContext
                    )

                    val credential = result.credential
                    Log.d("AUTH_AUDIT", "[STEP 2] Credential returned: type=${credential.type}, class=${credential.javaClass.name}")

                    idToken = when {
                        credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                            try {
                                val googleIdTokenCred = GoogleIdTokenCredential.createFrom(credential.data)
                                Log.d("AUTH_AUDIT", "[STEP 2] GoogleIdTokenCredential.createFrom SUCCESS. Email=${googleIdTokenCred.id}, Token Length=${googleIdTokenCred.idToken.length}")
                                googleIdTokenCred.idToken
                            } catch (e: Exception) {
                                Log.e("AUTH_AUDIT", "[STEP 2] GoogleIdTokenCredential.createFrom FAILED: ${e.message}", e)
                                lastException = e
                                null
                            }
                        }
                        credential is GoogleIdTokenCredential -> {
                            Log.d("AUTH_AUDIT", "[STEP 2] GoogleIdTokenCredential direct object received. Email=${credential.id}, Token Length=${credential.idToken.length}")
                            credential.idToken
                        }
                        else -> {
                            Log.w("AUTH_AUDIT", "[STEP 2] Unrecognized credential type: ${credential.type}")
                            null
                        }
                    }
                } catch (e: GetCredentialCancellationException) {
                    val msg = e.message ?: e.localizedMessage ?: "Cancellation exception"
                    Log.d("AUTH_AUDIT", "[STEP 2] GetCredentialCancellationException: type=${e.type}, message=$msg, cause=${e.cause}")
                    lastException = e
                } catch (e: GetCredentialException) {
                    val msg = e.message ?: e.localizedMessage ?: "GetCredentialException"
                    Log.d("AUTH_AUDIT", "[STEP 2] GetCredentialException (${e.type}): $msg")
                    lastException = e
                } catch (e: Exception) {
                    Log.e("AUTH_AUDIT", "[STEP 2] Unexpected exception: [${e.javaClass.name}] ${e.message}", e)
                    lastException = e
                }
            }

            // Step 3: Fallback B - GoogleSignIn Client check
            if (idToken == null) {
                try {
                    Log.d("AUTH_AUDIT", "[STEP 3] Checking GoogleSignIn client cache...")
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(resolvedWebClientId)
                        .requestEmail()
                        .build()
                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                    val accountTask = googleSignInClient.silentSignIn()
                    val account = if (accountTask.isSuccessful) {
                        accountTask.result
                    } else {
                        GoogleSignIn.getLastSignedInAccount(context)
                    }
                    idToken = account?.idToken
                    if (idToken != null) {
                        Log.d("AUTH_AUDIT", "[STEP 3] GoogleSignIn account found: ${account?.email}, Token Length=${idToken.length}")
                    } else {
                        Log.d("AUTH_AUDIT", "[STEP 3] No cached GoogleSignIn account.")
                    }
                } catch (e: Exception) {
                    Log.e("AUTH_AUDIT", "[STEP 3] GoogleSignIn check exception: [${e.javaClass.name}] ${e.message}", e)
                    if (lastException == null) lastException = e
                }
            }

            // Step 4: Perform Firebase Auth credential exchange if token obtained
            if (idToken != null) {
                Log.d("AUTH_AUDIT", "[STEP 4] Google ID Token obtained. Exchanging with Firebase...")
                val firebaseResult = authRepository.signInWithGoogleCredential(idToken)
                firebaseResult.fold(
                    onSuccess = { user ->
                        Log.d("AUTH_AUDIT", "[STEP 4] FirebaseAuth.signInWithCredential SUCCESS. User UID=${user.uid}, Email=${user.email}")
                        val profile = authRepository.fetchUserProfile(user.uid) ?: UserProfile(
                            uid = user.uid,
                            name = user.displayName ?: "Google User",
                            email = user.email ?: ""
                        )
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
                    },
                    onFailure = { error ->
                        val exClass = error.javaClass.simpleName
                        val exMsg = error.message ?: "Firebase authentication failed"
                        Log.e("AUTH_AUDIT", "[STEP 4] FirebaseAuth.signInWithCredential FAILED: [$exClass] $exMsg", error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Firebase Auth Error [$exClass]: $exMsg"
                            )
                        }
                    }
                )
            } else {
                Log.e("AUTH_AUDIT", "[SUMMARY] Failed to retrieve Google ID Token across all options. Last exception: [${lastException?.javaClass?.simpleName}] ${lastException?.message}", lastException)
                val realErrorMessage = if (lastException != null) {
                    val exClass = lastException.javaClass.simpleName
                    val exMsg = lastException.message ?: "No error detail provided"
                    "Google Sign-In failed [$exClass]: $exMsg"
                } else {
                    "Google Sign-In failed: No account or token received from Google Play Services."
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = realErrorMessage
                    )
                }
            }
        }
    }

    fun signOut(onComplete: () -> Unit) {
        authRepository.signOut()
        _uiState.update {
            AuthUiState()
        }
        onComplete()
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
