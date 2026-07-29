package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
class GeneralKnowledgeVerificationTest {

    @Test
    fun verifyGeneralKnowledgeDatabaseAndSelectionEngine() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val selectionEngine = QuestionSelectionEngine(context)

        // 1. Get question count for General Knowledge database
        val totalGkCount = selectionEngine.getQuestionCountForCategory("gk")
        println("Total General Knowledge Questions in category_gk.db: $totalGkCount")
        assertTrue("GK Question count should be at least 10", totalGkCount >= 10)

        // 2. Randomly retrieve 10 questions from category_gk.db
        val retrievedQuestions = selectionEngine.getQuestionsForCategory("gk", requestedCount = 10)
        assertNotNull(retrievedQuestions)
        assertEquals("Should retrieve exactly 10 questions", 10, retrievedQuestions.size)

        // 3. Confirm that ONLY General Knowledge questions are returned
        for (q in retrievedQuestions) {
            assertEquals("All retrieved questions must belong to 'gk' category", "gk", q.categoryId)
        }

        // 4. Extract Question IDs
        val selectedIds = retrievedQuestions.map { it.id }
        println("Selected Question IDs: $selectedIds")

        // 5. Assert IDs belong to GK series (e.g. GK001 to GK010)
        assertTrue("Selected IDs should contain GK series items", selectedIds.any { it.startsWith("GK") })
    }
}
