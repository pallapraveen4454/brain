package com.example.data

import android.content.Context
import android.util.Log
import com.example.BrainQuizApplication
import com.example.ui.screens.LeaderboardUser
import com.example.utils.RankUtils
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

    fun getLeaderboard(period: LeaderboardPeriod = LeaderboardPeriod.GLOBAL): LeaderboardData {
        val competitors = loadOrCreateCompetitors()
        val userProfile = userProfileStore.getProfile()
        val userStats = quizResultRepository.getUserStats()
        val achievements = achievementRepository.getAllAchievements(userProfile.xp, userProfile.coins, userProfile.streak)
        val unlockedCount = achievements.count { it.isUnlocked }

        val currentUserScore = calculateScore(
            xp = userProfile.xp,
            quizzesPlayed = userStats.totalQuizzesPlayed,
            achievementsCount = unlockedCount
        )

        val currentUserEntry = LeaderboardUser(
            rank = 0,
            id = userProfile.uid.ifBlank { "current_user" },
            name = userProfile.name.ifBlank { "Player" },
            avatarId = userProfile.avatarId.ifBlank { "brain" },
            xp = userProfile.xp,
            level = maxOf(1, userProfile.level),
            rankBadge = RankUtils.getRankForXp(userProfile.xp),
            quizzesPlayed = userStats.totalQuizzesPlayed,
            achievementsCount = unlockedCount,
            score = currentUserScore,
            isCurrentUser = true
        )

        // Combine user with global competitors
        val allEntries = (competitors.filterNot { it.isCurrentUser } + currentUserEntry)
            .sortedWith(compareByDescending<LeaderboardUser> { it.xp }
                .thenByDescending { it.score }
                .thenByDescending { it.achievementsCount })

        val rankedList = allEntries.mapIndexed { index, user ->
            user.copy(rank = index + 1)
        }

        val updatedCurrentUser = rankedList.find { it.isCurrentUser } ?: currentUserEntry.copy(rank = rankedList.size)

        return LeaderboardData(
            topPlayers = rankedList,
            currentUserEntry = updatedCurrentUser,
            period = period
        )
    }

    private fun calculateScore(xp: Int, quizzesPlayed: Int, achievementsCount: Int): Int {
        return xp + (achievementsCount * 50) + (quizzesPlayed * 15)
    }

    private fun loadOrCreateCompetitors(): List<LeaderboardUser> {
        val prefs = getPrefs()
        val jsonStr = prefs?.getString("competitors_json", "") ?: ""

        if (jsonStr.isNotBlank()) {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<LeaderboardUser>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        LeaderboardUser(
                            rank = obj.optInt("rank", i + 1),
                            id = obj.optString("id", "comp_$i"),
                            name = obj.optString("name", "Player"),
                            avatarId = obj.optString("avatarId", "brain"),
                            xp = obj.optInt("xp", 100),
                            level = obj.optInt("level", 1),
                            rankBadge = obj.optString("rankBadge", "Beginner"),
                            quizzesPlayed = obj.optInt("quizzesPlayed", 5),
                            achievementsCount = obj.optInt("achievementsCount", 2),
                            score = obj.optInt("score", 100),
                            isCurrentUser = false
                        )
                    )
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                Log.e("LeaderboardRepository", "Error reading saved competitors", e)
            }
        }

        // Initial competitors if none saved
        val initialCompetitors = listOf(
            LeaderboardUser(1, "comp_1", "Sophia Chen", "wizard", 3850, 8, "Legend", 42, 12, 4500),
            LeaderboardUser(2, "comp_2", "Alex Vance", "rocket", 3120, 7, "Grandmaster", 35, 10, 3700),
            LeaderboardUser(3, "comp_3", "Elena Rostova", "crown", 2650, 6, "Master", 28, 8, 3100),
            LeaderboardUser(4, "comp_4", "Marcus Brody", "star", 2100, 5, "Diamond", 22, 7, 2500),
            LeaderboardUser(5, "comp_5", "David Kim", "brain", 1680, 4, "Gold", 18, 5, 2000),
            LeaderboardUser(6, "comp_6", "Sarah Connor", "fire", 1350, 3, "Gold", 14, 4, 1600),
            LeaderboardUser(7, "comp_7", "Liam Neeson", "ninja", 1050, 3, "Silver", 11, 3, 1250),
            LeaderboardUser(8, "comp_8", "Priya Sharma", "cat", 820, 2, "Silver", 8, 2, 950),
            LeaderboardUser(9, "comp_9", "Lucas Meyer", "fox", 600, 2, "Bronze", 6, 2, 720),
            LeaderboardUser(10, "comp_10", "Aria Stark", "owl", 420, 1, "Novice", 4, 1, 500),
            LeaderboardUser(11, "comp_11", "Ethan Hunt", "robot", 280, 1, "Novice", 3, 1, 340),
            LeaderboardUser(12, "comp_12", "Maya Patel", "bear", 150, 1, "Novice", 2, 0, 180)
        )

        saveCompetitors(initialCompetitors)
        return initialCompetitors
    }

    private fun saveCompetitors(competitors: List<LeaderboardUser>) {
        try {
            val prefs = getPrefs() ?: return
            val array = JSONArray()
            competitors.forEach { comp ->
                val obj = JSONObject().apply {
                    put("rank", comp.rank)
                    put("id", comp.id)
                    put("name", comp.name)
                    put("avatarId", comp.avatarId)
                    put("xp", comp.xp)
                    put("level", comp.level)
                    put("rankBadge", comp.rankBadge)
                    put("quizzesPlayed", comp.quizzesPlayed)
                    put("achievementsCount", comp.achievementsCount)
                    put("score", comp.score)
                }
                array.put(obj)
            }
            prefs.edit().putString("competitors_json", array.toString()).apply()
        } catch (e: Exception) {
            Log.e("LeaderboardRepository", "Error saving competitors", e)
        }
    }
}
