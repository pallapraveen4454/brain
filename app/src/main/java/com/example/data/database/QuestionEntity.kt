package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.QuizQuestion

@Entity(tableName = "category_questions")
data class QuestionEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String,
    val explanation: String = "",
    val difficulty: String = "Medium" // Easy, Medium, Hard
) {
    fun toQuizQuestion(): QuizQuestion {
        val optionsList = listOf(optionA, optionB, optionC, optionD)
        
        // Find matching option index based on correctAnswer text or option key
        var correctIdx = optionsList.indexOfFirst { it.trim().equals(correctAnswer.trim(), ignoreCase = true) }
        if (correctIdx < 0) {
            correctIdx = when (correctAnswer.trim().uppercase()) {
                "A", "OPTION A", "0" -> 0
                "B", "OPTION B", "1" -> 1
                "C", "OPTION C", "2" -> 2
                "D", "OPTION D", "3" -> 3
                else -> 0
            }
        }

        return QuizQuestion(
            id = id,
            categoryId = categoryId,
            questionText = questionText,
            options = optionsList,
            correctOptionIndex = correctIdx,
            explanation = explanation
        )
    }
}
