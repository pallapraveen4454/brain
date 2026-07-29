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
    val nameInput: String = "",
    val isPasswordVisible: Boolean = false,
    val isSignUpMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val currentUserProfile: UserProfile? = null,
    val showResetPasswordNotice: Boolean = false
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
                                    coins = maxOf(fetched.coins, savedProfile.coins),
                                    streak = maxOf(fetched.streak, savedProfile.streak)
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

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(nameInput = name, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleAuthMode() {
        _uiState.update { 
            it.copy(
                isSignUpMode = !it.isSignUpMode,
                errorMessage = null
            ) 
        }
    }

    fun onForgotPasswordClicked() {
        _uiState.update { it.copy(showResetPasswordNotice = true) }
    }

    fun dismissResetPasswordNotice() {
        _uiState.update { it.copy(showResetPasswordNotice = false) }
    }

    fun submitEmailAuth(onSuccess: () -> Unit) {
        val state = _uiState.value
        val email = state.emailInput
        val password = state.passwordInput

        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters long.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            if (state.isSignUpMode) {
                // Sign Up Flow
                val result = authRepository.signUpWithEmail(email, password, state.nameInput)
                val local = quizResultRepository.getLocalProgress()
                result.fold(
                    onSuccess = { user ->
                        val profile = authRepository.fetchUserProfile(user.uid) ?: UserProfile(
                            uid = user.uid,
                            name = state.nameInput.ifBlank { email.substringBefore("@") },
                            email = email,
                            xp = local.totalXp,
                            level = maxOf(1, local.level),
                            coins = local.coins,
                            streak = local.streak,
                            lastActiveDate = local.lastActiveDate,
                            lastQuizCategory = local.lastCategoryName,
                            lastQuizScore = local.lastScoreOutOfTen,
                            lastQuizXpEarned = local.lastXpEarned,
                            lastQuizDate = local.lastQuizDate
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
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = getFriendlyErrorMessage(error)
                            )
                        }
                    }
                )
            } else {
                // Login Flow
                val result = authRepository.signInWithEmail(email, password)
                val local = quizResultRepository.getLocalProgress()
                result.fold(
                    onSuccess = { user ->
                        val profile = authRepository.fetchUserProfile(user.uid) ?: UserProfile(
                            uid = user.uid,
                            name = user.displayName ?: email.substringBefore("@"),
                            email = email,
                            xp = local.totalXp,
                            level = maxOf(1, local.level),
                            coins = local.coins,
                            streak = local.streak,
                            lastActiveDate = local.lastActiveDate,
                            lastQuizCategory = local.lastCategoryName,
                            lastQuizScore = local.lastScoreOutOfTen,
                            lastQuizXpEarned = local.lastXpEarned,
                            lastQuizDate = local.lastQuizDate
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
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = getFriendlyErrorMessage(error)
                            )
                        }
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

    fun signInWithGoogle(context: Context, webClientId: String, onSuccess: () -> Unit) {
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
                    val local = quizResultRepository.getLocalProgress()
                    firebaseResult.fold(
                        onSuccess = { user ->
                            val profile = authRepository.fetchUserProfile(user.uid) ?: UserProfile(
                                uid = user.uid,
                                name = user.displayName ?: "Google User",
                                email = user.email ?: "",
                                xp = local.totalXp,
                                level = maxOf(1, local.level),
                                coins = local.coins,
                                streak = local.streak,
                                lastActiveDate = local.lastActiveDate,
                                lastQuizCategory = local.lastCategoryName,
                                lastQuizScore = local.lastScoreOutOfTen,
                                lastQuizXpEarned = local.lastXpEarned,
                                lastQuizDate = local.lastQuizDate
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
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = getFriendlyErrorMessage(error)
                                )
                            }
                        }
                    )
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to retrieve Google token. Please try again."
                        )
                    }
                }
            } catch (e: GetCredentialCancellationException) {
                // User cancelled the prompt
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Google sign-in was cancelled."
                    )
                }
            } catch (e: GetCredentialException) {
                Log.e("AuthViewModel", "Credential Manager exception", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Google Sign-In unavailable. " + (e.localizedMessage ?: "")
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google Sign-In error", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = getFriendlyErrorMessage(e)
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
        return when (throwable) {
            is FirebaseAuthInvalidCredentialsException ->
                "Invalid email or password. Please check your login credentials."
            is FirebaseAuthInvalidUserException ->
                "No account found with this email. Please check your entry or create an account."
            is FirebaseAuthUserCollisionException ->
                "An account with this email address already exists. Try signing in instead."
            is FirebaseAuthWeakPasswordException ->
                "Password is too weak. Please use at least 6 characters."
            is FirebaseNetworkException ->
                "Network connection error. Please verify your internet connection and try again."
            else -> {
                val msg = throwable.localizedMessage ?: "An error occurred during authentication."
                if (msg.contains("API key", ignoreCase = true) || msg.contains("google-services", ignoreCase = true)) {
                    "Firebase service configuration notice: " + msg
                } else {
                    msg
                }
            }
        }
    }
}
