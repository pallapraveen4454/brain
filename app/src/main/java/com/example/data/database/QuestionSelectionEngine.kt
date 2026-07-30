package com.example.data.database

import android.content.Context
import android.util.Log
import com.example.BrainQuizApplication
import com.example.data.model.QuizQuestion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class QuestionSelectionEngine(private val context: Context? = null) {

    private val dbManager = CategoryDatabaseManager(context)

    companion object {
        private var inMemoryInstallDate: String? = null
        private val inMemoryAssignedIds = ConcurrentHashMap<String, String>()
        private val inMemoryLastDate = ConcurrentHashMap<String, String>()
        private val inMemoryDayIndex = ConcurrentHashMap<String, Int>()
    }

    private fun getAppContext(): Context? {
        return context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null }
    }

    /**
     * Permanent Install Date management.
     * Saved permanently on first launch and used as Day 1 reference.
     */
    fun getOrInitInstallDate(todayDateOverride: String? = null): String {
        val todayDate = todayDateOverride ?: getCurrentDateString()
        val prefs = getPrefs()
        var installDate = prefs?.getString("app_install_date", null) ?: inMemoryInstallDate
        if (installDate.isNullOrBlank()) {
            installDate = todayDate
            if (prefs != null) {
                prefs.edit().putString("app_install_date", installDate).apply()
            }
            inMemoryInstallDate = installDate
            Log.d("QuestionSelectionEngine", "Saved permanent install date as $installDate (Day 1)")
        }
        return installDate
    }

    /**
     * Calculates current Day Number: (Today's Date - Install Date) + 1
     */
    fun getCalculatedDayNumber(todayDateOverride: String? = null): Int {
        val todayDate = todayDateOverride ?: getCurrentDateString()
        val installDate = getOrInitInstallDate(todayDate)
        val diffDays = maxOf(0, calculateDaysBetween(installDate, todayDate))
        return diffDays + 1
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
     * Daily Fixed Question System (Install Date Based):
     * Day 1  = Install Date -> Questions 001-010
     * Day 2  = Install Date + 1 -> Questions 011-020
     * ...
     * Day 30 = Install Date + 29 -> Questions 291-300
     * Day 31 = Install Date + 30 -> Restarts from Day 1 (Questions 001-010)
     */
    suspend fun getQuestionsForCategory(
        categoryId: String,
        requestedCount: Int = 10,
        dateOverride: String? = null
    ): List<QuizQuestion> {
        val normKey = dbManager.normalizeCategoryKey(categoryId)
        val todayDate = dateOverride ?: getCurrentDateString()

        // 1. Check if questions are already assigned/cached for today
        val savedIds = getSavedQuestionIds(normKey, todayDate)
        if (savedIds.isNotEmpty()) {
            val allEntitiesMap = loadAllEntitiesForCategory(normKey).associateBy { it.id }
            val savedQuestions = savedIds.mapNotNull { allEntitiesMap[it] }
            if (savedQuestions.isNotEmpty()) {
                Log.d("QuestionSelectionEngine", "Loaded ${savedQuestions.size} daily fixed questions for '$normKey' on $todayDate from cache")
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

        // 3. Calculate Day Number based on Install Date
        val dayNumber = getCalculatedDayNumber(todayDate)
        val dayIndex = (dayNumber - 1) % 30  // 0-based day cycle index (0..29)

        // 4. Calculate batch selection offset
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

        Log.d("QuestionSelectionEngine", "Assigned ${selectedEntities.size} questions for '$normKey' on $todayDate (Day $dayNumber, cycle index: $dayIndex, start idx: $startIndex)")
        return selectedEntities.map { it.toQuizQuestion() }
    }

    fun getQuestionsForCategorySync(
        categoryId: String,
        requestedCount: Int = 10,
        dateOverride: String? = null
    ): List<QuizQuestion> {
        return try {
            kotlinx.coroutines.runBlocking {
                getQuestionsForCategory(categoryId, requestedCount, dateOverride)
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

    private fun calculateDaysBetween(startDateStr: String, endDateStr: String): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return try {
            val start = sdf.parse(startDateStr)
            val end = sdf.parse(endDateStr)
            if (start != null && end != null) {
                val diffMs = end.time - start.time
                val days = Math.round(diffMs.toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)).toInt()
                if (days >= 0) days else 0
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun getPrefs() = getAppContext()?.getSharedPreferences("daily_quiz_prefs", Context.MODE_PRIVATE)

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

