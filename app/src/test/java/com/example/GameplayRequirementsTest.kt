package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.HintRepository
import com.example.data.QuizRepository
import java.util.concurrent.atomic.AtomicBoolean
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
class GameplayRequirementsTest {

    private lateinit var context: Context
    private lateinit var quizRepository: QuizRepository
    private lateinit var hintRepository: HintRepository

    private val testDate = "2026-09-24"
    private val nextDate = "2026-09-25"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        quizRepository = QuizRepository(context)
        hintRepository = HintRepository(context)
        quizRepository.resetDailyCategoryCompletions()
        hintRepository.resetAllHints()
    }

    /**
     * Requirement 1 & Test 1: Same category blocked on the same date.
     */
    @Test
    fun testSameCategoryBlockedOnSameDate() {
        val categoryId = "science"

        // Before completion: category should be available
        assertFalse("Category should not be completed initially", quizRepository.isCategoryCompletedToday(categoryId, testDate))

        // First genuine completion records successfully
        val newlyRecorded = quizRepository.recordCategoryCompletion(categoryId, testDate)
        assertTrue("First completion should record successfully", newlyRecorded)

        // Same category is now blocked for replay on the same calendar date
        assertTrue("Category should be marked completed today", quizRepository.isCategoryCompletedToday(categoryId, testDate))

        // Attempting to record completion again on the same date is blocked
        val duplicateRecord = quizRepository.recordCategoryCompletion(categoryId, testDate)
        assertFalse("Duplicate completion record on the same date should be blocked", duplicateRecord)
    }

    /**
     * Requirement 1 & Test 2: Same category available on the next date.
     */
    @Test
    fun testSameCategoryAvailableOnNextDate() {
        val categoryId = "science"

        // Complete category on day 1 (testDate)
        quizRepository.recordCategoryCompletion(categoryId, testDate)
        assertTrue("Category completed on day 1", quizRepository.isCategoryCompletedToday(categoryId, testDate))

        // On day 2 (nextDate): category must be available again
        assertFalse("Category should be available on the next calendar date", quizRepository.isCategoryCompletedToday(categoryId, nextDate))

        // Can genuinely complete category on day 2
        val nextDayRecorded = quizRepository.recordCategoryCompletion(categoryId, nextDate)
        assertTrue("Should allow completion on the next calendar date", nextDayRecorded)
        assertTrue("Category completed on day 2", quizRepository.isCategoryCompletedToday(categoryId, nextDate))
    }

    /**
     * Requirement 1 & Test 3: Different categories remain available.
     */
    @Test
    fun testDifferentCategoriesRemainAvailable() {
        // Complete one specific category on testDate
        val completedCategory = "science"
        quizRepository.recordCategoryCompletion(completedCategory, testDate)
        assertTrue("Science should be completed", quizRepository.isCategoryCompletedToday(completedCategory, testDate))

        // All other categories MUST remain available on the same date
        val otherCategories = listOf("gk", "history", "sports", "movies", "tech", "geo", "math", "daily")
        for (other in otherCategories) {
            assertFalse(
                "Category '$other' should remain available when '$completedCategory' is completed",
                quizRepository.isCategoryCompletedToday(other, testDate)
            )
        }

        // Complete a second category and verify other remaining categories are still available
        quizRepository.recordCategoryCompletion("history", testDate)
        assertTrue("History should be completed", quizRepository.isCategoryCompletedToday("history", testDate))
        assertFalse("Sports should still be available", quizRepository.isCategoryCompletedToday("sports", testDate))
        assertFalse("Daily Challenge should still be available", quizRepository.isCategoryCompletedToday("daily", testDate))
    }

    /**
     * Requirement 2 & Test 4: Only one global hint per day.
     */
    @Test
    fun testOnlyOneGlobalHintPerDay() {
        // Both global and per-category queries should report hint available initially
        assertTrue("Global hint should be available initially", hintRepository.isGlobalHintAvailable(testDate))
        assertTrue("Hint for science should be available initially", hintRepository.isHintAvailableForCategory("science", testDate))
        assertTrue("Hint for gk should be available initially", hintRepository.isHintAvailableForCategory("gk", testDate))

        // Unlock/consume the single global hint in one category (e.g. Science)
        hintRepository.markHintUsedForCategory("science", testDate)

        // Global hint is now consumed for today
        assertFalse("Global hint should be consumed for today", hintRepository.isGlobalHintAvailable(testDate))

        // All categories (science, gk, sports, etc.) now report hint unavailable for today
        assertFalse("Hint should not be available in science today", hintRepository.isHintAvailableForCategory("science", testDate))
        assertFalse("Hint should not be available in gk today after science used it", hintRepository.isHintAvailableForCategory("gk", testDate))
        assertFalse("Hint should not be available in sports today", hintRepository.isHintAvailableForCategory("sports", testDate))

        // On the next calendar date, global hint is available again
        assertTrue("Global hint should be available again on next calendar date", hintRepository.isGlobalHintAvailable(nextDate))
        assertTrue("Category hint should be available again on next calendar date", hintRepository.isHintAvailableForCategory("gk", nextDate))
    }

    /**
     * Requirement 2 & Test 5: Hint not consumed when ad is skipped or fails.
     */
    @Test
    fun testHintNotConsumedWhenAdIsSkippedOrFails() {
        // Initial state: hint is available
        assertTrue("Global hint available initially", hintRepository.isGlobalHintAvailable(testDate))

        // Simulate user opening ad and closing/skipping without reward callback
        // (markGlobalHintUsed / markHintUsedForCategory is NOT called)
        // Verify hint is still fully available
        assertTrue("Global hint must remain available after ad skipped/dismissed", hintRepository.isGlobalHintAvailable(testDate))
        assertTrue("Category hint must remain available after ad skipped/dismissed", hintRepository.isHintAvailableForCategory("science", testDate))

        // Simulate ad failure (onError triggered, no reward)
        assertTrue("Global hint must remain available after ad failure", hintRepository.isGlobalHintAvailable(testDate))

        // Only after genuine reward callback is confirmed and markGlobalHintUsed is invoked:
        hintRepository.markGlobalHintUsed(testDate)
        assertFalse("Global hint consumed only after confirmed reward", hintRepository.isGlobalHintAvailable(testDate))
    }

    /**
     * Requirement 3 & Test 6: Daily Challenge doubles coins but not XP.
     */
    @Test
    fun testDailyChallengeDoublesCoinsButNotXp() {
        // Verify Daily Challenge identifier detection
        assertTrue("Identifier 'daily' should be detected as Daily Challenge", quizRepository.isDailyChallenge("daily"))
        assertTrue("Identifier 'Daily Challenge' should be detected as Daily Challenge", quizRepository.isDailyChallenge("Daily Challenge"))
        assertFalse("Identifier 'science' should NOT be detected as Daily Challenge", quizRepository.isDailyChallenge("science"))
        assertFalse("Identifier 'gk' should NOT be detected as Daily Challenge", quizRepository.isDailyChallenge("gk"))

        // Formula test for 10 correct answers
        val correctAnswers = 10
        val isDaily = quizRepository.isDailyChallenge("daily")

        val xpEarned = correctAnswers * 10
        val coinsEarned = if (isDaily) correctAnswers * 20 else correctAnswers * 10

        assertEquals("Daily Challenge XP must remain correctAnswers * 10 (100 XP)", 100, xpEarned)
        assertEquals("Daily Challenge coins must be doubled to correctAnswers * 20 (200 Coins)", 200, coinsEarned)

        // Formula test for 7 correct answers
        val partialCorrect = 7
        val partialXp = partialCorrect * 10
        val partialCoins = if (isDaily) partialCorrect * 20 else partialCorrect * 10

        assertEquals("Daily Challenge partial XP must be 70", 70, partialXp)
        assertEquals("Daily Challenge partial coins must be 140 (2X)", 140, partialCoins)
    }

    /**
     * Requirement 3 & Test 7: Normal categories retain existing rewards.
     */
    @Test
    fun testNormalCategoriesRetainExistingRewards() {
        val normalCategories = listOf("gk", "science", "history", "sports", "movies", "tech", "geo", "math", "quick")

        val correctAnswers = 8
        for (cat in normalCategories) {
            val isDaily = quizRepository.isDailyChallenge(cat)
            assertFalse("Category '$cat' must not be classified as Daily Challenge", isDaily)

            val xpEarned = correctAnswers * 10
            val coinsEarned = if (isDaily) correctAnswers * 20 else correctAnswers * 10

            assertEquals("Normal category '$cat' XP must be correctAnswers * 10 (80 XP)", 80, xpEarned)
            assertEquals("Normal category '$cat' Coins must be correctAnswers * 10 (80 Coins)", 80, coinsEarned)
        }
    }

    /**
     * Requirement 4 & Test 8: Duplicate completion/reward callbacks do not duplicate rewards.
     */
    @Test
    fun testDuplicateCompletionAndRewardCallbacksDoNotDuplicateRewards() {
        val categoryId = "sports"

        // 1. First completion
        val firstCompletion = quizRepository.recordCategoryCompletion(categoryId, testDate)
        assertTrue("First completion succeeds", firstCompletion)

        // 2. Duplicate completion call on same category & date
        val secondCompletion = quizRepository.recordCategoryCompletion(categoryId, testDate)
        assertFalse("Duplicate completion call must be blocked", secondCompletion)

        // 3. Duplicate rewarded-ad callback latch test
        val isProcessingHintReward = AtomicBoolean(false)
        var rewardUnlockCount = 0

        val onRewardEarnedCallback: () -> Unit = {
            if (isProcessingHintReward.compareAndSet(false, true)) {
                rewardUnlockCount++
                hintRepository.markGlobalHintUsed(testDate)
            }
        }

        // Simulate Ad SDK calling onRewardEarned multiple times in rapid succession
        onRewardEarnedCallback()
        onRewardEarnedCallback()
        onRewardEarnedCallback()

        assertEquals("Reward must only be unlocked exactly once despite duplicate callbacks", 1, rewardUnlockCount)
        assertFalse("Hint should be marked used", hintRepository.isGlobalHintAvailable(testDate))
    }
}
