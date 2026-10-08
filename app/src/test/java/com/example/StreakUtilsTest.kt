package com.example

import com.example.utils.StreakUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StreakUtilsTest {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun getPastDate(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return dateFormat.format(cal.time)
    }

    private fun getTodayDate(): String {
        return dateFormat.format(Date())
    }

    @Test
    fun testFirstActiveDayStartsAtStreak1() {
        // Blank lastActiveDate, streak 0
        val (newStreak, date) = StreakUtils.calculateStreak("", 0)
        assertEquals(1, newStreak)
        assertEquals(getTodayDate(), date)
    }

    @Test
    fun testMultipleQuizzesOnSameDayDoesNotIncreaseStreak() {
        val today = getTodayDate()
        // Day 1: quiz completed -> streak 1
        val (firstQuizStreak, _) = StreakUtils.calculateStreak("", 0)
        assertEquals(1, firstQuizStreak)

        // Day 1: second quiz completed on the same day -> streak must remain 1
        val (secondQuizStreak, secondDate) = StreakUtils.calculateStreak(today, 1)
        assertEquals(1, secondQuizStreak)
        assertEquals(today, secondDate)

        // Day 1: third quiz completed on the same day -> streak must remain 1
        val (thirdQuizStreak, thirdDate) = StreakUtils.calculateStreak(today, 1)
        assertEquals(1, thirdQuizStreak)
        assertEquals(today, thirdDate)
    }

    @Test
    fun testConsecutiveDaysIncreaseStreakCorrectly() {
        val yesterday = getPastDate(1)
        val today = getTodayDate()

        // Day 1 = 1, Day 2 = 2
        val (streakDay2, dateDay2) = StreakUtils.calculateStreak(yesterday, 1)
        assertEquals(2, streakDay2)
        assertEquals(today, dateDay2)

        // If current was 2 from yesterday, today completes -> 3
        val (streakDay3, dateDay3) = StreakUtils.calculateStreak(yesterday, 2)
        assertEquals(3, streakDay3)
        assertEquals(today, dateDay3)
    }

    @Test
    fun testMissedDayResetsToStreak1OnNextActiveDay() {
        val twoDaysAgo = getPastDate(2)
        val fiveDaysAgo = getPastDate(5)
        val today = getTodayDate()

        // Missed 1 full day (last active 2 days ago)
        val (resetStreak2, date2) = StreakUtils.calculateStreak(twoDaysAgo, 5)
        assertEquals(1, resetStreak2)
        assertEquals(today, date2)

        // Missed multiple days (last active 5 days ago)
        val (resetStreak5, date5) = StreakUtils.calculateStreak(fiveDaysAgo, 10)
        assertEquals(1, resetStreak5)
        assertEquals(today, date5)
    }

    @Test
    fun testDisplayStreak() {
        val today = getTodayDate()
        val yesterday = getPastDate(1)
        val twoDaysAgo = getPastDate(2)

        // Active today: returns current streak
        assertEquals(3, StreakUtils.getDisplayStreak(today, 3))

        // Active yesterday: streak is still alive waiting for today's activity
        assertEquals(3, StreakUtils.getDisplayStreak(yesterday, 3))

        // Missed yesterday (last active 2 days ago): streak has lapsed
        assertEquals(0, StreakUtils.getDisplayStreak(twoDaysAgo, 3))

        // No active date, streak 0: returns 0
        assertEquals(0, StreakUtils.getDisplayStreak("", 0))
    }

    @Test
    fun testResolveStreakDoesNotReviveStaleStreaksViaMaxOf() {
        val today = getTodayDate()
        val monthAgo = getPastDate(30)

        // Local active today (streak 1), Remote active month ago (streak 15)
        val (resolvedStreak, resolvedDate) = StreakUtils.resolveStreak(
            dateA = today,
            streakA = 1,
            dateB = monthAgo,
            streakB = 15
        )
        // Local is newer: must NOT be overridden by remote's stale 15
        assertEquals(1, resolvedStreak)
        assertEquals(today, resolvedDate)
    }
}
