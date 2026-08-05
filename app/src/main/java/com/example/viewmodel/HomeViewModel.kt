package com.example.viewmodel

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.QuizRepository
import com.example.data.QuizResultRepository
import com.example.data.AchievementRepository
import com.example.data.LeaderboardRepository
import com.example.data.LeaderboardData
import com.example.data.LeaderboardPeriod
import com.example.data.model.Achievement
import com.example.data.model.QuizResult
import com.example.ui.theme.CategoryGK
import com.example.ui.theme.CategoryGeo
import com.example.ui.theme.CategoryHistory
import com.example.ui.theme.CategoryMath
import com.example.ui.theme.CategoryMovies
import com.example.ui.theme.CategoryScience
import com.example.ui.theme.CategorySports
import com.example.ui.theme.CategoryTech
import com.example.utils.RankUtils
import com.example.utils.StreakUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuizCategory(
    val id: String,
    val title: String,
    val questionsCount: String? = null,
    val iconName: String,
    val accentColor: Color
)

data class QuickPlayOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val badgeText: String,
    val isComingSoon: Boolean = false
)

enum class BottomNavTab {
    Home, Leaderboard, Achievements, Profile
}

data class HomeUiState(
    val xp: Int = 0,
    val level: Int = 1,
    val coins: Int = 0,
    val streakDays: Int = 0,
    val rank: String = "Beginner",
    val playerName: String = "Guest",
    val playerEmail: String = "Guest Account",
    val avatarId: String = "brain",
    val unlockedAvatars: Set<String> = setOf("student_boy", "student_girl", "brain"),
    val totalQuizzesPlayed: Int = 0,
    val totalQuestionsAnswered: Int = 0,
    val totalCorrectAnswers: Int = 0,
    val accuracyPercentage: Int = 0,
    val bestScore: Int = 0,
    val longestStreak: Int = 0,
    val quizHistory: List<QuizResult> = emptyList(),
    val showEditUsernameDialog: Boolean = false,
    val showFirstTimeNameSetup: Boolean = false,
    val lastQuizCategory: String = "",
    val lastQuizScore: Int = 0,
    val lastQuizXpEarned: Int = 0,
    val lastQuizDate: String = "",
    val hasQuizHistory: Boolean = false,
    val selectedTab: BottomNavTab = BottomNavTab.Home,
    val showNotificationsDialog: Boolean = false,
    val unreadNotificationsCount: Int = 2,
    val leaderboardData: LeaderboardData? = null,
    val leaderboardPeriod: LeaderboardPeriod = LeaderboardPeriod.GLOBAL,
    val achievements: List<Achievement> = emptyList(),
    val unlockedAchievementsCount: Int = 0,
    val totalAchievementsCount: Int = 0,
    val newlyUnlockedAchievements: List<Achievement> = emptyList(),
    val categories: List<QuizCategory> = listOf(
        QuizCategory("gk", "General Knowledge", questionsCount = "Daily Quiz\n10 Questions", iconName = "Psychology", accentColor = CategoryGK),
        QuizCategory("science", "Science", questionsCount = "Daily Quiz\n10 Questions", iconName = "Science", accentColor = CategoryScience),
        QuizCategory("history", "History", questionsCount = "Daily Quiz\n10 Questions", iconName = "Museum", accentColor = CategoryHistory),
        QuizCategory("sports", "Sports", questionsCount = "Daily Quiz\n10 Questions", iconName = "SportsSoccer", accentColor = CategorySports),
        QuizCategory("movies", "Movies", questionsCount = "Daily Quiz\n10 Questions", iconName = "Movie", accentColor = CategoryMovies),
        QuizCategory("tech", "Technology", questionsCount = "Daily Quiz\n10 Questions", iconName = "Terminal", accentColor = CategoryTech),
        QuizCategory("geo", "Geography", questionsCount = "Daily Quiz\n10 Questions", iconName = "Public", accentColor = CategoryGeo),
        QuizCategory("math", "Mathematics", questionsCount = "Daily Quiz\n10 Questions", iconName = "Calculate", accentColor = CategoryMath)
    ),
    val quickPlayOptions: List<QuickPlayOption> = listOf(
        QuickPlayOption("quick", "Quick Play", "10 random questions", "POPULAR"),
        QuickPlayOption("daily", "Daily Challenge", "Earn 2x Coins & XP today", "2X REWARDS"),
        QuickPlayOption("ai_custom", "AI Quiz Generator", "Generate custom topic quizzes with Gemini", "GEMINI AI", isComingSoon = false)
    )
)

class HomeViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val quizRepository: QuizRepository = QuizRepository(),
    private val quizResultRepository: QuizResultRepository = QuizResultRepository(),
    private val achievementRepository: AchievementRepository = AchievementRepository(),
    private val leaderboardRepository: LeaderboardRepository = LeaderboardRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
        loadCategoryQuestionCounts()
        loadLeaderboard()
    }

    fun loadLeaderboard(period: LeaderboardPeriod = _uiState.value.leaderboardPeriod) {
        viewModelScope.launch {
            try {
                val data = leaderboardRepository.getLeaderboard(period)
                _uiState.update { it.copy(leaderboardData = data, leaderboardPeriod = period) }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading leaderboard data", e)
            }
        }
    }

    private fun loadCategoryQuestionCounts() {
        _uiState.update { state ->
            val updatedCategories = state.categories.map { category ->
                category.copy(questionsCount = "Daily Quiz\n10 Questions")
            }
            state.copy(categories = updatedCategories)
        }
    }

    fun loadUserProfile() {
        try {
            // 1. Load persistent user profile
            val profile = authRepository.getPersistentGuestProfile()
            val stats = quizResultRepository.getUserStats()

            val (calculatedStreak, localActiveDate) = StreakUtils.calculateStreak(
                profile.lastActiveDate,
                profile.streak
            )
            val localStreak = maxOf(profile.streak, calculatedStreak)
            val computedRank = RankUtils.getRankForXp(profile.xp)

            // Check and unlock achievements
            val achCheck = achievementRepository.checkAndUnlockAchievements(
                totalXp = profile.xp,
                totalCoins = profile.coins,
                currentStreak = localStreak
            )
            val updatedCoins = profile.coins + achCheck.extraCoinsEarned
            val achievementsList = achievementRepository.getAllAchievements(profile.xp, updatedCoins, localStreak)

            val quizzesPlayed = maxOf(profile.totalQuizzesPlayed, stats.totalQuizzesPlayed)
            val questionsAnswered = maxOf(profile.totalQuestionsAnswered, stats.totalQuestionsAnswered)
            val correctAnswers = maxOf(profile.totalCorrectAnswers, stats.totalCorrectAnswers)
            val bestScore = maxOf(profile.bestScore, stats.bestScore)
            val longestStreak = maxOf(profile.longestStreak, stats.longestStreak, localStreak)
            val accuracy = if (questionsAnswered > 0) ((correctAnswers.toDouble() / questionsAnswered.toDouble()) * 100).toInt() else 0
            val history = if (profile.quizHistory.isNotEmpty()) profile.quizHistory else quizResultRepository.getLocalQuizResultsList()

            _uiState.update {
                it.copy(
                    xp = profile.xp,
                    level = profile.level,
                    coins = updatedCoins,
                    streakDays = localStreak,
                    rank = computedRank,
                    playerName = if (profile.name.isBlank() || profile.name == "Player" || profile.name == "Guest Player") "Guest" else profile.name,
                    playerEmail = profile.email.ifBlank { "Guest Account" },
                    avatarId = profile.avatarId.ifBlank { "brain" },
                    unlockedAvatars = if (profile.unlockedAvatars.isNotEmpty()) profile.unlockedAvatars.toSet() + setOf("student_boy", "student_girl", "brain") else setOf("student_boy", "student_girl", "brain"),
                    totalQuizzesPlayed = quizzesPlayed,
                    totalQuestionsAnswered = questionsAnswered,
                    totalCorrectAnswers = correctAnswers,
                    accuracyPercentage = accuracy,
                    bestScore = bestScore,
                    longestStreak = longestStreak,
                    quizHistory = history,
                    lastQuizCategory = profile.lastQuizCategory,
                    lastQuizScore = profile.lastQuizScore,
                    lastQuizXpEarned = profile.lastQuizXpEarned,
                    lastQuizDate = profile.lastQuizDate,
                    hasQuizHistory = history.isNotEmpty() || profile.lastQuizCategory.isNotBlank(),
                    achievements = achievementsList,
                    unlockedAchievementsCount = achievementsList.count { a -> a.isUnlocked },
                    totalAchievementsCount = achievementsList.size,
                    newlyUnlockedAchievements = achCheck.newlyUnlocked
                )
            }

            // Save updated streak and active date
            viewModelScope.launch {
                val updatedProfile = profile.copy(
                    coins = updatedCoins,
                    streak = localStreak,
                    lastActiveDate = localActiveDate,
                    totalQuizzesPlayed = quizzesPlayed,
                    totalQuestionsAnswered = questionsAnswered,
                    totalCorrectAnswers = correctAnswers,
                    bestScore = bestScore,
                    longestStreak = longestStreak,
                    quizHistory = history
                )
                authRepository.saveUserProfileToFirestore(updatedProfile)

                // Load recent quiz results from persistent history if authenticated
                val currentUserId = authRepository.currentUser?.uid ?: profile.uid
                val recentResults = quizResultRepository.getRecentQuizResults(currentUserId)
                if (recentResults.isNotEmpty()) {
                    val latest = recentResults.first()
                    _uiState.update {
                        it.copy(
                            quizHistory = recentResults,
                            lastQuizCategory = latest.categoryName,
                            lastQuizScore = latest.scoreOutOfTen,
                            lastQuizXpEarned = latest.xpEarned,
                            lastQuizDate = latest.dateFormatted,
                            hasQuizHistory = true
                        )
                    }
                }

                // Sync remote profile if authenticated
                val user = authRepository.currentUser
                if (user != null) {
                    try {
                        val remoteProfile = authRepository.fetchUserProfile(user.uid)
                        if (remoteProfile != null) {
                            val userRank = RankUtils.getRankForXp(remoteProfile.xp)
                            val userName = if (remoteProfile.name.isNotBlank() && remoteProfile.name != "Player" && remoteProfile.name != "Guest Player") remoteProfile.name else (user.displayName ?: user.email?.substringBefore("@") ?: "Player")
                            val userEmail = user.email ?: remoteProfile.email

                            _uiState.update {
                                it.copy(
                                    playerName = userName,
                                    playerEmail = userEmail,
                                    avatarId = remoteProfile.avatarId.ifBlank { "brain" },
                                    xp = remoteProfile.xp,
                                    level = maxOf(1, (remoteProfile.xp / 500) + 1),
                                    coins = remoteProfile.coins,
                                    streakDays = remoteProfile.streak,
                                    rank = userRank,
                                    unlockedAvatars = if (remoteProfile.unlockedAvatars.isNotEmpty()) remoteProfile.unlockedAvatars.toSet() + setOf("student_boy", "student_girl", "brain") else setOf("student_boy", "student_girl", "brain"),
                                    totalQuizzesPlayed = remoteProfile.totalQuizzesPlayed,
                                    totalQuestionsAnswered = remoteProfile.totalQuestionsAnswered,
                                    totalCorrectAnswers = remoteProfile.totalCorrectAnswers,
                                    bestScore = remoteProfile.bestScore,
                                    longestStreak = remoteProfile.longestStreak,
                                    quizHistory = remoteProfile.quizHistory
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Error fetching remote profile", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error loading user profile", e)
        }
    }

    fun updateUsername(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotBlank()) {
            authRepository.saveCustomUsername(trimmed)
            _uiState.update { it.copy(playerName = trimmed, showEditUsernameDialog = false, showFirstTimeNameSetup = false) }
            viewModelScope.launch {
                val user = authRepository.currentUser
                val uid = user?.uid ?: authRepository.getPersistentGuestProfile().uid
                authRepository.updateProfileName(uid, trimmed)
                loadLeaderboard()
            }
        }
    }

    fun updateAvatar(newAvatarId: String) {
        authRepository.saveAvatarId(newAvatarId)
        _uiState.update { it.copy(avatarId = newAvatarId) }
        viewModelScope.launch {
            val user = authRepository.currentUser
            val uid = user?.uid ?: authRepository.getPersistentGuestProfile().uid
            authRepository.updateProfileAvatar(uid, newAvatarId)
            loadLeaderboard()
        }
    }

    fun buyAvatar(avatarId: String, price: Int): Boolean {
        val current = _uiState.value
        if (current.coins < price) return false
        if (current.unlockedAvatars.contains(avatarId)) return false

        val newCoins = current.coins - price
        val newUnlockedAvatars = current.unlockedAvatars + avatarId
        val profile = authRepository.getPersistentGuestProfile()
        val updatedProfile = profile.copy(
            coins = newCoins,
            unlockedAvatars = newUnlockedAvatars.toList()
        )

        _uiState.update {
            it.copy(
                coins = newCoins,
                unlockedAvatars = newUnlockedAvatars
            )
        }

        viewModelScope.launch {
            val user = authRepository.currentUser
            val uid = user?.uid ?: profile.uid
            authRepository.saveUserProfileToFirestore(updatedProfile)
            loadLeaderboard()
        }
        return true
    }

    fun equipAvatar(newAvatarId: String) {
        updateAvatar(newAvatarId)
    }

    fun setShowEditUsernameDialog(show: Boolean) {
        _uiState.update { it.copy(showEditUsernameDialog = show) }
    }

    fun setShowFirstTimeNameSetup(show: Boolean) {
        _uiState.update { it.copy(showFirstTimeNameSetup = show) }
    }

    fun selectNavTab(tab: BottomNavTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (tab == BottomNavTab.Leaderboard) {
            loadLeaderboard()
        }
    }

    fun toggleNotificationsDialog(show: Boolean) {
        _uiState.update {
            it.copy(
                showNotificationsDialog = show,
                unreadNotificationsCount = if (show) 0 else it.unreadNotificationsCount
            )
        }
    }

    fun dismissAchievementDialog() {
        _uiState.update { it.copy(newlyUnlockedAchievements = emptyList()) }
    }

    fun signOut() {
        try {
            authRepository.signOut()
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error signing out", e)
        }
    }

    fun resetGuestAccount() {
        authRepository.resetGuestAccount()
        loadUserProfile()
    }
}

