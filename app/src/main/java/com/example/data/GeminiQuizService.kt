package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.QuizQuestion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.util.Random
import java.util.concurrent.TimeUnit

class GeminiQuizService {

    companion object {
        private const val TAG = "GeminiQuickAnswer"
        private const val PRIMARY_MODEL = "gemini-3.6-flash"
        private const val FALLBACK_MODEL = "gemini-3.5-flash"

        // Reusable client with optimized, short timeouts and connection pooling for Quick Answer
        private val quickAnswerClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val random = Random()

    /**
     * Answers any general user question accurately, directly, and concisely using Gemini.
     * Optimized for fast response times:
     * - Fast primary model with at most one fallback
     * - Connection reuse with keep-alive pooling
     * - Concise bounded history (last 2 turns)
     * - Lightweight token limits (384 max output tokens)
     * - Non-blocking I/O execution
     */
    suspend fun generateQuickAnswer(
        question: String,
        recentHistory: List<Pair<String, String>> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val trimmedQuestion = question.trim()
        if (trimmedQuestion.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Question cannot be empty"))
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("GeminiQuizService", "Cannot call Gemini Quick Answer: API key is not configured.")
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured. Please check your settings."))
        }

        // Primary model + at most one carefully selected fallback
        val modelsToAttempt = listOf(PRIMARY_MODEL, FALLBACK_MODEL)
        var lastException: Exception? = null

        for ((index, model) in modelsToAttempt.withIndex()) {
            val startTime = System.currentTimeMillis()
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Request started: model=$model (attempt ${index + 1}/${modelsToAttempt.size})")
            }

            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

                val contentsArray = org.json.JSONArray()

                // Keep only the most recent 2 conversation turns to keep request payload compact and fast
                val boundedHistory = recentHistory.takeLast(2)
                for ((prevUser, prevModel) in boundedHistory) {
                    val u = prevUser.trim()
                    val m = prevModel.trim()
                    if (u.isNotBlank() && m.isNotBlank()) {
                        contentsArray.put(JSONObject().apply {
                            put("role", "user")
                            put("parts", org.json.JSONArray().apply {
                                put(JSONObject().apply { put("text", u) })
                            })
                        })
                        contentsArray.put(JSONObject().apply {
                            put("role", "model")
                            put("parts", org.json.JSONArray().apply {
                                put(JSONObject().apply { put("text", m) })
                            })
                        })
                    }
                }

                // Add current user question
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply { put("text", trimmedQuestion) })
                    })
                })

                val jsonPayload = JSONObject().apply {
                    put("contents", contentsArray)
                    put("systemInstruction", JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "You are BrainQuizAI Quick Answer, an intelligent, factual, and direct AI assistant. Provide concise, accurate, and direct answers in the user's language. Keep answers informative yet brief. Do not format as a quiz.")
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.4)
                        put("topP", 0.9)
                        put("maxOutputTokens", 384)
                    })
                }

                val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .header("x-goog-api-key", apiKey)
                    .post(requestBody)
                    .build()

                val response = quickAnswerClient.newCall(request).execute()
                val durationMs = System.currentTimeMillis() - startTime
                val body = response.body?.string()

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Request completed: model=$model in ${durationMs}ms, status=${response.code}")
                }

                if (response.isSuccessful && !body.isNullOrBlank()) {
                    val responseJson = JSONObject(body)
                    val candidates = responseJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val content = candidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val combinedText = StringBuilder()
                            for (i in 0 until parts.length()) {
                                val partText = parts.getJSONObject(i).optString("text", "")
                                if (partText.isNotBlank()) {
                                    combinedText.append(partText)
                                }
                            }
                            val answerText = combinedText.toString().trim()
                            if (answerText.isNotBlank()) {
                                if (BuildConfig.DEBUG) {
                                    val attemptType = if (index == 0) "primary" else "fallback"
                                    Log.d(TAG, "Success ($attemptType): model=$model answered in ${durationMs}ms")
                                }
                                return@withContext Result.success(answerText)
                            }
                        }
                    }
                    lastException = Exception("Model $model returned 200 with empty candidate text")
                } else {
                    val code = response.code
                    val errorMsg = "HTTP $code from $model: ${body?.take(200) ?: "Empty body"}"
                    Log.w(TAG, errorMsg)
                    lastException = Exception(errorMsg)

                    // Client/auth errors (400, 401, 403) will not succeed with fallback; fast-fail immediately
                    if (code == 400 || code == 401 || code == 403) {
                        break
                    }
                }
            } catch (e: CancellationException) {
                // Preserve coroutine cancellation when user leaves screen
                throw e
            } catch (e: SocketTimeoutException) {
                val durationMs = System.currentTimeMillis() - startTime
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Request timed out: model=$model after ${durationMs}ms")
                }
                lastException = e
                // Fast-fail on timeout: do not cascade another full timeout to fallback model
                break
            } catch (e: Exception) {
                val durationMs = System.currentTimeMillis() - startTime
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Request failed: model=$model after ${durationMs}ms, error=${e.javaClass.simpleName}")
                }
                lastException = e
            }
        }

        return@withContext Result.failure(lastException ?: Exception("Couldn't get an answer right now. Please try again."))
    }

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
