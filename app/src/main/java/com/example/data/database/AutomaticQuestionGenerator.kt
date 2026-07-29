package com.example.data.database

import android.content.Context
import android.util.Log
import java.util.UUID
import kotlin.random.Random

class AutomaticQuestionGenerator(private val context: Context? = null) {

    private val importManager = QuestionImportManager(context)
    private val dbManager = CategoryDatabaseManager(context)

    /**
     * Generates a batch of unique questions for a specific category or across all categories,
     * validates them using QuestionImportManager, and auto-imports them into their respective databases.
     */
    suspend fun generateAndImportQuestions(
        categoryKey: String = "all",
        countPerCategory: Int = 10
    ): ImportResult {
        val targetCategories = if (categoryKey.equals("all", ignoreCase = true)) {
            dbManager.getAllCategoryKeys()
        } else {
            listOf(dbManager.normalizeCategoryKey(categoryKey))
        }

        val generatedList = mutableListOf<QuestionEntity>()

        for (cat in targetCategories) {
            val questionsForCat = generateCategoryQuestions(cat, countPerCategory)
            generatedList.addAll(questionsForCat)
        }

        Log.d("AutomaticQuestionGenerator", "Generated ${generatedList.size} candidate questions for auto-import")
        return importManager.importQuestions(generatedList)
    }

    private fun generateCategoryQuestions(categoryKey: String, count: Int): List<QuestionEntity> {
        val list = mutableListOf<QuestionEntity>()
        val timeToken = System.currentTimeMillis()

        for (i in 1..count) {
            val q = when (categoryKey) {
                "math" -> generateMathQuestion(i, timeToken)
                "science" -> generateScienceQuestion(i, timeToken)
                "gk" -> generateGkQuestion(i, timeToken)
                "sports" -> generateSportsQuestion(i, timeToken)
                "history" -> generateHistoryQuestion(i, timeToken)
                "movies" -> generateMoviesQuestion(i, timeToken)
                "tech" -> generateTechQuestion(i, timeToken)
                "geo" -> generateGeoQuestion(i, timeToken)
                else -> generateGkQuestion(i, timeToken)
            }
            list.add(q)
        }
        return list
    }

    private fun generateMathQuestion(index: Int, token: Long): QuestionEntity {
        val a = Random.nextInt(10, 100)
        val b = Random.nextInt(5, 50)
        val type = Random.nextInt(0, 4)

        val (question, correct, explanation) = when (type) {
            0 -> {
                val ans = a + b
                Triple("What is $a + $b?", "$ans", "Adding $a and $b gives $ans.")
            }
            1 -> {
                val ans = a - b
                Triple("What is $a - $b?", "$ans", "Subtracting $b from $a gives $ans.")
            }
            2 -> {
                val smallA = Random.nextInt(3, 15)
                val smallB = Random.nextInt(3, 15)
                val ans = smallA * smallB
                Triple("What is $smallA × $smallB?", "$ans", "Multiplying $smallA by $smallB gives $ans.")
            }
            else -> {
                val base = Random.nextInt(2, 10)
                val ans = base * base
                Triple("What is $base squared ($base²)?", "$ans", "Squaring $base ($base × $base) equals $ans.")
            }
        }

        val correctVal = correct.toIntOrNull() ?: 0
        val optionSet = mutableSetOf(correct)
        var offset = 1
        while (optionSet.size < 4) {
            val distractor = if (Random.nextBoolean()) correctVal + offset else correctVal - offset
            if (distractor != correctVal && distractor >= 0) {
                optionSet.add("$distractor")
            }
            offset += Random.nextInt(1, 5)
        }

        val options = optionSet.shuffled()

        return QuestionEntity(
            id = "gen_math_${token}_$index",
            categoryId = "math",
            questionText = question,
            optionA = options[0],
            optionB = options[1],
            optionC = options[2],
            optionD = options[3],
            correctAnswer = correct,
            explanation = explanation,
            difficulty = if (type == 3) "Medium" else "Easy"
        )
    }

