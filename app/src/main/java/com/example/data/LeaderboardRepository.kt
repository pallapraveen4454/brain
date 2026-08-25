package com.example.data

import android.content.Context
import android.util.Log
import com.example.BrainQuizApplication
import com.example.ui.screens.LeaderboardUser
import com.example.utils.RankUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class LeaderboardPeriod {
    WEEKLY, GLOBAL, FRIENDS
}

data class LeaderboardData(
    val topPlayers: List<LeaderboardUser>,
    val currentUserEntry: LeaderboardUser,
    val period: LeaderboardPeriod
)

class LeaderboardRepository(
    private val context: Context? = try { BrainQuizApplication.instance } catch (e: Exception) { null },
    private val userProfileStore: UserProfileStore = UserProfileStore(context),
    private val achievementRepository: AchievementRepository = AchievementRepository(context),
    private val quizResultRepository: QuizResultRepository = QuizResultRepository(context)
) {
    private val prefsName = "leaderboard_prefs"

    private fun getPrefs() = context?.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    private fun getFirestore(): FirebaseFirestore? {
        val appCtx = context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null }
        if (appCtx != null) {
            BrainQuizApplication.ensureFirebaseInitialized(appCtx)
        }
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("LeaderboardRepository", "Failed to access FirebaseFirestore: ${e.message}")
            null
        }
    }

    fun getStartOfWeekMillis(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.firstDayOfWeek = java.util.Calendar.MONDAY
        calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getLeaderboard(period: LeaderboardPeriod = LeaderboardPeriod.GLOBAL): LeaderboardData {
        val cachedPlayers = loadCachedLeaderboard()
        val userProfile = userProfileStore.getProfile()
        val userStats = quizResultRepository.getUserStats()
        val achievements = achievementRepository.getAllAchievements(userProfile.xp, userProfile.coins, userProfile.streak)
        val unlockedCount = achievements.count { it.isUnlocked }

        val startOfWeek = getStartOfWeekMillis()
        val userWeeklyXp = if (userProfile.quizHistory.isNotEmpty()) {
            userProfile.quizHistory.filter { it.timestamp >= startOfWeek }.sumOf { it.xpEarned }
        } else {
            userProfile.xp
        }

        val currentUserScore = calculateScore(
            xp = if (period == LeaderboardPeriod.WEEKLY) userWeeklyXp else userProfile.xp,
            quizzesPlayed = userStats.totalQuizzesPlayed,
            achievementsCount = unlockedCount
        )

        val currentUserEntry = LeaderboardUser(
            rank = 1,
            id = userProfile.uid.ifBlank { "current_user" },
            name = userProfile.name.ifBlank { "Player" },
            avatarId = userProfile.avatarId.ifBlank { "brain" },
            xp = userProfile.xp,
            weeklyXp = userWeeklyXp,
            level = maxOf(1, userProfile.level),
            rankBadge = RankUtils.getRankForXp(userProfile.xp),
            quizzesPlayed = userStats.totalQuizzesPlayed,
            achievementsCount = unlockedCount,
            score = currentUserScore,
            countryFlag = "🌟",
            rankChange = 0,
            isCurrentUser = true
        )

        if (period == LeaderboardPeriod.FRIENDS) {
            return LeaderboardData(
                topPlayers = emptyList(),
                currentUserEntry = currentUserEntry,
                period = period
            )
        }

        // Combine real cached players with current user entry
        val otherPlayers = cachedPlayers.filterNot { it.id == currentUserEntry.id || it.isCurrentUser }
        val allEntries = (otherPlayers + currentUserEntry).distinctBy { it.id }

        val rankedList = when (period) {
            LeaderboardPeriod.WEEKLY -> {
                allEntries.sortedWith(
                    compareByDescending<LeaderboardUser> { it.weeklyXp }
                        .thenByDescending { it.xp }
                        .thenByDescending { it.score }
                ).mapIndexed { index, user -> user.copy(rank = index + 1) }
            }
            else -> {
                allEntries.sortedWith(
                    compareByDescending<LeaderboardUser> { it.xp }
                        .thenByDescending { it.score }
                        .thenByDescending { it.achievementsCount }
                ).mapIndexed { index, user -> user.copy(rank = index + 1) }
            }
        }

        val updatedCurrentUser = rankedList.find { it.id == currentUserEntry.id || it.isCurrentUser }
            ?: currentUserEntry.copy(rank = 1)

        return LeaderboardData(
            topPlayers = rankedList,
            currentUserEntry = updatedCurrentUser,
            period = period
        )
    }

    suspend fun fetchRemoteLeaderboard(period: LeaderboardPeriod = LeaderboardPeriod.GLOBAL): List<LeaderboardUser> {
        try {
            val firestore = getFirestore() ?: return loadCachedLeaderboard()
            val snapshot = firestore.collection("leaderboard")
                .limit(100)
                .get()
                .await()

            val remoteUsers = mutableListOf<LeaderboardUser>()
            val currentProfile = userProfileStore.getProfile()
            val currentUid = currentProfile.uid
            val startOfWeek = getStartOfWeekMillis()

            for (doc in snapshot.documents) {
                val id = doc.getString("id") ?: doc.id
                val name = doc.getString("name") ?: "Player"
                val avatarId = doc.getString("avatarId") ?: "brain"
                val xp = doc.getLong("xp")?.toInt() ?: 0
                val docWeeklyXp = doc.getLong("weeklyXp")?.toInt() ?: 0
                val docWeekStart = doc.getLong("weekStart") ?: 0L
                val validWeeklyXp = if (docWeekStart >= startOfWeek) docWeeklyXp else 0
                val level = doc.getLong("level")?.toInt() ?: 1
                val rankBadge = doc.getString("rankBadge") ?: RankUtils.getRankForXp(xp)
                val quizzesPlayed = doc.getLong("quizzesPlayed")?.toInt() ?: 0
                val achievementsCount = doc.getLong("achievementsCount")?.toInt() ?: 0
                val score = doc.getLong("score")?.toInt() ?: calculateScore(xp, quizzesPlayed, achievementsCount)
                val countryFlag = doc.getString("countryFlag") ?: "🌟"
                val isCurrent = id == currentUid

                remoteUsers.add(
                    LeaderboardUser(
                        rank = 0,
                        id = id,
                        name = name,
                        avatarId = avatarId,
                        xp = xp,
                        weeklyXp = validWeeklyXp,
                        level = level,
                        rankBadge = rankBadge,
                        quizzesPlayed = quizzesPlayed,
                        achievementsCount = achievementsCount,
                        score = score,
                        countryFlag = countryFlag,
                        rankChange = 0,
                        isCurrentUser = isCurrent
                    )
                )
            }

            if (remoteUsers.isNotEmpty()) {
                saveCachedLeaderboard(remoteUsers)
            }
            return remoteUsers
        } catch (e: CancellationException) {
            Log.d("LeaderboardRepository", "fetchRemoteLeaderboard cancelled")
            return loadCachedLeaderboard()
        } catch (e: Exception) {
            Log.e("LeaderboardRepository", "Error fetching remote leaderboard", e)
            return loadCachedLeaderboard()
        }
    }

    suspend fun syncCurrentUserToLeaderboard(profile: UserProfile? = null) {
        val userProfile = profile ?: userProfileStore.getProfile()
        if (userProfile.uid.isBlank() || userProfile.uid.startsWith("guest_") || userProfile.isGuest()) {
            return
        }

        try {
            val firestore = getFirestore() ?: return
            val userStats = quizResultRepository.getUserStats()
            val achievements = achievementRepository.getAllAchievements(userProfile.xp, userProfile.coins, userProfile.streak)
            val unlockedCount = achievements.count { it.isUnlocked }
            val score = calculateScore(userProfile.xp, userStats.totalQuizzesPlayed, unlockedCount)

            val startOfWeek = getStartOfWeekMillis()
            val weeklyXp = if (userProfile.quizHistory.isNotEmpty()) {
                userProfile.quizHistory.filter { it.timestamp >= startOfWeek }.sumOf { it.xpEarned }
            } else {
                userProfile.xp
            }

            val entry = hashMapOf(
                "id" to userProfile.uid,
                "name" to userProfile.name.ifBlank { "Player" },
                "avatarId" to userProfile.avatarId.ifBlank { "brain" },
                "xp" to userProfile.xp,
                "weeklyXp" to weeklyXp,
                "weekStart" to startOfWeek,
                "level" to maxOf(1, userProfile.level),
                "rankBadge" to RankUtils.getRankForXp(userProfile.xp),
                "quizzesPlayed" to maxOf(userProfile.totalQuizzesPlayed, userStats.totalQuizzesPlayed),
                "achievementsCount" to unlockedCount,
                "score" to score,
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection("leaderboard")
                .document(userProfile.uid)
                .set(entry)
                .await()

            Log.d("LeaderboardRepository", "Successfully synced user ${userProfile.uid} to Firestore leaderboard (XP: ${userProfile.xp}, Weekly XP: $weeklyXp)")

            // Update cached leaderboard to include current user
            val cached = loadCachedLeaderboard().filterNot { it.id == userProfile.uid }
            val currentUserItem = LeaderboardUser(
                rank = 0,
                id = userProfile.uid,
                name = userProfile.name.ifBlank { "Player" },
                avatarId = userProfile.avatarId.ifBlank { "brain" },
                xp = userProfile.xp,
                weeklyXp = weeklyXp,
                level = maxOf(1, userProfile.level),
                rankBadge = RankUtils.getRankForXp(userProfile.xp),
                quizzesPlayed = maxOf(userProfile.totalQuizzesPlayed, userStats.totalQuizzesPlayed),
                achievementsCount = unlockedCount,
                score = score,
                countryFlag = "🌟",
                rankChange = 0,
                isCurrentUser = true
            )
            saveCachedLeaderboard(listOf(currentUserItem) + cached)
        } catch (e: CancellationException) {
            Log.d("LeaderboardRepository", "syncCurrentUserToLeaderboard cancelled")
        } catch (e: Exception) {
            Log.e("LeaderboardRepository", "Error syncing user to leaderboard", e)
        }
    }

    private fun calculateScore(xp: Int, quizzesPlayed: Int, achievementsCount: Int): Int {
        return xp + (achievementsCount * 50) + (quizzesPlayed * 15)
    }

    private fun loadCachedLeaderboard(): List<LeaderboardUser> {
        val prefs = getPrefs()
        val jsonStr = prefs?.getString("cached_leaderboard_json", "") ?: ""

        if (jsonStr.isNotBlank()) {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<LeaderboardUser>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        LeaderboardUser(
                            rank = obj.optInt("rank", i + 1),
                            id = obj.optString("id", "user_${i + 1}"),
                            name = obj.optString("name", "Player"),
                            avatarId = obj.optString("avatarId", "brain"),
                            xp = obj.optInt("xp", 0),
                            weeklyXp = obj.optInt("weeklyXp", 0),
                            level = obj.optInt("level", 1),
                            rankBadge = obj.optString("rankBadge", "Beginner"),
                            quizzesPlayed = obj.optInt("quizzesPlayed", 0),
                            achievementsCount = obj.optInt("achievementsCount", 0),
                            score = obj.optInt("score", 0),
                            countryFlag = obj.optString("countryFlag", "🌟"),
                            rankChange = obj.optInt("rankChange", 0),
                            isCurrentUser = false
                        )
                    )
                }
                return list
            } catch (e: Exception) {
                Log.e("LeaderboardRepository", "Error reading cached leaderboard", e)
            }
        }
        return emptyList()
    }

    private fun saveCachedLeaderboard(players: List<LeaderboardUser>) {
        try {
            val prefs = getPrefs() ?: return
            val array = JSONArray()
            players.forEach { player ->
                val obj = JSONObject().apply {
                    put("rank", player.rank)
                    put("id", player.id)
                    put("name", player.name)
                    put("avatarId", player.avatarId)
                    put("xp", player.xp)
                    put("weeklyXp", player.weeklyXp)
                    put("level", player.level)
                    put("rankBadge", player.rankBadge)
                    put("quizzesPlayed", player.quizzesPlayed)
                    put("achievementsCount", player.achievementsCount)
                    put("score", player.score)
                    put("countryFlag", player.countryFlag)
                    put("rankChange", player.rankChange)
                }
                array.put(obj)
            }
            prefs.edit().putString("cached_leaderboard_json", array.toString()).apply()
        } catch (e: Exception) {
            Log.e("LeaderboardRepository", "Error saving cached leaderboard", e)
        }
    }
}
