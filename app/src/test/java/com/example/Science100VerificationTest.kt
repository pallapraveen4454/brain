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
class Science100VerificationTest {

    @Test
    fun verifyScience001To100ImportAndDatabaseCount() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("category_science.db")

        val importManager = QuestionImportManager(context)
        val selectionEngine = QuestionSelectionEngine(context)
        val dbManager = CategoryDatabaseManager(context)

        // 1. Get Science seed questions (SCI001 to SCI100)
        val scienceSeeds = DefaultQuestionSeeds.getSeedsForCategory("science")
        println("Total Science Seed Count: ${scienceSeeds.size}")
        assertEquals("Total Science seeds should be exactly 100", 100, scienceSeeds.size)

        // 2. Validate difficulty distribution: Easy 40, Medium 40, Hard 20
        val easyCount = scienceSeeds.count { it.difficulty.equals("Easy", ignoreCase = true) }
        val mediumCount = scienceSeeds.count { it.difficulty.equals("Medium", ignoreCase = true) }
        val hardCount = scienceSeeds.count { it.difficulty.equals("Hard", ignoreCase = true) }

        println("Science Difficulty Breakdown: Easy=$easyCount, Medium=$mediumCount, Hard=$hardCount")
        assertEquals("Easy questions count must be 40", 40, easyCount)
        assertEquals("Medium questions count must be 40", 40, mediumCount)
        assertEquals("Hard questions count must be 20", 20, hardCount)

        // 3. Check for unique IDs (SCI001 to SCI100)
        val allIds = scienceSeeds.map { it.id }
        val duplicateIds = allIds.groupBy { it }.filter { it.value.size > 1 }.keys
        assertEquals("All 100 IDs must be unique. Duplicates: $duplicateIds", 100, allIds.toSet().size)

        // 4. Check for unique question texts
        val textGroupMap = scienceSeeds.groupBy { it.questionText.trim().lowercase() }
        val duplicateTexts = textGroupMap.filter { it.value.size > 1 }
        assertTrue("No duplicate question texts allowed. Duplicates: ${duplicateTexts.keys}", duplicateTexts.isEmpty())

        // 5. Validate every single question using QuestionImportManager
        val validationErrors = mutableListOf<String>()
        for (q in scienceSeeds) {
            val res = importManager.validateQuestion(q)
            if (!res.isValid) {
                validationErrors.add("Question ${q.id} invalid: ${res.error}")
            }
        }
        assertTrue("All 100 Science questions must be valid. Errors: $validationErrors", validationErrors.isEmpty())

        // 6. Import questions into category_science.db
        val importResult = importManager.importQuestions(scienceSeeds)
        println("Import Result - Imported: ${importResult.importedCount}, Skipped: ${importResult.skippedDuplicatesCount}")

        // 7. Verify category_science.db database
        val db = dbManager.getDatabaseForCategory("science")
        val dao = db?.questionDao()
        val allQuestionsFromDb = dao?.getAllQuestions() ?: emptyList()

        val totalCountInDb = allQuestionsFromDb.size
        val firstQuestionId = allQuestionsFromDb.firstOrNull()?.id ?: "N/A"
        val lastQuestionId = allQuestionsFromDb.lastOrNull()?.id ?: "N/A"

        println("==================================================")
        println("SCIENCE DATABASE STATUS:")
        println("TOTAL SCIENCE QUESTIONS: $totalCountInDb")
        println("FIRST QUESTION ID: $firstQuestionId")
        println("LAST QUESTION ID: $lastQuestionId")
        println("==================================================")

        assertEquals("Total Science questions in DB should be 100", 100, totalCountInDb)
        assertEquals("First question ID should be SCI001", "SCI001", firstQuestionId)
        assertEquals("Last question ID should be SCI100", "SCI100", lastQuestionId)

        // 8. Test Daily Fixed Question System for Science (10 questions per day)
        val dailyQuestions = selectionEngine.getQuestionsForCategory("science", 10)
        assertEquals("Daily fixed question count for Science must be 10", 10, dailyQuestions.size)
        assertEquals("First daily question ID should be SCI001", "SCI001", dailyQuestions.first().id)
    }
}
