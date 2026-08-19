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
    val totalCorrectAnswers: Int = 0,
    val totalPerfectQuizzes: Int = 0,
    val consecutivePerfectQuizzes: Int = 0,
    val maxConsecutivePerfectQuizzes: Int = 0,
    val maxScoreOutOfTen: Int = 0,
    val hasCompletedAiQuiz: Boolean = false,
    val categoryQuestionCounts: Map<String, Int> = emptyMap()
)

data class AchievementCheckResult(
    val extraCoinsEarned: Int = 0,
    val newlyUnlocked: List<Achievement> = emptyList()
)

class AchievementRepository(
    private val context: Context? = try { BrainQuizApplication.instance } catch (e: Exception) { null },
    private val userProfileStore: UserProfileStore = UserProfileStore(context)
) {
    private fun getAccountKey(): String {
        val isGuest = userProfileStore.isGuestActive()
        if (isGuest) {
            return "guest_${userProfileStore.getGuestId()}"
        }
        val auth = try { com.google.firebase.auth.FirebaseAuth.getInstance() } catch (e: Exception) { null }
        val user = auth?.currentUser
        val profile = userProfileStore.getProfile()
        return when {
            user != null && user.uid.isNotBlank() -> "uid_${user.uid}"
            profile.uid.isNotBlank() && !profile.uid.startsWith("guest_") -> "uid_${profile.uid}"
            else -> "guest_${userProfileStore.getGuestId()}"
        }
    }

    private fun getSharedPreferences() = context?.getSharedPreferences("achievements_prefs_${getAccountKey()}", Context.MODE_PRIVATE)

    fun getStats(): AchievementStats {
        val prefs = getSharedPreferences() ?: return AchievementStats()
        val allPrefs = prefs.all
        val catMap = mutableMapOf<String, Int>()
        for ((key, value) in allPrefs) {
            if (key.startsWith("stat_cat_questions_") && value is Int) {
                val catKey = key.removePrefix("stat_cat_questions_")
                catMap[catKey] = value
            }
        }

        return AchievementStats(
            totalQuizzesCompleted = prefs.getInt("stat_total_quizzes", 0),
            totalQuestionsAnswered = prefs.getInt("stat_total_questions", 0),
            totalCorrectAnswers = prefs.getInt("stat_total_correct", 0),
            totalPerfectQuizzes = prefs.getInt("stat_total_perfect", 0),
            consecutivePerfectQuizzes = prefs.getInt("stat_consecutive_perfect", 0),
            maxConsecutivePerfectQuizzes = prefs.getInt("stat_max_consecutive_perfect", 0),
            maxScoreOutOfTen = prefs.getInt("stat_max_score", 0),
            hasCompletedAiQuiz = prefs.getBoolean("stat_ai_completed", false),
            categoryQuestionCounts = catMap
        )
    }

    fun recordQuizCompletion(
        scoreOutOfTen: Int,
        questionCount: Int,
        isAiCustom: Boolean,
        categoryId: String = "",
        correctCount: Int = scoreOutOfTen
    ) {
        val prefs = getSharedPreferences() ?: return
        val currentQuizzes = prefs.getInt("stat_total_quizzes", 0)
        val currentQuestions = prefs.getInt("stat_total_questions", 0)
        val currentCorrect = prefs.getInt("stat_total_correct", 0)
        val currentMaxScore = prefs.getInt("stat_max_score", 0)
        val currentPerfect = prefs.getInt("stat_total_perfect", 0)
        val currentConsecutive = prefs.getInt("stat_consecutive_perfect", 0)
        val currentMaxConsecutive = prefs.getInt("stat_max_consecutive_perfect", 0)

        val isPerfect = (scoreOutOfTen == 10)
        val newConsecutive = if (isPerfect) currentConsecutive + 1 else 0
        val newMaxConsecutive = maxOf(currentMaxConsecutive, newConsecutive)
        val newPerfectTotal = if (isPerfect) currentPerfect + 1 else currentPerfect

        val normalizedCat = normalizeCategoryKey(categoryId)

        prefs.edit().apply {
            putInt("stat_total_quizzes", currentQuizzes + 1)
            putInt("stat_total_questions", currentQuestions + questionCount)
            putInt("stat_total_correct", currentCorrect + correctCount)
            putInt("stat_max_score", maxOf(currentMaxScore, scoreOutOfTen))
            putInt("stat_total_perfect", newPerfectTotal)
            putInt("stat_consecutive_perfect", newConsecutive)
            putInt("stat_max_consecutive_perfect", newMaxConsecutive)
            if (isAiCustom) {
                putBoolean("stat_ai_completed", true)
            }
            if (normalizedCat.isNotBlank()) {
                val existingCatCount = prefs.getInt("stat_cat_questions_$normalizedCat", 0)
                putInt("stat_cat_questions_$normalizedCat", existingCatCount + questionCount)
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

        // Merge accumulated stats from profile for existing users
        val effectiveQuizzes = maxOf(stats.totalQuizzesCompleted, profile.totalQuizzesPlayed, profile.quizHistory.size)
        val effectiveQuestions = maxOf(stats.totalQuestionsAnswered, profile.totalQuestionsAnswered, effectiveQuizzes * 10)
        val effectiveCorrect = maxOf(stats.totalCorrectAnswers, profile.totalCorrectAnswers)
        val effectiveMaxScore = maxOf(stats.maxScoreOutOfTen, profile.bestScore)

        val mergedStats = stats.copy(
            totalQuizzesCompleted = effectiveQuizzes,
            totalQuestionsAnswered = effectiveQuestions,
            totalCorrectAnswers = effectiveCorrect,
            maxScoreOutOfTen = effectiveMaxScore
        )

        return initialList.map { ach ->
            val isUnlockedInPrefs = (prefs?.getBoolean("ach_unlocked_${ach.id}", false) ?: false) || profile.unlockedAchievements.contains(ach.id)
            val unlockDateInPrefs = prefs?.getString("ach_date_${ach.id}", "") ?: ""
            val isClaimedInPrefs = (prefs?.getBoolean("ach_claimed_${ach.id}", false) ?: false) || profile.claimedRewards.contains(ach.id)

            val progress = calculateProgress(ach.id, totalXp, totalCoins, currentStreak, mergedStats)
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
                unlockedAchievements = unlockedSet.toList(),
                claimedRewards = claimedSet.toList()
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
        val questions = stats.totalQuestionsAnswered
        val correct = stats.totalCorrectAnswers
        val accuracy = if (questions > 0) ((correct.toDouble() / questions.toDouble()) * 100).toInt() else 0
        val maxCatQuestions = if (stats.categoryQuestionCounts.isNotEmpty()) {
            stats.categoryQuestionCounts.values.maxOrNull() ?: 0
        } else 0

        return when (id) {
            // Progression
            "first_step", "first_quiz" -> stats.totalQuizzesCompleted
            "getting_started" -> stats.totalQuizzesCompleted
            "quiz_warrior" -> stats.totalQuizzesCompleted
            "sharp_mind" -> stats.totalQuizzesCompleted
            "dedicated_player", "quiz_master" -> stats.totalQuizzesCompleted
            "mastermind" -> stats.totalQuizzesCompleted
            "quiz_legend" -> stats.totalQuizzesCompleted

            // Accuracy
            "accuracy_pro" -> if (questions >= 50 && accuracy >= 80) 80 else if (questions >= 50) accuracy else (questions * 80 / 50)
            "accuracy_expert" -> if (questions >= 100 && accuracy >= 90) 90 else if (questions >= 100) accuracy else (questions * 90 / 100)
            "accuracy_master" -> if (questions >= 250 && accuracy >= 95) 95 else if (questions >= 250) accuracy else (questions * 95 / 250)

            // Perfect Score
            "perfect_10", "perfect_score" -> if (stats.maxScoreOutOfTen >= 10 || stats.totalPerfectQuizzes >= 1) 1 else 0
            "perfect_run" -> maxOf(stats.consecutivePerfectQuizzes, stats.maxConsecutivePerfectQuizzes)
            "perfectionist" -> stats.totalPerfectQuizzes

            // Daily Streak
            "streak_3_day" -> currentStreak
            "streak_7_day" -> currentStreak
            "streak_30_day" -> currentStreak

            // Category Mastery
            "category_explorer" -> maxCatQuestions
            "category_specialist" -> maxCatQuestions
            "category_expert" -> maxCatQuestions
            "category_master" -> maxCatQuestions

            // Extras preserved
            "xp_starter", "xp_master", "xp_legend" -> totalXp
            "coin_collector", "coin_master" -> totalCoins
            "ai_pioneer" -> if (stats.hasCompletedAiQuiz) 1 else 0
            else -> 0
        }
    }

    private fun normalizeCategoryKey(rawCategory: String): String {
        return rawCategory.lowercase(Locale.ROOT).trim().replace(" ", "_").replace("-", "_")
    }

    private fun getInitialAchievementsList(): List<Achievement> {
        return listOf(
            // 1. PROGRESSION
            Achievement(
                id = "first_step",
                title = "First Step",
                description = "Complete 1 quiz",
                category = "Progression",
                iconName = "first_step",
                targetProgress = 1,
                rewardCoins = 50
            ),
            Achievement(
                id = "getting_started",
                title = "Getting Started",
                description = "Complete 5 quizzes",
                category = "Progression",
                iconName = "getting_started",
                targetProgress = 5,
                rewardCoins = 75
            ),
            Achievement(
                id = "quiz_warrior",
                title = "Quiz Warrior",
                description = "Complete 25 quizzes",
                category = "Progression",
                iconName = "quiz_warrior",
                targetProgress = 25,
                rewardCoins = 150
            ),
            Achievement(
                id = "sharp_mind",
                title = "Sharp Mind",
                description = "Complete 50 quizzes",
                category = "Progression",
                iconName = "sharp_mind",
                targetProgress = 50,
                rewardCoins = 250
            ),
            Achievement(
                id = "dedicated_player",
                title = "Dedicated Player",
                description = "Complete 100 quizzes",
                category = "Progression",
                iconName = "dedicated_player",
                targetProgress = 100,
                rewardCoins = 500
            ),
            Achievement(
                id = "mastermind",
                title = "Mastermind",
                description = "Complete 250 quizzes",
                category = "Progression",
                iconName = "mastermind",
                targetProgress = 250,
                rewardCoins = 1000
            ),
            Achievement(
                id = "quiz_legend",
                title = "Quiz Legend",
                description = "Complete 500 quizzes",
                category = "Progression",
                iconName = "quiz_legend",
                targetProgress = 500,
                rewardCoins = 2000
            ),

            // 2. ACCURACY
            Achievement(
                id = "accuracy_pro",
                title = "Accuracy Pro",
                description = "Answer min 50 questions with >= 80% accuracy",
                category = "Accuracy",
                iconName = "accuracy_pro",
                targetProgress = 80,
                rewardCoins = 150
            ),
            Achievement(
                id = "accuracy_expert",
                title = "Accuracy Expert",
                description = "Answer min 100 questions with >= 90% accuracy",
                category = "Accuracy",
                iconName = "accuracy_expert",
                targetProgress = 90,
                rewardCoins = 300
            ),
            Achievement(
                id = "accuracy_master",
                title = "Accuracy Master",
                description = "Answer min 250 questions with >= 95% accuracy",
                category = "Accuracy",
                iconName = "accuracy_master",
                targetProgress = 95,
                rewardCoins = 600
            ),

            // 3. PERFECT SCORE
            Achievement(
                id = "perfect_10",
                title = "Perfect 10",
                description = "Score a perfect 10/10 in 1 quiz",
                category = "Perfect",
                iconName = "perfect_10",
                targetProgress = 1,
                rewardCoins = 100
            ),
            Achievement(
                id = "perfect_run",
                title = "Perfect Run",
                description = "Score 10/10 in 3 consecutive quizzes",
                category = "Perfect",
                iconName = "perfect_run",
                targetProgress = 3,
                rewardCoins = 350
            ),
            Achievement(
                id = "perfectionist",
                title = "Perfectionist",
                description = "Score 10/10 in 10 total quizzes",
                category = "Perfect",
                iconName = "perfectionist",
                targetProgress = 10,
                rewardCoins = 500
            ),

            // 4. DAILY STREAK (Preserved without modifying StreakCalculator)
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

            // 5. CATEGORY MASTERY
            Achievement(
                id = "category_explorer",
                title = "Category Explorer",
                description = "Answer 25 questions in a single category",
                category = "Category",
                iconName = "category_explorer",
                targetProgress = 25,
                rewardCoins = 100
            ),
            Achievement(
                id = "category_specialist",
                title = "Category Specialist",
                description = "Answer 50 questions in a single category",
                category = "Category",
                iconName = "category_specialist",
                targetProgress = 50,
                rewardCoins = 200
            ),
            Achievement(
                id = "category_expert",
                title = "Category Expert",
                description = "Answer 100 questions in a single category",
                category = "Category",
                iconName = "category_expert",
                targetProgress = 100,
                rewardCoins = 350
            ),
            Achievement(
                id = "category_master",
                title = "Category Master",
                description = "Answer 250 questions in a single category",
                category = "Category",
                iconName = "category_master",
                targetProgress = 250,
                rewardCoins = 750
            ),

            // 6. AI PIONEER
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

