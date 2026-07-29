package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AutomaticQuestionGenerator
import com.example.data.database.CategoryDatabaseManager
import com.example.data.database.QuestionEntity
import com.example.data.database.QuestionImportManager
import com.example.data.database.QuestionSelectionEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuestionGeneratorAndImportSystemTest {

    @Test
    fun testValidationRulesAndImportManager() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val importManager = QuestionImportManager(context)
        val selectionEngine = QuestionSelectionEngine(context)

        // 1. Invalid category validation
        val invalidCatQ = QuestionEntity(
            id = "q_invalid_1",
            categoryId = "Cooking", // Not in 8 categories
            questionText = "How do you make soup?",
            optionA = "Water",
            optionB = "Salt",
            optionC = "Heat",
            optionD = "Pot",
            correctAnswer = "Water",
            explanation = "Basic soup ingredient."
        )
        val res1 = importManager.validateQuestion(invalidCatQ)
        assertFalse("Invalid category should fail validation", res1.isValid)

        // 2. Duplicate options validation
        val duplicateOptQ = QuestionEntity(
            id = "q_dup_opt_1",
            categoryId = "science",
            questionText = "What is H2O?",
            optionA = "Water",
            optionB = "Water", // Duplicate option
            optionC = "Ice",
            optionD = "Steam",
            correctAnswer = "Water"
        )
        val res2 = importManager.validateQuestion(duplicateOptQ)
        assertFalse("Duplicate options should fail validation", res2.isValid)

        // 3. Non-matching correct answer validation
        val nonMatchCorrectQ = QuestionEntity(
            id = "q_non_match_1",
            categoryId = "math",
            questionText = "What is 2 + 2?",
            optionA = "1",
            optionB = "2",
            optionC = "3",
            optionD = "5",
            correctAnswer = "4" // 4 is not in options!
        )
        val res3 = importManager.validateQuestion(nonMatchCorrectQ)
        assertFalse("Non-matching correct answer should fail validation", res3.isValid)

        // 4. Valid question import
        val validQ = QuestionEntity(
            id = "valid_sci_99",
            categoryId = "Science",
            questionText = "What gas do humans breathe in for oxygen?",
            optionA = "Oxygen",
            optionB = "Carbon Dioxide",
            optionC = "Helium",
            optionD = "Methane",
            correctAnswer = "Oxygen",
            explanation = "Oxygen is required for cellular respiration.",
            difficulty = "Easy"
        )
        val initialCount = selectionEngine.getQuestionCountForCategory("science")
        val importRes = importManager.importQuestions(listOf(validQ))

        assertEquals("Should import 1 valid question", 1, importRes.importedCount)
        val updatedCount = selectionEngine.getQuestionCountForCategory("science")
        assertEquals("Science count should increase by 1", initialCount + 1, updatedCount)

        // 5. Test duplicate question text prevention
        val dupImportRes = importManager.importQuestions(listOf(validQ))
        assertEquals("Duplicate question should be skipped", 0, dupImportRes.importedCount)
        assertEquals("Should increment skipped duplicates count", 1, dupImportRes.skippedDuplicatesCount)
    }

    @Test
    fun testAutomaticGeneratorAndCategoryIsolation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val generator = AutomaticQuestionGenerator(context)
        val selectionEngine = QuestionSelectionEngine(context)

        val categories = listOf("gk", "science", "sports", "history", "movies", "tech", "geo", "math")
        val initialTotalCount = categories.sumOf { selectionEngine.getQuestionCountForCategory(it) }

        // Generate and auto-import 2 questions per category across all 8 categories
        val importResult = generator.generateAndImportQuestions(categoryKey = "all", countPerCategory = 2)

        assertTrue("Imported count should be > 0", importResult.importedCount > 0)
        val newTotalCount = categories.sumOf { selectionEngine.getQuestionCountForCategory(it) }

        assertEquals("Total count should update by imported count", initialTotalCount + importResult.importedCount, newTotalCount)

        // Verify Daily Quiz Engine / Selection Engine uses new questions
        val dailyQuestions = selectionEngine.getQuestionsForCategory("daily", requestedCount = 10)
        assertNotNull("Daily quiz should fetch questions", dailyQuestions)
        assertTrue("Daily quiz should return available questions", dailyQuestions.isNotEmpty())
    }
}
