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
        return isAuthLoggedIn || isGuest
    }

    fun setLoggedIn(loggedIn: Boolean) {
        val ctx = context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null }
        ctx?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)?.edit()
            ?.putBoolean("is_logged_in", loggedIn)
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

    fun createOrGetGuestProfile(): UserProfile {
        setGuestActive(true)
        val guestId = getGuestId()
        
        // Recover current progress if existing guest, or initialize new guest profile
        val currentJson = try {
            val jsonStr = getPrefs()?.getString(keyProfileJson, "") ?: ""
            if (jsonStr.isNotBlank()) profileFromJson(JSONObject(jsonStr)) else null
        } catch (e: Exception) { null }

        val guestXp = currentJson?.xp ?: 0
        val guestCoins = currentJson?.coins ?: 0
        val guestStreak = currentJson?.streak ?: 0

        val guestProfile = UserProfile(
            uid = guestId,
            name = "Guest",
            email = "Guest Account",
            avatarId = "brain",
            xp = guestXp,
            level = maxOf(1, (guestXp / 500) + 1),
            coins = guestCoins,
            streak = guestStreak,
            rank = RankUtils.getRankForXp(guestXp)
        )
        
        // Direct save without merging fields from non-guest accounts
        getPrefs()?.edit()?.putString(keyProfileJson, profileToJson(guestProfile).toString())?.apply()
        syncLegacyPrefs(guestProfile)
        return guestProfile
    }

    fun getProfile(): UserProfile {
        Log.d("GuestAccount", "App Started - Loading Profile")
        val guestId = getGuestId()
        val isGuest = isGuestActive()

        try {
            val jsonStr = getPrefs()?.getString(keyProfileJson, "") ?: ""
            if (jsonStr.isNotBlank()) {
                val profile = profileFromJson(JSONObject(jsonStr))
                
                if (isGuest) {
                    // Force complete guest isolation: never display previous authenticated user account details
                    val sanitized = profile.copy(
                        uid = if (profile.uid.startsWith("guest_")) profile.uid else guestId,
                        name = if (profile.name.isBlank() || profile.name == "Player" || profile.name == "Guest Player") "Guest" else profile.name,
                        email = "Guest Account"
                    )
                    Log.d("GuestAccount", "Guest Profile loaded cleanly and isolated")
                    return sanitized
                } else {
                    val finalUid = if (profile.uid.isNotBlank()) profile.uid else guestId
                    return profile.copy(uid = finalUid)
                }
            }
        } catch (e: Exception) {
            Log.e("UserProfileStore", "Error loading profile from JSON", e)
        }

        if (isGuest) {
            return createOrGetGuestProfile()
        }

        // Default initial user profile
        val defaultProfile = UserProfile(
            uid = guestId,
            name = "Guest",
            email = "Guest Account",
            avatarId = "brain",
            xp = 0,
            level = 1,
            coins = 0,
            streak = 0,
            rank = "Beginner"
        )
        getPrefs()?.edit()?.putString(keyProfileJson, profileToJson(defaultProfile).toString())?.apply()
        syncLegacyPrefs(defaultProfile)
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
        return createOrGetGuestProfile()
    }

    fun saveProfile(profile: UserProfile): UserProfile {
        try {
            val isGuest = isGuestActive() || profile.uid.startsWith("guest_")
            val current = try {
                val jsonStr = getPrefs()?.getString(keyProfileJson, "") ?: ""
                if (jsonStr.isNotBlank()) profileFromJson(JSONObject(jsonStr)) else null
            } catch (e: Exception) { null }

            val isSameUser = current != null && current.uid.isNotBlank() && current.uid == profile.uid && (current.uid.startsWith("guest_") == profile.uid.startsWith("guest_"))

            val mergedUid = if (isGuest) {
                if (profile.uid.startsWith("guest_")) profile.uid else getGuestId()
            } else {
                profile.uid.ifBlank { if (isSameUser) current?.uid ?: "" else "" }
            }

            val mergedName = if (isGuest) {
                if (profile.name.isNotBlank() && profile.name != "Player" && profile.name != "Guest Player") profile.name else "Guest"
            } else {
                val currentName = if (isSameUser) current?.name?.ifBlank { "" } ?: "" else ""
                when {
                    profile.name.isNotBlank() && profile.name != "Player" && profile.name != "Guest Player" -> profile.name
                    currentName.isNotBlank() && currentName != "Player" && currentName != "Guest Player" -> currentName
                    else -> profile.name.ifBlank { currentName.ifBlank { "Player" } }
                }
            }

            val mergedEmail = if (isGuest) {
                "Guest Account"
            } else {
                profile.email.ifBlank { if (isSameUser) current?.email ?: "" else "" }
            }

            val currentAvatar = if (isSameUser) current?.avatarId?.ifBlank { "" } ?: "" else ""
            val mergedAvatar = when {
                profile.avatarId.isNotBlank() && profile.avatarId != "brain" -> profile.avatarId
                currentAvatar.isNotBlank() && currentAvatar != "brain" -> currentAvatar
                profile.avatarId.isNotBlank() -> profile.avatarId
                currentAvatar.isNotBlank() -> currentAvatar
                else -> "brain"
            }

            val mergedXp = if (isSameUser) maxOf(profile.xp, current?.xp ?: 0) else profile.xp
            Log.d("XP_TRACE", "[UserProfileStore] saveProfile: isSameUser=$isSameUser, currentXp=${current?.xp}, incomingXp=${profile.xp}, mergedXp=$mergedXp")
            val mergedCoins = profile.coins
            val mergedStreak = if (isSameUser) maxOf(profile.streak, current?.streak ?: 0) else profile.streak
            val mergedLongestStreak = if (isSameUser) maxOf(profile.longestStreak, current?.longestStreak ?: 0, mergedStreak) else maxOf(profile.longestStreak, mergedStreak)
            val mergedLevel = maxOf(1, (mergedXp / 500) + 1)
            val mergedRank = RankUtils.getRankForXp(mergedXp)

            val mergedUnlocked = if (isSameUser) ((current?.unlockedAchievements ?: emptyList()) + profile.unlockedAchievements).distinct() else profile.unlockedAchievements
            val mergedClaimed = if (isSameUser) ((current?.claimedRewards ?: emptyList()) + profile.claimedRewards).distinct() else profile.claimedRewards
            val mergedUnlockedAvatars = if (isSameUser) ((current?.unlockedAvatars ?: listOf("student_boy", "student_girl", "brain")) + profile.unlockedAvatars).distinct() else profile.unlockedAvatars.ifEmpty { listOf("student_boy", "student_girl", "brain") }

            // Combine and deduplicate history if same user
            val rawHistory = if (isSameUser) (profile.quizHistory + (current?.quizHistory ?: emptyList())) else profile.quizHistory
            val combinedHistory = rawHistory
                .distinctBy { if (it.id.isNotBlank()) it.id else "${it.timestamp}_${it.categoryName}" }
                .sortedByDescending { it.timestamp }

            val mergedQuizzesPlayed = if (isSameUser) maxOf(profile.totalQuizzesPlayed, current?.totalQuizzesPlayed ?: 0, combinedHistory.size) else maxOf(profile.totalQuizzesPlayed, combinedHistory.size)
            val mergedQuestionsAnswered = if (isSameUser) maxOf(profile.totalQuestionsAnswered, current?.totalQuestionsAnswered ?: 0, combinedHistory.size * 10) else maxOf(profile.totalQuestionsAnswered, combinedHistory.size * 10)
            val mergedCorrectAnswers = if (isSameUser) maxOf(profile.totalCorrectAnswers, current?.totalCorrectAnswers ?: 0, combinedHistory.sumOf { it.scoreOutOfTen }) else maxOf(profile.totalCorrectAnswers, combinedHistory.sumOf { it.scoreOutOfTen })
            val mergedBestScore = if (isSameUser) maxOf(profile.bestScore, current?.bestScore ?: 0, combinedHistory.maxOfOrNull { it.scoreOutOfTen } ?: 0) else maxOf(profile.bestScore, combinedHistory.maxOfOrNull { it.scoreOutOfTen } ?: 0)

            val lastCategory = profile.lastQuizCategory.ifBlank { if (isSameUser) current?.lastQuizCategory ?: "" else "" }
            val lastScore = if (profile.lastQuizScore > 0) profile.lastQuizScore else (if (isSameUser) current?.lastQuizScore ?: 0 else 0)
            val lastXp = if (profile.lastQuizXpEarned > 0) profile.lastQuizXpEarned else (if (isSameUser) current?.lastQuizXpEarned ?: 0 else 0)
            val lastDate = profile.lastQuizDate.ifBlank { if (isSameUser) current?.lastQuizDate ?: "" else "" }
            val mergedInstallDate = profile.installDate.ifBlank { if (isSameUser) current?.installDate ?: "" else "" }

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
            unlockedAchievements = unlockedSet.toList(),
            claimedRewards = claimedSet.toList(),
            unlockedAvatars = unlockedAvatarsSet.toList(),
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
