package com.example.data.model

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val category: String, // "Beginner", "XP", "Quiz", "Streak", "Coins", "AI"
    val iconName: String,
    val targetProgress: Int,
    val rewardCoins: Int,
    val isUnlocked: Boolean = false,
    val currentProgress: Int = 0,
    val unlockDate: String = "",
    val isRewardClaimed: Boolean = false
)
