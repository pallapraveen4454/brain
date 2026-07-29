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
class GeneralKnowledge250VerificationTest {

    @Test
    fun verifyGk201ToGk250ImportAndDatabaseStatus() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val importManager = QuestionImportManager(context)
        val selectionEngine = QuestionSelectionEngine(context)
        val dbManager = CategoryDatabaseManager(context)

        // 1. Load all General Knowledge seed questions (GK001 to GK250)
        val allGkSeeds = DefaultQuestionSeeds.getSeedsForCategory("gk")
        println("Total General Knowledge Seeds Count: ${allGkSeeds.size}")
        assertEquals("Total GK Seeds should be exactly 250", 250, allGkSeeds.size)

        // Filter GK201 to GK250 group
        val newGkGroup = allGkSeeds.filter { q ->
            val num = q.id.filter { it.isDigit() }.toIntOrNull() ?: 0
            num in 201..250
        }
        assertEquals("GK201 to GK250 group must contain exactly 50 questions", 50, newGkGroup.size)

        // 2. Validate every question in GK201 to GK250
        val validationErrors = mutableListOf<String>()
        for (q in newGkGroup) {
            val res = importManager.validateQuestion(q)
            if (!res.isValid) {
                validationErrors.add("Question ${q.id} invalid: ${res.error}")
            }
        }
        assertTrue("All GK201 to GK250 questions must be valid. Errors: $validationErrors", validationErrors.isEmpty())

        // Validate difficulty distribution: Easy 20, Medium 20, Hard 10
        val easyCount = newGkGroup.count { it.difficulty.equals("Easy", ignoreCase = true) }
        val mediumCount = newGkGroup.count { it.difficulty.equals("Medium", ignoreCase = true) }
        val hardCount = newGkGroup.count { it.difficulty.equals("Hard", ignoreCase = true) }

        println("GK201-GK250 Difficulty Breakdown: Easy=$easyCount, Medium=$mediumCount, Hard=$hardCount")
        assertEquals("Easy questions count must be 20", 20, easyCount)
        assertEquals("Medium questions count must be 20", 20, mediumCount)
        assertEquals("Hard questions count must be 10", 10, hardCount)

        // Check for duplicate IDs or Questions across all 250 questions
        val allIds = allGkSeeds.map { it.id }
        assertEquals("All 250 IDs must be unique", allIds.size, allIds.toSet().size)

        val allQuestionsText = allGkSeeds.map { it.questionText.trim().lowercase() }
        assertEquals("All 250 question texts must be unique", allQuestionsText.size, allQuestionsText.toSet().size)

        // 3. Automatically import into category_gk.db
        val importResult = importManager.importQuestions(allGkSeeds)
        println("Import Result - Imported: ${importResult.importedCount}, Skipped: ${importResult.skippedDuplicatesCount}")

        // 4. Query total question count, First Question ID, and Last Question ID from category_gk.db
        val db = dbManager.getDatabaseForCategory("gk")
        val dao = db?.questionDao()
        val allQuestionsFromDb = dao?.getAllQuestions() ?: emptyList()

        val totalCountInDb = allQuestionsFromDb.size
        val firstQuestionId = allQuestionsFromDb.firstOrNull()?.id ?: "N/A"
        val lastQuestionId = allQuestionsFromDb.lastOrNull()?.id ?: "N/A"

        val importedIds = newGkGroup.map { it.id }

        println("==================================================")
        println("IMPORTED QUESTION IDs (GK201 to GK250):")
        println(importedIds)
        println("--------------------------------------------------")
        println("NUMBER OF IMPORTED QUESTIONS: ${importedIds.size}")
        println("CURRENT TOTAL GENERAL KNOWLEDGE QUESTION COUNT: $totalCountInDb")
        println("FIRST QUESTION ID: $firstQuestionId")
        println("LAST QUESTION ID: $lastQuestionId")
        println("==================================================")

        assertEquals("Total General Knowledge questions in DB should be 250", 250, totalCountInDb)
        assertEquals("First question ID should be GK001", "GK001", firstQuestionId)
        assertEquals("Last question ID should be GK250", "GK250", lastQuestionId)
    }
}
