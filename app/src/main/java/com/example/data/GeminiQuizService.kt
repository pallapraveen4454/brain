package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.QuizQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiQuizService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateQuizForTopic(topic: String): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        Log.d("GeminiQuizService", "Generating quiz for topic: '$topic' (API Key present: ${apiKey.isNotBlank()})")

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("GeminiQuizService", "Gemini API key is missing or default. Using topic-customized fallback questions.")
            return@withContext generateFallbackQuestions(topic)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val prompt = """
                Generate exactly 10 multiple-choice quiz questions about the topic: "$topic".
                Requirements:
                1. Provide exactly 4 distinct option strings for each question.
                2. Provide correctAnswer as the exact string matching one of the 4 options.
                3. Provide correctOptionIndex as a 0-based integer (0, 1, 2, or 3) corresponding to the index of correctAnswer in options.
                4. Include a concise 1-sentence explanation for the correct answer.
                5. Output strictly valid JSON matching this structure without Markdown formatting:
                {
                  "questions": [
                    {
                      "questionText": "Question string here?",
                      "options": ["Option A", "Option B", "Option C", "Option D"],
                      "correctAnswer": "Option A",
                      "correctOptionIndex": 0,
                      "explanation": "Explanation here."
                    }
                  ]
                }
            """.trimIndent()

            val jsonPayload = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("responseMimeType", "application/json")
                })
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBodyString = response.body?.string()

            if (!response.isSuccessful || responseBodyString.isNullOrBlank()) {
                Log.e("GeminiQuizService", "API request failed with code ${response.code}: $responseBodyString")
                return@withContext generateFallbackQuestions(topic)
            }

            val responseJson = JSONObject(responseBodyString)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                Log.e("GeminiQuizService", "No candidates returned from Gemini API")
                return@withContext generateFallbackQuestions(topic)
            }

            val candidate = candidates.getJSONObject(0)
            val content = candidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val rawText = parts.getJSONObject(0).getString("text")

            val questions = parseQuestionsJson(rawText, topic)
            if (questions.size >= 10) {
                return@withContext questions.take(10)
            } else if (questions.isNotEmpty()) {
                // If fewer than 10, fill remaining with fallback
                val remaining = generateFallbackQuestions(topic).filterNot { fb -> questions.any { q -> q.questionText == fb.questionText } }
                return@withContext (questions + remaining).take(10)
            } else {
                return@withContext generateFallbackQuestions(topic)
            }

        } catch (e: Exception) {
            Log.e("GeminiQuizService", "Error calling Gemini API for topic '$topic'", e)
            return@withContext generateFallbackQuestions(topic)
        }
    }

    private fun parseQuestionsJson(rawJson: String, topic: String): List<QuizQuestion> {
        val result = mutableListOf<QuizQuestion>()
        try {
            // Clean markdown blocks if present
            val cleanedJson = rawJson.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonObj = JSONObject(cleanedJson)
            val questionsArray = jsonObj.optJSONArray("questions") ?: org.json.JSONArray()

            for (i in 0 until questionsArray.length()) {
                val qObj = questionsArray.getJSONObject(i)
                val qText = qObj.getString("questionText")
                val optionsArray = qObj.getJSONArray("options")
                val optionsList = mutableListOf<String>()
                for (j in 0 until optionsArray.length()) {
                    optionsList.add(optionsArray.getString(j))
                }
                val correctIndex = qObj.optInt("correctOptionIndex", 0).coerceIn(0, 3)
                val explanation = qObj.optString("explanation", "The correct answer is ${optionsList.getOrNull(correctIndex) ?: ""}.")

                if (optionsList.size == 4 && qText.isNotBlank()) {
                    result.add(
                        QuizQuestion(
                            id = "ai_${topic.hashCode()}_$i",
                            categoryId = "ai_custom",
                            questionText = qText,
                            options = optionsList,
                            correctOptionIndex = correctIndex,
                            explanation = explanation
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiQuizService", "Failed to parse questions JSON: $rawJson", e)
        }
        return result
    }

    private fun generateFallbackQuestions(topic: String): List<QuizQuestion> {
        val normTopic = topic.trim().replaceFirstChar { it.uppercase() }
        val templateQuestions = listOf(
            QuizQuestion(
                id = "fb_1",
                categoryId = "ai_custom",
                questionText = "Which core concept is fundamental when studying $normTopic?",
                options = listOf("Systematic Analysis", "Random Speculation", "Unverified Assumptions", "Subjective Guessing"),
                correctOptionIndex = 0,
                explanation = "Systematic analysis and structured principles form the core foundation of $normTopic."
            ),
            QuizQuestion(
                id = "fb_2",
                categoryId = "ai_custom",
                questionText = "In the domain of $normTopic, what plays a vital role in practical applications?",
                options = listOf("Outdated Practices", "Theoretical Frameworks & Data", "Disregarded Evidence", "Random Variables"),
                correctOptionIndex = 1,
                explanation = "Theoretical frameworks backed by empirical data drive real-world applications in $normTopic."
            ),
            QuizQuestion(
                id = "fb_3",
                categoryId = "ai_custom",
                questionText = "Which milestone significantly transformed the study of $normTopic?",
                options = listOf("Methodological Innovations", "The Ban on Research", "Stagnation of Ideas", "Complete Disregard of Facts"),
                correctOptionIndex = 0,
                explanation = "Methodological breakthroughs and technological innovations transformed modern $normTopic."
            ),
            QuizQuestion(
                id = "fb_4",
                categoryId = "ai_custom",
                questionText = "When evaluating key developments in $normTopic, what feature stands out most?",
                options = listOf("Continuous Evolution & Refinement", "Static Knowledge", "Lack of Growth", "Irrelevance"),
                correctOptionIndex = 0,
                explanation = "$normTopic continues to evolve rapidly as new evidence and tools emerge."
            ),
            QuizQuestion(
                id = "fb_5",
                categoryId = "ai_custom",
                questionText = "What primary methodology is utilized by experts in $normTopic?",
                options = listOf("Empirical Testing & Verification", "Pure Coincidence", "Mythological Beliefs", "Trial without Observation"),
                correctOptionIndex = 0,
                explanation = "Experts rely on rigorous empirical testing and verification in $normTopic."
            ),
            QuizQuestion(
                id = "fb_6",
                categoryId = "ai_custom",
                questionText = "How does $normTopic impact modern global developments?",
                options = listOf("Minimal Impact", "Drives Innovation & Understanding", "Restricts Progress", "Has No Practical Value"),
                correctOptionIndex = 1,
                explanation = "$normTopic contributes significantly to advancements across global research and technology."
            ),
            QuizQuestion(
                id = "fb_7",
                categoryId = "ai_custom",
                questionText = "Which of the following best describes the global consensus on $normTopic?",
                options = listOf("It is a recognized field of study", "It is completely unknown", "It was discredited centuries ago", "It holds zero scientific value"),
                correctOptionIndex = 0,
                explanation = "$normTopic is widely recognized as an essential area of study and discussion."
            ),
            QuizQuestion(
                id = "fb_8",
                categoryId = "ai_custom",
                questionText = "What is a essential skill required for mastering $normTopic?",
                options = listOf("Critical Thinking & Analysis", "Ignoring Facts", "Rote Memorization Only", "Superficial Skimming"),
                correctOptionIndex = 0,
                explanation = "Critical thinking allows deeper comprehension and problem-solving in $normTopic."
            ),
            QuizQuestion(
                id = "fb_9",
                categoryId = "ai_custom",
                questionText = "Which factor is most crucial for future advancements in $normTopic?",
                options = listOf("Interdisciplinary Collaboration", "Isolation", "Discarding Historical Data", "Avoiding New Tools"),
                correctOptionIndex = 0,
                explanation = "Cross-disciplinary research accelerates innovations in $normTopic."
            ),
            QuizQuestion(
                id = "fb_10",
                categoryId = "ai_custom",
                questionText = "What ultimate objective guides scholars and practitioners of $normTopic?",
                options = listOf("Expanding Depth of Knowledge", "Promoting Confusion", "Halting Inquiry", "Creating Misinformation"),
                correctOptionIndex = 0,
                explanation = "Pursuing accuracy and expanding knowledge remains the guiding principle of $normTopic."
            )
        )
        return templateQuestions
    }
}
