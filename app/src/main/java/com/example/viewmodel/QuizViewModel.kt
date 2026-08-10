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
import android.app.Activity
import com.example.data.AchievementRepository
import com.example.data.HintRepository
import com.example.data.model.Achievement
import com.example.utils.RewardedAdManager
import com.example.utils.StreakUtils
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
    val isLoading: Boolean = true,
    val hiddenOptionIndices: Set<Int> = emptySet(),
    val isHintAvailableToday: Boolean = true,
    val isShowingAdForHint: Boolean = false,
    val hintErrorMessage: String? = null
)

class QuizViewModel(
    private val quizRepository: QuizRepository = QuizRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val geminiQuizService: GeminiQuizService = GeminiQuizService(),
    private val quizResultRepository: QuizResultRepository = QuizResultRepository(),
    private val achievementRepository: AchievementRepository = AchievementRepository(),
    private val hintRepository: HintRepository = HintRepository()
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
                    isLoading = false,
                    hiddenOptionIndices = emptySet(),
                    isHintAvailableToday = hintRepository.isHintAvailableForCategory("ai_custom"),
                    isShowingAdForHint = false,
                    hintErrorMessage = null
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
        val isRandomCategory = normCategoryId in listOf("quick", "daily")

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
                isLoading = false,
                hiddenOptionIndices = emptySet(),
                isHintAvailableToday = hintRepository.isHintAvailableForCategory(categoryId),
                isShowingAdForHint = false,
                hintErrorMessage = null
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

            Log.d("XP_TRACE", "[QuizViewModel] submitAnswer: questionIndex=${currentState.currentQuestionIndex}, isCorrect=$isCorrect, addedXp=$addedXp, quizXpEarned=$newXpEarned")

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
                    timeRemaining = 15,
                    hiddenOptionIndices = emptySet(),
                    isShowingAdForHint = false,
                    hintErrorMessage = null,
                    isHintAvailableToday = hintRepository.isHintAvailableForCategory(it.categoryId)
                )
            }
            startTimer()
        } else {
            completeQuiz()
        }
    }

    /**
     * Request a daily 50/50 hint for the current category.
     * Triggers the Rewarded Ad flow. The hint is ONLY consumed if the reward is confirmed.
     */
    fun requestHint(activity: Activity) {
        val currentState = _uiState.value
        if (currentState.isAnswerSubmitted || currentState.isQuizComplete) return
        if (currentState.timeRemaining <= 0) return
        if (currentState.isShowingAdForHint) return
        if (currentState.hiddenOptionIndices.isNotEmpty()) return
        if (!currentState.isHintAvailableToday) return

        _uiState.update { it.copy(isShowingAdForHint = true, hintErrorMessage = null) }

        RewardedAdManager.showRewardedAd(
            activity = activity,
            onRewardEarned = {
                applyHint5050()
            },
            onError = { errorMsg ->
                _uiState.update {
                    it.copy(
                        isShowingAdForHint = false,
                        hintErrorMessage = errorMsg
                    )
                }
            }
        )
    }

    private fun applyHint5050() {
        val currentState = _uiState.value
        val question = currentState.questions.getOrNull(currentState.currentQuestionIndex) ?: run {
            _uiState.update { it.copy(isShowingAdForHint = false) }
            return
        }

        val correctIndex = (0 until question.options.size).find { question.isAnswerCorrect(it) } ?: question.correctOptionIndex
        val incorrectIndices = (0 until question.options.size).filter { it != correctIndex }

        if (incorrectIndices.isEmpty()) {
            _uiState.update { it.copy(isShowingAdForHint = false) }
            return
        }

        val keptIncorrectIndex = incorrectIndices.random()
        val hiddenIndices = incorrectIndices.filter { it != keptIncorrectIndex }.toSet()

        // Persist hint consumption for today's calendar date for this category ONLY upon confirmed reward!
        hintRepository.markHintUsedForCategory(currentState.categoryId)

        _uiState.update {
            it.copy(
                hiddenOptionIndices = hiddenIndices,
                isHintAvailableToday = false,
                isShowingAdForHint = false,
                hintErrorMessage = null
            )
        }
    }

    fun clearHintError() {
        _uiState.update { it.copy(hintErrorMessage = null) }
    }

    private fun completeQuiz() {
        timerJob?.cancel()
        advanceJob?.cancel()
        val state = _uiState.value
        if (state.isQuizComplete) return

        val scoreOutOfTen = state.correctCount
        // Formula: 10 XP per correct answer
        val finalXpEarned = scoreOutOfTen * 10
        // Formula: Exactly 10 coins per correct answer
        val coinsGained = scoreOutOfTen * 10
        val timestamp = System.currentTimeMillis()
        val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

        Log.d("XP_TRACE", "[QuizViewModel] completeQuiz: scoreOutOfTen=$scoreOutOfTen, finalXpEarned=$finalXpEarned, coinsGained=$coinsGained")

        _uiState.update {
            it.copy(
                isQuizComplete = true,
                score = finalXpEarned,
                xpEarned = finalXpEarned,
                coinsEarned = coinsGained,
                lastQuizDate = dateFormatted
            )
        }

        // Point 1: Before saveQuizResultData is called
        Log.d("RUNTIME_TRACE", "[Point 1: Before saveQuizResultData] scoreOutOfTen=$scoreOutOfTen, finalXpEarned=$finalXpEarned, coinsGained=$coinsGained")

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
        // Point 2: At the first line of saveQuizResultData
        Log.d("RUNTIME_TRACE", "[Point 2: First line of saveQuizResultData] categoryName=$categoryName, scoreOutOfTen=$scoreOutOfTen, xpEarned=$xpEarned, coinsGained=$coinsGained")

        viewModelScope.launch {
            try {
                val user = authRepository.currentUser
                val isGuest = (user == null) || authRepository.isGuestSessionActive()
                val existingLocalProfile = authRepository.getPersistentGuestProfile()
                val userId = if (isGuest) existingLocalProfile.uid else (user?.uid ?: existingLocalProfile.uid)

                val currentProfile = if (!isGuest && user != null) {
                    authRepository.fetchUserProfile(user.uid) ?: existingLocalProfile
                } else {
                    authRepository.getPersistentGuestProfile()
                }

                // Point 3: Immediately after getPersistentGuestProfile / loading profile
                Log.d("RUNTIME_TRACE", "[Point 3: After loading profile] uid=${currentProfile.uid}, xp=${currentProfile.xp}, coins=${currentProfile.coins}, streak=${currentProfile.streak}, lastActiveDate=${currentProfile.lastActiveDate}, level=${currentProfile.level}, isGuest=$isGuest")

                // 1. Calculate XP and Coins
                val startXp = maxOf(currentProfile.xp, _uiState.value.totalXp)
                val newTotalXp = startXp + xpEarned
                Log.d("XP_TRACE", "[QuizViewModel] saveQuizResultData: startXp=$startXp, xpEarned=$xpEarned, newTotalXp=$newTotalXp")

                val startCoins = currentProfile.coins
                val newCoins = startCoins + coinsGained
                val newLevel = maxOf(1, (newTotalXp / 500) + 1)

                // 2. Calculate Streak and Active Date using StreakUtils
                val (calculatedStreak, newActiveDate) = StreakUtils.calculateStreak(
                    currentProfile.lastActiveDate,
                    currentProfile.streak
                )
                val updatedStreak = calculatedStreak
                val updatedLongestStreak = maxOf(currentProfile.longestStreak, updatedStreak)

                // 3. Record stats for achievement tracking
                val questionsCount = _uiState.value.questions.size.ifZero(10)
                val isAiCustom = _uiState.value.categoryId == "ai_custom"
                achievementRepository.recordQuizCompletion(
                    scoreOutOfTen = scoreOutOfTen,
                    questionCount = questionsCount,
                    isAiCustom = isAiCustom
                )

                // 4. Check and unlock achievements
                val achResult = achievementRepository.checkAndUnlockAchievements(
                    totalXp = newTotalXp,
                    totalCoins = newCoins,
                    currentStreak = updatedStreak
                )

                val finalCoins = newCoins + achResult.extraCoinsEarned

                // Point 4: After calculating new XP, Coins and Streak
                Log.d("RUNTIME_TRACE", "[Point 4: After calculating new XP, Coins, Streak] uid=$userId, xp=$newTotalXp, coins=$finalCoins, streak=$updatedStreak, lastActiveDate=$newActiveDate, level=$newLevel, isGuest=$isGuest")

                // 5. Save quiz result to history
                val quizResult = quizResultRepository.saveQuizResult(
                    userId = userId,
                    categoryName = categoryName,
                    scoreOutOfTen = scoreOutOfTen,
                    xpEarned = xpEarned,
                    totalXp = newTotalXp,
                    coins = finalCoins,
                    coinsEarned = coinsGained,
                    streak = updatedStreak,
                    lastActiveDate = newActiveDate,
                    timestamp = timestamp
                )

                // 6. Calculate updated stats and history for profile
                val newQuizzesPlayed = maxOf(currentProfile.totalQuizzesPlayed + 1, currentProfile.quizHistory.size + 1)
                val newQuestionsAnswered = maxOf(currentProfile.totalQuestionsAnswered + questionsCount, newQuizzesPlayed * 10)
                val newCorrectAnswers = currentProfile.totalCorrectAnswers + scoreOutOfTen
                val newBestScore = maxOf(currentProfile.bestScore, scoreOutOfTen)
                val newHistory = (listOf(quizResult) + currentProfile.quizHistory)
                    .distinctBy { if (it.id.isNotBlank()) it.id else "${it.timestamp}_${it.categoryName}" }
                    .sortedByDescending { it.timestamp }
                val newUnlockedAchievements = (currentProfile.unlockedAchievements + achResult.newlyUnlocked.map { it.id }).distinct()
                val newClaimedRewards = (currentProfile.claimedRewards + achResult.newlyUnlocked.map { it.id }).distinct()

                _uiState.update {
                    it.copy(
                        totalXp = newTotalXp,
                        coinsEarned = coinsGained,
                        savedQuizResult = quizResult,
                        newlyUnlockedAchievements = achResult.newlyUnlocked
                    )
                }

                // 7. Save updated user profile
                val updatedProfile = currentProfile.copy(
                    uid = userId,
                    xp = newTotalXp,
                    coins = finalCoins,
                    level = newLevel,
                    streak = updatedStreak,
                    longestStreak = updatedLongestStreak,
                    lastActiveDate = newActiveDate,
                    lastQuizCategory = categoryName,
                    lastQuizScore = scoreOutOfTen,
                    lastQuizXpEarned = xpEarned,
                    lastQuizDate = dateFormatted,
 totalQuizzesPlayed = newQuizzesPlayed,
                    totalQuestionsAnswered = newQuestionsAnswered,
                    totalCorrectAnswers = newCorrectAnswers,
                    bestScore = newBestScore,
                    quizHistory = newHistory,
                    unlockedAchievements = newUnlockedAchievements,
                    claimedRewards = newClaimedRewards
                )

                // Point 5: Immediately before saveProfile / saveUserProfileToFirestore
                Log.d("RUNTIME_TRACE", "[Point 5: Immediately before saveProfile] uid=${updatedProfile.uid}, xp=${updatedProfile.xp}, coins=${updatedProfile.coins}, streak=${updatedProfile.streak}, lastActiveDate=${updatedProfile.lastActiveDate}, level=${updatedProfile.level}, targetKey=${if (updatedProfile.uid.startsWith("guest_")) "guest_user_profile_json" else "auth_user_profile_json"}, isGuest=$isGuest")

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
