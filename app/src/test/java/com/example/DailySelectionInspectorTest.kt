package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.QuestionSelectionEngine
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DailySelectionInspectorTest {

    @Test
    fun inspectTodaySelection() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val selectionEngine = QuestionSelectionEngine(context)

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val installDate = selectionEngine.getOrInitInstallDate(todayDate)
        val dayNumber = selectionEngine.getCalculatedDayNumber(todayDate)

        println("================ INSTALL DATE BASED DAILY QUIZ INSPECTION ================")
        println("Install Date: $installDate")
        println("Today's Date: $todayDate")
        println("Calculated Day Number: Day $dayNumber")

        for (cat in listOf("gk", "science")) {
            val questions = selectionEngine.getQuestionsForCategory(cat, requestedCount = 10)
            val ids = questions.map { it.id }
            val dbName = "category_$cat.db"

            println("\n--- $dbName ---")
            println("Category: $cat")
            println("Day Number: Day $dayNumber")
            println("Selected Question IDs: $ids")
        }
        println("==========================================================================")
    }
}
