package com.example.data

import android.content.Context
import android.util.Log
import com.example.BrainQuizApplication
import com.example.data.model.Achievement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AchievementStats(
    val totalQuizzesCompleted: Int = 0,
    val totalQuestionsAnswered: Int = 0,
    val maxScoreOutOfTen: Int = 0,
    val hasCompletedAiQuiz: Boolean = false
)

data class AchievementCheckResult(
    val extraCoinsEarned: Int = 0,
    val newlyUnlocked: List<Achievement> = emptyList()
)

class AchievementRepository(
    private val context: Context? = try { BrainQuizApplication.instance } catch (e: Exception) { null },
    private val userProfileStore: UserProfileStore = UserProfileStore(context)
) {
    private val prefsName = "achievements_prefs"

    private fun getSharedPreferences() = context?.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun getStats(): AchievementStats {
        val prefs = getSharedPreferences() ?: return AchievementStats()
        return AchievementStats(
            totalQuizzesCompleted = prefs.getInt("stat_total_quizzes", 0),
            totalQuestionsAnswered = prefs.getInt("stat_total_questions", 0),
            maxScoreOutOfTen = prefs.getInt("stat_max_score", 0),
            hasCompletedAiQuiz = prefs.getBoolean("stat_ai_completed", false)
        )
    }

    fun recordQuizCompletion(scoreOutOfTen: Int, questionCount: Int, isAiCustom: Boolean) {
        val prefs = getSharedPreferences() ?: return
        val currentQuizzes = prefs.getInt("stat_total_quizzes", 0)
        val currentQuestions = prefs.getInt("stat_total_questions", 0)
        val currentMaxScore = prefs.getInt("stat_max_score", 0)
        val aiDone = prefs.getBoolean("stat_ai_completed", false)

        prefs.edit().apply {
            putInt("stat_total_quizzes", currentQuizzes + 1)
            putInt("stat_total_questions", currentQuestions + questionCount)
            putInt("stat_max_score", maxOf(currentMaxScore, scoreOutOfTen))
            if (isAiCustom) {
                putBoolean("stat_ai_completed", true)
            }
            apply()
        }
    }

    fun getAllAchievements(
        totalXp: Int,
        totalCoins: Int,
        currentStreak: Int
    ): List<Achievement> {
        val initialList = getInitialAchievementsList()
        val stats = getStats()
        val prefs = getSharedPreferences()
        val profile = userProfileStore.getProfile()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        return initialList.map { ach ->
            val isUnlockedInPrefs = (prefs?.getBoolean("ach_unlocked_${ach.id}", false) ?: false) || profile.unlockedAchievements.contains(ach.id)
            val unlockDateInPrefs = prefs?.getString("ach_date_${ach.id}", "") ?: ""
            val isClaimedInPrefs = (prefs?.getBoolean("ach_claimed_${ach.id}", false) ?: false) || profile.claimedRewards.contains(ach.id)

            val progress = calculateProgress(ach.id, totalXp, totalCoins, currentStreak, stats)
            val isUnlocked = isUnlockedInPrefs || progress >= ach.targetProgress

            val finalUnlockDate = if (isUnlocked) {
                if (unlockDateInPrefs.isNotBlank()) unlockDateInPrefs else todayStr
            } else ""

            // Persist auto-unlocked status if newly met
            if (isUnlocked && !isUnlockedInPrefs && prefs != null) {
                prefs.edit().apply {
                    putBoolean("ach_unlocked_${ach.id}", true)
                    putString("ach_date_${ach.id}", finalUnlockDate)
                    apply()
                }
            }

            ach.copy(
                isUnlocked = isUnlocked,
                currentProgress = minOf(progress, ach.targetProgress),
                unlockDate = finalUnlockDate,
                isRewardClaimed = isClaimedInPrefs
            )
        }
    }

    fun checkAndUnlockAchievements(
        totalXp: Int,
        totalCoins: Int,
        currentStreak: Int
    ): AchievementCheckResult {
        val prefs = getSharedPreferences()
        val achievements = getAllAchievements(totalXp, totalCoins, currentStreak)
        val newlyUnlockedList = mutableListOf<Achievement>()
        var extraCoins = 0
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val profile = userProfileStore.getProfile()
        val unlockedSet = profile.unlockedAchievements.toMutableSet()
        val claimedSet = profile.claimedRewards.toMutableSet()

        prefs?.edit()?.let { editor ->
            for (ach in achievements) {
                val isUnlockedInPrefs = prefs.getBoolean("ach_unlocked_${ach.id}", false) || unlockedSet.contains(ach.id)
                val isClaimedInPrefs = prefs.getBoolean("ach_claimed_${ach.id}", false) || claimedSet.contains(ach.id)

                if (ach.isUnlocked) {
                    if (!isUnlockedInPrefs) {
                        editor.putBoolean("ach_unlocked_${ach.id}", true)
                        editor.putString("ach_date_${ach.id}", todayStr)
                        unlockedSet.add(ach.id)
                    }

                    if (!isClaimedInPrefs) {
                        editor.putBoolean("ach_claimed_${ach.id}", true)
                        extraCoins += ach.rewardCoins
                        newlyUnlockedList.add(ach.copy(isRewardClaimed = true))
                        claimedSet.add(ach.id)
                    }
                }
            }
            editor.apply()
        }

        userProfileStore.saveProfile(
            profile.copy(
                coins = profile.coins + extraCoins,
                unlockedAchievements = unlockedSet,
                claimedRewards = claimedSet
            )
        )

        return AchievementCheckResult(
            extraCoinsEarned = extraCoins,
            newlyUnlocked = newlyUnlockedList
        )
    }

    private fun calculateProgress(
        id: String,
        totalXp: Int,
        totalCoins: Int,
        currentStreak: Int,
        stats: AchievementStats
    ): Int {
        return when (id) {
            "first_quiz" -> stats.totalQuizzesCompleted
            "first_10_q" -> stats.totalQuestionsAnswered
            "xp_starter", "xp_master", "xp_legend" -> totalXp
            "perfect_score" -> stats.maxScoreOutOfTen
            "quiz_player", "quiz_master" -> stats.totalQuizzesCompleted
            "streak_3_day", "streak_7_day", "streak_30_day" -> currentStreak
            "coin_collector", "coin_master" -> totalCoins
            "ai_pioneer" -> if (stats.hasCompletedAiQuiz) 1 else 0
            else -> 0
        }
    }

    private fun getInitialAchievementsList(): List<Achievement> {
        return listOf(
            Achievement(
                id = "first_quiz",
                title = "First Quiz",
                description = "Complete your first quiz",
                category = "Beginner",
                iconName = "first_quiz",
                targetProgress = 1,
                rewardCoins = 50
            ),
            Achievement(
                id = "first_10_q",
                title = "First 10 Questions",
                description = "Answer 10 questions total",
                category = "Beginner",
                iconName = "first_10_q",
                targetProgress = 10,
                rewardCoins = 50
            ),
            Achievement(
                id = "xp_starter",
                title = "XP Starter",
                description = "Earn 100 XP total",
                category = "XP",
                iconName = "xp_starter",
                targetProgress = 100,
                rewardCoins = 50
            ),
            Achievement(
                id = "xp_master",
                title = "XP Master",
                description = "Earn 500 XP total",
                category = "XP",
                iconName = "xp_master",
                targetProgress = 500,
                rewardCoins = 100
            ),
            Achievement(
                id = "xp_legend",
                title = "XP Legend",
                description = "Earn 1000 XP total",
                category = "XP",
                iconName = "xp_legend",
                targetProgress = 1000,
                rewardCoins = 200
            ),
            Achievement(
                id = "perfect_score",
                title = "Perfect Score",
                description = "Get a perfect 10/10 score in a quiz",
                category = "Quiz",
                iconName = "perfect_score",
                targetProgress = 10,
                rewardCoins = 100
            ),
            Achievement(
                id = "quiz_player",
                title = "Quiz Player",
                description = "Complete 10 quizzes",
                category = "Quiz",
                iconName = "quiz_player",
                targetProgress = 10,
                rewardCoins = 150
            ),
            Achievement(
                id = "quiz_master",
                title = "Quiz Master",
                description = "Complete 100 quizzes",
                category = "Quiz",
                iconName = "quiz_master",
                targetProgress = 100,
                rewardCoins = 500
            ),
            Achievement(
                id = "streak_3_day",
                title = "3 Day Warrior",
                description = "Maintain a 3-day active streak",
                category = "Streak",
                iconName = "streak_3_day",
                targetProgress = 3,
                rewardCoins = 50
            ),
            Achievement(
                id = "streak_7_day",
                title = "7 Day Warrior",
                description = "Maintain a 7-day active streak",
                category = "Streak",
                iconName = "streak_7_day",
                targetProgress = 7,
                rewardCoins = 100
            ),
            Achievement(
                id = "streak_30_day",
                title = "30 Day Champion",
                description = "Maintain a 30-day active streak",
                category = "Streak",
                iconName = "streak_30_day",
                targetProgress = 30,
                rewardCoins = 500
            ),
            Achievement(
                id = "coin_collector",
                title = "Coin Collector",
                description = "Accumulate 1000 coins",
                category = "Coins",
                iconName = "coin_collector",
                targetProgress = 1000,
                rewardCoins = 200
            ),
            Achievement(
                id = "coin_master",
                title = "Coin Master",
                description = "Accumulate 5000 coins",
                category = "Coins",
                iconName = "coin_master",
                targetProgress = 5000,
                rewardCoins = 500
            ),
            Achievement(
                id = "ai_pioneer",
                title = "Gemini AI Pioneer",
                description = "Generate and complete a custom AI quiz",
                category = "AI",
                iconName = "ai_pioneer",
                targetProgress = 1,
                rewardCoins = 50
            )
        )
    }
}
