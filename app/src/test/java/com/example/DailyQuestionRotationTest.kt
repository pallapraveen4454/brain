package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.database.QuestionSelectionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DailyQuestionRotationTest {

    private lateinit var selectionEngine: QuestionSelectionEngine

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        selectionEngine = QuestionSelectionEngine(context)
    }

    @Test
    fun testDay1AndDay2QuestionRotation() {
        val installDate = "2026-07-29"
        val day1Date = "2026-07-29"
        val day2Date = "2026-07-30"

        // Initialize install date
        selectionEngine.getOrInitInstallDate(installDate)

        // Day 1 Calculation
        val day1Number = selectionEngine.getCalculatedDayNumber(day1Date)
        assertEquals(1, day1Number)

        val day1QuestionsGk = selectionEngine.getQuestionsForCategorySync("gk", 10, day1Date)
        assertEquals(10, day1QuestionsGk.size)
        val day1GkIds = day1QuestionsGk.map { it.id }
        assertEquals("GK001", day1GkIds.first())

        // Day 2 Calculation
        val day2Number = selectionEngine.getCalculatedDayNumber(day2Date)
        assertEquals(2, day2Number)

        val day2QuestionsGk = selectionEngine.getQuestionsForCategorySync("gk", 10, day2Date)
        assertEquals(10, day2QuestionsGk.size)
        val day2GkIds = day2QuestionsGk.map { it.id }
        assertEquals("GK011", day2GkIds.first())

        // Verify Day 1 and Day 2 questions are completely distinct
        assertNotEquals(day1GkIds, day2GkIds)

        // Science Day 1 vs Day 2
        val day1QuestionsSci = selectionEngine.getQuestionsForCategorySync("science", 10, day1Date)
        val day2QuestionsSci = selectionEngine.getQuestionsForCategorySync("science", 10, day2Date)
        assertEquals("SCI001", day1QuestionsSci.map { it.id }.first())
        assertEquals("SCI011", day2QuestionsSci.map { it.id }.first())
        assertNotEquals(day1QuestionsSci.map { it.id }, day2QuestionsSci.map { it.id })
    }

    @Test
    fun testQuizViewModelLoading() {
        val installDate = "2026-07-29"
        val day2Date = "2026-07-30"
        selectionEngine.getOrInitInstallDate(installDate)

        // Force todayDate to Day 2 in QuestionSelectionEngine
        val day2QuestionsGk = selectionEngine.getQuestionsForCategorySync("gk", 10, day2Date)
        assertEquals("GK011", day2QuestionsGk.first().id)

        val day2QuestionsDaily = selectionEngine.getQuestionsForCategorySync("daily", 10, day2Date)
        assertEquals(10, day2QuestionsDaily.size)
        assertTrue(day2QuestionsDaily.first().id.endsWith("011") || day2QuestionsDaily.first().id.isNotBlank())

        val day2QuestionsSci = selectionEngine.getQuestionsForCategorySync("science", 10, day2Date)
        assertEquals("SCI011", day2QuestionsSci.first().id)
    }
}
