package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.DefaultQuestionSeeds
import com.example.data.database.QuestionImportManager
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
class GeneralKnowledge200VerificationTest {

    @Test
    fun verifyGk151ToGk200ImportAndDatabaseCount() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val importManager = QuestionImportManager(context)
        val selectionEngine = QuestionSelectionEngine(context)

        // 1. Load all General Knowledge seed questions (GK001 to GK200)
        val allGkSeeds = DefaultQuestionSeeds.getSeedsForCategory("gk")
        println("Total General Knowledge Seeds Count: ${allGkSeeds.size}")
        assertTrue("Total GK Seeds should be at least 200", allGkSeeds.size >= 200)

        // Filter GK151 to GK200 group
        val newGkGroup = allGkSeeds.filter { q ->
            val num = q.id.filter { it.isDigit() }.toIntOrNull() ?: 0
            num in 151..200
        }
        assertEquals("GK151 to GK200 group must contain exactly 50 questions", 50, newGkGroup.size)

        // 2. Validate every question in GK151 to GK200
        val validationErrors = mutableListOf<String>()
        for (q in newGkGroup) {
            val res = importManager.validateQuestion(q)
            if (!res.isValid) {
                validationErrors.add("Question ${q.id} invalid: ${res.error}")
            }
        }
        assertTrue("All GK151 to GK200 questions must be valid. Errors: $validationErrors", validationErrors.isEmpty())

        // Validate difficulty distribution: Easy 20, Medium 20, Hard 10
        val easyCount = newGkGroup.count { it.difficulty.equals("Easy", ignoreCase = true) }
        val mediumCount = newGkGroup.count { it.difficulty.equals("Medium", ignoreCase = true) }
        val hardCount = newGkGroup.count { it.difficulty.equals("Hard", ignoreCase = true) }

        println("GK151-GK200 Difficulty Breakdown: Easy=$easyCount, Medium=$mediumCount, Hard=$hardCount")
        assertEquals("Easy questions count must be 20", 20, easyCount)
        assertEquals("Medium questions count must be 20", 20, mediumCount)
        assertEquals("Hard questions count must be 10", 10, hardCount)

        // Check for duplicate IDs or Questions
        val ids = newGkGroup.map { it.id }
        assertEquals("IDs in GK151-GK200 must be unique", ids.size, ids.toSet().size)

        val questions = newGkGroup.map { it.questionText }
        assertEquals("Question texts in GK151-GK200 must be unique", questions.size, questions.toSet().size)

        // 3. Import / Sync into category_gk.db
        val importResult = importManager.importQuestions(allGkSeeds)
        println("Import Result - Imported: ${importResult.importedCount}, Skipped: ${importResult.skippedDuplicatesCount}")

        // 4. Query total question count from category_gk.db
        val totalCountInDb = selectionEngine.getQuestionCountForCategory("gk")

        println("==================================================")
        println("IMPORTED QUESTION IDs (GK151 to GK200):")
        println(ids)
        println("--------------------------------------------------")
        println("CURRENT TOTAL GENERAL KNOWLEDGE QUESTIONS: $totalCountInDb")
        println("==================================================")

        assertTrue("Total General Knowledge questions in DB should be at least 200", totalCountInDb >= 200)
    }
}
