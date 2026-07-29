package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.CategoryDatabaseManager
import com.example.data.database.DefaultQuestionSeeds
import com.example.data.database.QuestionSelectionEngine
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CategoryGkStatusInspectorTest {

    @Test
    fun inspectCategoryGkStatus() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbManager = CategoryDatabaseManager(context)
        val db = dbManager.getDatabaseForCategory("gk")

        val dao = db?.questionDao()
        val allQuestions = dao?.getAllQuestions() ?: emptyList()

        val totalCount = allQuestions.size
        val firstQuestionId = allQuestions.firstOrNull()?.id ?: "N/A"
        val lastQuestionId = allQuestions.lastOrNull()?.id ?: "N/A"

        println("==================================================")
        println("CATEGORY_GK.DB STATUS:")
        println("Total General Knowledge Question Count: $totalCount")
        println("First Question ID: $firstQuestionId")
        println("Last Question ID: $lastQuestionId")
        println("==================================================")
    }
}
