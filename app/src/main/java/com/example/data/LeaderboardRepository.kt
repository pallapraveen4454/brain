package com.example.data

import android.content.Context
import android.util.Log
import com.example.BrainQuizApplication
import com.example.ui.screens.LeaderboardUser
import com.example.utils.RankUtils
import org.json.JSONArray
import org.json.JSONObject

enum class LeaderboardPeriod {
    WEEKLY, GLOBAL, FRIENDS
}

data class LeaderboardData(
    val topPlayers: List<LeaderboardUser>,
    val currentUserEntry: LeaderboardUser,
    val period: LeaderboardPeriod
)

class LeaderboardRepository(
    private val context: Context? = try { BrainQuizApplication.instance } catch (e: Exception) { null },
    private val userProfileStore: UserProfileStore = UserProfileStore(context),
    private val achievementRepository: AchievementRepository = AchievementRepository(context),
    private val quizResultRepository: QuizResultRepository = QuizResultRepository(context)
) {
    private val prefsName = "leaderboard_prefs"

    private fun getPrefs() = context?.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun getLeaderboard(period: LeaderboardPeriod = LeaderboardPeriod.GLOBAL): LeaderboardData {
        val competitors = loadOrCreateCompetitors()
        val userProfile = userProfileStore.getProfile()
        val userStats = quizResultRepository.getUserStats()
        val achievements = achievementRepository.getAllAchievements(userProfile.xp, userProfile.coins, userProfile.streak)
        val unlockedCount = achievements.count { it.isUnlocked }

        val currentUserScore = calculateScore(
            xp = userProfile.xp,
            quizzesPlayed = userStats.totalQuizzesPlayed,
            achievementsCount = unlockedCount
        )

        val currentUserEntry = LeaderboardUser(
            rank = 0,
            id = userProfile.uid.ifBlank { "current_user" },
            name = userProfile.name.ifBlank { "Player" },
            avatarId = userProfile.avatarId.ifBlank { "brain" },
            xp = userProfile.xp,
            level = maxOf(1, userProfile.level),
            rankBadge = RankUtils.getRankForXp(userProfile.xp),
            quizzesPlayed = userStats.totalQuizzesPlayed,
            achievementsCount = unlockedCount,
            score = currentUserScore,
            isCurrentUser = true
        )

        // Combine user with global competitors
        val allEntries = (competitors.filterNot { it.isCurrentUser } + currentUserEntry)
            .sortedWith(compareByDescending<LeaderboardUser> { it.xp }
                .thenByDescending { it.score }
                .thenByDescending { it.achievementsCount })

        val rankedList = allEntries.mapIndexed { index, user ->
            user.copy(rank = index + 1)
        }

        val updatedCurrentUser = rankedList.find { it.isCurrentUser } ?: currentUserEntry.copy(rank = rankedList.size)

        return LeaderboardData(
            topPlayers = rankedList,
            currentUserEntry = updatedCurrentUser,
            period = period
        )
    }

    private fun calculateScore(xp: Int, quizzesPlayed: Int, achievementsCount: Int): Int {
        return xp + (achievementsCount * 50) + (quizzesPlayed * 15)
    }

    private fun loadOrCreateCompetitors(): List<LeaderboardUser> {
        val prefs = getPrefs()
        val jsonStr = prefs?.getString("competitors_json", "") ?: ""

        if (jsonStr.isNotBlank()) {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<LeaderboardUser>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        LeaderboardUser(
                            rank = obj.optInt("rank", i + 1),
                            id = obj.optString("id", "comp_${i + 1}"),
                            name = obj.optString("name", "Player"),
                            avatarId = obj.optString("avatarId", "brain"),
                            xp = obj.optInt("xp", 100),
                            level = obj.optInt("level", 1),
                            rankBadge = obj.optString("rankBadge", "Beginner"),
                            quizzesPlayed = obj.optInt("quizzesPlayed", 5),
                            achievementsCount = obj.optInt("achievementsCount", 2),
                            score = obj.optInt("score", 100),
                            countryFlag = obj.optString("countryFlag", "🇮🇳"),
                            rankChange = obj.optInt("rankChange", 0),
                            isCurrentUser = false
                        )
                    )
                }
                if (list.size >= 100) return list
            } catch (e: Exception) {
                Log.e("LeaderboardRepository", "Error reading saved competitors", e)
            }
        }

        // Initial competitors if none saved or upgrade to 150 competitors
        val initialCompetitors = generate150Competitors()
        saveCompetitors(initialCompetitors)
        return initialCompetitors
    }

    private fun generate150Competitors(): List<LeaderboardUser> {
        val rawPlayers = listOf(
            Triple("Sophia Chen", "🇺🇸", "wizard"),
            Triple("Alex Vance", "🇨🇦", "rocket"),
            Triple("Elena Rostova", "🇩🇪", "crown"),
            Triple("Marcus Brody", "🇬🇧", "star"),
            Triple("David Kim", "🇰🇷", "brain"),
            Triple("Sarah Connor", "🇦🇺", "fire"),
            Triple("Liam Neeson", "🇮🇪", "ninja"),
            Triple("Priya Sharma", "🇮🇳", "cat"),
            Triple("Lucas Meyer", "🇫🇷", "fox"),
            Triple("Aria Stark", "🇬🇧", "owl"),
            Triple("Ethan Hunt", "🇺🇸", "robot"),
            Triple("Maya Patel", "🇮🇳", "bear"),
            Triple("Kenji Takahashi", "🇯🇵", "dragon"),
            Triple("Carlos Silva", "🇧🇷", "shield"),
            Triple("Fatima Al-Mansoor", "🇦🇪", "gem"),
            Triple("Rahul Verma", "🇮🇳", "zap"),
            Triple("Hannah Schmidt", "🇦🇹", "lion"),
            Triple("Mateo Rossi", "🇮🇹", "tiger"),
            Triple("Wei Zhang", "🇨🇳", "dragon"),
            Triple("Amara Okonkwo", "🇳🇬", "fire"),
            Triple("Gabriel Garcia", "🇲🇽", "star"),
            Triple("Ananya Reddy", "🇮🇳", "brain"),
            Triple("Oliver Smith", "🇬🇧", "rocket"),
            Triple("Emma Watson", "🇬🇧", "crown"),
            Triple("Chloe Dubois", "🇫🇷", "cat"),
            Triple("Viktor Krum", "🇧🇬", "wizard"),
            Triple("Lars Lindqvist", "🇸🇪", "ninja"),
            Triple("Santiago Morales", "🇦🇷", "fox"),
            Triple("Vikram Malhotra", "🇮🇳", "owl"),
            Triple("Yuki Tanaka", "🇯🇵", "robot"),
            Triple("Isabella Santos", "🇧🇷", "bear"),
            Triple("Daniel Craig", "🇬🇧", "shield"),
            Triple("Sneha Kulkarni", "🇮🇳", "gem"),
            Triple("Noah Miller", "🇺🇸", "zap"),
            Triple("Mia Kowalski", "🇵🇱", "lion"),
            Triple("Rohan Das", "🇮🇳", "tiger"),
            Triple("Ingrid Johansen", "🇳🇴", "dragon"),
            Triple("Alejandro Fernandez", "🇪🇸", "fire"),
            Triple("Kavya Nair", "🇮🇳", "star"),
            Triple("Seung-Woo Lee", "🇰🇷", "brain"),
            Triple("Benjamin Franklin", "🇺🇸", "wizard"),
            Triple("Zeynep Yilmaz", "🇹🇷", "rocket"),
            Triple("Deepika Padukone", "🇮🇳", "crown"),
            Triple("James Bond", "🇬🇧", "ninja"),
            Triple("Astrid Lindgren", "🇸🇪", "fox"),
            Triple("Aditya Rao", "🇮🇳", "owl"),
            Triple("Nguyen Van Nam", "🇻🇳", "robot"),
            Triple("Charlotte Taylor", "🇦🇺", "bear"),
            Triple("Tariq Al-Hassan", "🇸🇦", "shield"),
            Triple("Arjun Kapoor", "🇮🇳", "gem"),
            Triple("Freja Mortensen", "🇩🇰", "zap"),
            Triple("Luc Besson", "🇫🇷", "lion"),
            Triple("Divya Teja", "🇮🇳", "tiger"),
            Triple("Hiroshi Sato", "🇯🇵", "dragon"),
            Triple("Camila Cabello", "🇨🇺", "fire"),
            Triple("Siddharth Roy", "🇮🇳", "star"),
            Triple("Oscar Isaac", "🇬🇹", "brain"),
            Triple("Elif Safak", "🇹🇷", "cat"),
            Triple("Suresh Raina", "🇮🇳", "fox"),
            Triple("Nora Mørk", "🇳🇴", "owl"),
            Triple("Benjamin Davies", "🇳🇿", "robot"),
            Triple("Varun Dhawan", "🇮🇳", "bear"),
            Triple("Mei-Ling Huang", "🇹🇼", "shield"),
            Triple("Leo Messi", "🇦🇷", "gem"),
            Triple("Pooja Hegde", "🇮🇳", "zap"),
            Triple("Arthur Pendelton", "🇬🇧", "lion"),
            Triple("Thabo Mbeki", "🇿🇦", "tiger"),
            Triple("Meera Joshi", "🇮🇳", "dragon"),
            Triple("Jan van der Meer", "🇳🇱", "fire"),
            Triple("Karthik Raja", "🇮🇳", "star"),
            Triple("Sonia Ben Ali", "🇲🇦", "brain"),
            Triple("Gautam Gambhir", "🇮🇳", "wizard"),
            Triple("Ines de Castro", "🇵🇹", "rocket"),
            Triple("Siddharth Nigam", "🇮🇳", "crown"),
            Triple("Klaus Weber", "🇩🇪", "ninja"),
            Triple("Bhavana Menon", "🇮🇳", "fox"),
            Triple("Bambang Soetjipto", "🇮🇩", "owl"),
            Triple("Harish Kumar", "🇮🇳", "robot"),
            Triple("Youssef El-Sherif", "🇪🇬", "bear"),
            Triple("Swati Sharma", "🇮🇳", "shield"),
            Triple("Mikhail Petrov", "🇷🇺", "gem"),
            Triple("Nisha Aggarwal", "🇮🇳", "zap"),
            Triple("Taro Yamada", "🇯🇵", "lion"),
            Triple("Aakash Gupta", "🇮🇳", "tiger"),
            Triple("Elena Gomez", "🇨🇱", "dragon"),
            Triple("Ramesh Babu", "🇮🇳", "fire"),
            Triple("Chiara Ferragni", "🇮🇹", "star"),
            Triple("Naveen Patnaik", "🇮🇳", "brain"),
            Triple("Sven Eriksson", "🇸🇪", "cat"),
            Triple("Shruti Haasan", "🇮🇳", "fox"),
            Triple("Dimitri Papas", "🇬🇷", "owl"),
            Triple("Manish Pandey", "🇮🇳", "robot"),
            Triple("Maja Novak", "🇸🇮", "bear"),
            Triple("Aravind Swamy", "🇮🇳", "shield"),
            Triple("Lukas Podolski", "🇩🇪", "gem"),
            Triple("Trisha Krishnan", "🇮🇳", "zap"),
            Triple("Elijah Wood", "🇺🇸", "lion"),
            Triple("Madhavan R", "🇮🇳", "tiger"),
            Triple("Siobhan O'Connor", "🇮🇪", "dragon"),
            Triple("Pradeep Kumar", "🇮🇳", "fire"),
            Triple("Eleni Georgiou", "🇨🇾", "star"),
            Triple("Vishal Dadlani", "🇮🇳", "brain"),
            Triple("Nico Rosberg", "🇲🇨", "wizard"),
            Triple("Anushka Shetty", "🇮🇳", "rocket"),
            Triple("Kasper Schmeichel", "🇩🇰", "crown"),
            Triple("Gokulnath M", "🇮🇳", "ninja"),
            Triple("Gael Monfils", "🇫🇷", "fox"),
            Triple("Sandhya Rani", "🇮🇳", "owl"),
            Triple("Boris Johnson", "🇬🇧", "robot"),
            Triple("Sujith Unnithan", "🇮🇳", "bear"),
            Triple("Ana de Armas", "🇨🇺", "shield"),
            Triple("Vijay Sethupathi", "🇮🇳", "gem"),
            Triple("Hanna Marin", "🇫🇮", "zap"),
            Triple("Bhuvan Bam", "🇮🇳", "lion"),
            Triple("Matthijs de Ligt", "🇳🇱", "tiger"),
            Triple("Keerthy Suresh", "🇮🇳", "dragon"),
            Triple("Mads Mikkelsen", "🇩🇰", "fire"),
            Triple("Rajesh Khanna", "🇮🇳", "star"),
            Triple("Shinya Yamanaka", "🇯🇵", "brain"),
            Triple("Tanya Bhatia", "🇮🇳", "cat"),
            Triple("Luka Modric", "🇭🇷", "fox"),
            Triple("Sunil Chhetri", "🇮🇳", "owl"),
            Triple("Alicia Vikander", "🇸🇪", "robot"),
            Triple("Nitin Gadkari", "🇮🇳", "bear"),
            Triple("Cillian Murphy", "🇮🇪", "shield"),
            Triple("Preeti Zinta", "🇮🇳", "gem"),
            Triple("Lando Norris", "🇬🇧", "zap"),
            Triple("Vivek Oberoi", "🇮🇳", "lion"),
            Triple("Pierre Gasly", "🇫🇷", "tiger"),
            Triple("Tanvi Shah", "🇮🇳", "dragon"),
            Triple("Kylian Mbappe", "🇫🇷", "fire"),
            Triple("Sanjay Dutt", "🇮🇳", "star"),
            Triple("Lewis Hamilton", "🇬🇧", "brain"),
            Triple("Rashmika Mandanna", "🇮🇳", "wizard"),
            Triple("Charles Leclerc", "🇲🇨", "rocket"),
            Triple("Yuvraj Singh", "🇮🇳", "crown"),
            Triple("Max Verstappen", "🇳🇱", "ninja"),
            Triple("Kalyani Priyadarshan", "🇮🇳", "fox"),
            Triple("Carlos Sainz", "🇪🇸", "owl"),
            Triple("Aishwarya Rai", "🇮🇳", "robot"),
            Triple("George Russell", "🇬🇧", "bear"),
            Triple("Samantha Ruth", "🇮🇳", "shield"),
            Triple("Fernando Alonso", "🇪🇸", "gem"),
            Triple("Mahendra Singh", "🇮🇳", "zap"),
            Triple("Sebastian Vettel", "🇩🇪", "lion"),
            Triple("Shreya Ghoshal", "🇮🇳", "tiger"),
            Triple("Novak Djokovic", "🇷🇸", "dragon"),
            Triple("Virat Kohli", "🇮🇳", "fire")
        )

        val total = rawPlayers.size
        val rankChangeChoices = listOf(0, 1, -1, 2, -2, 3, -3, 0, 1, 0, 2, -1)

        return rawPlayers.mapIndexed { index, (name, flag, avatar) ->
            val baseScale = (total - index).toDouble() / total
            val xp = maxOf(80, (4800 * (baseScale * baseScale * 0.85 + baseScale * 0.15)).toInt() + (index % 5) * 12)
            val level = maxOf(1, (xp / 350) + 1)
            val badge = when {
                xp >= 3500 -> "Legend"
                xp >= 2800 -> "Grandmaster"
                xp >= 2200 -> "Master"
                xp >= 1600 -> "Diamond"
                xp >= 1000 -> "Gold"
                xp >= 500 -> "Silver"
                xp >= 200 -> "Bronze"
                else -> "Novice"
            }
            val quizzes = maxOf(2, (xp / 50) + (index % 7))
            val achievements = maxOf(1, (xp / 280) + (index % 3))
            val score = xp + (achievements * 50) + (quizzes * 15)
            val rankChange = rankChangeChoices[index % rankChangeChoices.size]

            LeaderboardUser(
                rank = index + 1,
                id = "comp_${index + 1}",
                name = name,
                avatarId = avatar,
                xp = xp,
                level = level,
                rankBadge = badge,
                quizzesPlayed = quizzes,
                achievementsCount = achievements,
                score = score,
                countryFlag = flag,
                rankChange = rankChange,
                isCurrentUser = false
            )
        }
    }

    private fun saveCompetitors(competitors: List<LeaderboardUser>) {
        try {
            val prefs = getPrefs() ?: return
            val array = JSONArray()
            competitors.forEach { comp ->
                val obj = JSONObject().apply {
                    put("rank", comp.rank)
                    put("id", comp.id)
                    put("name", comp.name)
                    put("avatarId", comp.avatarId)
                    put("xp", comp.xp)
                    put("level", comp.level)
                    put("rankBadge", comp.rankBadge)
                    put("quizzesPlayed", comp.quizzesPlayed)
                    put("achievementsCount", comp.achievementsCount)
                    put("score", comp.score)
                    put("countryFlag", comp.countryFlag)
                    put("rankChange", comp.rankChange)
                }
                array.put(obj)
            }
            prefs.edit().putString("competitors_json", array.toString()).apply()
        } catch (e: Exception) {
            Log.e("LeaderboardRepository", "Error saving competitors", e)
        }
    }
}
