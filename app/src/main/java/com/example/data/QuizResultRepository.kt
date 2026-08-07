package com.example.data

import android.content.Context
import android.util.Log
import com.example.BrainQuizApplication
import com.example.data.model.QuizResult
import com.example.utils.RankUtils
import com.example.utils.StreakUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserStats(
    val totalQuizzesPlayed: Int = 0,
    val totalQuestionsAnswered: Int = 0,
    val totalCorrectAnswers: Int = 0,
    val accuracyPercentage: Int = 0,
    val bestScore: Int = 0,
    val longestStreak: Int = 0
)

data class LocalProgress(
    val totalXp: Int = 0,
    val level: Int = 1,
    val coins: Int = 0,
    val streak: Int = 0,
    val lastActiveDate: String = "",
    val rank: String = "Beginner",
    val lastCategoryName: String = "",
    val lastScoreOutOfTen: Int = 0,
    val lastXpEarned: Int = 0,
    val lastQuizDate: String = "",
    val lastQuizTimestamp: Long = 0L,
    val hasHistory: Boolean = false
)

class QuizResultRepository(
    private val context: Context? = try { BrainQuizApplication.instance } catch (e: Exception) { null },
    private val userProfileStore: UserProfileStore = UserProfileStore(context)
) {

    private fun getFirestore(): FirebaseFirestore? {
        val appCtx = context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null }
        if (appCtx != null) {
            BrainQuizApplication.ensureFirebaseInitialized(appCtx)
        }
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("QuizResultRepository", "Failed to access FirebaseFirestore instance: [${e.javaClass.name}] ${e.message}", e)
            null
        }
    }

    fun saveLocalProgress(
        totalXp: Int,
        level: Int,
        coins: Int,
        streak: Int,
        lastActiveDate: String = "",
        lastCategoryName: String = "",
        lastScoreOutOfTen: Int = 0,
        lastXpEarned: Int = 0,
        lastQuizDate: String = "",
        lastQuizTimestamp: Long = System.currentTimeMillis()
    ) {
        val current = userProfileStore.getProfile()
        Log.d("XP_TRACE", "[QuizResultRepository] saveLocalProgress: currentXp=${current.xp}, newTotalXp=$totalXp")
        val dateFormatted = if (lastQuizDate.isNotBlank()) lastQuizDate else SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(lastQuizTimestamp))
        val updated = current.copy(
            xp = totalXp,
            coins = coins,
            streak = streak,
            lastActiveDate = lastActiveDate.ifBlank { current.lastActiveDate },
            lastQuizCategory = lastCategoryName.ifBlank { current.lastQuizCategory },
            lastQuizScore = if (lastScoreOutOfTen > 0) lastScoreOutOfTen else current.lastQuizScore,
            lastQuizXpEarned = if (lastXpEarned > 0) lastXpEarned else current.lastQuizXpEarned,
            lastQuizDate = dateFormatted.ifBlank { current.lastQuizDate }
        )
        userProfileStore.saveProfile(updated)
    }

    fun getLocalProgress(): LocalProgress {
        val profile = userProfileStore.getProfile()
        val (calculatedStreak, updatedActiveDate) = if (profile.lastActiveDate.isNotBlank()) {
            StreakUtils.calculateStreak(profile.lastActiveDate, profile.streak)
        } else {
            Pair(profile.streak, profile.lastActiveDate)
        }

        return LocalProgress(
            totalXp = profile.xp,
            level = maxOf(1, profile.level),
            coins = profile.coins,
            streak = calculatedStreak,
            lastActiveDate = updatedActiveDate.ifBlank { profile.lastActiveDate },
            rank = RankUtils.getRankForXp(profile.xp),
            lastCategoryName = profile.lastQuizCategory,
            lastScoreOutOfTen = profile.lastQuizScore,
            lastXpEarned = profile.lastQuizXpEarned,
            lastQuizDate = profile.lastQuizDate,
            lastQuizTimestamp = profile.quizHistory.firstOrNull()?.timestamp ?: 0L,
            hasHistory = profile.quizHistory.isNotEmpty() || profile.lastQuizCategory.isNotBlank()
        )
    }

    fun getUserStats(): UserStats {
        val profile = userProfileStore.getProfile()
        val totalQuizzesPlayed = maxOf(profile.totalQuizzesPlayed, profile.quizHistory.size)
        val totalQuestionsAnswered = maxOf(profile.totalQuestionsAnswered, totalQuizzesPlayed * 10)
        val totalCorrectAnswers = maxOf(profile.totalCorrectAnswers, profile.quizHistory.sumOf { it.scoreOutOfTen })
        val accuracyPercentage = if (totalQuestionsAnswered > 0) {
            ((totalCorrectAnswers.toDouble() / totalQuestionsAnswered.toDouble()) * 100).toInt()
        } else 0
        val bestScore = maxOf(profile.bestScore, profile.quizHistory.maxOfOrNull { it.scoreOutOfTen } ?: 0)
        val longestStreak = maxOf(profile.longestStreak, profile.streak)

        return UserStats(
            totalQuizzesPlayed = totalQuizzesPlayed,
            totalQuestionsAnswered = totalQuestionsAnswered,
            totalCorrectAnswers = totalCorrectAnswers,
            accuracyPercentage = accuracyPercentage,
            bestScore = bestScore,
            longestStreak = longestStreak
        )
    }

    fun saveLocalQuizResult(quizResult: QuizResult) {
        val current = userProfileStore.getProfile()
        Log.d("XP_TRACE", "[QuizResultRepository] saveLocalQuizResult: currentXp=${current.xp}, resultTotalXp=${quizResult.totalXp}")
        val history = (listOf(quizResult) + current.quizHistory)
            .distinctBy { if (it.id.isNotBlank()) it.id else "${it.timestamp}_${it.categoryName}" }
            .sortedByDescending { it.timestamp }
        userProfileStore.saveProfile(current.copy(xp = maxOf(current.xp, quizResult.totalXp), quizHistory = history))
    }

    fun getLocalQuizResultsList(): List<QuizResult> {
        return userProfileStore.getProfile().quizHistory
    }

    suspend fun saveQuizResult(
        userId: String,
        categoryName: String,
        scoreOutOfTen: Int,
        xpEarned: Int,
        totalXp: Int,
        coins: Int = -1,
        coinsEarned: Int = 0,
        streak: Int = -1,
        lastActiveDate: String = "",
        timestamp: Long = System.currentTimeMillis()
    ): QuizResult {
        Log.d("XP_TRACE", "[QuizResultRepository] saveQuizResult: userId=$userId, scoreOutOfTen=$scoreOutOfTen, xpEarned=$xpEarned, totalXp=$totalXp")
        val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        val resultId = "result_${timestamp}_${(1000..9999).random()}"
        val finalCoinsEarned = if (coinsEarned >= 0) coinsEarned else (scoreOutOfTen * 10)

        val quizResult = QuizResult(
            id = resultId,
            userId = userId,
            categoryName = categoryName,
            scoreOutOfTen = scoreOutOfTen,
            xpEarned = xpEarned,
            coinsEarned = finalCoinsEarned,
            totalXp = totalXp,
            timestamp = timestamp,
            dateFormatted = dateFormatted
        )

        // 1. Save to local SharedPreferences history list
        saveLocalQuizResult(quizResult)

        // 2. Update user stats
        val ctx = context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null }
        ctx?.let {
            val prefs = it.getSharedPreferences("quiz_results_prefs", Context.MODE_PRIVATE)
            val currentStats = getUserStats()
            val newQuizzesPlayed = currentStats.totalQuizzesPlayed + 1
            val newQuestionsAnswered = currentStats.totalQuestionsAnswered + 10
            val newCorrectAnswers = currentStats.totalCorrectAnswers + scoreOutOfTen
            val newBestScore = maxOf(currentStats.bestScore, scoreOutOfTen)
            val newLongestStreak = maxOf(currentStats.longestStreak, if (streak >= 0) streak else getLocalProgress().streak)

            prefs.edit().apply {
                putInt("stats_quizzes_played", newQuizzesPlayed)
                putInt("stats_questions_answered", newQuestionsAnswered)
                putInt("stats_correct_answers", newCorrectAnswers)
                putInt("stats_best_score", newBestScore)
                putInt("stats_longest_streak", newLongestStreak)
                apply()
            }
        }

        // 3. Save to Firestore under user subcollection 'quiz_results'
        try {
            val firestore = getFirestore()
            if (firestore != null && userId.isNotBlank() && !userId.startsWith("guest_")) {
                withContext(NonCancellable) {
                    withTimeoutOrNull(3000L) {
                        firestore.collection("users")
                            .document(userId)
                            .collection("quiz_results")
                            .document(resultId)
                            .set(quizResult)
                            .await()
                    }
                }
            }
        } catch (e: CancellationException) {
            Log.d("QuizResultRepository", "saveQuizResult to Firestore cancelled")
        } catch (e: Exception) {
            Log.e("QuizResultRepository", "Error saving quiz result to Firestore", e)
        }

        // 4. Local SharedPreferences backup for offline state management
        val currentLocal = getLocalProgress()
        val finalCoins = if (coins >= 0) coins else currentLocal.coins
        val finalStreak = if (streak >= 0) streak else currentLocal.streak
        val finalActiveDate = if (lastActiveDate.isNotBlank()) lastActiveDate else currentLocal.lastActiveDate

        saveLocalProgress(
            totalXp = totalXp,
            level = (totalXp / 500) + 1,
            coins = finalCoins,
            streak = finalStreak,
            lastActiveDate = finalActiveDate,
            lastCategoryName = categoryName,
            lastScoreOutOfTen = scoreOutOfTen,
            lastXpEarned = xpEarned,
            lastQuizDate = dateFormatted,
            lastQuizTimestamp = timestamp
        )

        return quizResult
    }

    suspend fun getRecentQuizResults(userId: String): List<QuizResult> {
        val localList = getLocalQuizResultsList()
        try {
            val firestore = getFirestore()
            if (firestore != null && userId.isNotBlank() && !userId.startsWith("guest_")) {
                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("quiz_results")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .await()
                val remoteList = snapshot.toObjects(QuizResult::class.java)
                if (remoteList.isNotEmpty()) {
                    val merged = (localList + remoteList).distinctBy { it.id }.sortedByDescending { it.timestamp }
                    merged.forEach { saveLocalQuizResult(it) }
                    return merged
                }
            }
        } catch (e: CancellationException) {
            Log.d("QuizResultRepository", "getRecentQuizResults cancelled")
        } catch (e: Exception) {
            Log.e("QuizResultRepository", "Error fetching quiz results from Firestore", e)
        }
        return localList
    }
}
