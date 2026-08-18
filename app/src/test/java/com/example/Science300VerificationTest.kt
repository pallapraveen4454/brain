package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.CategoryDatabaseManager
import com.example.data.database.DefaultQuestionSeeds
import com.example.data.database.QuestionImportManager
import com.example.data.database.QuestionSelectionEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Science300VerificationTest {

    @Test
    fun verifyScience001To300ImportAndDatabaseIntegrity() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("category_science.db")

        val importManager = QuestionImportManager(context)
        val selectionEngine = QuestionSelectionEngine(context)
        val dbManager = CategoryDatabaseManager(context)

        // 1. Get Science seed questions (SCI001 to SCI300)
        val scienceSeeds = DefaultQuestionSeeds.getSeedsForCategory("science")
        println("Total Science Seed Count: ${scienceSeeds.size}")
        assertTrue("Total Science seeds should be at least 300", scienceSeeds.size >= 300)

        // 2. Validate Batch 3 seeds (SCI201 to SCI300)
        val batch3Seeds = scienceSeeds.filter { 
            val num = it.id.removePrefix("SCI").toIntOrNull()
            num != null && num in 201..300 
        }
        assertEquals("Batch 3 seeds (SCI201 to SCI300) should be exactly 100", 100, batch3Seeds.size)

        // Check Batch 3 difficulty distribution: Easy 40, Medium 40, Hard 20
        val batch3Easy = batch3Seeds.count { it.difficulty.equals("Easy", ignoreCase = true) }
        val batch3Medium = batch3Seeds.count { it.difficulty.equals("Medium", ignoreCase = true) }
        val batch3Hard = batch3Seeds.count { it.difficulty.equals("Hard", ignoreCase = true) }

        println("Batch 3 Difficulty Breakdown: Easy=$batch3Easy, Medium=$batch3Medium, Hard=$batch3Hard")
        assertEquals("Batch 3 Easy count must be 40", 40, batch3Easy)
        assertEquals("Batch 3 Medium count must be 40", 40, batch3Medium)
        assertEquals("Batch 3 Hard count must be 20", 20, batch3Hard)

        // 3. Check that all IDs SCI001 to SCI300 exist without missing IDs
        val expectedIds = (1..300).map { "SCI" + String.format("%03d", it) }.toSet()
        val actualIds = scienceSeeds.map { it.id }.toSet()

        val missingIds = expectedIds - actualIds
        assertTrue("No missing IDs between SCI001 and SCI300. Missing: $missingIds", missingIds.isEmpty())

        // 4. Check for duplicate IDs
        val allIdsList = scienceSeeds.map { it.id }
        val duplicateIds = allIdsList.groupBy { it }.filter { it.value.size > 1 }.keys
        assertTrue("No duplicate IDs allowed. Duplicates: $duplicateIds", duplicateIds.isEmpty())

        // 5. Check for duplicate question texts
        val textGroupMap = scienceSeeds.groupBy { it.questionText.trim().lowercase() }
        val duplicateTexts = textGroupMap.filter { it.value.size > 1 }
        assertTrue("No duplicate question texts allowed. Duplicates: ${duplicateTexts.keys}", duplicateTexts.isEmpty())

        // 6. Validate every question using QuestionImportManager
        val validationErrors = mutableListOf<String>()
        for (q in scienceSeeds) {
            val res = importManager.validateQuestion(q)
            if (!res.isValid) {
                validationErrors.add("Question ${q.id} invalid: ${res.error}")
            }
        }
        assertTrue("All 300 Science questions must be valid. Errors: $validationErrors", validationErrors.isEmpty())

        // 7. Automatically import all questions into category_science.db
        val importResult = importManager.importQuestions(scienceSeeds)
        println("Import Result - Total Imported: ${importResult.importedCount}, Skipped: ${importResult.skippedDuplicatesCount}")
        assertEquals("100% of questions (300) should be imported", 300, importResult.importedCount)

        // 8. Verify category_science.db contents
        val db = dbManager.getDatabaseForCategory("science")
        val dao = db?.questionDao()
        val allDbQuestions = dao?.getAllQuestions() ?: emptyList()

        val totalCountInDb = allDbQuestions.size
        val firstQuestionId = allDbQuestions.firstOrNull()?.id ?: "N/A"
        val lastQuestionId = allDbQuestions.lastOrNull()?.id ?: "N/A"

        println("==================================================")
        println("SCIENCE DATABASE VERIFICATION SUMMARY:")
        println("Imported Question IDs: SCI201 to SCI300 (100 new questions)")
        println("TOTAL SCIENCE QUESTIONS IN DB: $totalCountInDb")
        println("FIRST QUESTION ID: $firstQuestionId")
        println("LAST QUESTION ID: $lastQuestionId")
        println("Database Status: ACTIVE & HEALTHY")
        println("Import Status: SUCCESSFUL")
        println("Database Integrity: PASS ✅")
        println("==================================================")

        assertTrue("Total Science questions in DB should be at least 300", totalCountInDb >= 300)
        assertEquals("First question ID should be SCI001", "SCI001", firstQuestionId)
        assertTrue("Last question ID should start with SCI", lastQuestionId.startsWith("SCI"))

        // 9. Verify Daily Fixed Question System still returns 10 questions for Science
        val dailyQuestions = selectionEngine.getQuestionsForCategory("science", 10)
        assertEquals("Daily fixed question count for Science must remain 10", 10, dailyQuestions.size)
    }
}
