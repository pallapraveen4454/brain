package com.example.viewmodel

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.QuizRepository
import com.example.data.QuizResultRepository
import com.example.data.AchievementCheckResult
import com.example.data.AchievementRepository
import com.example.data.LeaderboardRepository
import com.example.data.LeaderboardData
import com.example.data.LeaderboardPeriod
import com.example.data.UserProfile
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
import com.example.utils.LevelUtils
import com.example.utils.RankUtils
import com.example.utils.StreakUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val avatarId: String = "student_boy",
    val unlockedAvatars: Set<String> = setOf("student_boy", "student_girl"),
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
    val isDailyChallengeClaimedToday: Boolean = false,
    val dailyChallengeNextDate: String? = null,
    val categories: List<QuizCategory> = listOf(
        QuizCategory("gk", "General Knowledge", questionsCount = "10 Questions", iconName = "Psychology", accentColor = CategoryGK),
        QuizCategory("science", "Science", questionsCount = "10 Questions", iconName = "Science", accentColor = CategoryScience),
        QuizCategory("history", "History", questionsCount = "10 Questions", iconName = "Museum", accentColor = CategoryHistory),
        QuizCategory("sports", "Sports", questionsCount = "10 Questions", iconName = "SportsSoccer", accentColor = CategorySports),
        QuizCategory("movies", "Movies", questionsCount = "10 Questions", iconName = "Movie", accentColor = CategoryMovies),
        QuizCategory("tech", "Technology", questionsCount = "10 Questions", iconName = "Terminal", accentColor = CategoryTech),
        QuizCategory("geo", "Geography", questionsCount = "10 Questions", iconName = "Public", accentColor = CategoryGeo),
        QuizCategory("math", "Mathematics", questionsCount = "10 Questions", iconName = "Calculate", accentColor = CategoryMath)
    ),
    val quickPlayOptions: List<QuickPlayOption> = listOf(
        QuickPlayOption("quick", "Quick Play", "10 random questions", "POPULAR"),
        QuickPlayOption("ai_custom", "AI Quiz Generator", "Generate custom topic quizzes with Gemini", "GEMINI AI", isComingSoon = false)
    )
)

private data class ProfileData(
    val profile: UserProfile,
    val isGuest: Boolean,
    val updatedCoins: Int,
    val localStreak: Int,
    val localActiveDate: String,
    val computedRank: String,
    val achCheck: AchievementCheckResult,
    val achievementsList: List<Achievement>,
    val quizzesPlayed: Int,
    val questionsAnswered: Int,
    val correctAnswers: Int,
    val bestScore: Int,
    val longestStreak: Int,
    val accuracy: Int,
    val history: List<QuizResult>,
    val displayName: String,
    val displayEmail: String
)

class HomeViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val quizRepository: QuizRepository = QuizRepository(),
    private val quizResultRepository: QuizResultRepository = QuizResultRepository(),
    private val achievementRepository: AchievementRepository = AchievementRepository(),
    private val leaderboardRepository: LeaderboardRepository = LeaderboardRepository(),
    private val dailyChallengeRepository: com.example.data.DailyChallengeRepository = com.example.data.DailyChallengeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var leaderboardListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var loadProfileJob: Job? = null

    init {
        loadUserProfile()
        loadCategoryQuestionCounts()
        loadLeaderboard()
        refreshDailyChallengeStatus()
        viewModelScope.launch {
            com.example.data.UserProfileStore.profileFlow.collect {
                loadUserProfile()
                loadLeaderboard()
                refreshDailyChallengeStatus()
            }
        }
    }

    fun refreshDailyChallengeStatus() {
        val isAvailable = dailyChallengeRepository.isDailyRewardAvailable()
        val nextDate = dailyChallengeRepository.getNextAvailableDateString()
        _uiState.update {
            it.copy(
                isDailyChallengeClaimedToday = !isAvailable,
                dailyChallengeNextDate = if (!isAvailable) nextDate else null
            )
        }
    }

    fun loadLeaderboard(period: LeaderboardPeriod = _uiState.value.leaderboardPeriod) {
        // 1. Instant local render from cache/store
        try {
            val initialData = leaderboardRepository.getLeaderboard(period)
            _uiState.update { it.copy(leaderboardData = initialData, leaderboardPeriod = period) }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error reading local leaderboard", e)
        }

        // 2. Real-time Firestore snapshot listener for instant synchronization with friends
        try {
            leaderboardListenerRegistration?.remove()
            leaderboardListenerRegistration = leaderboardRepository.observeRemoteLeaderboard(period) { freshData ->
                _uiState.update { it.copy(leaderboardData = freshData) }
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error attaching leaderboard listener", e)
        }

        // 3. Refresh from Firestore and sync current user in background
        viewModelScope.launch {
            try {
                val profile = authRepository.getPersistentGuestProfile()
                leaderboardRepository.syncCurrentUserToLeaderboard(profile)
                leaderboardRepository.fetchRemoteLeaderboard(period)
                val freshData = leaderboardRepository.getLeaderboard(period)
                _uiState.update { it.copy(leaderboardData = freshData) }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching remote leaderboard data", e)
            }
        }
    }

    private fun loadCategoryQuestionCounts() {
        // Categories have standard counts
    }

    fun loadUserProfile() {
        if (loadProfileJob?.isActive == true) return
        loadProfileJob = viewModelScope.launch {
            try {
                // 1. Offload disk I/O, JSON parsing, stats calculation, and achievements to Dispatchers.IO
                val profileComputation = withContext(Dispatchers.IO) {
                    val profile = authRepository.getPersistentGuestProfile()

                    // Point 8: Inside HomeViewModel.loadUserProfile
                    val isGuest = authRepository.isGuestSessionActive()
                    val targetKey = if (isGuest) "guest_user_profile_json" else "auth_user_profile_json"
                    Log.d("RUNTIME_TRACE", "[Point 8: Inside HomeViewModel.loadUserProfile] profile loaded: uid=${profile.uid}, xp=${profile.xp}, coins=${profile.coins}, streak=${profile.streak}, lastActiveDate=${profile.lastActiveDate}, level=${profile.level}, isGuestActive=$isGuest, targetKey=$targetKey")

                    val stats = quizResultRepository.getUserStats()

                    val (calculatedStreak, localActiveDate) = if (profile.lastActiveDate.isNotBlank()) {
                        StreakUtils.calculateStreak(
                            profile.lastActiveDate,
                            profile.streak
                        )
                    } else {
                        Pair(profile.streak, profile.lastActiveDate)
                    }
                    val localStreak = calculatedStreak
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

                    val currentUser = if (!isGuest) authRepository.currentUser else null
                    val authEmail = currentUser?.email ?: ""
                    val authName = currentUser?.displayName ?: authEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

                    val displayName = if (isGuest) {
                        if (profile.name.isBlank() || profile.name == "Player" || profile.name == "Guest Player") "Guest" else profile.name
                    } else {
                        when {
                            profile.name.isNotBlank() && profile.name != "Player" && profile.name != "Guest Player" && profile.name != "Guest" -> profile.name
                            authName.isNotBlank() && authName != "Player" -> authName
                            else -> "Player"
                        }
                    }

                    val displayEmail = if (isGuest) {
                        "Guest Account"
                    } else {
                        when {
                            profile.email.isNotBlank() && profile.email != "Guest Account" && profile.email != "guest@brainquiz.ai" -> profile.email
                            authEmail.isNotBlank() -> authEmail
                            else -> ""
                        }
                    }

                    ProfileData(
                        profile = profile,
                        isGuest = isGuest,
                        updatedCoins = updatedCoins,
                        localStreak = localStreak,
                        localActiveDate = localActiveDate,
                        computedRank = computedRank,
                        achCheck = achCheck,
                        achievementsList = achievementsList,
                        quizzesPlayed = quizzesPlayed,
                        questionsAnswered = questionsAnswered,
                        correctAnswers = correctAnswers,
                        bestScore = bestScore,
                        longestStreak = longestStreak,
                        accuracy = accuracy,
                        history = history,
                        displayName = displayName,
                        displayEmail = displayEmail
                    )
                }

                // 2. Safe StateFlow update on Main dispatcher
                _uiState.update {
                    it.copy(
                        xp = profileComputation.profile.xp,
                        level = profileComputation.profile.level,
                        coins = profileComputation.updatedCoins,
                        streakDays = profileComputation.localStreak,
                        rank = profileComputation.computedRank,
                        playerName = profileComputation.displayName,
                        playerEmail = profileComputation.displayEmail,
                        avatarId = profileComputation.profile.avatarId.let { av -> if (av.isBlank() || av == "brain") "student_boy" else av },
                        unlockedAvatars = if (profileComputation.profile.unlockedAvatars.isNotEmpty()) (profileComputation.profile.unlockedAvatars.toSet() + setOf("student_boy", "student_girl")) - "brain" else setOf("student_boy", "student_girl"),
                        totalQuizzesPlayed = profileComputation.quizzesPlayed,
                        totalQuestionsAnswered = profileComputation.questionsAnswered,
                        totalCorrectAnswers = profileComputation.correctAnswers,
                        accuracyPercentage = profileComputation.accuracy,
                        bestScore = profileComputation.bestScore,
                        longestStreak = profileComputation.longestStreak,
                        quizHistory = profileComputation.history,
                        lastQuizCategory = profileComputation.profile.lastQuizCategory,
                        lastQuizScore = profileComputation.profile.lastQuizScore,
                        lastQuizXpEarned = profileComputation.profile.lastQuizXpEarned,
                        lastQuizDate = profileComputation.profile.lastQuizDate,
                        hasQuizHistory = profileComputation.history.isNotEmpty() || profileComputation.profile.lastQuizCategory.isNotBlank(),
                        achievements = profileComputation.achievementsList,
                        unlockedAchievementsCount = profileComputation.achievementsList.count { a -> a.isUnlocked },
                        totalAchievementsCount = profileComputation.achievementsList.size,
                        newlyUnlockedAchievements = profileComputation.achCheck.newlyUnlocked
                    )
                }
                refreshDailyChallengeStatus()

                // 3. Save updated streak and sync on IO
                val updatedProfile = profileComputation.profile.copy(
                    coins = profileComputation.updatedCoins,
                    streak = profileComputation.localStreak,
                    lastActiveDate = profileComputation.localActiveDate,
                    totalQuizzesPlayed = profileComputation.quizzesPlayed,
                    totalQuestionsAnswered = profileComputation.questionsAnswered,
                    totalCorrectAnswers = profileComputation.correctAnswers,
                    bestScore = profileComputation.bestScore,
                    longestStreak = profileComputation.longestStreak,
                    quizHistory = profileComputation.history
                )
                authRepository.saveUserProfileToFirestore(updatedProfile)

                // Load recent quiz results from persistent history if authenticated
                val currentUserId = if (profileComputation.isGuest) profileComputation.profile.uid else (authRepository.currentUser?.uid ?: profileComputation.profile.uid)
                val recentResults = withContext(Dispatchers.IO) {
                    quizResultRepository.getRecentQuizResults(currentUserId)
                }
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

                // Sync remote profile ONLY if authenticated and NOT in guest mode
                if (!profileComputation.isGuest) {
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
                                        avatarId = remoteProfile.avatarId.let { av -> if (av.isBlank() || av == "brain") "student_boy" else av },
                                        xp = maxOf(profileComputation.profile.xp, remoteProfile.xp),
                                        level = LevelUtils.getLevel(maxOf(profileComputation.profile.xp, remoteProfile.xp)),
                                        coins = maxOf(profileComputation.updatedCoins, remoteProfile.coins),
                                        streakDays = maxOf(profileComputation.localStreak, remoteProfile.streak),
                                        rank = userRank,
                                        unlockedAvatars = if (remoteProfile.unlockedAvatars.isNotEmpty()) (remoteProfile.unlockedAvatars.toSet() + setOf("student_boy", "student_girl")) - "brain" else setOf("student_boy", "student_girl"),
                                        totalQuizzesPlayed = maxOf(profileComputation.quizzesPlayed, remoteProfile.totalQuizzesPlayed),
                                        totalQuestionsAnswered = maxOf(profileComputation.questionsAnswered, remoteProfile.totalQuestionsAnswered),
                                        totalCorrectAnswers = maxOf(profileComputation.correctAnswers, remoteProfile.totalCorrectAnswers),
                                        bestScore = maxOf(profileComputation.bestScore, remoteProfile.bestScore),
                                        longestStreak = maxOf(profileComputation.longestStreak, remoteProfile.longestStreak),
                                        quizHistory = if (remoteProfile.quizHistory.isNotEmpty()) remoteProfile.quizHistory else profileComputation.history
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("HomeViewModel", "Error fetching remote profile", e)
                        }
                    }
                }
                refreshDailyChallengeStatus()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading user profile", e)
            }
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

    fun resetAccountProgress(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val result = authRepository.resetAccountProgress()
                if (result.isSuccess) {
                    val cleanProfile = result.getOrNull() ?: authRepository.getPersistentGuestProfile()
                    val currentUser = if (!authRepository.isGuestSessionActive()) authRepository.currentUser else null
                    val authEmail = currentUser?.email ?: ""
                    val authName = currentUser?.displayName ?: authEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

                    val displayName = if (authRepository.isGuestSessionActive()) {
                        if (cleanProfile.name.isBlank() || cleanProfile.name == "Player" || cleanProfile.name == "Guest Player") "Guest" else cleanProfile.name
                    } else {
                        when {
                            cleanProfile.name.isNotBlank() && cleanProfile.name != "Player" && cleanProfile.name != "Guest Player" && cleanProfile.name != "Guest" -> cleanProfile.name
                            authName.isNotBlank() && authName != "Player" -> authName
                            else -> "Player"
                        }
                    }

                    val displayEmail = if (authRepository.isGuestSessionActive()) {
                        "Guest Account"
                    } else {
                        when {
                            cleanProfile.email.isNotBlank() && cleanProfile.email != "Guest Account" && cleanProfile.email != "guest@brainquiz.ai" -> cleanProfile.email
                            authEmail.isNotBlank() -> authEmail
                            else -> ""
                        }
                    }

                    val achievementsList = achievementRepository.getAllAchievements(0, 0, 0)

                    _uiState.update {
                        it.copy(
                            xp = 0,
                            level = 1,
                            coins = 0,
                            streakDays = 0,
                            rank = "Beginner",
                            playerName = displayName,
                            playerEmail = displayEmail,
                            avatarId = "student_boy",
                            unlockedAvatars = setOf("student_boy", "student_girl"),
                            totalQuizzesPlayed = 0,
                            totalQuestionsAnswered = 0,
                            totalCorrectAnswers = 0,
                            accuracyPercentage = 0,
                            bestScore = 0,
                            longestStreak = 0,
                            quizHistory = emptyList(),
                            lastQuizCategory = "",
                            lastQuizScore = 0,
                            lastQuizXpEarned = 0,
                            lastQuizDate = "",
                            hasQuizHistory = false,
                            achievements = achievementsList,
                            unlockedAchievementsCount = 0,
                            totalAchievementsCount = achievementsList.size,
                            newlyUnlockedAchievements = emptyList()
                        )
                    }

                    dailyChallengeRepository.resetDailyReward()
                    refreshDailyChallengeStatus()

                    // Reload leaderboard to reflect the reset stats
                    loadLeaderboard(_uiState.value.leaderboardPeriod)
                    onComplete?.invoke(true)
                } else {
                    onComplete?.invoke(false)
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error in resetAccountProgress", e)
                onComplete?.invoke(false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            leaderboardListenerRegistration?.remove()
            leaderboardListenerRegistration = null
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error detaching leaderboard listener", e)
        }
    }
}

