package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class QuizResult(
    val id: String = "",
    val userId: String = "",
    val categoryName: String = "",
    val scoreOutOfTen: Int = 0,
    val xpEarned: Int = 0,
    val coinsEarned: Int = 0,
    val totalXp: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val dateFormatted: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
)
