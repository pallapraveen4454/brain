package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.GeminiQuizService
import com.example.data.QuizRepository
import com.example.data.QuizResultRepository
import com.example.data.UserProfile
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.data.AchievementRepository
import com.example.data.model.Achievement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class QuizUiState(
    val categoryId: String = "",
    val categoryTitle: String = "Brain Quiz",
    val questions: List<QuizQuestion> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isAnswerSubmitted: Boolean = false,
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val timeRemaining: Int = 15,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val streak: Int = 0,
    val maxStreak: Int = 0,
    val xpEarned: Int = 0,
    val coinsEarned: Int = 0,
    val totalXp: Int = 0,
    val lastQuizDate: String = "",
    val savedQuizResult: QuizResult? = null,
    val newlyUnlockedAchievements: List<Achievement> = emptyList(),
    val isQuizComplete: Boolean = false,
    val isLoading: Boolean = true
)

class QuizViewModel(
    private val quizRepository: QuizRepository = QuizRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val geminiQuizService: GeminiQuizService = GeminiQuizService(),
    private val quizResultRepository: QuizResultRepository = QuizResultRepository(),
    private val achievementRepository: AchievementRepository = AchievementRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var advanceJob: Job? = null

    fun loadAiQuiz(topic: String) {
        timerJob?.cancel()
        advanceJob?.cancel()
        _uiState.update {
            it.copy(
                categoryId = "ai_custom",
                categoryTitle = "AI: $topic",
                isLoading = true,
                questions = emptyList()
            )
        }

        viewModelScope.launch {
            val questions = geminiQuizService.generateQuizForTopic(topic)
            val currentLocalXp = quizResultRepository.getLocalProgress().totalXp
            _uiState.update {
                QuizUiState(
                    categoryId = "ai_custom",
                    categoryTitle = "AI: $topic",
                    questions = questions,
                    currentQuestionIndex = 0,
                    selectedOptionIndex = null,
                    isAnswerSubmitted = false,
                    isCorrect = null,
                    score = 0,
                    timeRemaining = 15,
                    correctCount = 0,
                    wrongCount = 0,
                    streak = 0,
                    maxStreak = 0,
                    xpEarned = 0,
                    coinsEarned = 0,
                    totalXp = currentLocalXp,
                    isQuizComplete = false,
                    isLoading = false
                )
            }

            if (questions.isNotEmpty()) {
                startTimer()
            }
        }
    }

    fun loadQuiz(categoryId: String) {
        if (categoryId.startsWith("ai_topic_")) {
            val topic = categoryId.removePrefix("ai_topic_")
            loadAiQuiz(topic)
            return
        }
        timerJob?.cancel()
        advanceJob?.cancel()
        val rawQuestions = quizRepository.getQuestionsForCategory(categoryId)
        val normCategoryId = categoryId.lowercase()
        val isRandomCategory = normCategoryId in listOf("quick", "daily", "practice")

        // Automated Category Validation: Ensure every question belongs strictly to the selected category
        val validatedQuestions = if (isRandomCategory) {
            rawQuestions
        } else {
            rawQuestions.filter { question ->
                val isValid = question.categoryId.lowercase() == normCategoryId
                if (!isValid) {
                    Log.e("QuizViewModel", "Category validation mismatch: Question ${question.id} (category '${question.categoryId}') loaded for requested category '$normCategoryId'")
                }
                isValid
            }
        }

        val title = quizRepository.getCategoryTitle(categoryId)
        val currentLocalXp = quizResultRepository.getLocalProgress().totalXp

        _uiState.update {
            QuizUiState(
                categoryId = categoryId,
                categoryTitle = title,
                questions = validatedQuestions,
                currentQuestionIndex = 0,
                selectedOptionIndex = null,
                isAnswerSubmitted = false,
                isCorrect = null,
                score = 0,
                timeRemaining = 15,
                correctCount = 0,
                wrongCount = 0,
                streak = 0,
                maxStreak = 0,
                xpEarned = 0,
                coinsEarned = 0,
                totalXp = currentLocalXp,
                isQuizComplete = false,
                isLoading = false
            )
        }

        if (validatedQuestions.isNotEmpty()) {
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeRemaining > 0 && !_uiState.value.isAnswerSubmitted && !_uiState.value.isQuizComplete) {
                delay(1000)
                if (_uiState.value.isAnswerSubmitted || _uiState.value.isQuizComplete) break
                _uiState.update { currentState ->
                    if (currentState.isAnswerSubmitted || currentState.isQuizComplete) {
                        currentState
                    } else {
                        val nextTime = currentState.timeRemaining - 1
                        currentState.copy(timeRemaining = nextTime)
                    }
                }
            }

            // If time ran out and answer was not submitted
            if (_uiState.value.timeRemaining <= 0 && !_uiState.value.isAnswerSubmitted && !_uiState.value.isQuizComplete) {
                handleTimeOut()
            }
        }
    }

    private fun handleTimeOut() {
        advanceJob?.cancel()
        var wasAlreadySubmitted = false

        _uiState.update { currentState ->
            if (currentState.isAnswerSubmitted || currentState.isQuizComplete) {
                wasAlreadySubmitted = true
                return@update currentState
            }
            currentState.copy(
                isAnswerSubmitted = true,
                isCorrect = false,
                wrongCount = currentState.wrongCount + 1,
                streak = 0
            )
        }

        if (wasAlreadySubmitted) return

        advanceJob = viewModelScope.launch {
            delay(1500)
            advanceToNextQuestion()
        }
    }

    fun submitAnswer(optionIndex: Int) {
        timerJob?.cancel()
        advanceJob?.cancel()

        var wasAlreadySubmitted = false

        _uiState.update { currentState ->
            if (currentState.isAnswerSubmitted || currentState.isQuizComplete) {
                wasAlreadySubmitted = true
                return@update currentState
            }

            val currentQuestion = currentState.questions.getOrNull(currentState.currentQuestionIndex)
            if (currentQuestion == null) {
                wasAlreadySubmitted = true
                return@update currentState
            }

            val isCorrect = currentQuestion.isAnswerCorrect(optionIndex)

            val addedXp: Int
            val newCorrectCount: Int
            val newWrongCount: Int
            val newStreak: Int

            if (isCorrect) {
                addedXp = 10
                newCorrectCount = currentState.correctCount + 1
                newWrongCount = currentState.wrongCount
                newStreak = currentState.streak + 1
            } else {
                addedXp = 0
                newCorrectCount = currentState.correctCount
                newWrongCount = currentState.wrongCount + 1
                newStreak = 0
            }

            val newScore = currentState.score + addedXp
            val newXpEarned = currentState.xpEarned + addedXp
            val newMaxStreak = maxOf(currentState.maxStreak, newStreak)

            currentState.copy(
                selectedOptionIndex = optionIndex,
                isAnswerSubmitted = true,
                isCorrect = isCorrect,
                score = newScore,
                xpEarned = newXpEarned,
                correctCount = newCorrectCount,
                wrongCount = newWrongCount,
                streak = newStreak,
                maxStreak = newMaxStreak
            )
        }

        if (wasAlreadySubmitted) return

        advanceJob = viewModelScope.launch {
            delay(1500)
            advanceToNextQuestion()
        }
    }

    private fun advanceToNextQuestion() {
        advanceJob?.cancel()
        timerJob?.cancel()
        val currentState = _uiState.value
        val nextIndex = currentState.currentQuestionIndex + 1

        if (nextIndex < currentState.questions.size) {
            _uiState.update {
                it.copy(
                    currentQuestionIndex = nextIndex,
                    selectedOptionIndex = null,
                    isAnswerSubmitted = false,
                    isCorrect = null,
                    timeRemaining = 15
                )
            }
            startTimer()
        } else {
            completeQuiz()
        }
    }

    private fun completeQuiz() {
        timerJob?.cancel()
        advanceJob?.cancel()
        val state = _uiState.value
        val scoreOutOfTen = state.correctCount
        // Formula: 10 XP per correct answer
        val finalXpEarned = scoreOutOfTen * 10
        val coinsGained = (scoreOutOfTen * 10) + (if (scoreOutOfTen >= 8) 50 else 20)
        val timestamp = System.currentTimeMillis()
        val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

        _uiState.update {
            it.copy(
                isQuizComplete = true,
                score = finalXpEarned,
                xpEarned = finalXpEarned,
                coinsEarned = coinsGained,
                lastQuizDate = dateFormatted
            )
        }

        // Save quiz result data storage system
        saveQuizResultData(
            categoryName = state.categoryTitle,
            scoreOutOfTen = scoreOutOfTen,
            xpEarned = finalXpEarned,
            coinsGained = coinsGained,
            dateFormatted = dateFormatted,
            timestamp = timestamp
        )
    }

    private fun saveQuizResultData(
        categoryName: String,
        scoreOutOfTen: Int,
        xpEarned: Int,
        coinsGained: Int,
        dateFormatted: String,
        timestamp: Long
    ) {
        viewModelScope.launch {
            try {
                val user = authRepository.currentUser
                val existingLocalProfile = authRepository.getPersistentGuestProfile()
                val userId = user?.uid ?: existingLocalProfile.uid

                val currentProfile = if (user != null) {
                    authRepository.fetchUserProfile(user.uid) ?: existingLocalProfile
                } else existingLocalProfile

                val currentTotalXp = maxOf(currentProfile.xp, existingLocalProfile.xp)
                val newTotalXp = currentTotalXp + xpEarned
                val currentCoins = maxOf(currentProfile.coins, existingLocalProfile.coins)
                val newCoins = currentCoins + coinsGained
                val newLevel = (newTotalXp / 500) + 1

                val currentStreak = maxOf(currentProfile.streak, existingLocalProfile.streak)
                val activeDate = existingLocalProfile.lastActiveDate.ifBlank { currentProfile.lastActiveDate }

                // Record stats for achievement tracking
                val questionsCount = _uiState.value.questions.size.ifZero(10)
                val isAiCustom = _uiState.value.categoryId == "ai_custom"
                achievementRepository.recordQuizCompletion(
                    scoreOutOfTen = scoreOutOfTen,
                    questionCount = questionsCount,
                    isAiCustom = isAiCustom
                )

                // Check and unlock achievements
                val achResult = achievementRepository.checkAndUnlockAchievements(
                    totalXp = newTotalXp,
                    totalCoins = newCoins,
                    currentStreak = currentStreak
                )

                val finalCoins = newCoins + achResult.extraCoinsEarned
                val totalCoinsGainedThisQuiz = coinsGained + achResult.extraCoinsEarned

                // Save quiz result to history
                val quizResult = quizResultRepository.saveQuizResult(
                    userId = userId,
                    categoryName = categoryName,
                    scoreOutOfTen = scoreOutOfTen,
                    xpEarned = xpEarned,
                    totalXp = newTotalXp,
                    coins = finalCoins,
                    coinsEarned = totalCoinsGainedThisQuiz,
                    streak = currentStreak,
                    lastActiveDate = activeDate,
                    timestamp = timestamp
                )

                _uiState.update {
                    it.copy(
                        totalXp = newTotalXp,
                        coinsEarned = totalCoinsGainedThisQuiz,
                        savedQuizResult = quizResult,
                        newlyUnlockedAchievements = achResult.newlyUnlocked
                    )
                }

                // Save updated user profile
                val updatedProfile = currentProfile.copy(
                    uid = userId,
                    xp = newTotalXp,
                    coins = finalCoins,
                    level = newLevel,
                    streak = currentStreak,
                    lastActiveDate = activeDate,
                    lastQuizCategory = categoryName,
                    lastQuizScore = scoreOutOfTen,
                    lastQuizXpEarned = xpEarned,
                    lastQuizDate = dateFormatted
                )
                authRepository.saveUserProfileToFirestore(updatedProfile)
            } catch (e: Exception) {
                Log.e("QuizViewModel", "Error saving quiz result data", e)
            }
        }
    }

    fun dismissAchievementDialog() {
        _uiState.update { it.copy(newlyUnlockedAchievements = emptyList()) }
    }

    private fun Int.ifZero(default: Int): Int = if (this == 0) default else this

    fun restartQuiz() {
        loadQuiz(_uiState.value.categoryId)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
