package com.example.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StreakUtils {
    /**
     * Calculates the updated streak and last active date.
     * Returns Pair(updatedStreak, updatedLastActiveDate).
     *
     * Rules:
     * - Save last active date (yyyy-MM-dd).
     * - When user opens the app on the next day (diff == 1 day): increase streak by 1.
     * - When user opens the app on the same day (diff == 0 days): retain current streak.
     * - If the user misses a day (diff > 1 day): reset streak to 1.
     * - If no previous active date exists: set streak to 1 and set last active date to today.
     */
    fun calculateStreak(lastActiveDateStr: String, currentStreak: Int): Pair<Int, String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date())

        if (lastActiveDateStr.isBlank()) {
            return Pair(maxOf(0, currentStreak), todayStr)
        }

        if (lastActiveDateStr == todayStr) {
            return Pair(maxOf(0, currentStreak), todayStr)
        }

        return try {
            val lastDate = dateFormat.parse(lastActiveDateStr)
            val todayDate = dateFormat.parse(todayStr)
            if (lastDate != null && todayDate != null) {
                val diffMs = todayDate.time - lastDate.time
                val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
                when {
                    diffDays == 1 -> Pair(maxOf(0, currentStreak) + 1, todayStr)
                    diffDays > 1 -> Pair(maxOf(0, currentStreak), todayStr)
                    else -> Pair(maxOf(0, currentStreak), todayStr)
                }
            } else {
                Pair(maxOf(0, currentStreak), todayStr)
            }
        } catch (e: Exception) {
            Pair(maxOf(0, currentStreak), todayStr)
        }
    }
}
