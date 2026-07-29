package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.CategoryDatabaseManager
import com.example.data.database.QuestionEntity
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
class GeneralKnowledgeImportTest {

    @Test
    fun testGkImportAndRetrieval() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val importManager = QuestionImportManager(context)
        val selectionEngine = QuestionSelectionEngine(context)

        val gkQuestions = listOf(
            QuestionEntity("GK001", "General Knowledge", "Which famous playwright wrote 'Romeo and Juliet'?", "Charles Dickens", "William Shakespeare", "Mark Twain", "Jane Austen", "William Shakespeare", "William Shakespeare wrote the play in the late 16th century.", "Easy"),
            QuestionEntity("GK002", "General Knowledge", "What is the capital city of France?", "Berlin", "Madrid", "Paris", "Rome", "Paris", "Paris is the capital and largest city of France.", "Easy"),
            QuestionEntity("GK003", "General Knowledge", "Which currency is used in Japan?", "Yuan", "Yen", "Won", "Baht", "Yen", "The official currency of Japan is the Japanese Yen.", "Easy"),
            QuestionEntity("GK004", "General Knowledge", "How many colors are in a standard rainbow?", "5", "6", "7", "8", "7", "A rainbow has 7 colors: Red, Orange, Yellow, Green, Blue, Indigo, Violet.", "Easy"),
            QuestionEntity("GK005", "General Knowledge", "What is the main ingredient in traditional Mexican guacamole?", "Tomato", "Avocado", "Onion", "Pepper", "Avocado", "Guacamole is an avocado-based dip originating from Mexico.", "Easy"),
            QuestionEntity("GK006", "General Knowledge", "Who painted the Mona Lisa?", "Vincent van Gogh", "Pablo Picasso", "Leonardo da Vinci", "Michelangelo", "Leonardo da Vinci", "Painted by Italian Renaissance artist Leonardo da Vinci.", "Easy"),
            QuestionEntity("GK007", "General Knowledge", "What is the national flower of Japan?", "Rose", "Tulip", "Cherry Blossom (Sakura)", "Lotus", "Cherry Blossom (Sakura)", "Cherry Blossom is widely regarded as the national flower of Japan.", "Easy"),
            QuestionEntity("GK008", "General Knowledge", "Which animal is known as the 'Ship of the Desert'?", "Camel", "Elephant", "Horse", "Llama", "Camel", "Camels are suited for desert transport due to humps and endurance.", "Easy"),
            QuestionEntity("GK009", "General Knowledge", "Which is the smallest country in the world?", "Monaco", "Vatican City", "San Marino", "Liechtenstein", "Vatican City", "Vatican City is an independent state surrounded by Rome.", "Easy"),
            QuestionEntity("GK010", "General Knowledge", "What is the official home and office of the US President?", "The Capitol", "The White House", "Pentagon", "Empire State", "The White House", "Located in Washington, D.C.", "Easy")
        )

        val importResult = importManager.importQuestions(gkQuestions)

        val gkCount = selectionEngine.getQuestionCountForCategory("gk")
        assertTrue("GK Question count should be at least 10", gkCount >= 10)

        val retrievedQuestions = selectionEngine.getQuestionsForCategory("gk", requestedCount = 50)
        assertNotNull(retrievedQuestions)
        assertTrue("Retrieved questions should contain imported GK questions", retrievedQuestions.isNotEmpty())

        val gk001 = retrievedQuestions.find { it.id == "GK001" }
        assertNotNull("GK001 should be present in retrieved list", gk001)
        assertEquals("GK001 category should be gk", "gk", gk001?.categoryId)
    }
}
