package com.example.data

import android.content.Context
import android.util.Log
import com.example.BrainQuizApplication
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HintRepository(
    private val context: Context? = try { BrainQuizApplication.instance } catch (e: Exception) { null }
) {
    private val prefsName = "brain_quiz_hint_prefs"

    private fun getPrefs() = context?.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun getCanonicalCategoryKey(rawCategoryId: String): String {
        val norm = rawCategoryId.lowercase().trim()
        return when {
            norm == "gk" || norm == "general knowledge" || norm == "generalknowledge" -> "gk"
            norm == "science" -> "science"
            norm == "sports" -> "sports"
            norm == "history" -> "history"
            norm == "movies" -> "movies"
            norm == "tech" || norm == "technology" -> "tech"
            norm == "geo" || norm == "geography" -> "geo"
            norm == "math" || norm == "mathematics" -> "math"
            norm == "daily" || norm == "daily challenge" || norm == "dailychallenge" -> "daily"
            norm == "quick" || norm == "quick play" || norm == "quickplay" -> "quick"
            else -> norm.ifBlank { "gk" }
        }
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun isHintAvailableForCategory(categoryId: String): Boolean {
        val key = getCanonicalCategoryKey(categoryId)
        val today = getTodayDateString()
        val prefs = getPrefs() ?: return true
        val lastUsedDate = prefs.getString("hint_used_date_$key", "") ?: ""
        return (lastUsedDate != today)
    }

    fun markHintUsedForCategory(categoryId: String) {
        val key = getCanonicalCategoryKey(categoryId)
        val today = getTodayDateString()
        getPrefs()?.edit()?.putString("hint_used_date_$key", today)?.apply()
        Log.d("HINT_SYSTEM", "markHintUsedForCategory: Marked hint used for categoryId='$categoryId' on date '$today'")
    }

    fun getLastUsedDateForCategory(categoryId: String): String {
        val key = getCanonicalCategoryKey(categoryId)
        return getPrefs()?.getString("hint_used_date_$key", "") ?: ""
    }

    fun resetAllHints() {
        try {
            getPrefs()?.edit()?.clear()?.apply()
            Log.d("HINT_SYSTEM", "resetAllHints: Cleared all hint usage preferences.")
        } catch (e: Exception) {
            Log.e("HINT_SYSTEM", "resetAllHints failed", e)
        }
    }
}
