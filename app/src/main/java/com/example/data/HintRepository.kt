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

    /**
     * Map raw category IDs to canonical keys for the 8 official categories.
     * 1. General Knowledge -> "gk"
     * 2. Science           -> "science"
     * 3. Sports            -> "sports"
     * 4. History           -> "history"
     * 5. Movies            -> "movies"
     * 6. Technology        -> "tech"
     * 7. Geography         -> "geo"
     * 8. Mathematics       -> "math"
     */
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
            else -> norm.ifBlank { "gk" }
        }
    }

    /**
     * Get current local calendar date string (YYYY-MM-DD).
     */
    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    /**
     * Check if the daily hint for the given category is available for today's calendar date.
     * Returns true if available (has NOT been used today).
     * Returns false if used today.
     */
    fun isHintAvailableForCategory(categoryId: String): Boolean {
        val key = getCanonicalCategoryKey(categoryId)
        val prefs = getPrefs() ?: return true
        val lastUsedDate = prefs.getString("hint_used_date_$key", "") ?: ""
        val today = getTodayDateString()
        val isAvailable = (lastUsedDate != today)
        Log.d("HINT_SYSTEM", "isHintAvailableForCategory: categoryId='$categoryId' (key='$key'), lastUsedDate='$lastUsedDate', today='$today' -> isAvailable=$isAvailable")
        return isAvailable
    }

    /**
     * Mark the daily hint for the given category as consumed for today's calendar date.
     * Must ONLY be called after a rewarded ad is completed and reward callback confirmed.
     */
    fun markHintUsedForCategory(categoryId: String) {
        val key = getCanonicalCategoryKey(categoryId)
        val today = getTodayDateString()
        getPrefs()?.edit()?.putString("hint_used_date_$key", today)?.apply()
        Log.d("HINT_SYSTEM", "markHintUsedForCategory: Marked hint used for categoryId='$categoryId' (key='$key') on date '$today'")
    }

    /**
     * Returns the last used date string for debug/testing if needed.
     */
    fun getLastUsedDateForCategory(categoryId: String): String {
        val key = getCanonicalCategoryKey(categoryId)
        return getPrefs()?.getString("hint_used_date_$key", "") ?: ""
    }
}
