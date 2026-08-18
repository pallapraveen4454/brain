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
class GeneralKnowledge300VerificationTest {

    @Test
    fun verifyGk251ToGk300ImportAndFull300Validation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("category_gk.db")

        val importManager = QuestionImportManager(context)
        val selectionEngine = QuestionSelectionEngine(context)
        val dbManager = CategoryDatabaseManager(context)

        // 1. Load all General Knowledge seed questions (GK001 to GK300)
        val allGkSeeds = DefaultQuestionSeeds.getSeedsForCategory("gk")
        println("Total General Knowledge Seeds Count: ${allGkSeeds.size}")
        assertTrue("Total GK Seeds should be at least 300", allGkSeeds.size >= 300)

        // Filter GK251 to GK300 group
        val newGkGroup = allGkSeeds.filter { q ->
            val num = q.id.filter { it.isDigit() }.toIntOrNull() ?: 0
            num in 251..300
        }
        assertEquals("GK251 to GK300 group must contain exactly 50 questions", 50, newGkGroup.size)

        // 2. Validate every question in ALL 300 questions
        val validationErrors = mutableListOf<String>()
        for (q in allGkSeeds) {
            val res = importManager.validateQuestion(q)
            if (!res.isValid) {
                validationErrors.add("Question ${q.id} invalid: ${res.error}")
            }
        }
        assertTrue("All 300 GK questions must be valid. Errors: $validationErrors", validationErrors.isEmpty())

        // Validate difficulty distribution for new group GK251-GK300: Easy 20, Medium 20, Hard 10
        val easyCount = newGkGroup.count { it.difficulty.equals("Easy", ignoreCase = true) }
        val mediumCount = newGkGroup.count { it.difficulty.equals("Medium", ignoreCase = true) }
        val hardCount = newGkGroup.count { it.difficulty.equals("Hard", ignoreCase = true) }

        println("GK251-GK300 Difficulty Breakdown: Easy=$easyCount, Medium=$mediumCount, Hard=$hardCount")
        assertEquals("Easy questions count must be 20", 20, easyCount)
        assertEquals("Medium questions count must be 20", 20, mediumCount)
        assertEquals("Hard questions count must be 10", 10, hardCount)

        // Check for duplicate IDs across all 300 questions
        val allIds = allGkSeeds.map { it.id }
        val duplicateIds = allIds.groupBy { it }.filter { it.value.size > 1 }.keys
        println("Duplicate IDs found: $duplicateIds")
        assertEquals("All 300 IDs must be unique. Duplicates: $duplicateIds", 300, allIds.toSet().size)

        // Check for duplicate question text across all 300 questions
        val textGroupMap = allGkSeeds.groupBy { it.questionText.trim().lowercase() }
        val duplicateTexts = textGroupMap.filter { it.value.size > 1 }
        for ((text, list) in duplicateTexts) {
            println("DUPLICATE QUESTION TEXT: '$text' in IDs: ${list.map { it.id }}")
        }
        assertTrue("No duplicate question texts allowed. Duplicates: ${duplicateTexts.keys}", duplicateTexts.isEmpty())


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
        println("IMPORTED IDs (GK251 to GK300):")
        println(importedIds.first() + " to " + importedIds.last())
        println("TOTAL QUESTIONS: $totalCountInDb")
        println("FIRST ID: $firstQuestionId")
        println("LAST ID: $lastQuestionId")
        println("==================================================")

        assertTrue("Total General Knowledge questions in DB should be at least 300", totalCountInDb >= 300)
        assertEquals("First question ID should be GK001", "GK001", firstQuestionId)
        assertTrue("Last question ID should start with GK", lastQuestionId.startsWith("GK"))
    }
}