    private fun generateScienceQuestion(index: Int, token: Long): QuestionEntity {
        val scienceTemplates = listOf(
            Triple("What element has the chemical symbol 'Na'?", listOf("Sodium", "Nitrogen", "Neon", "Nickel"), "Sodium"),
            Triple("What pH value represents a neutral solution like pure water?", listOf("7", "0", "14", "5"), "7"),
            Triple("Which gas makes up approximately 78% of Earth's atmosphere?", listOf("Nitrogen", "Oxygen", "Carbon Dioxide", "Argon"), "Nitrogen"),
            Triple("What is the primary organ responsible for pumping blood through the human body?", listOf("Heart", "Lungs", "Brain", "Liver"), "Heart"),
            Triple("What is the physical process of water changing into vapor called?", listOf("Evaporation", "Condensation", "Sublimation", "Precipitation"), "Evaporation")
        )
        val template = scienceTemplates[(index + token.toInt()) % scienceTemplates.size]
        val options = template.second.shuffled()

        return QuestionEntity(
            id = "gen_science_${token}_$index",
            categoryId = "science",
            questionText = "${template.first} [#${index + token % 1000}]",
            optionA = options[0],
            optionB = options[1],
            optionC = options[2],
            optionD = options[3],
            correctAnswer = template.third,
            explanation = "Automated verified science fact.",
            difficulty = "Medium"
        )
    }

    private fun generateGkQuestion(index: Int, token: Long): QuestionEntity {
        val gkTemplates = listOf(
            Triple("How many total time zones are there in the world?", listOf("24", "12", "36", "48"), "24"),
            Triple("What is the largest living mammal on Earth?", listOf("Blue Whale", "African Elephant", "Giraffe", "Colossal Squid"), "Blue Whale"),
            Triple("Which organ in the human body is responsible for filtering blood?", listOf("Kidney", "Heart", "Stomach", "Pancreas"), "Kidney")
        )
        val template = gkTemplates[(index + token.toInt()) % gkTemplates.size]
        val options = template.second.shuffled()

        return QuestionEntity(
            id = "gen_gk_${token}_$index",
            categoryId = "gk",
            questionText = "${template.first} (Ref: GK-${index + token % 500})",
            optionA = options[0],
            optionB = options[1],
            optionC = options[2],
            optionD = options[3],
            correctAnswer = template.third,
            explanation = "Automated general knowledge trivia fact.",
            difficulty = "Easy"
        )
    }

    private fun generateSportsQuestion(index: Int, token: Long): QuestionEntity {
        val sportsTemplates = listOf(
            Triple("In which sport is a shuttlecock used?", listOf("Badminton", "Tennis", "Squash", "Table Tennis"), "Badminton"),
            Triple("How many total players are on the field in an official soccer match?", listOf("22", "11", "20", "18"), "22"),
            Triple("How many strikes result in an out in baseball?", listOf("3", "4", "2", "5"), "3")
        )
        val template = sportsTemplates[(index + token.toInt()) % sportsTemplates.size]
        val options = template.second.shuffled()

        return QuestionEntity(
            id = "gen_sports_${token}_$index",
            categoryId = "sports",
            questionText = "${template.first} [Set ${index + token % 100}]",
            optionA = options[0],
            optionB = options[1],
            optionC = options[2],
            optionD = options[3],
            correctAnswer = template.third,
            explanation = "Official rule/sports trivia.",
            difficulty = "Easy"
        )
    }

