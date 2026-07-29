package com.example.data.model

data class QuizQuestion(
    val id: String,
    val categoryId: String,
    val questionText: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String = ""
) {
    val correctAnswer: String
        get() = options.getOrElse(correctOptionIndex) { "" }

    fun isAnswerCorrect(selectedOptionIndex: Int): Boolean {
        if (selectedOptionIndex == correctOptionIndex) return true
        val selectedText = options.getOrNull(selectedOptionIndex) ?: return false
        val correctText = options.getOrNull(correctOptionIndex) ?: return false
        return selectedText.trim().equals(correctText.trim(), ignoreCase = true)
    }
}
