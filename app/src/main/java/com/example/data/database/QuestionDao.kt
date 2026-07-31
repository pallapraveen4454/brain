package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface QuestionDao {

    @Query("SELECT COUNT(*) FROM category_questions")
    suspend fun getQuestionCount(): Int

    @Query("SELECT COUNT(*) FROM category_questions")
    fun getQuestionCountSync(): Int

    @Query("SELECT * FROM category_questions ORDER BY RANDOM()")
    suspend fun getAllQuestionsRandomized(): List<QuestionEntity>

    @Query("SELECT * FROM category_questions ORDER BY id ASC")
    suspend fun getAllQuestions(): List<QuestionEntity>

    @Query("SELECT * FROM category_questions WHERE difficulty = :difficulty ORDER BY RANDOM()")
    suspend fun getQuestionsByDifficulty(difficulty: String): List<QuestionEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM category_questions WHERE LOWER(TRIM(id)) = LOWER(TRIM(:id)))")
    suspend fun existsById(id: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM category_questions WHERE LOWER(TRIM(questionText)) = LOWER(TRIM(:questionText)))")
    suspend fun existsByText(questionText: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM category_questions WHERE LOWER(TRIM(questionText)) = LOWER(TRIM(:questionText)) AND LOWER(TRIM(id)) != LOWER(TRIM(:excludeId)))")
    suspend fun existsByTextExcludingId(questionText: String, excludeId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertQuestionsSync(questions: List<QuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)

    @Query("DELETE FROM category_questions")
    suspend fun deleteAllQuestions()
}
