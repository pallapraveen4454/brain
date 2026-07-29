package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.CategoryDatabaseManager
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeneralKnowledgeInspectorTest {

    @Test
    fun inspectGeneralKnowledgeDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbManager = CategoryDatabaseManager(context)
        val db = dbManager.getDatabaseForCategory("gk")

        val dao = db?.questionDao()
        val allQuestions = dao?.getAllQuestions() ?: emptyList()

        println("==================================================")
        println("DATABASE: category_gk.db")
        println("TOTAL QUESTION COUNT: ${allQuestions.size}")
        println("ALL QUESTION IDs: ${allQuestions.map { it.id }}")
        println("==================================================")

        println("\n--- FIRST 10 QUESTIONS ---")
        allQuestions.take(10).forEachIndexed { index, q ->
            println("${index + 1}. ID: ${q.id} | ${q.questionText}")
            println("   Options: A) ${q.optionA} | B) ${q.optionB} | C) ${q.optionC} | D) ${q.optionD}")
            println("   Correct: ${q.correctAnswer} | Difficulty: ${q.difficulty}")
            println("   Explanation: ${q.explanation}\n")
        }

        println("--- LAST 10 QUESTIONS ---")
        allQuestions.takeLast(10).forEachIndexed { index, q ->
            println("${index + 1}. ID: ${q.id} | ${q.questionText}")
            println("   Options: A) ${q.optionA} | B) ${q.optionB} | C) ${q.optionC} | D) ${q.optionD}")
            println("   Correct: ${q.correctAnswer} | Difficulty: ${q.difficulty}")
            println("   Explanation: ${q.explanation}\n")
        }
        println("==================================================")
    }
}
