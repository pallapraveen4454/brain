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
            val initialStreak = if (currentStreak <= 0) 1 else currentStreak
            return Pair(initialStreak, todayStr)
        }

        if (lastActiveDateStr == todayStr) {
            val sameDayStreak = if (currentStreak <= 0) 1 else currentStreak
            return Pair(sameDayStreak, todayStr)
        }

        return try {
            val lastDate = dateFormat.parse(lastActiveDateStr)
            val todayDate = dateFormat.parse(todayStr)
            if (lastDate != null && todayDate != null) {
                val diffMs = todayDate.time - lastDate.time
                val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
                when {
                    diffDays == 1 -> {
                        val nextStreak = if (currentStreak <= 0) 2 else currentStreak + 1
                        Pair(nextStreak, todayStr)
                    }
                    diffDays > 1 -> {
                        Pair(1, todayStr)
                    }
                    else -> {
                        val fallbackStreak = if (currentStreak <= 0) 1 else currentStreak
                        Pair(fallbackStreak, todayStr)
                    }
                }
            } else {
                val fallbackStreak = if (currentStreak <= 0) 1 else currentStreak
                Pair(fallbackStreak, todayStr)
            }
        } catch (e: Exception) {
            val fallbackStreak = if (currentStreak <= 0) 1 else currentStreak
            Pair(fallbackStreak, todayStr)
        }
    }
}
