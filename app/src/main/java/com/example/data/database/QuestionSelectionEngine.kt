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

    data class InstallDateDetails(
        val previousInstallDate: String,
        val currentInstallDate: String,
        val calculatedDayNumber: Int
    )

    private fun subtractDaysFromDate(dateStr: String, days: Int): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return try {
            val date = sdf.parse(dateStr)
            if (date != null) {
                val cal = java.util.Calendar.getInstance()
                cal.time = date
                cal.add(java.util.Calendar.DAY_OF_YEAR, -days)
                sdf.format(cal.time)
            } else {
                dateStr
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun formatTimestampToDate(timestamp: Long): String {
        if (timestamp <= 0) return ""
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Permanent Install Date details and recovery logic.
     * Saved permanently on first launch and preserved across APK updates.
     */
    fun getInstallDateDetails(todayDateOverride: String? = null): InstallDateDetails {
        val todayDate = todayDateOverride ?: getCurrentDateString()
        val ctx = getAppContext()
        val prefs = getPrefs()

        var savedInstallDate = prefs?.getString("app_install_date", null) ?: inMemoryInstallDate

        if (savedInstallDate.isNullOrBlank() && ctx != null) {
            val authPrefs = ctx.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            val quizPrefs = ctx.getSharedPreferences("quiz_results_prefs", Context.MODE_PRIVATE)
            val profilePrefs = ctx.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)

            val candidates = listOfNotNull(
                authPrefs.getString("app_install_date", null),
                quizPrefs.getString("app_install_date", null),
                profilePrefs.getString("app_install_date", null),
                authPrefs.getString("user_created_date", null)
            ).filter { it.isNotBlank() }

            if (candidates.isNotEmpty()) {
                savedInstallDate = candidates.first()
            }
        }

        if (savedInstallDate.isNullOrBlank() && ctx != null) {
            try {
                val profilePrefs = ctx.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)
                val jsonStr = profilePrefs.getString("persistent_user_profile", "") ?: ""
                if (jsonStr.isNotBlank()) {
                    val jsonObj = org.json.JSONObject(jsonStr)
                    val jsonInstallDate = jsonObj.optString("installDate", "")
                    if (jsonInstallDate.isNotBlank()) {
                        savedInstallDate = jsonInstallDate
                    }
                }
            } catch (e: Exception) {
                Log.e("QuestionSelectionEngine", "Error reading installDate from profile json", e)
            }
        }

        // Check if there is an earlier user activity date from profile/streak
        var derivedEarliestDate: String? = null
        if (ctx != null) {
            try {
                val profileStore = com.example.data.UserProfileStore(ctx)
                if (profileStore.hasSavedProfile()) {
                    val profile = profileStore.getProfile()
                    val activityDates = mutableListOf<String>()

                    // 1. Streak derived date
                    if (profile.streak > 0) {
                        val refDate = if (profile.lastActiveDate.isNotBlank()) profile.lastActiveDate else todayDate
                        val streakDaysToSubtract = maxOf(0, profile.streak - 1)
                        val streakDate = subtractDaysFromDate(refDate, streakDaysToSubtract)
                        if (streakDate <= todayDate) {
                            activityDates.add(streakDate)
                        }
                    }

                    // 2. CreatedAt date
                    if (profile.createdAt > 0 && profile.createdAt <= System.currentTimeMillis()) {
                        val createdDate = formatTimestampToDate(profile.createdAt)
                        if (createdDate.isNotBlank() && createdDate <= todayDate) {
                            activityDates.add(createdDate)
                        }
                    }

                    // 3. Earliest quiz history date
                    if (profile.quizHistory.isNotEmpty()) {
                        profile.quizHistory.forEach { res ->
                            val d = if (res.dateFormatted.isNotBlank()) res.dateFormatted else formatTimestampToDate(res.timestamp)
                            if (d.isNotBlank() && d <= todayDate) {
                                activityDates.add(d)
                            }
                        }
                    }

                    // 4. Last quiz date / Last active date
                    if (profile.lastQuizDate.isNotBlank() && profile.lastQuizDate <= todayDate) {
                        activityDates.add(profile.lastQuizDate)
                    }
                    if (profile.lastActiveDate.isNotBlank() && profile.lastActiveDate <= todayDate) {
                        activityDates.add(profile.lastActiveDate)
                    }

                    if (activityDates.isNotEmpty()) {
                        derivedEarliestDate = activityDates.minOrNull()
                    }
                }
            } catch (e: Exception) {
                Log.e("QuestionSelectionEngine", "Error deriving earliest activity date", e)
            }
        }

        var previousInstallDate = prefs?.getString("previous_install_date", null) ?: savedInstallDate ?: todayDate
        var finalInstallDate: String

        if (savedInstallDate.isNullOrBlank()) {
            finalInstallDate = derivedEarliestDate ?: todayDate
            previousInstallDate = todayDate
        } else if (savedInstallDate == todayDate && derivedEarliestDate != null && derivedEarliestDate < todayDate) {
            // Defaulted/overwritten to today previously, recover to earliest activity date permanently
            previousInstallDate = savedInstallDate
            finalInstallDate = derivedEarliestDate
        } else {
            finalInstallDate = savedInstallDate
        }

        if (ctx != null) {
            prefs?.edit()
                ?.putString("app_install_date", finalInstallDate)
                ?.putString("previous_install_date", previousInstallDate)
                ?.commit()

            ctx.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit()
                .putString("app_install_date", finalInstallDate)
                .putString("previous_install_date", previousInstallDate)
                .commit()

            ctx.getSharedPreferences("quiz_results_prefs", Context.MODE_PRIVATE).edit()
                .putString("app_install_date", finalInstallDate)
                .putString("previous_install_date", previousInstallDate)
                .commit()

            ctx.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE).edit()
                .putString("app_install_date", finalInstallDate)
                .putString("previous_install_date", previousInstallDate)
                .commit()

            try {
                val profileStore = com.example.data.UserProfileStore(ctx)
                val profile = profileStore.getProfile()
                if (profile.installDate != finalInstallDate) {
                    profileStore.saveProfile(profile.copy(installDate = finalInstallDate))
                }
            } catch (e: Exception) {
                Log.e("QuestionSelectionEngine", "Error updating profile json installDate", e)
            }
        }

        inMemoryInstallDate = finalInstallDate

        val diffDays = maxOf(0, calculateDaysBetween(finalInstallDate, todayDate))
        val dayNumber = diffDays + 1

        return InstallDateDetails(
            previousInstallDate = previousInstallDate,
            currentInstallDate = finalInstallDate,
            calculatedDayNumber = dayNumber
        )
    }

    fun getOrInitInstallDate(todayDateOverride: String? = null): String {
        return getInstallDateDetails(todayDateOverride).currentInstallDate
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
            "quick", "daily", "all" -> {
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

    data class OffsetDetails(
        val offsetValue: Int = 0,
        val startIndex: Int = 0,
        val endIndex: Int = 0,
        val generatedIds: List<String> = emptyList()
    )

    private val lastOffsetDetails = java.util.concurrent.ConcurrentHashMap<String, OffsetDetails>()

    fun getOffsetDetailsForCategory(categoryId: String, requestedCount: Int = 10, dateOverride: String? = null): OffsetDetails {
        val normKey = dbManager.normalizeCategoryKey(categoryId)

        val todayDate = dateOverride ?: getCurrentDateString()
        val dayNumber = getCalculatedDayNumber(todayDate)
        val rawEntities = loadAllEntitiesForCategorySync(normKey)
        val allEntities = sortQuestionsDeterministically(rawEntities)
        val totalCount = allEntities.size
        if (totalCount == 0) return OffsetDetails()

        val countToSelect = minOf(requestedCount, totalCount)
        val totalDaysInCycle = maxOf(1, totalCount / countToSelect)
        val dayIndex = (dayNumber - 1) % totalDaysInCycle
        val startIndex = (dayIndex * countToSelect) % totalCount
        val endIndex = if (countToSelect > 0) (startIndex + countToSelect - 1) else 0
        val offsetValue = dayIndex * countToSelect
        val generatedIds = (0 until countToSelect).map { i ->
            allEntities[(startIndex + i) % totalCount].id
        }

        val offsetDetails = OffsetDetails(
            offsetValue = offsetValue,
            startIndex = startIndex,
            endIndex = endIndex,
            generatedIds = generatedIds
        )
        lastOffsetDetails[normKey] = offsetDetails
        return offsetDetails
    }

    private fun loadAllEntitiesForCategorySync(normKey: String): List<QuestionEntity> {
        return try {
            kotlinx.coroutines.runBlocking { loadAllEntitiesForCategory(normKey) }
        } catch (e: Exception) {
            emptyList()
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

        // 1. Calculate Day Number based on Install Date
        val dayNumber = getCalculatedDayNumber(todayDate)

        // 3. Load and deterministically sort all available questions for this category
        val rawEntities = loadAllEntitiesForCategory(normKey)
        val allEntities = sortQuestionsDeterministically(rawEntities)

        if (allEntities.isEmpty()) {
            Log.w("QuestionSelectionEngine", "No questions available for category '$normKey'")
            return emptyList()
        }

        val totalCount = allEntities.size
        val countToSelect = minOf(requestedCount, totalCount)
        val totalDaysInCycle = maxOf(1, totalCount / countToSelect)
        val dayIndex = (dayNumber - 1) % totalDaysInCycle

        // 2. Check if questions are already assigned/cached for today AND dayIndex matches
        val savedIds = getSavedQuestionIds(normKey, todayDate, expectedDayIndex = dayIndex)
        if (savedIds.isNotEmpty()) {
            val allEntitiesMap = allEntities.associateBy { it.id }
            val savedQuestions = savedIds.mapNotNull { allEntitiesMap[it] }
            if (savedQuestions.isNotEmpty()) {
                val startIndex = (dayIndex * countToSelect) % totalCount
                val endIndex = if (countToSelect > 0) (startIndex + countToSelect - 1) else 0
                val offsetValue = dayIndex * countToSelect

                val offsetDetails = OffsetDetails(
                    offsetValue = offsetValue,
                    startIndex = startIndex,
                    endIndex = endIndex,
                    generatedIds = savedQuestions.map { it.id }
                )
                lastOffsetDetails[normKey] = offsetDetails

                Log.d("DEBUG_DAILY_QUIZ", "1. Calculated Day Number: $dayNumber")
                Log.d("DEBUG_DAILY_QUIZ", "2. assigned_ids received: $savedIds")
                Log.d("DEBUG_DAILY_QUIZ", "3. Question IDs returned from database: ${savedQuestions.map { it.id }}")
                Log.d("QuestionSelectionEngine", "Loaded ${savedQuestions.size} daily fixed questions for '$normKey' on $todayDate from cache")
                return savedQuestions.map { it.toQuizQuestion() }
            }
        }

        // 4. Calculate batch selection offset
        val startIndex = (dayIndex * countToSelect) % totalCount
        val endIndex = if (countToSelect > 0) (startIndex + countToSelect - 1) else 0
        val offsetValue = dayIndex * countToSelect

        val selectedEntities = mutableListOf<QuestionEntity>()
        for (i in 0 until countToSelect) {
            val idx = (startIndex + i) % totalCount
            selectedEntities.add(allEntities[idx])
        }

        val assignedIds = selectedEntities.map { it.id }

        val offsetDetails = OffsetDetails(
            offsetValue = offsetValue,
            startIndex = startIndex,
            endIndex = endIndex,
            generatedIds = assignedIds
        )
        lastOffsetDetails[normKey] = offsetDetails

        // 5. Permanently save assigned question IDs, last date, and day index
        saveAssignedState(normKey, todayDate, dayIndex, assignedIds)

        Log.d("DEBUG_OFFSET", "1. Calculated offset value: $offsetValue (dayIndex: $dayIndex)")
        Log.d("DEBUG_OFFSET", "2. Start index: $startIndex")
        Log.d("DEBUG_OFFSET", "3. End index: $endIndex")
        Log.d("DEBUG_OFFSET", "4. Exact list of IDs generated before querying database: $assignedIds")

        Log.d("DEBUG_DAILY_QUIZ", "1. Calculated Day Number: $dayNumber")
        Log.d("DEBUG_DAILY_QUIZ", "2. assigned_ids received: $assignedIds")
        Log.d("DEBUG_DAILY_QUIZ", "3. Question IDs returned from database: ${selectedEntities.map { it.id }}")

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
                val cmp = num1.compareTo(num2)
                if (cmp != 0) cmp else id1.compareTo(id2, ignoreCase = true)
            } else {
                id1.compareTo(id2, ignoreCase = true)
            }
        }
    }

    fun getCurrentDateString(): String {
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

    private fun getSavedQuestionIds(normKey: String, todayDate: String, expectedDayIndex: Int? = null): List<String> {
        val lastDate = getLastDate(normKey)
        if (lastDate != null && lastDate != todayDate) {
            // Invalidate stale cache from previous date
            invalidateOldCache(normKey, lastDate)
            return emptyList()
        }

        if (expectedDayIndex != null) {
            val savedIndex = getDayIndex(normKey)
            if (savedIndex != expectedDayIndex) {
                if (lastDate != null) invalidateOldCache(normKey, lastDate)
                Log.d("QuestionSelectionEngine", "Invalidated stale cache due to day index mismatch (saved: $savedIndex, expected: $expectedDayIndex)")
                return emptyList()
            }
        }

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

    private fun invalidateOldCache(normKey: String, oldDate: String) {
        val prefs = getPrefs()
        val oldIdsKey = "assigned_ids_${normKey}_${oldDate}"
        val oldDateKey = "last_date_${normKey}"
        val oldIndexKey = "day_index_${normKey}"
        prefs?.edit()
            ?.remove(oldIdsKey)
            ?.remove(oldDateKey)
            ?.remove(oldIndexKey)
            ?.apply()
        inMemoryAssignedIds.remove(oldIdsKey)
        inMemoryLastDate.remove(oldDateKey)
        inMemoryDayIndex.remove(oldIndexKey)
        Log.d("QuestionSelectionEngine", "Invalidated stale daily cache for '$normKey' from $oldDate")
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

