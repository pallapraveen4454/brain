package com.example.data

import android.content.Context
import android.util.Log
import com.example.BrainQuizApplication
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class HintRepository(
    private val context: Context? = try { BrainQuizApplication.instance } catch (e: Exception) { null }
) {
    private val prefsName = "brain_quiz_hint_prefs"

    companion object {
        const val KEY_GLOBAL_HINT_USED_DATE = "global_hint_used_date"
        // In-memory fallback for JVM unit testing when Context is unavailable
        internal var inMemoryGlobalHintDate: String? = null
        internal val inMemoryCategoryHintDates = ConcurrentHashMap<String, String>()
    }

    private fun getPrefs() = context?.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    /**
     * Map raw category IDs to canonical keys for categories.
     * 1. General Knowledge -> "gk"
     * 2. Science           -> "science"
     * 3. Sports            -> "sports"
     * 4. History           -> "history"
     * 5. Movies            -> "movies"
     * 6. Technology        -> "tech"
     * 7. Geography         -> "geo"
     * 8. Mathematics       -> "math"
     * Plus Daily Challenge -> "daily" and Quick Play -> "quick"
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
            norm == "daily" || norm == "daily challenge" || norm == "dailychallenge" -> "daily"
            norm == "quick" || norm == "quick play" || norm == "quickplay" -> "quick"
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
     * Check if the single global daily hint is available for today's calendar date.
     * Only 1 hint is allowed across the entire app per calendar day.
     * Returns true if available (has NOT been used today).
     * Returns false if already used today.
     */
    fun isGlobalHintAvailable(todayDateOverride: String? = null): Boolean {
        val today = todayDateOverride ?: getTodayDateString()
        val prefs = getPrefs()
        val lastUsedDate = if (prefs != null) {
            prefs.getString(KEY_GLOBAL_HINT_USED_DATE, "") ?: ""
        } else {
            inMemoryGlobalHintDate ?: ""
        }
        val isAvailable = (lastUsedDate != today)
        Log.d("HINT_SYSTEM", "isGlobalHintAvailable: lastUsedDate='$lastUsedDate', today='$today' -> isAvailable=$isAvailable")
        return isAvailable
    }

    /**
     * Mark the global daily hint as consumed for today's calendar date.
     * Must ONLY be called after a rewarded ad is completed and hint unlock confirmed.
     */
    fun markGlobalHintUsed(todayDateOverride: String? = null) {
        val today = todayDateOverride ?: getTodayDateString()
        val prefs = getPrefs()
        if (prefs != null) {
            prefs.edit().putString(KEY_GLOBAL_HINT_USED_DATE, today).apply()
        } else {
            inMemoryGlobalHintDate = today
        }
        Log.d("HINT_SYSTEM", "markGlobalHintUsed: Marked global hint used on date '$today'")
    }

    /**
     * Check if the daily hint for the given category is available for today's calendar date.
     * Delegates to global hint availability to enforce one global hint per calendar day across all categories.
     * Returns true if available (has NOT been used today).
     * Returns false if used today.
     */
    fun isHintAvailableForCategory(categoryId: String, todayDateOverride: String? = null): Boolean {
        return isGlobalHintAvailable(todayDateOverride)
    }

    /**
     * Mark the daily hint as consumed for today's calendar date for both category and global state.
     * Must ONLY be called after a rewarded ad is completed and reward callback confirmed.
     */
    fun markHintUsedForCategory(categoryId: String, todayDateOverride: String? = null) {
        val key = getCanonicalCategoryKey(categoryId)
        val today = todayDateOverride ?: getTodayDateString()
        val prefs = getPrefs()
        if (prefs != null) {
            prefs.edit()
                .putString(KEY_GLOBAL_HINT_USED_DATE, today)
                .putString("hint_used_date_$key", today)
                .apply()
        } else {
            inMemoryGlobalHintDate = today
            inMemoryCategoryHintDates[key] = today
        }
        Log.d("HINT_SYSTEM", "markHintUsedForCategory: Marked hint used for categoryId='$categoryId' (key='$key') globally on date '$today'")
    }

    /**
     * Returns the global last used date string for debug/testing if needed.
     */
    fun getGlobalHintLastUsedDate(): String {
        val prefs = getPrefs()
        return if (prefs != null) {
            prefs.getString(KEY_GLOBAL_HINT_USED_DATE, "") ?: ""
        } else {
            inMemoryGlobalHintDate ?: ""
        }
    }

    /**
     * Returns the last used date string for category for debug/testing if needed.
     */
    fun getLastUsedDateForCategory(categoryId: String): String {
        val key = getCanonicalCategoryKey(categoryId)
        val prefs = getPrefs()
        return if (prefs != null) {
            prefs.getString("hint_used_date_$key", "") ?: ""
        } else {
            inMemoryCategoryHintDates[key] ?: ""
        }
    }

    /**
     * Reset all global and category hint usage state.
     */
    fun resetAllHints() {
        try {
            getPrefs()?.edit()?.clear()?.apply()
            inMemoryGlobalHintDate = null
            inMemoryCategoryHintDates.clear()
            Log.d("HINT_SYSTEM", "resetAllHints: Cleared all hint usage preferences and in-memory caches.")
        } catch (e: Exception) {
            Log.e("HINT_SYSTEM", "resetAllHints failed", e)
        }
    }
}
