package com.example.viewmodel

import android.content.Context
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
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
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
                            if (fetched != null) {
                                val merged = savedProfile.copy(
                                    uid = user.uid,
                                    email = user.email ?: fetched.email,
                                    xp = maxOf(fetched.xp, savedProfile.xp),
                                    coins = savedProfile.coins,
                                    streak = maxOf(fetched.streak, savedProfile.streak),
                                    unlockedAvatars = (savedProfile.unlockedAvatars + fetched.unlockedAvatars).distinct()
                                )
                                authRepository.saveUserProfileToFirestore(merged)
                                merged
                            } else {
                                savedProfile
                            }
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
            if (state.isSignUpMode) {
                // Sign Up Flow - Real Firebase createUserWithEmailAndPassword
                Log.d("AuthViewModel", "Attempting Firebase createUserWithEmailAndPassword for email='$email', username='$username'")
                val result = authRepository.signUpWithEmail(email, password, username)
                result.fold(
                    onSuccess = { user ->
                        Log.d("AuthViewModel", "Firebase createUserWithEmailAndPassword SUCCESS: uid=${user.uid}")
                        val profile = authRepository.fetchUserProfile(user.uid) ?: UserProfile(
                            uid = user.uid,
                            name = username.ifBlank { user.displayName ?: email.substringBefore("@") },
                            email = user.email ?: email,
                            avatarId = state.selectedAvatarId
                        )
                        if (state.selectedAvatarId.isNotBlank()) {
                            authRepository.updateProfileAvatar(profile.uid, state.selectedAvatarId)
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                currentUserProfile = profile
                            )
                        }
                        onSuccess()
                    },
                    onFailure = { error ->
                        Log.e("AuthViewModel", "Firebase createUserWithEmailAndPassword FAILED: [${error.javaClass.name}] ${error.message}", error)
                        triggerError(getFriendlyErrorMessage(error))
                    }
                )
            } else {
                // Login Flow - Real Firebase signInWithEmailAndPassword
                Log.d("AuthViewModel", "Attempting Firebase signInWithEmailAndPassword for email='$email'")
                val result = authRepository.signInWithEmail(email, password)
                result.fold(
                    onSuccess = { user ->
                        Log.d("AuthViewModel", "Firebase signInWithEmailAndPassword SUCCESS: uid=${user.uid}")
                        val profile = authRepository.fetchUserProfile(user.uid) ?: UserProfile(
                            uid = user.uid,
                            name = user.displayName ?: email.substringBefore("@"),
                            email = user.email ?: email
                        )
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                currentUserProfile = profile
                            )
                        }
                        onSuccess()
                    },
                    onFailure = { error ->
                        Log.e("AuthViewModel", "Firebase signInWithEmailAndPassword FAILED: [${error.javaClass.name}] ${error.message}", error)
                        triggerError(getFriendlyErrorMessage(error))
                    }
                )
            }
        }
    }

    fun signInAsGuest(onSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.signInAsGuestProfile()
            val guestProfile = result.getOrElse { authRepository.getPersistentGuestProfile() }
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

    fun signInWithGoogle(context: Context, webClientId: String = "106236832575-nv10u3crcpl0dh353k88c8hkfidh448e.apps.googleusercontent.com", onSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val credential = result.credential
                val idToken = when {
                    credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                        GoogleIdTokenCredential.createFrom(credential.data).idToken
                    }
                    credential is GoogleIdTokenCredential -> {
                        credential.idToken
                    }
                    else -> null
                }

                if (idToken != null) {
                    val firebaseResult = authRepository.signInWithGoogleCredential(idToken)
                    firebaseResult.fold(
                        onSuccess = { user ->
                            val profile = authRepository.fetchUserProfile(user.uid) ?: UserProfile(
                                uid = user.uid,
                                name = user.displayName ?: "Google User",
                                email = user.email ?: ""
                            )
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = true,
                                    currentUserProfile = profile
                                )
                            }
                            onSuccess()
                        },
                        onFailure = { error ->
                            Log.e("AuthViewModel", "Google Sign-In Firebase auth failed: [${error.javaClass.name}] ${error.message}", error)
                            triggerError(getFriendlyErrorMessage(error))
                        }
                    )
                } else {
                    triggerError("Failed to retrieve Google authentication token. Please try again.")
                }
            } catch (e: GetCredentialCancellationException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Google sign-in was cancelled."
                    )
                }
            } catch (e: GetCredentialException) {
                Log.e("AuthViewModel", "Google Credential Manager exception: [${e.javaClass.name}] ${e.message}", e)
                triggerError("Google Sign-In error: ${e.localizedMessage ?: e.message}")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google Sign-In error: [${e.javaClass.name}] ${e.message}", e)
                triggerError(getFriendlyErrorMessage(e))
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
