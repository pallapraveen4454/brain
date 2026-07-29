package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.CategoryDatabaseManager
import com.example.data.database.QuestionEntity
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
class QuestionDatabaseManagementTest {

    @Test
    fun testSeparateDatabasePerCategoryAndAutomaticCountDetection() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbManager = CategoryDatabaseManager(context)
        val selectionEngine = QuestionSelectionEngine(context)

        val categories = listOf("gk", "science", "sports", "history", "movies", "tech", "geo", "math")

        // 1. Verify every category has its own database instance & initial seeds
        for (category in categories) {
            val db = dbManager.getDatabaseForCategory(category)
            assertNotNull("Database for category $category should be created", db)

            val count = selectionEngine.getQuestionCountForCategory(category)
            assertTrue("Initial question count for $category should be >= 10", count >= 10)
        }

        // 2. Test adding new questions without changing application code (Science: 10 -> 12)
        val initialScienceCount = selectionEngine.getQuestionCountForCategory("science")

        val newScienceQuestions = listOf(
            QuestionEntity(
                id = "sci_new_101",
                categoryId = "science",
                questionText = "What element does 'O' represent on the periodic table?",
                optionA = "Gold",
                optionB = "Oxygen",
                optionC = "Osmium",
                optionD = "Zinc",
                correctAnswer = "Oxygen",
                explanation = "'O' is the chemical symbol for Oxygen.",
                difficulty = "Easy"
            ),
            QuestionEntity(
                id = "sci_new_102",
                categoryId = "science",
                questionText = "What planet is closest to the Sun?",
                optionA = "Venus",
                optionB = "Earth",
                optionC = "Mercury",
                optionD = "Mars",
                correctAnswer = "Mercury",
                explanation = "Mercury is the smallest and innermost planet in the Solar System.",
                difficulty = "Easy"
            )
        )

        selectionEngine.addQuestionsToCategory("science", newScienceQuestions)

        val updatedScienceCount = selectionEngine.getQuestionCountForCategory("science")
        assertEquals("Science question count should dynamically update", initialScienceCount + 2, updatedScienceCount)

        // 3. Verify Question Selection Engine automatically fetches new questions
        val fetchedQuestions = selectionEngine.getQuestionsForCategory("science", requestedCount = 100)
        assertTrue("Newly inserted question sci_new_101 must be available", fetchedQuestions.any { it.id == "sci_new_101" })
        assertTrue("Newly inserted question sci_new_102 must be available", fetchedQuestions.any { it.id == "sci_new_102" })
    }
}
