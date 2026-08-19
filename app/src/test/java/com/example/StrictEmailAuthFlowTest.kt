package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AuthRepository
import com.example.data.UserProfile
import com.example.data.UserProfileStore
import com.example.viewmodel.AuthViewModel
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
