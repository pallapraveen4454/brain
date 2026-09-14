package com.example.data

import android.content.Context
import android.util.Log
import com.example.BrainQuizApplication
import com.example.ui.screens.LeaderboardUser
import com.example.utils.LevelUtils
import com.example.utils.RankUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
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
        if (calendar.timeInMillis > System.currentTimeMillis()) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -7)
        }
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
            0
        }
        val userWeeklyQuizzesCount = if (userProfile.quizHistory.isNotEmpty()) {
            userProfile.quizHistory.count { it.timestamp >= startOfWeek }
        } else {
            0
        }

        val currentUserScore = if (period == LeaderboardPeriod.WEEKLY) {
            calculateScore(
                xp = userWeeklyXp,
                quizzesPlayed = userWeeklyQuizzesCount,
                achievementsCount = 0
            )
        } else {
            calculateScore(
                xp = userProfile.xp,
                quizzesPlayed = maxOf(userProfile.totalQuizzesPlayed, userStats.totalQuizzesPlayed),
                achievementsCount = unlockedCount
            )
        }

        val currentUserId = userProfile.uid.ifBlank { "current_user" }
        val currentUserName = userProfile.name.ifBlank {
            if (userProfile.email.isNotBlank() && !userProfile.email.startsWith("Guest")) {
                userProfile.email.substringBefore("@").replaceFirstChar { it.uppercase() }
            } else {
                "Player"
            }
        }

        val currentUserEntry = LeaderboardUser(
            rank = 0,
            id = currentUserId,
            name = currentUserName,
            avatarId = userProfile.avatarId.ifBlank { "brain" },
            xp = if (period == LeaderboardPeriod.WEEKLY) userWeeklyXp else userProfile.xp,
            weeklyXp = userWeeklyXp,
            level = maxOf(1, userProfile.level),
            rankBadge = RankUtils.getRankForXp(userProfile.xp),
            quizzesPlayed = if (period == LeaderboardPeriod.WEEKLY) userWeeklyQuizzesCount else maxOf(userProfile.totalQuizzesPlayed, userStats.totalQuizzesPlayed),
            achievementsCount = if (period == LeaderboardPeriod.WEEKLY) 0 else unlockedCount,
            score = currentUserScore,
            countryFlag = "🌟",
            rankChange = 0,
            isCurrentUser = true
        )

        // Filter out duplicate current user entries from cached players
        val otherPlayers = cachedPlayers
            .filterNot { it.id == currentUserEntry.id || it.isCurrentUser }
            .map { it.copy(isCurrentUser = false) }

        val rankedList = when (period) {
            LeaderboardPeriod.WEEKLY -> {
                val eligibleOther = otherPlayers.map { player ->
                    val wXp = if (player.weeklyXp > 0) player.weeklyXp else maxOf(50, (player.xp * 0.15).toInt())
                    val wScore = calculateScore(wXp, maxOf(1, player.quizzesPlayed / 4), 0)
                    player.copy(xp = wXp, weeklyXp = wXp, score = wScore)
                }
                val eligibleAll = (eligibleOther + currentUserEntry).distinctBy { it.id }
                eligibleAll.sortedWith(
                    compareByDescending<LeaderboardUser> { it.xp }
                        .thenByDescending { it.score }
                        .thenByDescending { it.quizzesPlayed }
                        .thenBy { it.name }
                ).mapIndexed { index, user ->
                    user.copy(rank = index + 1, isCurrentUser = (user.id == currentUserEntry.id))
                }
            }
            LeaderboardPeriod.GLOBAL -> {
                val allEntries = (otherPlayers + currentUserEntry).distinctBy { it.id }
                allEntries.sortedWith(
                    compareByDescending<LeaderboardUser> { it.xp }
                        .thenByDescending { it.score }
                        .thenByDescending { it.quizzesPlayed }
                        .thenBy { it.name }
                ).mapIndexed { index, user ->
                    user.copy(rank = index + 1, isCurrentUser = (user.id == currentUserEntry.id))
                }
            }
            LeaderboardPeriod.FRIENDS -> {
                // In Friends mode, display registered app users and community peers
                val registeredUsers = loadRegisteredAppUsers()
                    .filterNot { it.id == currentUserEntry.id }
                    .map { it.copy(isCurrentUser = false) }
                val peers = (otherPlayers.take(6) + registeredUsers).distinctBy { it.id }
                val allFriends = (peers + currentUserEntry).distinctBy { it.id }
                allFriends.sortedWith(
                    compareByDescending<LeaderboardUser> { it.xp }
                        .thenByDescending { it.score }
                        .thenBy { it.name }
                ).mapIndexed { index, user ->
                    user.copy(rank = index + 1, isCurrentUser = (user.id == currentUserEntry.id))
                }
            }
        }

        val updatedCurrentUser = rankedList.find { it.id == currentUserEntry.id || it.isCurrentUser }
            ?: currentUserEntry.copy(rank = rankedList.size + 1)

        return LeaderboardData(
            topPlayers = rankedList,
            currentUserEntry = updatedCurrentUser,
            period = period
        )
    }

    suspend fun fetchRemoteLeaderboard(period: LeaderboardPeriod = LeaderboardPeriod.GLOBAL): List<LeaderboardUser> {
        try {
            val firestore = getFirestore() ?: return loadCachedLeaderboard()
            val remoteUsersMap = mutableMapOf<String, LeaderboardUser>()
            val currentProfile = userProfileStore.getProfile()
            val currentUid = currentProfile.uid
            val startOfWeek = getStartOfWeekMillis()

            // 1. Fetch from 'leaderboard' collection
            try {
                val leaderboardSnapshot = firestore.collection("leaderboard")
                    .limit(100)
                    .get()
                    .await()

                for (doc in leaderboardSnapshot.documents) {
                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("name") ?: "Player"
                    val avatarId = doc.getString("avatarId") ?: "brain"
                    val xp = doc.getLong("xp")?.toInt() ?: 0
                    val docWeeklyXp = doc.getLong("weeklyXp")?.toInt() ?: 0
                    val docWeekStart = doc.getLong("weekStart") ?: 0L
                    val validWeeklyXp = if (docWeekStart >= startOfWeek) docWeeklyXp else maxOf(0, (xp * 0.15).toInt())
                    val level = doc.getLong("level")?.toInt() ?: LevelUtils.getLevel(xp)
                    val rankBadge = doc.getString("rankBadge") ?: RankUtils.getRankForXp(xp)
                    val quizzesPlayed = doc.getLong("quizzesPlayed")?.toInt() ?: 0
                    val achievementsCount = doc.getLong("achievementsCount")?.toInt() ?: 0
                    val score = doc.getLong("score")?.toInt() ?: calculateScore(xp, quizzesPlayed, achievementsCount)
                    val countryFlag = doc.getString("countryFlag") ?: "🌟"
                    val isCurrent = currentUid.isNotBlank() && id == currentUid

                    remoteUsersMap[id] = LeaderboardUser(
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
                }
            } catch (e: Exception) {
                Log.w("LeaderboardRepository", "Firestore 'leaderboard' collection query skipped or failed: ${e.message}")
            }

            // 2. Fetch from 'users' collection to capture every user who signed in / registered
            try {
                val usersSnapshot = firestore.collection("users")
                    .limit(100)
                    .get()
                    .await()

                for (doc in usersSnapshot.documents) {
                    val id = doc.getString("uid") ?: doc.id
                    val existing = remoteUsersMap[id]
                    val email = doc.getString("email") ?: ""
                    val nameRaw = doc.getString("name") ?: ""
                    val name = when {
                        nameRaw.isNotBlank() && nameRaw != "Player" && nameRaw != "Guest Player" -> nameRaw
                        email.isNotBlank() && !email.startsWith("guest") -> email.substringBefore("@").replaceFirstChar { it.uppercase() }
                        existing != null && existing.name.isNotBlank() && existing.name != "Player" -> existing.name
                        else -> nameRaw.ifBlank { "Player" }
                    }
                    val avatarId = doc.getString("avatarId") ?: existing?.avatarId ?: "brain"
                    val xp = maxOf(doc.getLong("xp")?.toInt() ?: 0, existing?.xp ?: 0)
                    val level = maxOf(doc.getLong("level")?.toInt() ?: 1, existing?.level ?: 1, LevelUtils.getLevel(xp))
                    val rankBadge = doc.getString("rank") ?: existing?.rankBadge ?: RankUtils.getRankForXp(xp)
                    val quizzesPlayed = maxOf(doc.getLong("totalQuizzesPlayed")?.toInt() ?: 0, existing?.quizzesPlayed ?: 0)
                    val achievementsList = doc.get("unlockedAchievements") as? List<*>
                    val achievementsCount = maxOf(achievementsList?.size ?: 0, existing?.achievementsCount ?: 0)
                    val score = calculateScore(xp, quizzesPlayed, achievementsCount)
                    val validWeeklyXp = existing?.weeklyXp ?: maxOf(0, (xp * 0.15).toInt())
                    val isCurrent = currentUid.isNotBlank() && id == currentUid

                    remoteUsersMap[id] = LeaderboardUser(
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
                        countryFlag = existing?.countryFlag ?: "🌟",
                        rankChange = 0,
                        isCurrentUser = isCurrent
                    )
                }
            } catch (e: Exception) {
                Log.w("LeaderboardRepository", "Firestore 'users' collection query skipped or failed: ${e.message}")
            }

            val remoteUsers = remoteUsersMap.values.toList()
            if (remoteUsers.isNotEmpty()) {
                saveCachedLeaderboard(remoteUsers)
                saveRegisteredAppUsers(remoteUsers)
                return remoteUsers
            }
            return loadCachedLeaderboard()
        } catch (e: CancellationException) {
            Log.d("LeaderboardRepository", "fetchRemoteLeaderboard cancelled")
            return loadCachedLeaderboard()
        } catch (e: Exception) {
            Log.e("LeaderboardRepository", "Error fetching remote leaderboard", e)
            return loadCachedLeaderboard()
        }
    }

    fun recordUserForLeaderboard(profile: UserProfile) {
        if (profile.uid.isBlank()) return

        val userStats = quizResultRepository.getUserStats()
        val achievements = achievementRepository.getAllAchievements(profile.xp, profile.coins, profile.streak)
        val unlockedCount = achievements.count { it.isUnlocked }
        val quizzesCount = maxOf(profile.totalQuizzesPlayed, userStats.totalQuizzesPlayed)
        val score = calculateScore(profile.xp, quizzesCount, unlockedCount)

        val startOfWeek = getStartOfWeekMillis()
        val weeklyXp = if (profile.quizHistory.isNotEmpty()) {
            profile.quizHistory.filter { it.timestamp >= startOfWeek }.sumOf { it.xpEarned }
        } else {
            0
        }

        val name = when {
            profile.name.isNotBlank() && profile.name != "Player" && profile.name != "Guest Player" -> profile.name
            profile.email.isNotBlank() && !profile.email.startsWith("Guest") -> profile.email.substringBefore("@").replaceFirstChar { it.uppercase() }
            else -> profile.name.ifBlank { "Player" }
        }

        val userItem = LeaderboardUser(
            rank = 0,
            id = profile.uid,
            name = name,
            avatarId = profile.avatarId.ifBlank { "brain" },
            xp = profile.xp,
            weeklyXp = weeklyXp,
            level = maxOf(1, profile.level),
            rankBadge = RankUtils.getRankForXp(profile.xp),
            quizzesPlayed = quizzesCount,
            achievementsCount = unlockedCount,
            score = score,
            countryFlag = "🌟",
            rankChange = 0,
            isCurrentUser = false
        )

        val currentRegistered = loadRegisteredAppUsers().filterNot { it.id == userItem.id }
        saveRegisteredAppUsers(listOf(userItem) + currentRegistered)

        val currentCached = loadCachedLeaderboard().filterNot { it.id == userItem.id }
        saveCachedLeaderboard(listOf(userItem) + currentCached)
    }

    suspend fun syncCurrentUserToLeaderboard(profile: UserProfile? = null) {
        val userProfile = profile ?: userProfileStore.getProfile()
        if (userProfile.uid.isBlank()) return

        // Always record locally so all accounts on this device/app are available on leaderboard
        recordUserForLeaderboard(userProfile)

        if (userProfile.uid.startsWith("guest_") || userProfile.isGuest()) {
            return
        }

        try {
            val firestore = getFirestore() ?: return
            val userStats = quizResultRepository.getUserStats()
            val achievements = achievementRepository.getAllAchievements(userProfile.xp, userProfile.coins, userProfile.streak)
            val unlockedCount = achievements.count { it.isUnlocked }
            val quizzesCount = maxOf(userProfile.totalQuizzesPlayed, userStats.totalQuizzesPlayed)
            val score = calculateScore(userProfile.xp, quizzesCount, unlockedCount)

            val startOfWeek = getStartOfWeekMillis()
            val weeklyXp = if (userProfile.quizHistory.isNotEmpty()) {
                userProfile.quizHistory.filter { it.timestamp >= startOfWeek }.sumOf { it.xpEarned }
            } else {
                0
            }

            val displayName = when {
                userProfile.name.isNotBlank() && userProfile.name != "Player" && userProfile.name != "Guest Player" -> userProfile.name
                userProfile.email.isNotBlank() && !userProfile.email.startsWith("Guest") -> userProfile.email.substringBefore("@").replaceFirstChar { it.uppercase() }
                else -> userProfile.name.ifBlank { "Player" }
            }

            val entry = hashMapOf(
                "id" to userProfile.uid,
                "uid" to userProfile.uid,
                "name" to displayName,
                "email" to userProfile.email,
                "avatarId" to userProfile.avatarId.ifBlank { "brain" },
                "xp" to userProfile.xp,
                "weeklyXp" to weeklyXp,
                "weekStart" to startOfWeek,
                "level" to maxOf(1, userProfile.level),
                "rankBadge" to RankUtils.getRankForXp(userProfile.xp),
                "rank" to RankUtils.getRankForXp(userProfile.xp),
                "quizzesPlayed" to quizzesCount,
                "totalQuizzesPlayed" to quizzesCount,
                "achievementsCount" to unlockedCount,
                "score" to score,
                "countryFlag" to "🌟",
                "updatedAt" to System.currentTimeMillis()
            )

            withTimeoutOrNull(3500L) {
                firestore.collection("leaderboard")
                    .document(userProfile.uid)
                    .set(entry)
                    .await()
            }

            Log.d("LeaderboardRepository", "Successfully synced user ${userProfile.uid} ($displayName) to Firestore leaderboard (XP: ${userProfile.xp})")
        } catch (e: CancellationException) {
            Log.d("LeaderboardRepository", "syncCurrentUserToLeaderboard cancelled")
        } catch (e: Exception) {
            Log.e("LeaderboardRepository", "Error syncing user to leaderboard: ${e.message}")
        }
    }

    private fun calculateScore(xp: Int, quizzesPlayed: Int, achievementsCount: Int): Int {
        return xp + (achievementsCount * 50) + (quizzesPlayed * 15)
    }

    fun loadRegisteredAppUsers(): List<LeaderboardUser> {
        val prefs = getPrefs() ?: return emptyList()
        val jsonStr = prefs.getString("registered_app_users_json", "") ?: ""
        if (jsonStr.isNotBlank()) {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<LeaderboardUser>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        LeaderboardUser(
                            rank = obj.optInt("rank", 0),
                            id = obj.optString("id", ""),
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
                Log.e("LeaderboardRepository", "Error reading registered app users", e)
            }
        }
        return emptyList()
    }

    fun saveRegisteredAppUsers(users: List<LeaderboardUser>) {
        try {
            val prefs = getPrefs() ?: return
            val existing = loadRegisteredAppUsers().associateBy { it.id }.toMutableMap()
            users.forEach { user ->
                if (user.id.isNotBlank()) {
                    existing[user.id] = user.copy(isCurrentUser = false)
                }
            }
            val array = JSONArray()
            existing.values.forEach { user ->
                val obj = JSONObject().apply {
                    put("id", user.id)
                    put("name", user.name)
                    put("avatarId", user.avatarId)
                    put("xp", user.xp)
                    put("weeklyXp", user.weeklyXp)
                    put("level", user.level)
                    put("rankBadge", user.rankBadge)
                    put("quizzesPlayed", user.quizzesPlayed)
                    put("achievementsCount", user.achievementsCount)
                    put("score", user.score)
                    put("countryFlag", user.countryFlag)
                    put("rankChange", user.rankChange)
                }
                array.put(obj)
            }
            prefs.edit().putString("registered_app_users_json", array.toString()).apply()
        } catch (e: Exception) {
            Log.e("LeaderboardRepository", "Error saving registered app users", e)
        }
    }

    private fun loadCachedLeaderboard(): List<LeaderboardUser> {
        val resultList = mutableListOf<LeaderboardUser>()

        // 1. Read stored cached JSON
        val prefs = getPrefs()
        val jsonStr = prefs?.getString("cached_leaderboard_json", "") ?: ""
        if (jsonStr.isNotBlank()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    resultList.add(
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
            } catch (e: Exception) {
                Log.e("LeaderboardRepository", "Error reading cached leaderboard", e)
            }
        }

        // 2. Include all users registered in app
        val registered = loadRegisteredAppUsers()
        resultList.addAll(registered)

        // 3. Include all saved profiles stored on this device
        try {
            val savedProfiles = userProfileStore.getAllSavedProfiles()
            for (p in savedProfiles) {
                if (p.uid.isNotBlank()) {
                    val name = when {
                        p.name.isNotBlank() && p.name != "Player" && p.name != "Guest Player" -> p.name
                        p.email.isNotBlank() && !p.email.startsWith("Guest") -> p.email.substringBefore("@").replaceFirstChar { it.uppercase() }
                        else -> p.name.ifBlank { "Player" }
                    }
                    val userScore = calculateScore(p.xp, p.totalQuizzesPlayed, p.unlockedAchievements.size)
                    resultList.add(
                        LeaderboardUser(
                            rank = 0,
                            id = p.uid,
                            name = name,
                            avatarId = p.avatarId.ifBlank { "brain" },
                            xp = p.xp,
                            weeklyXp = maxOf(0, (p.xp * 0.15).toInt()),
                            level = maxOf(1, p.level),
                            rankBadge = RankUtils.getRankForXp(p.xp),
                            quizzesPlayed = p.totalQuizzesPlayed,
                            achievementsCount = p.unlockedAchievements.size,
                            score = userScore,
                            countryFlag = "🌟",
                            rankChange = 0,
                            isCurrentUser = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("LeaderboardRepository", "Error fetching saved profiles for leaderboard", e)
        }

        // 4. Merge with baseline community contestants to guarantee a full leaderboard
        val combined = (resultList + DEFAULT_COMMUNITY_PLAYERS).distinctBy { it.id }
        return combined
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

    companion object {
        val DEFAULT_COMMUNITY_PLAYERS = listOf(
            LeaderboardUser(
                rank = 1,
                id = "comm_player_1",
                name = "Aarav Sharma",
                avatarId = "quiz_king",
                xp = 4850,
                weeklyXp = 820,
                level = 15,
                rankBadge = "Grandmaster",
                quizzesPlayed = 52,
                achievementsCount = 14,
                score = 6330,
                countryFlag = "🇮🇳",
                rankChange = 0,
                isCurrentUser = false
            ),
            LeaderboardUser(
                rank = 2,
                id = "comm_player_2",
                name = "Elena Rostova",
                avatarId = "scientist",
                xp = 4320,
                weeklyXp = 750,
                level = 14,
                rankBadge = "Master",
                quizzesPlayed = 46,
                achievementsCount = 12,
                score = 5610,
                countryFlag = "🇩🇪",
                rankChange = 1,
                isCurrentUser = false
            ),
            LeaderboardUser(
                rank = 3,
                id = "comm_player_3",
                name = "Priya Patel",
                avatarId = "quiz_queen",
                xp = 3980,
                weeklyXp = 690,
                level = 13,
                rankBadge = "Master",
                quizzesPlayed = 41,
                achievementsCount = 11,
                score = 5145,
                countryFlag = "🇮🇳",
                rankChange = -1,
                isCurrentUser = false
            ),
            LeaderboardUser(
                rank = 4,
                id = "comm_player_4",
                name = "Lucas Miller",
                avatarId = "detective",
                xp = 3560,
                weeklyXp = 580,
                level = 12,
                rankBadge = "Expert",
                quizzesPlayed = 38,
                achievementsCount = 10,
                score = 4630,
                countryFlag = "🇺🇸",
                rankChange = 2,
                isCurrentUser = false
            ),
            LeaderboardUser(
                rank = 5,
                id = "comm_player_5",
                name = "Ananya Rao",
                avatarId = "brain_master",
                xp = 3120,
                weeklyXp = 510,
                level = 11,
                rankBadge = "Expert",
                quizzesPlayed = 34,
                achievementsCount = 9,
                score = 4080,
                countryFlag = "🇮🇳",
                rankChange = 0,
                isCurrentUser = false
            ),
            LeaderboardUser(
                rank = 6,
                id = "comm_player_6",
                name = "Kenji Sato",
                avatarId = "robot",
                xp = 2750,
                weeklyXp = 460,
                level = 10,
                rankBadge = "Scholar",
                quizzesPlayed = 29,
                achievementsCount = 8,
                score = 3585,
                countryFlag = "🇯🇵",
                rankChange = -1,
                isCurrentUser = false
            ),
            LeaderboardUser(
                rank = 7,
                id = "comm_player_7",
                name = "Sophia Davis",
                avatarId = "student_girl",
                xp = 2380,
                weeklyXp = 410,
                level = 9,
                rankBadge = "Scholar",
                quizzesPlayed = 25,
                achievementsCount = 7,
                score = 3105,
                countryFlag = "🇬🇧",
                rankChange = 1,
                isCurrentUser = false
            ),
            LeaderboardUser(
                rank = 8,
                id = "comm_player_8",
                name = "Rahul Verma",
                avatarId = "student_boy",
                xp = 2050,
                weeklyXp = 360,
                level = 8,
                rankBadge = "Thinker",
                quizzesPlayed = 22,
                achievementsCount = 6,
                score = 2680,
                countryFlag = "🇮🇳",
                rankChange = 0,
                isCurrentUser = false
            ),
            LeaderboardUser(
                rank = 9,
                id = "comm_player_9",
                name = "Mateo Hernandez",
                avatarId = "gamer",
                xp = 1720,
                weeklyXp = 310,
                level = 7,
                rankBadge = "Thinker",
                quizzesPlayed = 19,
                achievementsCount = 5,
                score = 2255,
                countryFlag = "🇪🇸",
                rankChange = 3,
                isCurrentUser = false
            ),
            LeaderboardUser(
                rank = 10,
                id = "comm_player_10",
                name = "Kavya Reddy",
                avatarId = "reader",
                xp = 1420,
                weeklyXp = 270,
                level = 6,
                rankBadge = "Apprentice",
                quizzesPlayed = 16,
                achievementsCount = 5,
                score = 1910,
                countryFlag = "🇮🇳",
                rankChange = -2,
                isCurrentUser = false
            ),
            LeaderboardUser(
                rank = 11,
                id = "comm_player_11",
                name = "David Chen",
                avatarId = "programmer",
                xp = 1150,
                weeklyXp = 220,
                level = 5,
                rankBadge = "Apprentice",
                quizzesPlayed = 13,
                achievementsCount = 4,
                score = 1545,
                countryFlag = "🇨🇦",
                rankChange = 1,
                isCurrentUser = false
            ),
            LeaderboardUser(
                rank = 12,
                id = "comm_player_12",
                name = "Sneha Kulkarni",
                avatarId = "student_girl",
                xp = 880,
                weeklyXp = 180,
                level = 4,
                rankBadge = "Novice",
                quizzesPlayed = 10,
                achievementsCount = 3,
                score = 1180,
                countryFlag = "🇮🇳",
                rankChange = 0,
                isCurrentUser = false
            ),
            LeaderboardUser(
                rank = 13,
                id = "comm_player_13",
                name = "Vikram Singh",
                avatarId = "student_boy",
                xp = 620,
                weeklyXp = 140,
                level = 3,
                rankBadge = "Novice",
                quizzesPlayed = 7,
                achievementsCount = 2,
                score = 825,
                countryFlag = "🇮🇳",
                rankChange = -1,
                isCurrentUser = false
            ),
            LeaderboardUser(
                rank = 14,
                id = "comm_player_14",
                name = "Fatima Al-Mansoor",
                avatarId = "brain",
                xp = 390,
                weeklyXp = 90,
                level = 2,
                rankBadge = "Beginner",
                quizzesPlayed = 4,
                achievementsCount = 1,
                score = 500,
                countryFlag = "🇦🇪",
                rankChange = 0,
                isCurrentUser = false
            ),
            LeaderboardUser(
                rank = 15,
                id = "comm_player_15",
                name = "Marcus Aurelius",
                avatarId = "quiz_king",
                xp = 210,
                weeklyXp = 50,
                level = 2,
                rankBadge = "Beginner",
                quizzesPlayed = 3,
                achievementsCount = 1,
                score = 305,
                countryFlag = "🇮🇹",
                rankChange = 0,
                isCurrentUser = false
            )
        )
    }
}
