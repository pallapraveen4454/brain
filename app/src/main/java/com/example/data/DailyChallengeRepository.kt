package com.example.data

import android.content.Context
import android.util.Log
import com.example.BrainQuizApplication
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Dedicated repository for the Daily Challenge reward system.
 *
 * Enforces one-claim-per-calendar-day for the Daily Challenge reward.
 * This is strictly separate from category quizzes and hint rewards.
 */
class DailyChallengeRepository(
    private val context: Context? = try { BrainQuizApplication.instance } catch (e: Exception) { null }
) {
    companion object {
        private const val PREFS_NAME = "daily_challenge_prefs"
        private const val KEY_LAST_CLAIMED_DATE = "daily_challenge_last_claimed_date"
        const val REWARD_ID_PREFIX = "daily_challenge_claimed_"
    }

    private var inMemoryLastClaimedDate: String? = null

    private fun getPrefs() = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns current calendar date string in YYYY-MM-DD format.
     */
    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    /**
     * Calculates the next calendar date in YYYY-MM-DD format.
     */
    fun getNextAvailableDateString(): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, 1)
            sdf.format(cal.time)
        } catch (e: Exception) {
            "Tomorrow"
        }
    }

    /**
     * Checks if today's Daily Challenge reward is available to be claimed.
     * Returns true if NOT yet claimed on today's calendar date.
     * Returns false if already claimed today.
     *
     * Automatically resets on the next calendar day because today's date string changes.
     */
    fun isDailyRewardAvailable(userProfile: UserProfile? = null): Boolean {
        val today = getTodayDateString()

        // 1. Check local persistent SharedPreferences (survives app restart for Guest and Auth)
        val savedDate = getPrefs()?.getString(KEY_LAST_CLAIMED_DATE, "") ?: inMemoryLastClaimedDate ?: ""
        if (savedDate == today) {
            return false
        }

        val expectedRewardKey = "$REWARD_ID_PREFIX$today"

        // 2. Check provided UserProfile claimedRewards
        if (userProfile != null && userProfile.claimedRewards.contains(expectedRewardKey)) {
            // Keep local prefs in sync
            inMemoryLastClaimedDate = today
            getPrefs()?.edit()?.putString(KEY_LAST_CLAIMED_DATE, today)?.apply()
            return false
        }

        // 3. Check active profile in UserProfileStore
        try {
            val store = UserProfileStore(context)
            val profile = store.getProfile()
            if (profile.claimedRewards.contains(expectedRewardKey)) {
                inMemoryLastClaimedDate = today
                getPrefs()?.edit()?.putString(KEY_LAST_CLAIMED_DATE, today)?.apply()
                return false
            }
        } catch (e: Exception) {
            // Context or store unavailable; fallback to savedDate check
        }

        return true
    }

    /**
     * Marks the Daily Challenge reward as successfully claimed for today.
     * Must ONLY be called after the quiz completion is actually successful.
     * Returns the claimed reward key to sync into UserProfile.claimedRewards.
     */
    fun markDailyRewardClaimed(): String {
        val today = getTodayDateString()
        inMemoryLastClaimedDate = today
        getPrefs()?.edit()?.putString(KEY_LAST_CLAIMED_DATE, today)?.apply()
        val rewardKey = "$REWARD_ID_PREFIX$today"
        Log.d("DailyChallenge", "Successfully marked Daily Challenge reward claimed for date: $today (key: $rewardKey)")
        return rewardKey
    }

    /**
     * Returns the date when Daily Challenge reward was last claimed.
     */
    fun getLastClaimedDate(): String {
        return getPrefs()?.getString(KEY_LAST_CLAIMED_DATE, "") ?: inMemoryLastClaimedDate ?: ""
    }

    /**
     * Resets Daily Challenge reward claim state (e.g., during account progress reset).
     */
    fun resetDailyReward() {
        try {
            inMemoryLastClaimedDate = null
            getPrefs()?.edit()?.remove(KEY_LAST_CLAIMED_DATE)?.apply()
            Log.d("DailyChallenge", "Reset Daily Challenge reward claim state.")
        } catch (e: Exception) {
            Log.e("DailyChallenge", "Failed to reset daily challenge reward", e)
        }
    }
}
