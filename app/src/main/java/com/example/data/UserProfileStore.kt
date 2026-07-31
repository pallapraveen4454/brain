package com.example.data

import android.content.Context
import android.util.Log
import com.example.BrainQuizApplication
import com.example.data.model.QuizResult
import com.example.utils.RankUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class UserProfileStore(
    private val context: Context? = try { BrainQuizApplication.instance } catch (e: Exception) { null }
) {
    private val prefsName = "user_profile_prefs"
    private val keyProfileJson = "persistent_user_profile"

    private fun getPrefs() = context?.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun getGuestId(): String {
        val ctx = context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null }
        val authPrefs = ctx?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val quizPrefs = ctx?.getSharedPreferences("quiz_results_prefs", Context.MODE_PRIVATE)
        val profilePrefs = getPrefs()

        var id = authPrefs?.getString("guest_user_id", "") ?: ""

        if (id.isBlank()) {
            val jsonStr = profilePrefs?.getString(keyProfileJson, "") ?: ""
            if (jsonStr.isNotBlank()) {
                try {
                    val jsonObj = JSONObject(jsonStr)
                    val jsonUid = jsonObj.optString("uid", "")
                    if (jsonUid.isNotBlank() && jsonUid.startsWith("guest_")) {
                        id = jsonUid
                    }
                } catch (e: Exception) {
                    Log.e("UserProfileStore", "Error parsing json in getGuestId", e)
                }
            }
        }

        if (id.isBlank()) {
            id = quizPrefs?.getString("guest_user_id", "") ?: ""
        }

        if (id.isBlank()) {
            val randomNum = (100000..999999).random()
            id = "guest_$randomNum"
            Log.d("GuestAccount", "First-ever launch - Creating initial Guest ID: $id")
        } else {
            Log.d("GuestAccount", "Existing Guest ID Found: $id")
            Log.d("GuestAccount", "No New Guest Account Created")
        }

        authPrefs?.edit()?.putString("guest_user_id", id)?.apply()
        quizPrefs?.edit()?.putString("guest_user_id", id)?.apply()

        return id
    }

    fun isGuestActive(): Boolean {
        val ctx = context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null }
        val prefs = ctx?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return prefs?.getBoolean("is_guest_active", false) ?: true
    }

    fun setGuestActive(active: Boolean) {
        val ctx = context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null }
        ctx?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)?.edit()?.putBoolean("is_guest_active", active)?.apply()
    }

    fun isLoggedIn(): Boolean {
        val ctx = context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null }
        val prefs = ctx?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val isAuthLoggedIn = prefs?.getBoolean("is_logged_in", false) ?: false
        val isGuest = isGuestActive()
        return isAuthLoggedIn || isGuest || hasSavedProfile() || true
    }

    fun setLoggedIn(loggedIn: Boolean) {
        val ctx = context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null }
        ctx?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)?.edit()
            ?.putBoolean("is_logged_in", loggedIn)
            ?.putBoolean("is_guest_active", loggedIn)
            ?.apply()
    }

    fun hasSavedProfile(): Boolean {
        val jsonStr = getPrefs()?.getString(keyProfileJson, "") ?: ""
        if (jsonStr.isNotBlank()) return true

        val ctx = context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null }
        val quizPrefs = ctx?.getSharedPreferences("quiz_results_prefs", Context.MODE_PRIVATE)
        val hasXp = (quizPrefs?.getInt("user_total_xp", 0) ?: 0) > 0
        val hasHistory = quizPrefs?.getBoolean("has_quiz_history", false) ?: false
        return hasXp || hasHistory
    }

    fun getProfile(): UserProfile {
        Log.d("GuestAccount", "App Started")
        val guestId = getGuestId()
        Log.d("GuestAccount", "Loading Guest Profile")

        try {
            val jsonStr = getPrefs()?.getString(keyProfileJson, "") ?: ""
            if (jsonStr.isNotBlank()) {
                val profile = profileFromJson(JSONObject(jsonStr))
                val finalUid = if (profile.uid.isNotBlank()) profile.uid else guestId
                val loadedProfile = profile.copy(uid = finalUid)
                Log.d("GuestAccount", "Guest Profile Loaded Successfully")
                return loadedProfile
            }
        } catch (e: Exception) {
            Log.e("UserProfileStore", "Error loading profile from JSON", e)
        }

        // Recover from legacy SharedPreferences before creating a default new profile
        try {
            val ctx = context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null }
            if (ctx != null) {
                val quizPrefs = ctx.getSharedPreferences("quiz_results_prefs", Context.MODE_PRIVATE)
                val authPrefs = ctx.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

                val legacyXp = quizPrefs.getInt("user_total_xp", 0)
                val legacyCoins = quizPrefs.getInt("user_coins", 0)
                val legacyStreak = quizPrefs.getInt("user_streak", 0)
                val legacyName = authPrefs.getString("saved_custom_username", "") ?: ""
                val legacyAvatar = authPrefs.getString("saved_avatar_id", "brain") ?: "brain"

                if (legacyXp > 0 || legacyCoins > 0 || legacyName.isNotBlank()) {
                    val legacyProfile = UserProfile(
                        uid = guestId,
                        name = if (legacyName.isNotBlank()) legacyName else "Player",
                        email = "guest@brainquiz.ai",
                        avatarId = if (legacyAvatar.isNotBlank()) legacyAvatar else "brain",
                        xp = legacyXp,
                        level = maxOf(1, (legacyXp / 500) + 1),
                        coins = legacyCoins,
                        streak = legacyStreak,
                        rank = RankUtils.getRankForXp(legacyXp)
                    )
                    saveProfile(legacyProfile)
                    Log.d("GuestAccount", "Guest Profile Loaded Successfully")
                    return legacyProfile
                }
            }
        } catch (e: Exception) {
            Log.e("UserProfileStore", "Error recovering legacy profile", e)
        }

        // Truly a new initial user profile
        val defaultProfile = UserProfile(
            uid = guestId,
            name = "Player",
            email = "guest@brainquiz.ai",
            avatarId = "brain",
            xp = 0,
            level = 1,
            coins = 0,
            streak = 0,
            rank = "Beginner"
        )
        saveProfile(defaultProfile)
        setLoggedIn(true)
        Log.d("GuestAccount", "Guest Profile Loaded Successfully")
        return defaultProfile
    }

    fun resetGuestAccount(): UserProfile {
        Log.d("GuestAccount", "Guest Account explicitly reset by user")
        try {
            val ctx = context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null }
            getPrefs()?.edit()?.clear()?.apply()
            ctx?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
            ctx?.getSharedPreferences("quiz_results_prefs", Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
            ctx?.getSharedPreferences("achievements_prefs", Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
        } catch (e: Exception) {
            Log.e("UserProfileStore", "Error during resetGuestAccount", e)
        }
        return getProfile()
    }

    fun saveProfile(profile: UserProfile): UserProfile {
        try {
            val current = try {
                val jsonStr = getPrefs()?.getString(keyProfileJson, "") ?: ""
                if (jsonStr.isNotBlank()) profileFromJson(JSONObject(jsonStr)) else null
            } catch (e: Exception) { null }

            val mergedUid = profile.uid.ifBlank { current?.uid ?: getGuestId() }
            val currentName = current?.name?.ifBlank { "" } ?: ""
            val mergedName = when {
                profile.name.isNotBlank() && profile.name != "Player" && profile.name != "Guest Player" -> profile.name
                currentName.isNotBlank() && currentName != "Player" && currentName != "Guest Player" -> currentName
                profile.name.isNotBlank() -> profile.name
                currentName.isNotBlank() -> currentName
                else -> "Player"
            }

            val currentAvatar = current?.avatarId?.ifBlank { "" } ?: ""
            val mergedAvatar = when {
                profile.avatarId.isNotBlank() && profile.avatarId != "brain" -> profile.avatarId
                currentAvatar.isNotBlank() && currentAvatar != "brain" -> currentAvatar
                profile.avatarId.isNotBlank() -> profile.avatarId
                currentAvatar.isNotBlank() -> currentAvatar
                else -> "brain"
            }
            val mergedEmail = profile.email.ifBlank { current?.email ?: "guest@brainquiz.ai" }

            val mergedXp = maxOf(profile.xp, current?.xp ?: 0)
            val mergedCoins = maxOf(profile.coins, current?.coins ?: 0)
            val mergedStreak = maxOf(profile.streak, current?.streak ?: 0)
            val mergedLongestStreak = maxOf(profile.longestStreak, current?.longestStreak ?: 0, mergedStreak)
            val mergedLevel = maxOf(1, (mergedXp / 500) + 1)
            val mergedRank = RankUtils.getRankForXp(mergedXp)

            val mergedUnlocked = (current?.unlockedAchievements ?: emptySet()) + profile.unlockedAchievements
            val mergedClaimed = (current?.claimedRewards ?: emptySet()) + profile.claimedRewards
            val mergedUnlockedAvatars = (current?.unlockedAvatars ?: setOf("student_boy", "student_girl", "brain")) + profile.unlockedAvatars

            // Combine and deduplicate history
            val rawHistory = (profile.quizHistory + (current?.quizHistory ?: emptyList()))
            val combinedHistory = rawHistory
                .distinctBy { if (it.id.isNotBlank()) it.id else "${it.timestamp}_${it.categoryName}" }
                .sortedByDescending { it.timestamp }

            val mergedQuizzesPlayed = maxOf(profile.totalQuizzesPlayed, current?.totalQuizzesPlayed ?: 0, combinedHistory.size)
            val mergedQuestionsAnswered = maxOf(profile.totalQuestionsAnswered, current?.totalQuestionsAnswered ?: 0, combinedHistory.size * 10)
            val mergedCorrectAnswers = maxOf(profile.totalCorrectAnswers, current?.totalCorrectAnswers ?: 0, combinedHistory.sumOf { it.scoreOutOfTen })
            val mergedBestScore = maxOf(profile.bestScore, current?.bestScore ?: 0, combinedHistory.maxOfOrNull { it.scoreOutOfTen } ?: 0)

            val lastCategory = profile.lastQuizCategory.ifBlank { current?.lastQuizCategory ?: "" }
            val lastScore = if (profile.lastQuizScore > 0) profile.lastQuizScore else (current?.lastQuizScore ?: 0)
            val lastXp = if (profile.lastQuizXpEarned > 0) profile.lastQuizXpEarned else (current?.lastQuizXpEarned ?: 0)
            val lastDate = profile.lastQuizDate.ifBlank { current?.lastQuizDate ?: "" }
            val mergedInstallDate = profile.installDate.ifBlank { current?.installDate ?: "" }

            val updated = profile.copy(
                uid = mergedUid,
                name = mergedName,
                email = mergedEmail,
                avatarId = mergedAvatar,
                xp = mergedXp,
                level = mergedLevel,
                coins = mergedCoins,
                streak = mergedStreak,
                rank = mergedRank,
                unlockedAchievements = mergedUnlocked,
                claimedRewards = mergedClaimed,
                unlockedAvatars = mergedUnlockedAvatars,
                quizHistory = combinedHistory,
                lastQuizCategory = lastCategory,
                lastQuizScore = lastScore,
                lastQuizXpEarned = lastXp,
                lastQuizDate = lastDate,
                totalQuizzesPlayed = mergedQuizzesPlayed,
                totalQuestionsAnswered = mergedQuestionsAnswered,
                totalCorrectAnswers = mergedCorrectAnswers,
                bestScore = mergedBestScore,
                longestStreak = mergedLongestStreak,
                installDate = mergedInstallDate
            )

            val jsonObj = profileToJson(updated)
            getPrefs()?.edit()?.putString(keyProfileJson, jsonObj.toString())?.apply()

            // Keep legacy SharedPreferences keys in sync
            syncLegacyPrefs(updated)

            return updated
        } catch (e: Exception) {
            Log.e("UserProfileStore", "Error saving profile", e)
            return profile
        }
    }

    private fun syncLegacyPrefs(profile: UserProfile) {
        try {
            val ctx = context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null } ?: return
            
            // Sync auth_prefs
            ctx.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit().apply {
                putString("guest_user_id", profile.uid)
                putBoolean("is_guest_active", isGuestActive())
                putString("saved_custom_username", profile.name)
                putString("saved_avatar_id", profile.avatarId)
                apply()
            }

            // Sync quiz_results_prefs
            ctx.getSharedPreferences("quiz_results_prefs", Context.MODE_PRIVATE).edit().apply {
                putInt("user_total_xp", profile.xp)
                putInt("last_total_xp", profile.xp)
                putInt("user_level", profile.level)
                putInt("user_coins", profile.coins)
                putInt("user_streak", profile.streak)
                putString("user_last_active_date", profile.lastActiveDate)
                putString("last_category_name", profile.lastQuizCategory)
                putInt("last_score_out_of_10", profile.lastQuizScore)
                putInt("last_xp_earned", profile.lastQuizXpEarned)
                putString("last_quiz_date", profile.lastQuizDate)
                putBoolean("has_quiz_history", profile.quizHistory.isNotEmpty())
                putInt("stats_quizzes_played", profile.totalQuizzesPlayed)
                putInt("stats_questions_answered", profile.totalQuestionsAnswered)
                putInt("stats_correct_answers", profile.totalCorrectAnswers)
                putInt("stats_best_score", profile.bestScore)
                putInt("stats_longest_streak", profile.longestStreak)

                // Save quiz history JSON
                val array = JSONArray()
                profile.quizHistory.forEach { res ->
                    val obj = JSONObject().apply {
                        put("id", res.id)
                        put("categoryName", res.categoryName)
                        put("scoreOutOfTen", res.scoreOutOfTen)
                        put("xpEarned", res.xpEarned)
                        put("coinsEarned", res.coinsEarned)
                        put("totalXp", res.totalXp)
                        put("timestamp", res.timestamp)
                        put("dateFormatted", res.dateFormatted)
                    }
                    array.put(obj)
                }
                putString("local_quiz_results_list", array.toString())
                apply()
            }

            // Sync achievements_prefs
            ctx.getSharedPreferences("achievements_prefs", Context.MODE_PRIVATE).edit().apply {
                profile.unlockedAchievements.forEach { achId ->
                    putBoolean("ach_unlocked_$achId", true)
                }
                profile.claimedRewards.forEach { achId ->
                    putBoolean("ach_claimed_$achId", true)
                }
                putInt("stat_total_quizzes", profile.totalQuizzesPlayed)
                putInt("stat_total_questions", profile.totalQuestionsAnswered)
                putInt("stat_max_score", profile.bestScore)
                apply()
            }
        } catch (e: Exception) {
            Log.e("UserProfileStore", "Error syncing legacy prefs", e)
        }
    }

    private fun profileToJson(profile: UserProfile): JSONObject {
        val json = JSONObject()
        json.put("uid", profile.uid)
        json.put("name", profile.name)
        json.put("email", profile.email)
        json.put("avatarId", profile.avatarId)
        json.put("xp", profile.xp)
        json.put("level", profile.level)
        json.put("coins", profile.coins)
        json.put("streak", profile.streak)
        json.put("lastActiveDate", profile.lastActiveDate)
        json.put("createdAt", profile.createdAt)
        json.put("installDate", profile.installDate)
        json.put("rank", profile.rank)
        json.put("lastQuizCategory", profile.lastQuizCategory)
        json.put("lastQuizScore", profile.lastQuizScore)
        json.put("lastQuizXpEarned", profile.lastQuizXpEarned)
        json.put("lastQuizDate", profile.lastQuizDate)
        json.put("totalQuizzesPlayed", profile.totalQuizzesPlayed)
        json.put("totalQuestionsAnswered", profile.totalQuestionsAnswered)
        json.put("totalCorrectAnswers", profile.totalCorrectAnswers)
        json.put("bestScore", profile.bestScore)
        json.put("longestStreak", profile.longestStreak)

        val unlockedArr = JSONArray()
        profile.unlockedAchievements.forEach { unlockedArr.put(it) }
        json.put("unlockedAchievements", unlockedArr)

        val claimedArr = JSONArray()
        profile.claimedRewards.forEach { claimedArr.put(it) }
        json.put("claimedRewards", claimedArr)

        val unlockedAvatarsArr = JSONArray()
        profile.unlockedAvatars.forEach { unlockedAvatarsArr.put(it) }
        json.put("unlockedAvatars", unlockedAvatarsArr)

        val historyArr = JSONArray()
        profile.quizHistory.forEach { res ->
            val obj = JSONObject().apply {
                put("id", res.id)
                put("categoryName", res.categoryName)
                put("scoreOutOfTen", res.scoreOutOfTen)
                put("xpEarned", res.xpEarned)
                put("coinsEarned", res.coinsEarned)
                put("totalXp", res.totalXp)
                put("timestamp", res.timestamp)
                put("dateFormatted", res.dateFormatted)
            }
            historyArr.put(obj)
        }
        json.put("quizHistory", historyArr)

        return json
    }

    private fun profileFromJson(json: JSONObject): UserProfile {
        val unlockedSet = mutableSetOf<String>()
        val unlockedArr = json.optJSONArray("unlockedAchievements")
        if (unlockedArr != null) {
            for (i in 0 until unlockedArr.length()) {
                unlockedSet.add(unlockedArr.getString(i))
            }
        }

        val claimedSet = mutableSetOf<String>()
        val claimedArr = json.optJSONArray("claimedRewards")
        if (claimedArr != null) {
            for (i in 0 until claimedArr.length()) {
                claimedSet.add(claimedArr.getString(i))
            }
        }

        val unlockedAvatarsSet = mutableSetOf<String>("student_boy", "student_girl", "brain")
        val unlockedAvatarsArr = json.optJSONArray("unlockedAvatars")
        if (unlockedAvatarsArr != null) {
            for (i in 0 until unlockedAvatarsArr.length()) {
                unlockedAvatarsSet.add(unlockedAvatarsArr.getString(i))
            }
        }

        val historyList = mutableListOf<QuizResult>()
        val historyArr = json.optJSONArray("quizHistory")
        if (historyArr != null) {
            for (i in 0 until historyArr.length()) {
                val obj = historyArr.getJSONObject(i)
                historyList.add(
                    QuizResult(
                        id = obj.optString("id", ""),
                        categoryName = obj.optString("categoryName", ""),
                        scoreOutOfTen = obj.optInt("scoreOutOfTen", 0),
                        xpEarned = obj.optInt("xpEarned", 0),
                        coinsEarned = obj.optInt("coinsEarned", 0),
                        totalXp = obj.optInt("totalXp", 0),
                        timestamp = obj.optLong("timestamp", 0L),
                        dateFormatted = obj.optString("dateFormatted", "")
                    )
                )
            }
        }

        val xp = json.optInt("xp", 0)
        val level = json.optInt("level", maxOf(1, (xp / 500) + 1))

        return UserProfile(
            uid = json.optString("uid", ""),
            name = json.optString("name", "Player"),
            email = json.optString("email", "guest@brainquiz.ai"),
            avatarId = json.optString("avatarId", "brain"),
            xp = xp,
            level = level,
            coins = json.optInt("coins", 0),
            streak = json.optInt("streak", 0),
            lastActiveDate = json.optString("lastActiveDate", ""),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            installDate = json.optString("installDate", ""),
            rank = json.optString("rank", RankUtils.getRankForXp(xp)),
            unlockedAchievements = unlockedSet,
            claimedRewards = claimedSet,
            unlockedAvatars = unlockedAvatarsSet,
            quizHistory = historyList,
            lastQuizCategory = json.optString("lastQuizCategory", ""),
            lastQuizScore = json.optInt("lastQuizScore", 0),
            lastQuizXpEarned = json.optInt("lastQuizXpEarned", 0),
            lastQuizDate = json.optString("lastQuizDate", ""),
            totalQuizzesPlayed = json.optInt("totalQuizzesPlayed", historyList.size),
            totalQuestionsAnswered = json.optInt("totalQuestionsAnswered", historyList.size * 10),
            totalCorrectAnswers = json.optInt("totalCorrectAnswers", historyList.sumOf { it.scoreOutOfTen }),
            bestScore = json.optInt("bestScore", historyList.maxOfOrNull { it.scoreOutOfTen } ?: 0),
            longestStreak = json.optInt("longestStreak", 0)
        )
    }
}
