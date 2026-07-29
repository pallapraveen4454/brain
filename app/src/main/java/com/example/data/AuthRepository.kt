package com.example.data

import android.content.Context
import android.util.Log
import com.example.BrainQuizApplication
import com.example.data.model.QuizResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class UserProfile(
    val uid: String = "",
    val name: String = "Player",
    val email: String = "",
    val avatarId: String = "brain",
    val xp: Int = 0,
    val level: Int = 1,
    val coins: Int = 0,
    val streak: Int = 0,
    val lastActiveDate: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val rank: String = "Beginner",
    val unlockedAchievements: Set<String> = emptySet(),
    val claimedRewards: Set<String> = emptySet(),
    val quizHistory: List<QuizResult> = emptyList(),
    val lastQuizCategory: String = "",
    val lastQuizScore: Int = 0,
    val lastQuizXpEarned: Int = 0,
    val lastQuizDate: String = "",
    val totalQuizzesPlayed: Int = 0,
    val totalQuestionsAnswered: Int = 0,
    val totalCorrectAnswers: Int = 0,
    val bestScore: Int = 0,
    val longestStreak: Int = 0
)

class AuthRepository(
    private val context: Context? = try { BrainQuizApplication.instance } catch (e: Exception) { null },
    private val quizResultRepository: QuizResultRepository = QuizResultRepository(),
    private val userProfileStore: UserProfileStore = UserProfileStore(context)
) {

    private fun getAuth(): FirebaseAuth? {
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to access FirebaseAuth instance", e)
            null
        }
    }

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to access FirebaseFirestore instance", e)
            null
        }
    }

    val currentUser: FirebaseUser?
        get() = getAuth()?.currentUser

    fun getOrCreateGuestId(): String {
        return userProfileStore.getGuestId()
    }

    fun isGuestSessionActive(): Boolean {
        return userProfileStore.isGuestActive()
    }

    fun setGuestSessionActive(active: Boolean) {
        userProfileStore.setGuestActive(active)
        userProfileStore.setLoggedIn(active)
    }

    fun hasSavedUserSession(): Boolean {
        return (currentUser != null) || userProfileStore.isLoggedIn()
    }

    fun isUserLoggedIn(): Boolean {
        return hasSavedUserSession()
    }

    fun saveCustomUsername(name: String) {
        val current = userProfileStore.getProfile()
        userProfileStore.saveProfile(current.copy(name = name))
    }

    fun getSavedCustomUsername(): String {
        return userProfileStore.getProfile().name
    }

    fun saveAvatarId(avatarId: String) {
        val current = userProfileStore.getProfile()
        userProfileStore.saveProfile(current.copy(avatarId = avatarId))
    }

    fun getSavedAvatarId(): String {
        return userProfileStore.getProfile().avatarId
    }

    suspend fun updateProfileName(uid: String, newName: String): Boolean {
        saveCustomUsername(newName)
        return try {
            val firestore = getFirestore() ?: return true
            if (uid.isNotBlank() && !uid.startsWith("guest_")) {
                firestore.collection("users").document(uid)
                    .update("name", newName)
                    .await()
            }
            true
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error updating profile name in Firestore", e)
            true
        }
    }

    suspend fun updateProfileAvatar(uid: String, newAvatarId: String): Boolean {
        saveAvatarId(newAvatarId)
        return try {
            val firestore = getFirestore() ?: return true
            if (uid.isNotBlank() && !uid.startsWith("guest_")) {
                firestore.collection("users").document(uid)
                    .update("avatarId", newAvatarId)
                    .await()
            }
            true
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error updating avatar in Firestore", e)
            true
        }
    }

    fun getPersistentGuestProfile(): UserProfile {
        return userProfileStore.getProfile()
    }

    fun resetGuestAccount(): UserProfile {
        return userProfileStore.resetGuestAccount()
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val auth = getAuth() ?: throw Exception("Authentication service is unavailable.")
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Authentication returned empty user")
            userProfileStore.setLoggedIn(true)
            ensureUserProfileExists(user)
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error signing in with email", e)
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, name: String): Result<FirebaseUser> {
        return try {
            val auth = getAuth() ?: throw Exception("Authentication service is unavailable.")
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User creation returned empty user")
            userProfileStore.setLoggedIn(true)
            val displayName = name.ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } }
            
            val local = quizResultRepository.getLocalProgress()
            // Create user profile in Firestore seeded with local progress
            val profile = UserProfile(
                uid = user.uid,
                name = displayName,
                email = email,
                xp = local.totalXp,
                level = maxOf(1, local.level),
                coins = local.coins,
                streak = local.streak,
                lastActiveDate = local.lastActiveDate,
                lastQuizCategory = local.lastCategoryName,
                lastQuizScore = local.lastScoreOutOfTen,
                lastQuizXpEarned = local.lastXpEarned,
                lastQuizDate = local.lastQuizDate
            )
            saveUserProfileToFirestore(profile)
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error signing up with email", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleCredential(idToken: String): Result<FirebaseUser> {
        return try {
            val auth = getAuth() ?: throw Exception("Authentication service is unavailable.")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("Google auth returned empty user")
            userProfileStore.setLoggedIn(true)
            ensureUserProfileExists(user)
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error signing in with Google credential", e)
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val auth = getAuth() ?: throw Exception("Authentication service is unavailable.")
            val result = auth.signInAnonymously().await()
            val user = result.user ?: throw Exception("Guest auth returned empty user")
            val local = quizResultRepository.getLocalProgress()
            val profile = UserProfile(
                uid = user.uid,
                name = "Guest Player",
                email = "guest@brainquiz.ai",
                xp = local.totalXp,
                level = maxOf(1, local.level),
                coins = local.coins,
                streak = local.streak,
                lastActiveDate = local.lastActiveDate,
                lastQuizCategory = local.lastCategoryName,
                lastQuizScore = local.lastScoreOutOfTen,
                lastQuizXpEarned = local.lastXpEarned,
                lastQuizDate = local.lastQuizDate
            )
            saveUserProfileToFirestore(profile)
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error signing in anonymously", e)
            Result.failure(e)
        }
    }

    suspend fun fetchUserProfile(uid: String): UserProfile? {
        return try {
            val firestore = getFirestore() ?: return null
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                doc.toObject(UserProfile::class.java)
            } else null
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetching user profile", e)
            null
        }
    }

    suspend fun saveUserProfileToFirestore(profile: UserProfile): Boolean {
        userProfileStore.saveProfile(profile)
        return try {
            val firestore = getFirestore() ?: return true
            if (profile.uid.isNotBlank()) {
                firestore.collection("users").document(profile.uid)
                    .set(profile)
                    .await()
            }
            true
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error saving user profile to Firestore", e)
            true
        }
    }

    private suspend fun ensureUserProfileExists(user: FirebaseUser) {
        try {
            val firestore = getFirestore() ?: return
            val doc = firestore.collection("users").document(user.uid).get().await()
            val local = quizResultRepository.getLocalProgress()

            if (!doc.exists()) {
                val displayName = user.displayName
                    ?: user.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                    ?: "Player"
                val profile = UserProfile(
                    uid = user.uid,
                    name = displayName,
                    email = user.email ?: "",
                    xp = local.totalXp,
                    level = maxOf(1, local.level),
                    coins = local.coins,
                    streak = local.streak,
                    lastActiveDate = local.lastActiveDate,
                    lastQuizCategory = local.lastCategoryName,
                    lastQuizScore = local.lastScoreOutOfTen,
                    lastQuizXpEarned = local.lastXpEarned,
                    lastQuizDate = local.lastQuizDate
                )
                saveUserProfileToFirestore(profile)
            } else {
                val existing = doc.toObject(UserProfile::class.java)
                if (existing != null) {
                    val mergedXp = maxOf(existing.xp, local.totalXp)
                    val mergedCoins = maxOf(existing.coins, local.coins)
                    val mergedStreak = maxOf(existing.streak, local.streak)
                    val mergedActiveDate = if (local.lastActiveDate.isNotBlank()) local.lastActiveDate else existing.lastActiveDate
                    val mergedCategory = existing.lastQuizCategory.ifBlank { local.lastCategoryName }
                    val mergedScore = if (existing.lastQuizScore > 0) existing.lastQuizScore else local.lastScoreOutOfTen
                    val mergedXpEarned = if (existing.lastQuizXpEarned > 0) existing.lastQuizXpEarned else local.lastXpEarned
                    val mergedDate = existing.lastQuizDate.ifBlank { local.lastQuizDate }

                    val updated = existing.copy(
                        xp = mergedXp,
                        level = maxOf(1, (mergedXp / 500) + 1),
                        coins = mergedCoins,
                        streak = mergedStreak,
                        lastActiveDate = mergedActiveDate,
                        lastQuizCategory = mergedCategory,
                        lastQuizScore = mergedScore,
                        lastQuizXpEarned = mergedXpEarned,
                        lastQuizDate = mergedDate
                    )
                    saveUserProfileToFirestore(updated)
                    // Also sync local SharedPreferences
                    quizResultRepository.saveLocalProgress(
                        totalXp = mergedXp,
                        level = updated.level,
                        coins = mergedCoins,
                        streak = mergedStreak,
                        lastActiveDate = mergedActiveDate,
                        lastCategoryName = mergedCategory,
                        lastScoreOutOfTen = mergedScore,
                        lastXpEarned = mergedXpEarned,
                        lastQuizDate = mergedDate
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error ensuring user profile exists", e)
        }
    }

    suspend fun signInAsGuestProfile(): Result<UserProfile> {
        return try {
            setGuestSessionActive(true)
            val profile = getPersistentGuestProfile()
            saveUserProfileToFirestore(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error signing in as guest profile", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            setGuestSessionActive(false)
            getAuth()?.signOut()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error signing out", e)
        }
    }
}
