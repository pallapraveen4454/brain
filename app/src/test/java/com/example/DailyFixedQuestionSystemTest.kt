package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.QuestionSelectionEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DailyFixedQuestionSystemTest {

    @Test
    fun testDailyFixedQuestionPersistenceAndSequence() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val selectionEngine = QuestionSelectionEngine(context)

        val categories = listOf("gk", "science", "sports", "history", "movies", "tech", "geo", "math")

        for (cat in categories) {
            // 1. Fetch questions first time on Day 1
            val day1Fetch1 = selectionEngine.getQuestionsForCategory(cat, requestedCount = 10)
            assertNotNull("Category $cat should return non-null questions", day1Fetch1)
            assertTrue("Category $cat should return at least 1 question", day1Fetch1.isNotEmpty())

            // 2. Fetch questions second time on Day 1 (same day, app restart simulation)
            val selectionEngineReopened = QuestionSelectionEngine(context)
            val day1Fetch2 = selectionEngineReopened.getQuestionsForCategory(cat, requestedCount = 10)

            // 3. Confirm exact same questions returned on same day
            assertEquals("Day 1 reopening must yield identical question count for $cat", day1Fetch1.size, day1Fetch2.size)
            val ids1 = day1Fetch1.map { it.id }
            val ids2 = day1Fetch2.map { it.id }
            assertEquals("Day 1 reopening must yield exact same question IDs for $cat", ids1, ids2)
            println("Category '$cat' Day 1 assigned IDs: $ids1")
        }
    }
}
