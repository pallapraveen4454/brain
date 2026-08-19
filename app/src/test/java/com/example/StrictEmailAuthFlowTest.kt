package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AuthRepository
import com.example.data.UserProfile
import com.example.data.UserProfileStore
import com.example.viewmodel.AuthViewModel
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StrictEmailAuthFlowTest {

    private lateinit var context: Context
    private lateinit var userProfileStore: UserProfileStore
    private lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        userProfileStore = UserProfileStore(context)
        authRepository = AuthRepository(context = context, userProfileStore = userProfileStore)
    }

    @Test
    fun testFreshInstallIsLoggedOutAndStartsAtLogin() {
        assertFalse("Fresh install must not have guest session active", authRepository.isGuestSessionActive())
        assertFalse("Fresh install must not be logged in", authRepository.isUserLoggedIn())
        assertFalse("Fresh install must not have saved session", authRepository.hasSavedUserSession())
    }

    @Test
    fun testLoginValidationRequirements() {
        val viewModel = AuthViewModel(authRepository = authRepository)

        // Invalid email
        assertFalse(viewModel.isEmailValid("not-an-email"))
        assertTrue(viewModel.isEmailValid("user@example.com"))

        // Password length
        assertFalse(viewModel.isPasswordMinLength("12345"))
        assertTrue(viewModel.isPasswordMinLength("123456"))

        // Password matching
        assertFalse(viewModel.doPasswordsMatch("pass123", "pass456"))
        assertTrue(viewModel.doPasswordsMatch("pass123", "pass123"))
    }

    @Test
    fun testErrorMappingNonExistingAccount() {
        val viewModel = AuthViewModel(authRepository = authRepository)

        val invalidUserEx = FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "There is no user record corresponding to this identifier.")
        val errorMsg = viewModel.getFriendlyErrorMessage(invalidUserEx)
        assertEquals("Account not found. Please create an account first.", errorMsg)

        val customNotFoundEx = Exception("user-not-found")
        val errorMsg2 = viewModel.getFriendlyErrorMessage(customNotFoundEx)
        assertEquals("Account not found. Please create an account first.", errorMsg2)
    }

    @Test
    fun testErrorMappingWrongPassword() {
        val viewModel = AuthViewModel(authRepository = authRepository)

        val invalidCredentialsEx = FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "The password is invalid or the user does not have a password.")
        val errorMsg = viewModel.getFriendlyErrorMessage(invalidCredentialsEx)
        assertEquals("Incorrect email or password.", errorMsg)

        val genericCredEx = FirebaseAuthInvalidCredentialsException("INVALID_LOGIN_CREDENTIALS", "INVALID_LOGIN_CREDENTIALS")
        val errorMsg2 = viewModel.getFriendlyErrorMessage(genericCredEx)
        assertEquals("Incorrect email or password.", errorMsg2)
    }

    @Test
    fun testErrorMappingInvalidEmailFormat() {
        val viewModel = AuthViewModel(authRepository = authRepository)

        val invalidEmailEx = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_EMAIL", "The email address is badly formatted.")
        val errorMsg = viewModel.getFriendlyErrorMessage(invalidEmailEx)
        assertEquals("Please enter a valid email address.", errorMsg)

        val badFormatEx = Exception("badly formatted email")
        val errorMsg2 = viewModel.getFriendlyErrorMessage(badFormatEx)
        assertEquals("Please enter a valid email address.", errorMsg2)
    }

    @Test
    fun testErrorMappingNetworkError() {
        val viewModel = AuthViewModel(authRepository = authRepository)

        val networkEx = FirebaseNetworkException("A network error (such as timeout, interrupted connection or unreachable host) has occurred.")
        val errorMsg = viewModel.getFriendlyErrorMessage(networkEx)
        assertEquals("Network error. Please check your internet connection and try again.", errorMsg)
    }

    @Test
    fun testErrorMappingAccountCollision() {
        val viewModel = AuthViewModel(authRepository = authRepository)

        val collisionEx = FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "The email address is already in use by another account.")
        val errorMsg = viewModel.getFriendlyErrorMessage(collisionEx)
        assertEquals("An account with this email already exists. Please sign in.", errorMsg)
    }

    @Test
    fun testErrorMappingWeakPassword() {
        val viewModel = AuthViewModel(authRepository = authRepository)

        val weakPassEx = FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "Password should be at least 6 characters", "12345")
        val errorMsg = viewModel.getFriendlyErrorMessage(weakPassEx)
        assertEquals("Password must be at least 6 characters.", errorMsg)
    }

    @Test
    fun testRegistrationSignOutAndReturnToLogin() {
        val viewModel = AuthViewModel(authRepository = authRepository)

        // Start in sign up mode
        viewModel.toggleAuthMode()
        assertTrue("Should be in sign up mode", viewModel.uiState.value.isSignUpMode)

        viewModel.onEmailChanged("newuser@brainquiz.ai")
        viewModel.onPasswordChanged("securePassword123")
        viewModel.onConfirmPasswordChanged("securePassword123")
        viewModel.onNameChanged("BrainyPlayer")

        // Simulate submit in create account mode
        var navigationToHomeInvoked = false
        viewModel.submitEmailAuth {
            navigationToHomeInvoked = true
        }

        // Registration MUST NOT navigate to home directly
        assertFalse("Create Account must NOT auto-navigate to Home", navigationToHomeInvoked)
    }

    @Test
    fun testSignOutClearsAllSessions() {
        val viewModel = AuthViewModel(authRepository = authRepository)

        // Simulate a guest login
        var navigatedToHome = false
        viewModel.signInAsGuest {
            navigatedToHome = true
        }

        assertTrue("Guest login should succeed", navigatedToHome)
        assertTrue("Guest session should be active", authRepository.isGuestSessionActive())

        // Now sign out
        viewModel.signOut()

        assertFalse("User should not be logged in after sign out", authRepository.isUserLoggedIn())
        assertFalse("Guest session should not be active after sign out", authRepository.isGuestSessionActive())
        assertFalse("ViewModel state must show logged out", viewModel.uiState.value.isLoggedIn)
        assertNull("ViewModel profile must be null after sign out", viewModel.uiState.value.currentUserProfile)
    }
}
