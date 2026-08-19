package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AchievementRepository
import com.example.data.AuthRepository
import com.example.data.UserProfile
import com.example.data.UserProfileStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthenticationAndAchievementIsolationTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        // Clear preferences to simulate fresh install
        context.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun testFreshInstallDoesNotAutoLoginToGuest() {
        val userProfileStore = UserProfileStore(context)
        val authRepository = AuthRepository(context = context, userProfileStore = userProfileStore)

        // 1. Fresh install: User is NOT logged in and Guest is NOT active
        assertFalse("Guest session must not be active on fresh install", authRepository.isGuestSessionActive())
        assertFalse("User must not be logged in on fresh install", authRepository.isUserLoggedIn())
        assertFalse("No saved user session on fresh install", authRepository.hasSavedUserSession())
    }

    @Test
    fun testExplicitGuestSignInInitializesClean0Of21Achievements() {
        val userProfileStore = UserProfileStore(context)
        val authRepository = AuthRepository(context = context, userProfileStore = userProfileStore)
        val achievementRepo = AchievementRepository(context = context, userProfileStore = userProfileStore)

        // 1. Explicit guest sign in
        val guestProfile = userProfileStore.createOrGetGuestProfile()
        authRepository.setGuestSessionActive(true)

        assertTrue("Guest session must now be active", authRepository.isGuestSessionActive())
        assertTrue("User must now be considered logged in", authRepository.isUserLoggedIn())
        assertEquals("Guest", guestProfile.name)
        assertEquals("Guest Account", guestProfile.email)
        assertEquals(0, guestProfile.xp)
        assertEquals(1, guestProfile.level)
        assertEquals(0, guestProfile.coins)
        assertEquals(0, guestProfile.streak)

        // 2. Verify all achievements are locked (0/21)
        val achievements = achievementRepo.getAllAchievements(
            totalXp = guestProfile.xp,
            totalCoins = guestProfile.coins,
            currentStreak = guestProfile.streak
        )
        assertEquals(21, achievements.size)
        val unlockedCount = achievements.count { it.isUnlocked }
        assertEquals("New Guest must have 0/21 unlocked achievements", 0, unlockedCount)
    }

    @Test
    fun testAchievementIsolationBetweenGuestAndAuthenticatedAccounts() {
        val userProfileStore = UserProfileStore(context)
        val authRepository = AuthRepository(context = context, userProfileStore = userProfileStore)
        val achievementRepo = AchievementRepository(context = context, userProfileStore = userProfileStore)

        // 1. Guest signs in and completes 1 quiz with 10/10 perfect score
        userProfileStore.createOrGetGuestProfile()
        authRepository.setGuestSessionActive(true)

        achievementRepo.recordQuizCompletion(
            scoreOutOfTen = 10,
            questionCount = 10,
            isAiCustom = false,
            categoryId = "science"
        )
        val updatedGuestProfile = userProfileStore.saveProfile(
            userProfileStore.getProfile().copy(
                xp = 250,
                coins = 70,
                totalQuizzesPlayed = 1,
                totalQuestionsAnswered = 10,
                totalCorrectAnswers = 10,
                bestScore = 10
            )
        )

        // Check guest unlocked achievements
        val guestAchievements = achievementRepo.getAllAchievements(
            totalXp = updatedGuestProfile.xp,
            totalCoins = updatedGuestProfile.coins,
            currentStreak = updatedGuestProfile.streak
        )
        val guestUnlockedFirstStep = guestAchievements.find { it.id == "first_step" }?.isUnlocked ?: false
        val guestUnlockedPerfect10 = guestAchievements.find { it.id == "perfect_10" }?.isUnlocked ?: false
        assertTrue("Guest should unlock First Step after 1 quiz", guestUnlockedFirstStep)
        assertTrue("Guest should unlock Perfect 10 after 10/10 score", guestUnlockedPerfect10)

        // 2. User logs out
        authRepository.signOut()
        assertFalse("Guest session must be inactive after logout", authRepository.isGuestSessionActive())
        assertFalse("User must not be logged in after logout", authRepository.isUserLoggedIn())

        // 3. User logs in with a NEW Google/Email Account
        val newAuthProfile = authRepository.createOrGetLocalEmailProfile("newuser@example.com", "New User")
        assertEquals(0, newAuthProfile.xp)
        assertEquals(1, newAuthProfile.level)
        assertEquals(0, newAuthProfile.coins)
        assertEquals(0, newAuthProfile.streak)
        assertEquals(0, newAuthProfile.totalQuizzesPlayed)

        // 4. Verify the new user sees 0/21 unlocked achievements (NO LEAKAGE FROM GUEST!)
        val newUserAchievements = achievementRepo.getAllAchievements(
            totalXp = newAuthProfile.xp,
            totalCoins = newAuthProfile.coins,
            currentStreak = newAuthProfile.streak
        )
        assertEquals(21, newUserAchievements.size)
        val newUserUnlockedCount = newUserAchievements.count { it.isUnlocked }
        assertEquals("New authenticated user must have 0/21 achievements unlocked (no leak from guest)", 0, newUserUnlockedCount)

        val newFirstStep = newUserAchievements.find { it.id == "first_step" }?.isUnlocked ?: false
        val newPerfect10 = newUserAchievements.find { it.id == "perfect_10" }?.isUnlocked ?: false
        assertFalse("New user must NOT have First Step unlocked", newFirstStep)
        assertFalse("New user must NOT have Perfect 10 unlocked", newPerfect10)
    }

    @Test
    fun testSignOutClearsSessionAndReturnsToUnauthenticatedState() {
        val userProfileStore = UserProfileStore(context)
        val authRepository = AuthRepository(context = context, userProfileStore = userProfileStore)

        // Sign in as guest
        userProfileStore.createOrGetGuestProfile()
        authRepository.setGuestSessionActive(true)
        assertTrue(authRepository.isUserLoggedIn())

        // Sign out
        authRepository.signOut()
        assertFalse("isGuestSessionActive must be false after signOut", authRepository.isGuestSessionActive())
        assertFalse("isUserLoggedIn must be false after signOut", authRepository.isUserLoggedIn())
        assertFalse("hasSavedUserSession must be false after signOut", authRepository.hasSavedUserSession())
    }
}
