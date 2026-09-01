package com.example.data

import android.content.Context
import android.util.Log
import com.example.BrainQuizApplication
import com.example.data.model.QuizResult
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.actionCodeSettings
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
    val unlockedAchievements: List<String> = emptyList(),
    val claimedRewards: List<String> = emptyList(),
    val unlockedAvatars: List<String> = listOf("student_boy", "student_girl", "brain"),
    val quizHistory: List<QuizResult> = emptyList(),
    val lastQuizCategory: String = "",
    val lastQuizScore: Int = 0,
    val lastQuizXpEarned: Int = 0,
    val lastQuizDate: String = "",
    val totalQuizzesPlayed: Int = 0,
    val totalQuestionsAnswered: Int = 0,
    val totalCorrectAnswers: Int = 0,
    val bestScore: Int = 0,
    val longestStreak: Int = 0,
    val installDate: String = ""
) {
    fun isGuest(): Boolean = uid.isBlank() || uid.startsWith("guest_")
}

class AuthRepository(
    private val context: Context? = try { BrainQuizApplication.instance } catch (e: Exception) { null },
    private val quizResultRepository: QuizResultRepository = QuizResultRepository(),
    private val userProfileStore: UserProfileStore = UserProfileStore(context)
) {

    private fun getAuth(): FirebaseAuth? {
        val appCtx = context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null }
        if (appCtx != null) {
            BrainQuizApplication.ensureFirebaseInitialized(appCtx)
        }
        return try {
            val auth = FirebaseAuth.getInstance()
            val runtimeApiKey = auth.app.options.apiKey ?: ""
            val googleServicesKey = "AIzaSyApStHvA17YLLkNv-H75VIOJjCvPMr1azM"
            val isFromGoogleServicesJson = (runtimeApiKey == googleServicesKey)
            val keyMasked = if (runtimeApiKey.length > 8) "${runtimeApiKey.take(6)}...${runtimeApiKey.takeLast(4)}" else runtimeApiKey
            Log.d("FirebaseAuthCheck", "getAuth(): Active FirebaseAuth instance obtained -> API Key: $keyMasked (Source: ${if (isFromGoogleServicesJson) "google-services.json" else "other source"})")
            auth
        } catch (e: Exception) {
            Log.e("AuthRepository", "getAuth(): Failed to obtain FirebaseAuth instance: [${e.javaClass.name}] ${e.message}", e)
            null
        }
    }

    private fun getFirestore(): FirebaseFirestore? {
        val appCtx = context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null }
        if (appCtx != null) {
            BrainQuizApplication.ensureFirebaseInitialized(appCtx)
        }
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to access FirebaseFirestore instance: [${e.javaClass.name}] ${e.message}", e)
            null
        }
    }

    val currentUser: FirebaseUser?
        get() = try {
            if (isGuestSessionActive()) null else getAuth()?.currentUser
        } catch (e: Exception) { null }

    fun getOrCreateGuestId(): String {
        return userProfileStore.getGuestId()
    }

    fun isGuestSessionActive(): Boolean {
        return userProfileStore.isGuestActive()
    }

    fun setGuestSessionActive(active: Boolean) {
        userProfileStore.setGuestActive(active)
        if (active) {
            userProfileStore.setLoggedIn(true)
        }
    }

    fun hasSavedUserSession(): Boolean {
        val isGuest = isGuestSessionActive()
        val user = currentUser
        return when {
            isGuest -> true
            user != null -> userProfileStore.isLoggedIn()
            else -> false
        }
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
                withContext(NonCancellable) {
                    withTimeoutOrNull(3000L) {
                        firestore.collection("users").document(uid)
                            .update("name", newName)
                            .await()
                    }
                }
            }
            true
        } catch (e: CancellationException) {
            Log.d("AuthRepository", "updateProfileName cancelled")
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
                withContext(NonCancellable) {
                    withTimeoutOrNull(3000L) {
                        firestore.collection("users").document(uid)
                            .update("avatarId", newAvatarId)
                            .await()
                    }
                }
            }
            true
        } catch (e: CancellationException) {
            Log.d("AuthRepository", "updateProfileAvatar cancelled")
            true
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error updating avatar in Firestore", e)
            true
        }
    }

    fun createOrGetGuestProfile(): UserProfile {
        return userProfileStore.createOrGetGuestProfile()
    }

    fun getPersistentGuestProfile(): UserProfile {
        return userProfileStore.getProfile()
    }

    fun createOrGetLocalEmailProfile(email: String, name: String): UserProfile {
        val localUid = "local_${Math.abs(email.hashCode())}"
        val existing = userProfileStore.getProfile()
        val isSame = existing.uid == localUid || existing.email == email
        val displayName = name.ifBlank {
            if (isSame && existing.name.isNotBlank() && existing.name != "Player" && existing.name != "Guest Player") {
                existing.name
            } else {
                email.substringBefore("@").replaceFirstChar { it.uppercase() }
            }
        }
        val profile = if (isSame) {
            existing.copy(uid = localUid, email = email, name = displayName)
        } else {
            UserProfile(
                uid = localUid,
                email = email,
                name = displayName,
                avatarId = "brain",
                xp = 0,
                level = 1,
                coins = 0,
                streak = 0,
                rank = "Beginner",
                unlockedAchievements = emptyList(),
                claimedRewards = emptyList(),
                unlockedAvatars = listOf("student_boy", "student_girl", "brain"),
                quizHistory = emptyList(),
                totalQuizzesPlayed = 0,
                totalQuestionsAnswered = 0,
                totalCorrectAnswers = 0,
                bestScore = 0,
                longestStreak = 0
            )
        }
        userProfileStore.saveProfile(profile)
        userProfileStore.setLoggedIn(true)
        setGuestSessionActive(false)
        return profile
    }

    fun resetGuestAccount(): UserProfile {
        return userProfileStore.resetGuestAccount()
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        Log.d("AuthRepository", "signInWithEmail starting for email='$email'")
        return try {
            val auth = getAuth() ?: throw Exception("Firebase Authentication service is unavailable.")
            Log.d("AuthRepository", "Calling FirebaseAuth.signInWithEmailAndPassword(email='$email')...")
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Authentication returned empty user object")
            Log.d("AuthRepository", "FirebaseAuth.signInWithEmailAndPassword SUCCESS -> user.uid=${user.uid}, email=${user.email}")
            setGuestSessionActive(false)
            userProfileStore.setLoggedIn(true)
            ensureUserProfileExists(user)
            Result.success(user)
        } catch (e: Exception) {
            Log.w("AuthRepository", "FirebaseAuth.signInWithEmailAndPassword FAILED -> [${e.javaClass.name}] ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        name: String,
        avatarId: String = ""
    ): Result<UserProfile> {
        val tStart = System.currentTimeMillis()
        Log.d("AUTH_PERF", "[TIMING] Sign-up process initiated at $tStart ms for email='$email', name='$name'")
        return try {
            val auth = getAuth() ?: throw Exception("Firebase Authentication service is unavailable.")
            Log.d("AUTH_PERF", "[TIMING] Step 1 - Initiating FirebaseAuth.createUserWithEmailAndPassword...")
            val tAuthStart = System.currentTimeMillis()
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User creation returned empty user object")
            val tAuthEnd = System.currentTimeMillis()
            val authMs = tAuthEnd - tAuthStart
            Log.d("AUTH_PERF", "[TIMING] Step 1 - FirebaseAuth.createUserWithEmailAndPassword SUCCESS in $authMs ms (uid=${user.uid})")
            
            val displayName = name.ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } }
            
            val tUpdateStart = System.currentTimeMillis()
            try {
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    this.displayName = displayName
                }
                user.updateProfile(profileUpdates).await()
                val tUpdateEnd = System.currentTimeMillis()
                Log.d("AUTH_PERF", "[TIMING] Step 2 - Firebase Auth user profile displayName updated in ${tUpdateEnd - tUpdateStart} ms")
            } catch (e: Exception) {
                Log.w("AUTH_PERF", "Failed to update Firebase user profile name: [${e.javaClass.name}] ${e.message}")
            }

            val tProfileStart = System.currentTimeMillis()
            val chosenAvatar = if (avatarId.isNotBlank()) avatarId else "brain"
            val profile = UserProfile(
                uid = user.uid,
                name = displayName,
                email = email,
                avatarId = chosenAvatar,
                xp = 0,
                level = 1,
                coins = 0,
                streak = 0,
                rank = "Beginner",
                unlockedAchievements = emptyList(),
                claimedRewards = emptyList(),
                unlockedAvatars = listOf("student_boy", "student_girl", "brain"),
                quizHistory = emptyList(),
                totalQuizzesPlayed = 0,
                totalQuestionsAnswered = 0,
                totalCorrectAnswers = 0,
                bestScore = 0,
                longestStreak = 0
            )
            
            // Clear guest session and do NOT log in yet
            setGuestSessionActive(false)
            userProfileStore.setLoggedIn(false)

            // Save brand-new clean profile locally and to Firestore
            userProfileStore.saveProfile(profile)
            saveUserProfileToFirestore(profile)

            val tProfileEnd = System.currentTimeMillis()
            val profileMs = tProfileEnd - tProfileStart
            Log.d("AUTH_PERF", "[TIMING] Step 3 - Profile creation & Firestore sync completed in $profileMs ms")

            // Sign out Firebase user so user is not automatically logged in
            try {
                auth.signOut()
            } catch (e: Exception) {
                Log.w("AuthRepository", "Error signing out after sign up", e)
            }

            val totalMs = System.currentTimeMillis() - tStart
            Log.d("AUTH_PERF", "[TIMING] SUMMARY - Total Sign-Up Completion: $totalMs ms")

            Result.success(profile)
        } catch (e: Exception) {
            val totalMs = System.currentTimeMillis() - tStart
            Log.e("AUTH_PERF", "[TIMING] Sign-up FAILED after $totalMs ms: [${e.javaClass.name}] ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleCredential(idToken: String): Result<FirebaseUser> {
        Log.d("GOOGLE_AUTH_FLOW", "AuthRepository: signInWithGoogleCredential starting with idToken (length=${idToken.length})...")
        return try {
            val auth = getAuth() ?: throw Exception("Firebase Authentication service is unavailable.")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            Log.d("GOOGLE_AUTH_FLOW", "AuthRepository: Calling FirebaseAuth.signInWithCredential(Google)...")
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("Google auth returned empty user object")
            Log.d("GOOGLE_AUTH_FLOW", "AuthRepository: FirebaseAuth.signInWithCredential SUCCESS -> user.uid=${user.uid}, email=${user.email}")
            setGuestSessionActive(false)
            userProfileStore.setLoggedIn(true)

            // Immediately construct and save local auth profile so UI picks up Google user immediately
            val googleName = user.displayName?.ifBlank { null }
                ?: user.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                ?: "Google User"
            val googleEmail = user.email ?: ""
            val initialProfile = UserProfile(
                uid = user.uid,
                name = googleName,
                email = googleEmail
            )
            userProfileStore.saveProfile(initialProfile)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ensureUserProfileExists(user)
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Async ensureUserProfileExists failed: ${e.message}")
                }
            }
            Result.success(user)
        } catch (e: Exception) {
            Log.e("GOOGLE_AUTH_FLOW", "AuthRepository: FirebaseAuth.signInWithCredential FAILED -> [${e.javaClass.name}] ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        Log.d("AuthRepository", "sendPasswordResetEmail starting for email='$email'")
        return try {
            val auth = getAuth() ?: throw Exception("Firebase Authentication service is unavailable.")
            Log.d("AuthRepository", "Calling FirebaseAuth.sendPasswordResetEmail(email='$email')...")
            
            try {
                val settings = actionCodeSettings {
                    url = "https://brainquiz-ai-app.web.app/reset-password/?postReset=true"
                    handleCodeInApp = true
                    setAndroidPackageName("com.aistudio.brainquizai.app", true, "24")
                }
                auth.sendPasswordResetEmail(email, settings).await()
                Log.d("AuthRepository", "FirebaseAuth.sendPasswordResetEmail with ActionCodeSettings SUCCESS -> email='$email'")
            } catch (e: Exception) {
                Log.w("AuthRepository", "ActionCodeSettings reset email failed (${e.message}), falling back to standard reset email")
                auth.sendPasswordResetEmail(email).await()
                Log.d("AuthRepository", "FirebaseAuth.sendPasswordResetEmail fallback SUCCESS -> email='$email'")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "FirebaseAuth.sendPasswordResetEmail FAILED -> [${e.javaClass.name}] ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun verifyPasswordResetCode(code: String): Result<String> {
        return try {
            val auth = getAuth() ?: throw Exception("Firebase Authentication service is unavailable.")
            val email = auth.verifyPasswordResetCode(code).await()
            Result.success(email)
        } catch (e: Exception) {
            Log.e("AuthRepository", "verifyPasswordResetCode FAILED", e)
            Result.failure(e)
        }
    }

    suspend fun confirmPasswordReset(code: String, newPassword: String): Result<Unit> {
        return try {
            val auth = getAuth() ?: throw Exception("Firebase Authentication service is unavailable.")
            auth.confirmPasswordReset(code, newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "confirmPasswordReset FAILED", e)
            Result.failure(e)
        }
    }

    enum class AccountAuthType {
        EMAIL_PASSWORD,
        GOOGLE_SIGN_IN,
        GUEST
    }

    fun getAccountAuthType(): AccountAuthType {
        if (isGuestSessionActive()) return AccountAuthType.GUEST
        val user = currentUser ?: return AccountAuthType.GUEST
        if (user.isAnonymous) return AccountAuthType.GUEST
        val providers = user.providerData.map { it.providerId }
        return when {
            providers.contains("google.com") && !providers.contains("password") -> AccountAuthType.GOOGLE_SIGN_IN
            providers.contains("password") -> AccountAuthType.EMAIL_PASSWORD
            else -> AccountAuthType.EMAIL_PASSWORD
        }
    }

    suspend fun changePassword(currentPassword: String?, newPassword: String): Result<Unit> {
        val user = currentUser
        if (user == null || isGuestSessionActive() || user.isAnonymous) {
            return Result.failure(Exception("Guest accounts do not use a password."))
        }

        val providers = user.providerData.map { it.providerId }
        if (providers.contains("google.com") && !providers.contains("password")) {
            return Result.failure(Exception("This account is signed in with Google. Passwords are managed via your Google Account."))
        }

        if (newPassword.length < 6) {
            return Result.failure(Exception("Password must be at least 6 characters long."))
        }

        return try {
            val email = user.email
            if (!currentPassword.isNullOrBlank() && !email.isNullOrBlank()) {
                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                user.reauthenticate(credential).await()
            }
            user.updatePassword(newPassword).await()
            Log.d("AuthRepository", "Password updated successfully for user ${user.uid}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to update password: [${e.javaClass.name}] ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<FirebaseUser?> {
        return try {
            setGuestSessionActive(true)
            val auth = getAuth()
            val user = if (auth != null) {
                try {
                    val result = auth.signInAnonymously().await()
                    result.user
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Firebase anonymous auth failed, using local guest session: ${e.message}")
                    null
                }
            } else null

            val local = quizResultRepository.getLocalProgress()
            val guestUid = user?.uid ?: getOrCreateGuestId()
            val profile = UserProfile(
                uid = guestUid,
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
            userProfileStore.setLoggedIn(true)
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error signing in anonymously", e)
            Result.failure(e)
        }
    }

    suspend fun fetchUserProfile(uid: String): UserProfile? {
        return try {
            val firestore = getFirestore() ?: return null
            withTimeoutOrNull(2500L) {
                val doc = firestore.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    doc.toObject(UserProfile::class.java)
                } else null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetching user profile", e)
            null
        }
    }

    suspend fun saveUserProfileToFirestore(profile: UserProfile): Boolean {
        Log.d("XP_TRACE", "[AuthRepository] saveUserProfileToFirestore: profile.xp=${profile.xp}")
        userProfileStore.saveProfile(profile)
        return try {
            val firestore = getFirestore() ?: return true
            if (profile.uid.isNotBlank() && !profile.uid.startsWith("guest_") && !isGuestSessionActive()) {
                withContext(NonCancellable) {
                    withTimeoutOrNull(3000L) {
                        firestore.collection("users").document(profile.uid)
                            .set(profile)
                            .await()
                    }
                    try {
                        val leaderboardRepo = LeaderboardRepository(context)
                        leaderboardRepo.syncCurrentUserToLeaderboard(profile)
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "Error syncing to leaderboard from saveUserProfileToFirestore", e)
                    }
                }
            }
            true
        } catch (e: CancellationException) {
            Log.d("AuthRepository", "saveUserProfileToFirestore cancelled")
            true
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error saving user profile to Firestore", e)
            true
        }
    }

    private suspend fun ensureUserProfileExists(user: FirebaseUser) {
        val currentAuthUserUid = currentUser?.uid
        val isGuestActive = isGuestSessionActive()
        Log.d("RUNTIME_TRACE", "[ENSURE_PROFILE] ENTER ensureUserProfileExists: uid=${user.uid}, isGuestSessionActive=$isGuestActive, currentFirebaseUserUid=$currentAuthUserUid")
        try {
            val firestore = getFirestore() ?: return
            withTimeoutOrNull(2500L) {
                val doc = firestore.collection("users").document(user.uid).get().await()

                if (!doc.exists()) {
                    val displayName = user.displayName
                        ?: user.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                        ?: "Player"
                    val profile = UserProfile(
                        uid = user.uid,
                        name = displayName,
                        email = user.email ?: "",
                        avatarId = "brain",
                        xp = 0,
                        level = 1,
                        coins = 0,
                        streak = 0,
                        rank = "Beginner",
                        unlockedAchievements = emptyList(),
                        claimedRewards = emptyList(),
                        unlockedAvatars = listOf("student_boy", "student_girl", "brain"),
                        quizHistory = emptyList(),
                        totalQuizzesPlayed = 0,
                        totalQuestionsAnswered = 0,
                        totalCorrectAnswers = 0,
                        bestScore = 0,
                        longestStreak = 0
                    )
                    Log.d("RUNTIME_TRACE", "[ENSURE_PROFILE] Doc does not exist. Saving default profile: uid=${profile.uid}, xp=${profile.xp}, coins=${profile.coins}, streak=${profile.streak}, fullProfile=$profile")
                    Log.d("RUNTIME_TRACE", "[ENSURE_PROFILE] Executing saveUserProfileToFirestore(profile)...")
                    saveUserProfileToFirestore(profile)
                } else {
                    val existing = doc.toObject(UserProfile::class.java)
                    if (existing != null) {
                        Log.d("RUNTIME_TRACE", "[ENSURE_PROFILE] Doc exists. Existing profile: uid=${existing.uid}, xp=${existing.xp}, coins=${existing.coins}, streak=${existing.streak}, fullProfile=$existing")
                        Log.d("RUNTIME_TRACE", "[ENSURE_PROFILE] Executing saveUserProfileToFirestore(existing)...")
                        saveUserProfileToFirestore(existing)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("RUNTIME_TRACE", "[ENSURE_PROFILE] Exception in ensureUserProfileExists", e)
        }
    }

    suspend fun signInAsGuestProfile(): Result<UserProfile> {
        return try {
            // 1. Clear any cached authenticated user / Firebase Auth session
            try {
                getAuth()?.signOut()
            } catch (e: Exception) {
                Log.w("AuthRepository", "Error signing out of Firebase Auth prior to Guest session: ${e.message}")
            }

            // 2. Activate Guest session
            setGuestSessionActive(true)

            // 3. Obtain clean independent guest profile (Username: Guest, Email: Guest Account)
            val profile = userProfileStore.createOrGetGuestProfile()

            // 4. Guest profile remains isolated from Firebase Authentication (do NOT call saveUserProfileToFirestore)
            Log.d("AuthRepository", "Signed in as Guest profile: name='${profile.name}', email='${profile.email}', uid='${profile.uid}'")
            Result.success(profile)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error signing in as guest profile", e)
            Result.failure(e)
        }
    }

    suspend fun syncUserData(): Result<Unit> {
        return try {
            if (isGuestSessionActive() || currentUser == null) {
                return Result.failure(Exception("Cloud sync is available for signed-in accounts. Please sign in to sync your progress."))
            }
            val user = currentUser ?: return Result.failure(Exception("Not signed in"))
            val localProfile = userProfileStore.getProfile()
            val firestore = getFirestore() ?: return Result.failure(Exception("Database connection unavailable"))

            val doc = firestore.collection("users").document(user.uid).get().await()
            val remoteProfile = doc.toObject(UserProfile::class.java)

            val mergedProfile = if (remoteProfile != null) {
                localProfile.copy(
                    uid = user.uid,
                    email = user.email ?: "",
                    xp = maxOf(localProfile.xp, remoteProfile.xp),
                    coins = maxOf(localProfile.coins, remoteProfile.coins),
                    streak = maxOf(localProfile.streak, remoteProfile.streak),
                    bestScore = maxOf(localProfile.bestScore, remoteProfile.bestScore),
                    totalQuizzesPlayed = maxOf(localProfile.totalQuizzesPlayed, remoteProfile.totalQuizzesPlayed),
                    totalQuestionsAnswered = maxOf(localProfile.totalQuestionsAnswered, remoteProfile.totalQuestionsAnswered),
                    totalCorrectAnswers = maxOf(localProfile.totalCorrectAnswers, remoteProfile.totalCorrectAnswers),
                    unlockedAchievements = (localProfile.unlockedAchievements + remoteProfile.unlockedAchievements).distinct(),
                    unlockedAvatars = (localProfile.unlockedAvatars + remoteProfile.unlockedAvatars).distinct(),
                    name = if (localProfile.name.isNotBlank() && localProfile.name != "Player") localProfile.name else remoteProfile.name,
                    avatarId = if (localProfile.avatarId.isNotBlank()) localProfile.avatarId else remoteProfile.avatarId
                )
            } else {
                localProfile.copy(uid = user.uid, email = user.email ?: "")
            }

            userProfileStore.saveProfile(mergedProfile)
            firestore.collection("users").document(user.uid).set(mergedProfile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error during cloud sync", e)
            Result.failure(e)
        }
    }

    suspend fun backupUserData(): Result<Unit> {
        return try {
            if (isGuestSessionActive() || currentUser == null) {
                return Result.failure(Exception("Cloud backup is available for signed-in accounts. Please sign in to backup."))
            }
            val user = currentUser ?: return Result.failure(Exception("Not signed in"))
            val firestore = getFirestore() ?: return Result.failure(Exception("Database connection unavailable"))
            val profile = userProfileStore.getProfile().copy(uid = user.uid, email = user.email ?: "")

            firestore.collection("users").document(user.uid)
                .collection("backups").document("latest")
                .set(profile)
                .await()
            firestore.collection("users").document(user.uid).set(profile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error during cloud backup", e)
            Result.failure(e)
        }
    }

    suspend fun restoreUserData(): Result<Unit> {
        return try {
            if (isGuestSessionActive() || currentUser == null) {
                return Result.failure(Exception("Cloud restore is available for signed-in accounts. Please sign in to restore."))
            }
            val user = currentUser ?: return Result.failure(Exception("Not signed in"))
            val firestore = getFirestore() ?: return Result.failure(Exception("Database connection unavailable"))

            var doc = firestore.collection("users").document(user.uid)
                .collection("backups").document("latest").get().await()
            if (!doc.exists()) {
                doc = firestore.collection("users").document(user.uid).get().await()
            }
            if (!doc.exists()) {
                return Result.failure(Exception("No backup found in cloud for this account."))
            }
            val restoredProfile = doc.toObject(UserProfile::class.java)
                ?: return Result.failure(Exception("Failed to parse backup profile."))
            userProfileStore.saveProfile(restoredProfile)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error during cloud restore", e)
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(currentPassword: String? = null): Result<Unit> {
        return try {
            if (isGuestSessionActive()) {
                userProfileStore.resetGuestAccount()
                signOut()
                return Result.success(Unit)
            }
            val user = currentUser ?: return Result.failure(Exception("No active account session found to delete."))
            val email = user.email

            // Re-authenticate if password is provided
            if (!currentPassword.isNullOrBlank() && !email.isNullOrBlank()) {
                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                user.reauthenticate(credential).await()
            }

            // Delete Firestore user data
            try {
                val firestore = getFirestore()
                firestore?.collection("users")?.document(user.uid)?.delete()?.await()
            } catch (e: Exception) {
                Log.w("AuthRepository", "Failed to delete firestore user document: ${e.message}")
            }

            // Delete Firebase user
            user.delete().await()

            // Clear local storage and sign out
            userProfileStore.clearAuthProfile()
            signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error deleting account", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            userProfileStore.setGuestActive(false)
            userProfileStore.setLoggedIn(false)
            userProfileStore.clearAuthProfile()
            getAuth()?.signOut()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error signing out", e)
        }
    }
}
