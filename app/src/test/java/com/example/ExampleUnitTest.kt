package com.example

import com.example.data.QuizRepository
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testAllQuizRepositoryQuestions() {
    val repo = QuizRepository()
    val categories = listOf("gk", "science", "history", "geo", "sports", "movies", "tech", "math")
    
    for (cat in categories) {
      val questions = repo.getQuestionsForCategory(cat)
      assertTrue("Category $cat should have 10 questions", questions.size == 10)
      for (q in questions) {
        assertEquals("Question ${q.id} should have 4 options", 4, q.options.size)
        assertTrue("Question ${q.id} correctOptionIndex ${q.correctOptionIndex} out of bounds", q.correctOptionIndex in 0..3)
        val selectedCorrectOption = q.options[q.correctOptionIndex]
        println("CAT: $cat | ID: ${q.id} | Correct: '$selectedCorrectOption' | Expl: '${q.explanation}'")
      }
    }
  }
}

