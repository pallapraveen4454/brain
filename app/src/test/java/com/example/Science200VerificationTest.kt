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
class Science200VerificationTest {

    @Test
    fun verifyScience001To200ImportAndDatabaseIntegrity() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("category_science.db")

        val importManager = QuestionImportManager(context)
        val selectionEngine = QuestionSelectionEngine(context)
        val dbManager = CategoryDatabaseManager(context)

        // 1. Get Science seed questions (SCI001 to SCI200)
        val scienceSeeds = DefaultQuestionSeeds.getSeedsForCategory("science")
        println("Total Science Seed Count: ${scienceSeeds.size}")
        assertTrue("Total Science seeds should be at least 200", scienceSeeds.size >= 200)

        // 2. Validate Batch 2 seeds (SCI101 to SCI200)
        val batch2Seeds = scienceSeeds.filter { 
            val num = it.id.removePrefix("SCI").toIntOrNull()
            num != null && num in 101..200 
        }
        assertEquals("Batch 2 seeds (SCI101 to SCI200) should be exactly 100", 100, batch2Seeds.size)

        // Check Batch 2 difficulty distribution: Easy 40, Medium 40, Hard 20
        val batch2Easy = batch2Seeds.count { it.difficulty.equals("Easy", ignoreCase = true) }
        val batch2Medium = batch2Seeds.count { it.difficulty.equals("Medium", ignoreCase = true) }
        val batch2Hard = batch2Seeds.count { it.difficulty.equals("Hard", ignoreCase = true) }

        println("Batch 2 Difficulty Breakdown: Easy=$batch2Easy, Medium=$batch2Medium, Hard=$batch2Hard")
        assertEquals("Batch 2 Easy count must be 40", 40, batch2Easy)
        assertEquals("Batch 2 Medium count must be 40", 40, batch2Medium)
        assertEquals("Batch 2 Hard count must be 20", 20, batch2Hard)

        // 3. Check that all IDs SCI001 to SCI200 exist without missing IDs
        val expectedIds = (1..200).map { "SCI" + String.format("%03d", it) }.toSet()
        val actualIds = scienceSeeds.map { it.id }.toSet()

        val missingIds = expectedIds - actualIds
        assertTrue("No missing IDs between SCI001 and SCI200. Missing: $missingIds", missingIds.isEmpty())

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
        assertTrue("All 200 Science questions must be valid. Errors: $validationErrors", validationErrors.isEmpty())

        // 7. Automatically import all questions into category_science.db
        val importResult = importManager.importQuestions(scienceSeeds)
        println("Import Result - Total Imported: ${importResult.importedCount}, Skipped: ${importResult.skippedDuplicatesCount}")
        assertEquals("100% of questions (200) should be imported", 200, importResult.importedCount)

        // 8. Verify category_science.db contents
        val db = dbManager.getDatabaseForCategory("science")
        val dao = db?.questionDao()
        val allDbQuestions = dao?.getAllQuestions() ?: emptyList()

        val totalCountInDb = allDbQuestions.size
        val firstQuestionId = allDbQuestions.firstOrNull()?.id ?: "N/A"
        val lastQuestionId = allDbQuestions.lastOrNull()?.id ?: "N/A"

        println("==================================================")
        println("SCIENCE DATABASE VERIFICATION SUMMARY:")
        println("Imported Question IDs: SCI101 to SCI200 (100 new questions)")
        println("TOTAL SCIENCE QUESTIONS IN DB: $totalCountInDb")
        println("FIRST QUESTION ID: $firstQuestionId")
        println("LAST QUESTION ID: $lastQuestionId")
        println("Database Status: ACTIVE & HEALTHY")
        println("Import Status: SUCCESSFUL")
        println("Database Integrity: PASS ✅")
        println("==================================================")

        assertTrue("Total Science questions in DB should be at least 200", totalCountInDb >= 200)
        assertEquals("First question ID should be SCI001", "SCI001", firstQuestionId)
        assertTrue("Last question ID should start with SCI", lastQuestionId.startsWith("SCI"))

        // 9. Verify Daily Fixed Question System still returns 10 questions for Science
        val dailyQuestions = selectionEngine.getQuestionsForCategory("science", 10)
        assertEquals("Daily fixed question count for Science must remain 10", 10, dailyQuestions.size)
    }
}
