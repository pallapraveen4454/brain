package com.example.utils

object RankUtils {
    /**
     * Rank progression based on Total XP:
     * - 0-99 XP: Beginner
     * - 100-499 XP: Learner
     * - 500-999 XP: Master
     * - 1000+ XP: Genius
     */
    fun getRankForXp(xp: Int): String {
        return when {
            xp >= 1000 -> "Genius"
            xp >= 500 -> "Master"
            xp >= 100 -> "Learner"
            else -> "Beginner"
        }
    }
}
