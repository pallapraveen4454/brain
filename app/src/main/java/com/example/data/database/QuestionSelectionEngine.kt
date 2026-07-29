package com.example.data.database

import android.content.Context
import android.util.Log
import com.example.data.model.QuizQuestion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class QuestionSelectionEngine(private val context: Context? = null) {

    private val dbManager = CategoryDatabaseManager(context)

    companion object {
        // Fallback in-memory persistence when context is null or for current process lifetime
        private val inMemoryAssignedIds = ConcurrentHashMap<String, String>()
        private val inMemoryLastDate = ConcurrentHashMap<String, String>()
        private val inMemoryDayIndex = ConcurrentHashMap<String, Int>()
    }

    /**
     * Gets total question count for a specific category directly from its separate database.
     * Automatically reflects any new questions added to the category database.
     */
    fun getQuestionCountForCategory(categoryId: String): Int {
        val normKey = dbManager.normalizeCategoryKey(categoryId)

        return when (normKey) {
            "quick", "daily", "practice", "all" -> {
                dbManager.getAllCategoryKeys().sumOf { cat ->
                    val db = dbManager.getDatabaseForCategory(cat)
                    db?.questionDao()?.getQuestionCountSync() ?: 0
                }
            }
            else -> {
                val db = dbManager.getDatabaseForCategory(normKey)
                db?.questionDao()?.getQuestionCountSync() ?: 0
            }
        }
    }

    /**
     * Daily Fixed Question System:
     * Assigns 10 questions per category per day in sequence (e.g., Day 1 -> 1..10, Day 2 -> 11..20).
     * On the same calendar date, opening the app multiple times always returns the exact same 10 assigned questions.
     * Automatically advances to the next 10 questions when the calendar date changes.
     * Wraps around sequentially once all questions in the category have been served.
     */
    suspend fun getQuestionsForCategory(categoryId: String, requestedCount: Int = 10): List<QuizQuestion> {
        val normKey = dbManager.normalizeCategoryKey(categoryId)
        val todayDate = getCurrentDateString()

        // 1. Check if questions are already assigned for today
        val savedIds = getSavedQuestionIds(normKey, todayDate)
        if (savedIds.isNotEmpty()) {
            val allEntitiesMap = loadAllEntitiesForCategory(normKey).associateBy { it.id }
            val savedQuestions = savedIds.mapNotNull { allEntitiesMap[it] }
            if (savedQuestions.isNotEmpty()) {
                Log.d("QuestionSelectionEngine", "Loaded ${savedQuestions.size} daily fixed questions for category '$normKey' on $todayDate from cache")
                return savedQuestions.map { it.toQuizQuestion() }
            }
        }

        // 2. Load and deterministically sort all available questions for this category
        val rawEntities = loadAllEntitiesForCategory(normKey)
        val allEntities = sortQuestionsDeterministically(rawEntities)

        if (allEntities.isEmpty()) {
            Log.w("QuestionSelectionEngine", "No questions available for category '$normKey'")
            return emptyList()
        }

        // 3. Determine Day Index
        val lastDate = getLastDate(normKey)
        var dayIndex = getDayIndex(normKey)

        if (lastDate != null && lastDate != todayDate) {
            dayIndex += 1
        }

        // 4. Calculate batch selection (sequence of requestedCount questions)
        val totalCount = allEntities.size
        val countToSelect = minOf(requestedCount, totalCount)
        val startIndex = (dayIndex * countToSelect) % totalCount

        val selectedEntities = mutableListOf<QuestionEntity>()
        for (i in 0 until countToSelect) {
            val idx = (startIndex + i) % totalCount
            selectedEntities.add(allEntities[idx])
        }

        // 5. Permanently save assigned question IDs, last date, and day index
        val assignedIds = selectedEntities.map { it.id }
        saveAssignedState(normKey, todayDate, dayIndex, assignedIds)

        Log.d("QuestionSelectionEngine", "Assigned ${selectedEntities.size} new daily fixed questions for '$normKey' on $todayDate (Day index: $dayIndex, start idx: $startIndex)")
        return selectedEntities.map { it.toQuizQuestion() }
    }

    fun getQuestionsForCategorySync(categoryId: String, requestedCount: Int = 10): List<QuizQuestion> {
        return try {
            kotlinx.coroutines.runBlocking {
                getQuestionsForCategory(categoryId, requestedCount)
            }
        } catch (e: Exception) {
            Log.e("QuestionSelectionEngine", "Error fetching questions sync for $categoryId", e)
            emptyList()
        }
    }

    suspend fun addQuestionsToCategory(categoryId: String, newQuestions: List<QuestionEntity>) {
        val normKey = dbManager.normalizeCategoryKey(categoryId)
        val db = dbManager.getDatabaseForCategory(normKey)
        if (db != null) {
            db.questionDao().insertQuestions(newQuestions)
            Log.d("QuestionSelectionEngine", "Added ${newQuestions.size} new questions to category_$normKey.db")
        }
    }

    private suspend fun loadAllEntitiesForCategory(normKey: String): List<QuestionEntity> {
        return if (normKey in listOf("quick", "daily", "practice", "all")) {
            val combined = mutableListOf<QuestionEntity>()
            for (catKey in dbManager.getAllCategoryKeys()) {
                val db = dbManager.getDatabaseForCategory(catKey)
                if (db != null) {
                    combined.addAll(db.questionDao().getAllQuestions())
                }
            }
            combined
        } else {
            val db = dbManager.getDatabaseForCategory(normKey)
            db?.questionDao()?.getAllQuestions() ?: emptyList()
        }
    }

    /**
     * Sorts questions deterministically so that Day 1 is always items 1..10, Day 2 is 11..20, etc.
     * Handles ID formats like GK001, GK002, gk_1, sci_1, etc.
     */
    private fun sortQuestionsDeterministically(questions: List<QuestionEntity>): List<QuestionEntity> {
        return questions.sortedWith { q1, q2 ->
            val id1 = q1.id.trim()
            val id2 = q2.id.trim()

            val num1 = id1.filter { it.isDigit() }.toLongOrNull()
            val num2 = id2.filter { it.isDigit() }.toLongOrNull()
            val prefix1 = id1.filter { it.isLetter() }
            val prefix2 = id2.filter { it.isLetter() }

            if (prefix1.equals(prefix2, ignoreCase = true) && num1 != null && num2 != null) {
                num1.compareTo(num2)
            } else {
                id1.compareTo(id2, ignoreCase = true)
            }
        }
    }

    private fun getCurrentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun getPrefs() = context?.getSharedPreferences("daily_quiz_prefs", Context.MODE_PRIVATE)

    private fun getSavedQuestionIds(normKey: String, todayDate: String): List<String> {
        val prefs = getPrefs()
        val key = "assigned_ids_${normKey}_${todayDate}"
        val saved = if (prefs != null) {
            prefs.getString(key, null)
        } else {
            inMemoryAssignedIds[key]
        }
        return if (!saved.isNullOrBlank()) {
            saved.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
    }

    private fun getLastDate(normKey: String): String? {
        val prefs = getPrefs()
        val key = "last_date_${normKey}"
        return prefs?.getString(key, null) ?: inMemoryLastDate[key]
    }

    private fun getDayIndex(normKey: String): Int {
        val prefs = getPrefs()
        val key = "day_index_${normKey}"
        return prefs?.getInt(key, 0) ?: (inMemoryDayIndex[key] ?: 0)
    }

    private fun saveAssignedState(normKey: String, todayDate: String, dayIndex: Int, assignedIds: List<String>) {
        val prefs = getPrefs()
        val idsKey = "assigned_ids_${normKey}_${todayDate}"
        val dateKey = "last_date_${normKey}"
        val indexKey = "day_index_${normKey}"
        val idsString = assignedIds.joinToString(",")

        if (prefs != null) {
            prefs.edit()
                .putString(idsKey, idsString)
                .putString(dateKey, todayDate)
                .putInt(indexKey, dayIndex)
                .apply()
        }

        inMemoryAssignedIds[idsKey] = idsString
        inMemoryLastDate[dateKey] = todayDate
        inMemoryDayIndex[indexKey] = dayIndex
    }
}
