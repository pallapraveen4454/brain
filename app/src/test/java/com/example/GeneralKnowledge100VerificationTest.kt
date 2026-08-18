package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.CategoryDatabaseManager
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
class GeneralKnowledge100VerificationTest {

    @Test
    fun verifyGk051ToGk100ImportAndDatabaseCount() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val importManager = QuestionImportManager(context)
        val selectionEngine = QuestionSelectionEngine(context)

        // 1. Get all seed questions for General Knowledge (GK001 to GK100)
        val allGkSeeds = DefaultQuestionSeeds.getSeedsForCategory("gk")
        println("Total General Knowledge Seed Count: ${allGkSeeds.size}")
        assertTrue("Total GK Seeds should be at least 100", allGkSeeds.size >= 100)

        // Filter GK051 to GK100
        val newGkGroup = allGkSeeds.filter { q ->
            val num = q.id.filter { it.isDigit() }.toIntOrNull() ?: 0
            num in 51..100
        }
        assertEquals("GK051 to GK100 group should contain 50 questions", 50, newGkGroup.size)

        // 2. Validate every question in GK051 to GK100
        val validationErrors = mutableListOf<String>()
        for (q in newGkGroup) {
            val res = importManager.validateQuestion(q)
            if (!res.isValid) {
                validationErrors.add("Question ${q.id} invalid: ${res.error}")
            }
        }
        assertTrue("All GK051 to GK100 questions must be valid. Errors: $validationErrors", validationErrors.isEmpty())

        // Validate difficulty distribution
        val easyCount = newGkGroup.count { it.difficulty.equals("Easy", ignoreCase = true) }
        val mediumCount = newGkGroup.count { it.difficulty.equals("Medium", ignoreCase = true) }
        val hardCount = newGkGroup.count { it.difficulty.equals("Hard", ignoreCase = true) }

        println("GK051-GK100 Difficulty Breakdown: Easy=$easyCount, Medium=$mediumCount, Hard=$hardCount")
        assertTrue("All questions in GK051-GK100 should have a valid difficulty (Easy, Medium, or Hard)", (easyCount + mediumCount + hardCount) == 50)

        // 3. Import / Sync into category_gk.db
        val importResult = importManager.importQuestions(allGkSeeds)
        println("Import Result - Imported: ${importResult.importedCount}, Skipped: ${importResult.skippedDuplicatesCount}")

        // 4. Query total question count from category_gk.db
        val totalCountInDb = selectionEngine.getQuestionCountForCategory("gk")
        println("==================================================")
        println("CURRENT TOTAL GENERAL KNOWLEDGE QUESTIONS: $totalCountInDb")
        println("IMPORTED QUESTION IDs (GK051 to GK100):")
        val importedIds = newGkGroup.map { it.id }
        println(importedIds)
        println("==================================================")

        assertTrue("Total General Knowledge questions in DB should be at least 100", totalCountInDb >= 100)
    }
}
