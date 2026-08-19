package com.example

import com.example.utils.LevelUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelUtilsTest {

    @Test
    fun testZeroXp_showsLevel1AndZeroProgress() {
        assertEquals(1, LevelUtils.getLevel(0))
        assertEquals(0 to 100, LevelUtils.getLevelThresholds(0))
        assertEquals(0f, LevelUtils.getProgress(0), 0.0001f)
        assertEquals(100, LevelUtils.getXpToNextLevel(0))
    }

    @Test
    fun testLevel1Progression() {
        assertEquals(1, LevelUtils.getLevel(50))
        assertEquals(0.5f, LevelUtils.getProgress(50), 0.0001f)
        assertEquals(50, LevelUtils.getXpToNextLevel(50))
    }

    @Test
    fun testLevel2Threshold_resetsProgressToZero() {
        assertEquals(2, LevelUtils.getLevel(100))
        assertEquals(100 to 500, LevelUtils.getLevelThresholds(100))
        assertEquals(0f, LevelUtils.getProgress(100), 0.0001f)
        assertEquals(400, LevelUtils.getXpToNextLevel(100))

        // Mid Level 2 (300 XP -> 200/400 = 50%)
        assertEquals(2, LevelUtils.getLevel(300))
        assertEquals(0.5f, LevelUtils.getProgress(300), 0.0001f)
        assertEquals(200, LevelUtils.getXpToNextLevel(300))
    }

    @Test
    fun testLevel3Threshold_resetsProgressToZero() {
        assertEquals(3, LevelUtils.getLevel(500))
        assertEquals(500 to 1000, LevelUtils.getLevelThresholds(500))
        assertEquals(0f, LevelUtils.getProgress(500), 0.0001f)
        assertEquals(500, LevelUtils.getXpToNextLevel(500))

        // Mid Level 3 (750 XP -> 250/500 = 50%)
        assertEquals(3, LevelUtils.getLevel(750))
        assertEquals(0.5f, LevelUtils.getProgress(750), 0.0001f)
        assertEquals(250, LevelUtils.getXpToNextLevel(750))
    }

    @Test
    fun testLevel4Threshold_resetsProgressToZero() {
        assertEquals(4, LevelUtils.getLevel(1000))
        assertEquals(1000 to 2000, LevelUtils.getLevelThresholds(1000))
        assertEquals(0f, LevelUtils.getProgress(1000), 0.0001f)
        assertEquals(1000, LevelUtils.getXpToNextLevel(1000))
    }

    @Test
    fun testClamping() {
        assertTrue(LevelUtils.getProgress(-10) >= 0f)
        assertTrue(LevelUtils.getProgress(50000) <= 1f)
    }
}
