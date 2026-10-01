package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.BrainQuizApplication
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Dedicated repository for the Daily Challenge reward system.
 *
 * Enforces one-claim-per-calendar-day for the Daily Challenge reward per account.
 * This is strictly separate from category quizzes and hint rewards, and strictly
 * isolated between Guest and authenticated Google/Email accounts.
 */
class DailyChallengeRepository(
    private val context: Context? = try { BrainQuizApplication.instance } catch (e: Exception) { null }
) {
    companion object {
        private const val BASE_PREFS_NAME = "daily_challenge_prefs"
        private const val KEY_LAST_CLAIMED_DATE = "daily_challenge_last_claimed_date"
        const val REWARD_ID_PREFIX = "daily_challenge_claimed_"
    }

    private val inMemoryLastClaimedDates = ConcurrentHashMap<String, String>()

    init {
        // Clean up legacy unisolated shared preferences file if it exists
        try {
            val appCtx = context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null }
            appCtx?.getSharedPreferences(BASE_PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
        } catch (e: Exception) {
            // Ignore legacy cleanup errors
        }
    }

    private fun getAccountKey(targetProfile: UserProfile? = null): String {
        val store = try { UserProfileStore(context) } catch (e: Exception) { null }
        if (targetProfile != null) {
            val uid = targetProfile.uid
            val isTargetGuest = uid.startsWith("guest_") || targetProfile.email == "Guest Account"
            return if (isTargetGuest) {
                val guestId = if (uid.isNotBlank() && uid.startsWith("guest_")) uid else {
                    try { store?.getGuestId() ?: "default_guest" } catch (e: Exception) { "default_guest" }
                }
                "guest_$guestId"
            } else if (uid.isNotBlank()) {
                "uid_$uid"
            } else {
                "guest_default"
            }
        }

        val isGuest = try { store?.isGuestActive() ?: false } catch (e: Exception) { false }
        val profile = try { store?.getProfile() } catch (e: Exception) { null }
        val uid = profile?.uid ?: ""

        return if (isGuest || uid.startsWith("guest_") || profile?.email == "Guest Account") {
            val guestId = if (uid.isNotBlank() && uid.startsWith("guest_")) uid else {
                try { store?.getGuestId() ?: "default_guest" } catch (e: Exception) { "default_guest" }
            }
            "guest_$guestId"
        } else if (uid.isNotBlank()) {
            "uid_$uid"
        } else {
            val fbUid = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid } catch (e: Exception) { null }
            if (!fbUid.isNullOrBlank()) "uid_$fbUid" else "guest_default"
        }
    }

    private fun getPrefs(accountKey: String): SharedPreferences? {
        val appCtx = context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null }
        return appCtx?.getSharedPreferences("${BASE_PREFS_NAME}_$accountKey", Context.MODE_PRIVATE)
    }

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
     * Checks if today's Daily Challenge reward is available to be claimed for the current or given account.
     * Returns true if NOT yet claimed on today's calendar date for this specific account.
     * Returns false if already claimed today by this specific account.
     *
     * Automatically resets on the next calendar day because today's date string changes.
     */
    fun isDailyRewardAvailable(userProfile: UserProfile? = null): Boolean {
        val today = getTodayDateString()
        val accountKey = getAccountKey(userProfile)
        val expectedRewardKey = "$REWARD_ID_PREFIX$today"

        // 1. Check in-memory cache for this specific account
        if (inMemoryLastClaimedDates[accountKey] == today) {
            return false
        }

        // 2. Check local persistent SharedPreferences for this specific account
        val prefs = getPrefs(accountKey)
        val savedDate = prefs?.getString(KEY_LAST_CLAIMED_DATE, "") ?: ""
        if (savedDate == today) {
            inMemoryLastClaimedDates[accountKey] = today
            return false
        }

        // 3. Check provided UserProfile claimedRewards
        if (userProfile != null && userProfile.claimedRewards.contains(expectedRewardKey)) {
            inMemoryLastClaimedDates[accountKey] = today
            prefs?.edit()?.putString(KEY_LAST_CLAIMED_DATE, today)?.apply()
            return false
        }

        // 4. Check active profile in UserProfileStore only if targetProfile was not explicitly provided
        if (userProfile == null) {
            try {
                val store = UserProfileStore(context)
                val profile = store.getProfile()
                if (profile.claimedRewards.contains(expectedRewardKey)) {
                    inMemoryLastClaimedDates[accountKey] = today
                    prefs?.edit()?.putString(KEY_LAST_CLAIMED_DATE, today)?.apply()
                    return false
                }
            } catch (e: Exception) {
                // Context or store unavailable; fallback
            }
        }

        return true
    }

    /**
     * Marks the Daily Challenge reward as successfully claimed for today for the current or given account.
     * Must ONLY be called after the quiz completion is actually successful.
     * Returns the claimed reward key to sync into UserProfile.claimedRewards.
     */
    fun markDailyRewardClaimed(userProfile: UserProfile? = null): String {
        val today = getTodayDateString()
        val accountKey = getAccountKey(userProfile)
        inMemoryLastClaimedDates[accountKey] = today
        getPrefs(accountKey)?.edit()?.putString(KEY_LAST_CLAIMED_DATE, today)?.apply()
        val rewardKey = "$REWARD_ID_PREFIX$today"
        Log.d("DailyChallenge", "Successfully marked Daily Challenge claimed for account '$accountKey' on date: $today (key: $rewardKey)")
        return rewardKey
    }

    /**
     * Returns the date when Daily Challenge reward was last claimed for the current or given account.
     */
    fun getLastClaimedDate(userProfile: UserProfile? = null): String {
        val accountKey = getAccountKey(userProfile)
        return getPrefs(accountKey)?.getString(KEY_LAST_CLAIMED_DATE, "")
            ?: inMemoryLastClaimedDates[accountKey]
            ?: ""
    }

    /**
     * Resets Daily Challenge reward claim state for this specific account.
     */
    fun resetDailyReward(userProfile: UserProfile? = null) {
        try {
            val accountKey = getAccountKey(userProfile)
            inMemoryLastClaimedDates.remove(accountKey)
            getPrefs(accountKey)?.edit()?.remove(KEY_LAST_CLAIMED_DATE)?.apply()
            Log.d("DailyChallenge", "Reset Daily Challenge reward claim state for account: $accountKey")
        } catch (e: Exception) {
            Log.e("DailyChallenge", "Failed to reset daily challenge reward", e)
        }
    }
}
