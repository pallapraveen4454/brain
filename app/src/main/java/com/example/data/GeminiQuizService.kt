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
import java.util.Random
import java.util.concurrent.TimeUnit

class GeminiQuizService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val random = Random()

    /**
     * Generates exactly 10 genuinely topic-specific multiple-choice quiz questions for the selected topic.
     */
    suspend fun generateQuizForTopic(topic: String): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val trimmedTopic = topic.trim()
        Log.d("GeminiQuizService", "Generating quiz for topic: '$trimmedTopic' (API Key present: ${apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"})")

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d("GeminiQuizService", "Using TopicKnowledgeEngine for topic: '$trimmedTopic'")
            return@withContext TopicKnowledgeEngine.generateQuestionsForTopic(trimmedTopic)
        }

        try {
            val candidateModels = listOf("gemini-3.5-flash", "gemini-2.5-flash", "gemini-flash-latest")
            var responseJsonString: String? = null

            for (model in candidateModels) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                    val prompt = buildGeminiPrompt(trimmedTopic)

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
                            put("temperature", 0.75)
                            put("topP", 0.95)
                            put("responseMimeType", "application/json")
                        })
                    }

                    val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val body = response.body?.string()

                    if (response.isSuccessful && !body.isNullOrBlank()) {
                        responseJsonString = body
                        break
                    } else {
                        Log.w("GeminiQuizService", "Model $model returned status ${response.code}: $body")
                    }
                } catch (e: Exception) {
                    Log.w("GeminiQuizService", "Error requesting model $model: ${e.message}")
                }
            }

            if (responseJsonString.isNullOrBlank()) {
                Log.w("GeminiQuizService", "API requests unsuccessful. Falling back to TopicKnowledgeEngine.")
                return@withContext TopicKnowledgeEngine.generateQuestionsForTopic(trimmedTopic)
            }

            val responseJson = JSONObject(responseJsonString)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                Log.w("GeminiQuizService", "No candidates in response. Falling back to TopicKnowledgeEngine.")
                return@withContext TopicKnowledgeEngine.generateQuestionsForTopic(trimmedTopic)
            }

            val candidate = candidates.getJSONObject(0)
            val content = candidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val rawText = parts.getJSONObject(0).getString("text")

            val parsedQuestions = parseAndValidateQuestionsJson(rawText, trimmedTopic)
            val sanitizedList = sanitizeAndEnforceDiversity(parsedQuestions, trimmedTopic)

            if (sanitizedList.size == 10) {
                return@withContext sanitizedList
            } else {
                Log.w("GeminiQuizService", "Parsed ${sanitizedList.size} questions after sanitization. Topping up to 10.")
                val fallbackPool = TopicKnowledgeEngine.generateQuestionsForTopic(trimmedTopic)
                val combined = (sanitizedList + fallbackPool.filterNot { fb -> sanitizedList.any { q -> q.questionText.equals(fb.questionText, ignoreCase = true) } }).take(10)
                return@withContext combined
            }

        } catch (e: Exception) {
            Log.e("GeminiQuizService", "Exception in generateQuizForTopic('$trimmedTopic')", e)
            return@withContext TopicKnowledgeEngine.generateQuestionsForTopic(trimmedTopic)
        }
    }

    private fun buildGeminiPrompt(topic: String): String {
        return """
            You are an expert trivia master and quiz creator. Generate exactly 10 distinct, high-quality, multiple-choice quiz questions specifically and exclusively about the topic: "$topic".

            STRICT QUALITY REQUIREMENTS:
            1. TOPIC-SPECIFIC KNOWLEDGE:
               - Every single question, its correct answer, and all 3 distractors MUST be factually and deeply grounded in "$topic".
               - For historical topics (e.g. World War II), ask about real battles, commanders, treaties, strategies, dates, alliances, and causes.
               - For entertainment/fiction topics (e.g. Marvel Cinematic Universe), ask about specific characters, Infinity Stones, weapons, movie plots, actors, directors, and lore.
               - For science topics (e.g. Quantum Physics), ask about real principles, particles, equations, experiments, and scientists.
               - For sports topics (e.g. Football World Cup), ask about tournaments, records, legendary players, rules, and memorable matches.

            2. FORBIDDEN GENERIC TEMPLATES & DISTRACTORS:
               - DO NOT use generic template questions like:
                 * "Which milestone significantly transformed the study of [topic]?"
                 * "How does [topic] impact modern global developments?"
                 * "Which factor is most crucial for future advancements in [topic]?"
                 * "Which core concept is fundamental when studying [topic]?"
               - DO NOT use generic academic distractors like:
                 * "Methodological Innovations", "The Ban on Research", "Stagnation of Ideas", "Complete Disregard of Facts", "Ignoring Facts", "Random Speculation".
               - Every distractor must be a genuine, plausible, topic-relevant alternative.

            3. QUESTION & ANSWER DIVERSITY:
               - Mix question styles (e.g. key figures, chronology/milestones, technical mechanisms, identification, cause & effect, defining quotes/artifacts).
               - Do not ask about the same entity twice.
               - Exactly 4 options per question.
               - Exactly 1 correct answer.
               - Distribute the correct answer position randomly across the 4 options (roughly an equal mix of 0, 1, 2, and 3).
               - Include a concise, 1-2 sentence explanation of why the correct answer is true.

            OUTPUT FORMAT:
            Output strictly valid JSON matching this schema without Markdown formatting:
            {
              "questions": [
                {
                  "questionText": "What specific factual question here?",
                  "options": ["Option A", "Option B", "Option C", "Option D"],
                  "correctOptionIndex": 0,
                  "explanation": "Clear factual explanation."
                }
              ]
            }
        """.trimIndent()
    }

    /**
     * Parses raw JSON from Gemini and applies structural and anti-template validation.
     */
    fun parseAndValidateQuestionsJson(rawJson: String, topic: String): List<QuizQuestion> {
        val result = mutableListOf<QuizQuestion>()
        try {
            val cleanedJson = rawJson.trim()
                .removePrefix("```json")
                .removePrefix("```JSON")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonObj = JSONObject(cleanedJson)
            val questionsArray = jsonObj.optJSONArray("questions") ?: org.json.JSONArray()

            for (i in 0 until questionsArray.length()) {
                val qObj = questionsArray.getJSONObject(i)
                val qText = qObj.optString("questionText", "").trim()
                val optionsArray = qObj.optJSONArray("options") ?: org.json.JSONArray()
                val optionsList = mutableListOf<String>()

                for (j in 0 until optionsArray.length()) {
                    val opt = optionsArray.getString(j).trim()
                    if (opt.isNotBlank()) {
                        optionsList.add(opt)
                    }
                }

                // If correctAnswer string was supplied instead of or alongside index
                val correctAnswerStr = qObj.optString("correctAnswer", "")
                var correctIndex = qObj.optInt("correctOptionIndex", -1)
                if (correctIndex !in 0..3 && correctAnswerStr.isNotBlank()) {
                    correctIndex = optionsList.indexOfFirst { it.equals(correctAnswerStr, ignoreCase = true) }
                }
                if (correctIndex !in 0..3) {
                    correctIndex = 0
                }

                val explanation = qObj.optString("explanation", "").ifBlank {
                    "The correct answer is ${optionsList.getOrNull(correctIndex) ?: ""}."
                }

                // Validate question
                if (qText.isNotBlank() && optionsList.size == 4 && !TopicKnowledgeEngine.isGenericOrInvalid(qText, optionsList)) {
                    val initialQuestion = QuizQuestion(
                        id = "ai_${topic.hashCode()}_${System.currentTimeMillis()}_$i",
                        categoryId = "ai_custom",
                        questionText = qText,
                        options = optionsList,
                        correctOptionIndex = correctIndex,
                        explanation = explanation
                    )
                    result.add(initialQuestion)
                } else {
                    Log.w("GeminiQuizService", "Rejected generic or invalid question: '$qText'")
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiQuizService", "Failed to parse questions JSON: $rawJson", e)
        }
        return result
    }

    /**
     * Sanitizes question list: rejects duplicates, ensures option diversity,
     * randomizes answer positions so correct answers are not stuck at index 0,
     * and fills any missing slots with genuine topic-specific questions.
     */
    fun sanitizeAndEnforceDiversity(questions: List<QuizQuestion>, topic: String): List<QuizQuestion> {
        val uniqueQuestions = mutableListOf<QuizQuestion>()
        val seenTexts = mutableSetOf<String>()
        val seenOptionSets = mutableSetOf<Set<String>>()

        for (q in questions) {
            val normalizedText = q.questionText.trim().lowercase()
            val optionSet = q.options.map { it.trim().lowercase() }.toSet()

            if (normalizedText !in seenTexts && optionSet !in seenOptionSets) {
                seenTexts.add(normalizedText)
                seenOptionSets.add(optionSet)
                // Randomize option order to avoid index 0 bias from LLMs
                val randomizedQuestion = TopicKnowledgeEngine.randomizeOptionOrder(q)
                uniqueQuestions.add(randomizedQuestion)
            }
            if (uniqueQuestions.size == 10) break
        }

        // If fewer than 10 valid questions, fill remaining from topic pool
        if (uniqueQuestions.size < 10) {
            val fallbackPool = TopicKnowledgeEngine.generateQuestionsForTopic(topic)
            for (fb in fallbackPool) {
                val normFbText = fb.questionText.trim().lowercase()
                val fbOptionSet = fb.options.map { it.trim().lowercase() }.toSet()

                if (normFbText !in seenTexts && fbOptionSet !in seenOptionSets) {
                    seenTexts.add(normFbText)
                    seenOptionSets.add(fbOptionSet)
                    uniqueQuestions.add(fb)
                }
                if (uniqueQuestions.size == 10) break
            }
        }

        return uniqueQuestions.take(10)
    }
}
