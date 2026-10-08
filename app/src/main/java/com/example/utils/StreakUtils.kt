package com.example.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object StreakUtils {
    /**
     * Calculates the updated streak and last active date when an active quiz is completed.
     * Returns Pair(updatedStreak, updatedLastActiveDate).
     *
     * Rules:
     * - Save last active date (yyyy-MM-dd).
     * - Day 1 (first active day or after a reset): starts at Streak 1.
     * - When completing multiple quizzes on the same day (diff == 0 days): retain current streak (do not increase).
     * - When completing a quiz on the next consecutive active day (diff == 1 day): increase streak by 1 (Day 1 = 1, Day 2 = 2, Day 3 = 3, etc.).
     * - If a full day is missed (diff > 1 day): the next active day starts at Streak 1.
     */
    fun calculateStreak(lastActiveDateStr: String, currentStreak: Int): Pair<Int, String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date())

        if (lastActiveDateStr.isBlank()) {
            return Pair(1, todayStr)
        }

        if (lastActiveDateStr == todayStr) {
            val sameDayStreak = if (currentStreak <= 0) 1 else currentStreak
            return Pair(sameDayStreak, todayStr)
        }

        return try {
            val lastDate = dateFormat.parse(lastActiveDateStr)
            val todayDate = dateFormat.parse(todayStr)
            if (lastDate != null && todayDate != null) {
                val lastCal = Calendar.getInstance().apply {
                    time = lastDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayCal = Calendar.getInstance().apply {
                    time = todayDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val diffMs = todayCal.timeInMillis - lastCal.timeInMillis
                // Add 12 hours (43200000L) for daylight saving safety before dividing by 24h
                val diffDays = ((diffMs + 43200000L) / 86400000L).toInt()

                when {
                    diffDays <= 0 -> {
                        // Same day or clock skew: retain current streak
                        val fallbackStreak = if (currentStreak <= 0) 1 else currentStreak
                        Pair(fallbackStreak, todayStr)
                    }
                    diffDays == 1 -> {
                        // Next consecutive active day: increment by 1
                        val nextStreak = if (currentStreak <= 0) 1 else currentStreak + 1
                        Pair(nextStreak, todayStr)
                    }
                    else -> {
                        // Missed a full day (diffDays > 1): next active day starts at Streak 1
                        Pair(1, todayStr)
                    }
                }
            } else {
                Pair(1, todayStr)
            }
        } catch (e: Exception) {
            Pair(1, todayStr)
        }
    }

    /**
     * Returns the current active streak for display or checking without advancing it.
     * - If user was active today: returns currentStreak.
     * - If user was active yesterday (diff == 1): streak is still active from yesterday, returns currentStreak.
     * - If user missed a full day (diff > 1): streak has lapsed, returns 0.
     * - If no active date is recorded: returns currentStreak (or 0 if <= 0).
     */
    fun getDisplayStreak(lastActiveDateStr: String, currentStreak: Int): Int {
        if (currentStreak <= 0) return 0
        if (lastActiveDateStr.isBlank()) return currentStreak
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date())
        if (lastActiveDateStr == todayStr) return currentStreak

        return try {
            val lastDate = dateFormat.parse(lastActiveDateStr)
            val todayDate = dateFormat.parse(todayStr)
            if (lastDate != null && todayDate != null) {
                val lastCal = Calendar.getInstance().apply {
                    time = lastDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayCal = Calendar.getInstance().apply {
                    time = todayDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val diffMs = todayCal.timeInMillis - lastCal.timeInMillis
                val diffDays = ((diffMs + 43200000L) / 86400000L).toInt()
                if (diffDays <= 1) currentStreak else 0
            } else 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Resolves the current streak and active date between two profile sources (e.g. local and remote).
     * Prevents stale streaks with older dates from reviving expired streaks via maxOf.
     */
    fun resolveStreak(dateA: String, streakA: Int, dateB: String, streakB: Int): Pair<Int, String> {
        return when {
            dateA.isBlank() && dateB.isBlank() -> Pair(maxOf(0, maxOf(streakA, streakB)), "")
            dateA.isBlank() -> Pair(getDisplayStreak(dateB, streakB), dateB)
            dateB.isBlank() -> Pair(getDisplayStreak(dateA, streakA), dateA)
            dateA == dateB -> Pair(maxOf(streakA, streakB), dateA)
            dateA > dateB -> Pair(getDisplayStreak(dateA, streakA), dateA)
            else -> Pair(getDisplayStreak(dateB, streakB), dateB)
        }
    }
}