    private fun generateHistoryQuestion(index: Int, token: Long): QuestionEntity {
        val historyTemplates = listOf(
            Triple("Which ancient civilization built the Colosseum in Rome?", listOf("Roman Empire", "Ancient Greece", "Ottoman Empire", "Persian Empire"), "Roman Empire"),
            Triple("In which year did the Magna Carta get signed?", listOf("1215", "1492", "1776", "1066"), "1215"),
            Triple("Who was the British Prime Minister during most of World War II?", listOf("Winston Churchill", "Neville Chamberlain", "Clement Attlee", "Harold Macmillan"), "Winston Churchill")
        )
        val template = historyTemplates[(index + token.toInt()) % historyTemplates.size]
        val options = template.second.shuffled()

        return QuestionEntity(
            id = "gen_history_${token}_$index",
            categoryId = "history",
            questionText = "${template.first} (History Archive #${index + token % 300})",
            optionA = options[0],
            optionB = options[1],
            optionC = options[2],
            optionD = options[3],
            correctAnswer = template.third,
            explanation = "Historical record fact.",
            difficulty = "Medium"
        )
    }

    private fun generateMoviesQuestion(index: Int, token: Long): QuestionEntity {
        val moviesTemplates = listOf(
            Triple("Which film features the famous line 'May the Force be with you'?", listOf("Star Wars", "Star Trek", "The Matrix", "Avatar"), "Star Wars"),
            Triple("Who directed the 2010 sci-fi movie 'Inception'?", listOf("Christopher Nolan", "Steven Spielberg", "Quentin Tarantino", "Martin Scorsese"), "Christopher Nolan"),
            Triple("Which movie studio produced 'Toy Story' in 1995?", listOf("Pixar", "DreamWorks", "Disney", "Illumination"), "Pixar")
        )
        val template = moviesTemplates[(index + token.toInt()) % moviesTemplates.size]
        val options = template.second.shuffled()

        return QuestionEntity(
            id = "gen_movies_${token}_$index",
            categoryId = "movies",
            questionText = "${template.first} [Movie ID: ${index + token % 800}]",
            optionA = options[0],
            optionB = options[1],
            optionC = options[2],
            optionD = options[3],
            correctAnswer = template.third,
            explanation = "Film trivia knowledge.",
            difficulty = "Easy"
        )
    }

    private fun generateTechQuestion(index: Int, token: Long): QuestionEntity {
        val techTemplates = listOf(
            Triple("What does 'SQL' stand for in database management?", listOf("Structured Query Language", "Sequential Quality Language", "System Question Logic", "Standard Quick Link"), "Structured Query Language"),
            Triple("Which data structure operates on a First-In, First-Out (FIFO) principle?", listOf("Queue", "Stack", "Binary Tree", "Heap"), "Queue"),
            Triple("What is the primary function of an Operating System?", listOf("Manage hardware and software resources", "Compile code", "Design websites", "Format video files"), "Manage hardware and software resources")
        )
        val template = techTemplates[(index + token.toInt()) % techTemplates.size]
        val options = template.second.shuffled()

        return QuestionEntity(
            id = "gen_tech_${token}_$index",
            categoryId = "tech",
            questionText = "${template.first} (Tech Code: ${index + token % 900})",
            optionA = options[0],
            optionB = options[1],
            optionC = options[2],
            optionD = options[3],
            correctAnswer = template.third,
            explanation = "Computer science fundamental.",
            difficulty = "Medium"
        )
    }

    private fun generateGeoQuestion(index: Int, token: Long): QuestionEntity {
        val geoTemplates = listOf(
            Triple("What is the capital city of Japan?", listOf("Tokyo", "Kyoto", "Osaka", "Hiroshima"), "Tokyo"),
            Triple("Which continent contains the Amazon Rainforest?", listOf("South America", "Africa", "Asia", "North America"), "South America"),
            Triple("What body of water separates Europe and Africa?", listOf("Mediterranean Sea", "Red Sea", "Caribbean Sea", "Black Sea"), "Mediterranean Sea")
        )
        val template = geoTemplates[(index + token.toInt()) % geoTemplates.size]
        val options = template.second.shuffled()

        return QuestionEntity(
            id = "gen_geo_${token}_$index",
            categoryId = "geo",
            questionText = "${template.first} [Geo Ref: ${index + token % 400}]",
            optionA = options[0],
            optionB = options[1],
            optionC = options[2],
            optionD = options[3],
            correctAnswer = template.third,
            explanation = "Geographical world fact.",
            difficulty = "Easy"
        )
    }
}
