package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.QuestionSelectionEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DailyFixedQuestionRotation30DayTest {

    @Test
    fun testInstallDateBased30DayRotation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val selectionEngine = QuestionSelectionEngine(context)

        // Clear preferences for clean test environment
        context.getSharedPreferences("daily_quiz_prefs", Context.MODE_PRIVATE).edit().clear().commit()

        val installDateStr = "2026-08-01"
        val savedInstallDate = selectionEngine.getOrInitInstallDate(installDateStr)
        assertEquals("Install date must be permanently saved as $installDateStr", installDateStr, savedInstallDate)

        val categoriesToTest = listOf("gk", "science")

        for (cat in categoriesToTest) {
            val totalQuestions = selectionEngine.getQuestionCountForCategory(cat)
            assertEquals("Category $cat must have 300 total questions", 300, totalQuestions)

            val servedIdsPerDay = mutableListOf<List<String>>()

            // Simulate Day 1 to Day 30 starting from install date 2026-08-01
            for (dayOffset in 0 until 30) {
                val dayNumber = dayOffset + 1
                val dateStr = String.format("2026-08-%02d", dayNumber)
                
                val calculatedDayNum = selectionEngine.getCalculatedDayNumber(dateStr)
                assertEquals("Calculated Day Number for $dateStr", dayNumber, calculatedDayNum)

                val questions = selectionEngine.getQuestionsForCategory(cat, requestedCount = 10, dateOverride = dateStr)
                assertEquals("Day $dayNumber for $cat must return exactly 10 questions", 10, questions.size)

                val ids = questions.map { it.id }
                servedIdsPerDay.add(ids)

                val startIdx = dayOffset * 10
                val prefix = if (cat == "gk") "GK" else "SCI"
                val expectedFirstId = String.format("%s%03d", prefix, startIdx + 1)
                val expectedLastId = String.format("%s%03d", prefix, startIdx + 10)

                assertEquals("Day $dayNumber first question ID for $cat", expectedFirstId, ids.first())
                assertEquals("Day $dayNumber last question ID for $cat", expectedLastId, ids.last())
            }

            // Verify no duplicate sets and full coverage of 300 questions
            val all300Ids = servedIdsPerDay.flatten()
            assertEquals("30 days x 10 questions = 300 total served questions", 300, all300Ids.size)
            val unique300Ids = all300Ids.toSet()
            assertEquals("All 300 served questions over 30 days must be unique", 300, unique300Ids.size)

            // Day 31 (2026-08-31) -> Must wrap around to Day 1 questions (001-010)
            val day31Num = selectionEngine.getCalculatedDayNumber("2026-08-31")
            assertEquals("Calculated Day Number for 2026-08-31", 31, day31Num)
            val day31Questions = selectionEngine.getQuestionsForCategory(cat, requestedCount = 10, dateOverride = "2026-08-31")
            assertEquals("Day 31 questions must equal Day 1 questions", servedIdsPerDay[0], day31Questions.map { it.id })
        }
    }
}
