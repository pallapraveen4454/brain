package com.example.data

import android.util.Log
import com.example.data.model.QuizQuestion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Random

/**
 * Production-hardened Gemini service for BrainQuizAI.
 *
 * SECURITY ARCHITECTURE:
 * - NO Gemini API keys or credentials are stored or embedded in the Android APK.
 * - All AI requests are securely routed through authenticated Firebase Cloud Functions.
 * - Callers must have a valid Firebase authentication session (anonymous or signed-in user).
 * - Primary model: gemini-3.6-flash (server-side)
 * - Fallback model: gemini-3.5-flash (server-side)
 * - Anti-abuse rate limiting is enforced server-side.
 */
class GeminiQuizService {

    companion object {
        private const val TAG = "GeminiQuizService"
        private const val FUNCTION_QUIZ_GENERATOR = "geminiQuizGenerator"
    }

    private val random = Random()

    /**
     * Ensures an active Firebase session exists before calling authenticated backend functions.
     * If user is a guest or not yet authenticated with Firebase Auth, automatically establishes
     * an anonymous Firebase session so that request.auth is cryptographically verified by the server.
     */
    private suspend fun ensureFirebaseAuth() {
        try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
                Log.d(TAG, "Initialized anonymous Firebase session for AI request: ${auth.currentUser?.uid}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "ensureFirebaseAuth notice: ${e.message}")
        }
    }

    /**
     * Generates exactly 10 topic-specific multiple-choice quiz questions for the selected topic.
     * Securely requests generation from Firebase Cloud Functions.
     * Falls back to high-quality TopicKnowledgeEngine if offline or server is unavailable.
     */
    suspend fun generateQuizForTopic(topic: String): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val trimmedTopic = topic.trim()
        if (trimmedTopic.isBlank()) {
            return@withContext TopicKnowledgeEngine.generateQuestionsForTopic(trimmedTopic)
        }

        try {
            ensureFirebaseAuth()

            val functions = FirebaseFunctions.getInstance()
            val payload = hashMapOf("topic" to trimmedTopic)
            val result = functions.getHttpsCallable(FUNCTION_QUIZ_GENERATOR).call(payload).await()

            val data = result.data as? Map<*, *>
            val rawJson = data?.get("rawJson") as? String

            if (!rawJson.isNullOrBlank()) {
                val parsedQuestions = parseAndValidateQuestionsJson(rawJson, trimmedTopic)
                val sanitizedList = sanitizeAndEnforceDiversity(parsedQuestions, trimmedTopic)

                if (sanitizedList.size == 10) {
                    return@withContext sanitizedList
                } else if (sanitizedList.isNotEmpty()) {
                    val fallbackPool = TopicKnowledgeEngine.generateQuestionsForTopic(trimmedTopic)
                    val combined = (sanitizedList + fallbackPool.filterNot { fb ->
                        sanitizedList.any { q -> q.questionText.equals(fb.questionText, ignoreCase = true) }
                    }).take(10)
                    return@withContext combined
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Backend quiz generation notice (${e.javaClass.simpleName}): ${e.message}. Using TopicKnowledgeEngine.")
        }

        return@withContext TopicKnowledgeEngine.generateQuestionsForTopic(trimmedTopic)
    }

    fun cancelActiveRequest() {
        // Maintained for backward compatibility; coroutines handle cooperative cancellation
    }

    fun buildGeminiPrompt(topic: String): String {
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
                    Log.w(TAG, "Rejected generic or invalid question: '$qText'")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse questions JSON: $rawJson", e)
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
                val randomizedQuestion = TopicKnowledgeEngine.randomizeOptionOrder(q)
                uniqueQuestions.add(randomizedQuestion)
            }
            if (uniqueQuestions.size == 10) break
        }

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
