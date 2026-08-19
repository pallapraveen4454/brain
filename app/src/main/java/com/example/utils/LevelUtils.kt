package com.example.utils

object LevelUtils {
    /**
     * Level progression based on Total XP:
     * - Level 1: 0 - 99 XP (Target: 100 XP)
     * - Level 2: 100 - 499 XP (Target: 500 XP)
     * - Level 3: 500 - 999 XP (Target: 1000 XP)
     * - Level 4: 1000 - 1999 XP (Target: 2000 XP)
     * - Level N: 1000 XP per level above 2000 XP
     */
    fun getLevel(xp: Int): Int {
        val safeXp = maxOf(0, xp)
        return when {
            safeXp >= 2000 -> 4 + ((safeXp - 1000) / 1000)
            safeXp >= 1000 -> 4
            safeXp >= 500 -> 3
            safeXp >= 100 -> 2
            else -> 1
        }
    }

    /**
     * Returns a Pair(prevLevelThresholdXp, nextLevelThresholdXp)
     */
    fun getLevelThresholds(xp: Int): Pair<Int, Int> {
        val safeXp = maxOf(0, xp)
        return when {
            safeXp >= 2000 -> {
                val lvl = getLevel(safeXp)
                val base = 1000 + (lvl - 4) * 1000
                base to (base + 1000)
            }
            safeXp >= 1000 -> 1000 to 2000
            safeXp >= 500 -> 500 to 1000
            safeXp >= 100 -> 100 to 500
            else -> 0 to 100
        }
    }

    /**
     * Progress clamped strictly between 0.0f and 1.0f.
     * Guaranteed to return 0.0f for 0 XP and correctly reset upon reaching new levels.
     */
    fun getProgress(xp: Int): Float {
        val safeXp = maxOf(0, xp)
        val (prev, next) = getLevelThresholds(safeXp)
        val span = (next - prev).coerceAtLeast(1)
        return ((safeXp - prev).toFloat() / span.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Remaining XP required to advance to the next level.
     */
    fun getXpToNextLevel(xp: Int): Int {
        val safeXp = maxOf(0, xp)
        val (_, next) = getLevelThresholds(safeXp)
        return maxOf(0, next - safeXp)
    }
}
