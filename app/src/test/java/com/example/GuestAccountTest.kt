package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.UserProfile
import com.example.data.UserProfileStore
import com.example.data.model.QuizResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GuestAccountTest {

    @Test
    fun testPermanentGuestAccountPreservationAcross100AppLaunches() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. First Launch - Guest account initialization
        val storeLaunch1 = UserProfileStore(context)
        val initialGuestId = storeLaunch1.getGuestId()
        assertNotNull("Guest ID should not be null", initialGuestId)
        assertTrue("Guest ID must start with guest_", initialGuestId.startsWith("guest_"))

        val profileLaunch1 = storeLaunch1.getProfile()
        assertEquals(initialGuestId, profileLaunch1.uid)

        // 2. Customizing user info & completing quizzes
        val customName = "BrainMaster"
        val customAvatar = "wizard"
        val updatedProfile = profileLaunch1.copy(
            name = customName,
            avatarId = customAvatar,
            xp = 500,
            coins = 150,
            streak = 3,
            longestStreak = 3,
            totalQuizzesPlayed = 2,
            totalQuestionsAnswered = 20,
            totalCorrectAnswers = 18,
            bestScore = 9,
            unlockedAchievements = listOf("ach_1", "ach_2"),
            quizHistory = listOf(
                QuizResult(
                    id = "1",
                    userId = initialGuestId,
                    categoryName = "Science",
                    scoreOutOfTen = 9,
                    xpEarned = 250,
                    coinsEarned = 75,
                    totalXp = 250,
                    timestamp = System.currentTimeMillis(),
                    dateFormatted = "Today"
                ),
                QuizResult(
                    id = "2",
                    userId = initialGuestId,
                    categoryName = "History",
                    scoreOutOfTen = 9,
                    xpEarned = 250,
                    coinsEarned = 75,
                    totalXp = 500,
                    timestamp = System.currentTimeMillis() - 1000,
                    dateFormatted = "Today"
                )
            )
        )
        storeLaunch1.saveProfile(updatedProfile)

        // Verify save succeeded
        val saved1 = storeLaunch1.getProfile()
        assertEquals(customName, saved1.name)
        assertEquals(customAvatar, saved1.avatarId)
        assertEquals(500, saved1.xp)
        assertEquals(150, saved1.coins)

        // 3. Simulate closing and reopening app 100 times
        for (i in 1..100) {
            val reopenedStore = UserProfileStore(context)
            val guestIdOnLaunchN = reopenedStore.getGuestId()

            assertEquals("Guest ID must remain identical on launch $i", initialGuestId, guestIdOnLaunchN)

            val profileOnLaunchN = reopenedStore.getProfile()
            assertEquals("Guest UID must match initial Guest ID on launch $i", initialGuestId, profileOnLaunchN.uid)
            assertEquals("Username must be preserved on launch $i", customName, profileOnLaunchN.name)
            assertEquals("Avatar must be preserved on launch $i", customAvatar, profileOnLaunchN.avatarId)
            assertEquals("XP must be preserved on launch $i", 500, profileOnLaunchN.xp)
            assertEquals("Coins must be preserved on launch $i", 150, profileOnLaunchN.coins)
            assertEquals("Streak must be preserved on launch $i", 3, profileOnLaunchN.streak)
            assertEquals("Quizzes played count preserved on launch $i", 2, profileOnLaunchN.totalQuizzesPlayed)
            assertEquals("Questions answered count preserved on launch $i", 20, profileOnLaunchN.totalQuestionsAnswered)
            assertEquals("Correct answers count preserved on launch $i", 18, profileOnLaunchN.totalCorrectAnswers)
            assertEquals("Best score preserved on launch $i", 9, profileOnLaunchN.bestScore)
            assertTrue("Achievements preserved on launch $i", profileOnLaunchN.unlockedAchievements.contains("ach_1"))
            assertEquals("Quiz history size preserved on launch $i", 2, profileOnLaunchN.quizHistory.size)
        }
    }
}
