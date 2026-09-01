package com.example

import com.example.data.GeminiQuizService
import com.example.data.TopicKnowledgeEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiQuizGeneratorTopicDiversityTest {

    private lateinit var service: GeminiQuizService

    @Before
    fun setUp() {
        service = GeminiQuizService()
    }

    @Test
    fun testWorldWarIIvsMarvelProduceCompletelyDifferentQuestionsAndOptions() = runBlocking {
        val ww2Questions = service.generateQuizForTopic("World War II History")
        val marvelQuestions = service.generateQuizForTopic("Marvel Cinematic Universe")

        assertEquals("WW2 quiz must contain exactly 10 questions", 10, ww2Questions.size)
        assertEquals("Marvel quiz must contain exactly 10 questions", 10, marvelQuestions.size)

        // 1. Question texts must be completely distinct between the two topics
        val ww2Texts = ww2Questions.map { it.questionText.lowercase() }
        val marvelTexts = marvelQuestions.map { it.questionText.lowercase() }

        for (ww2Text in ww2Texts) {
            assertFalse("WW2 question must not appear in Marvel quiz: $ww2Text", marvelTexts.contains(ww2Text))
        }

        // 2. Options must not be identical generic templates
        val ww2OptionSets = ww2Questions.map { it.options.map { opt -> opt.lowercase() }.toSet() }
        val marvelOptionSets = marvelQuestions.map { it.options.map { opt -> opt.lowercase() }.toSet() }

        for (ww2Set in ww2OptionSets) {
            assertFalse("WW2 options must not be identical to any Marvel option set", marvelOptionSets.contains(ww2Set))
        }

        // 3. WW2 questions must contain domain-specific WW2 knowledge
        val ww2Keywords = listOf("pearl harbor", "normandy", "stalingrad", "overlord", "churchill", "manhattan", "midway", "rommel", "yalta", "blitzkrieg", "iwo jima", "1945", "enigma")
        val ww2RelevantCount = ww2Questions.count { q ->
            val fullContent = (q.questionText + " " + q.options.joinToString(" ")).lowercase()
            ww2Keywords.any { fullContent.contains(it) }
        }
        assertTrue("At least 8 WW2 questions must directly reference historical WW2 facts, found $ww2RelevantCount", ww2RelevantCount >= 8)

        // 4. Marvel questions must contain MCU-specific knowledge
        val marvelKeywords = listOf("infinity stone", "vision", "loki", "wakanda", "mjolnir", "tony stark", "iron man", "vibranium", "thanos", "spider-man", "black widow", "doctor strange", "guardians", "avengers")
        val marvelRelevantCount = marvelQuestions.count { q ->
            val fullContent = (q.questionText + " " + q.options.joinToString(" ")).lowercase()
            marvelKeywords.any { fullContent.contains(it) }
        }
        assertTrue("At least 8 Marvel questions must directly reference MCU characters/lore, found $marvelRelevantCount", marvelRelevantCount >= 8)
    }

    @Test
    fun testNoGenericTemplatesOrBannedDistractorsInGeneratedQuizzes() = runBlocking {
        val topics = listOf(
            "World War II",
            "Marvel Cinematic Universe",
            "Quantum Physics",
            "Space Exploration & NASA",
            "Cybersecurity & Hacking",
            "Human Anatomy & Brain",
            "90s Rock Music",
            "Culinary Arts & Cooking",
            "Artificial Intelligence",
            "Football World Cup",
            "Ancient Roman History",
            "Electric Vehicles"
        )

        for (topic in topics) {
            val questions = service.generateQuizForTopic(topic)
            assertEquals("Quiz for '$topic' must have exactly 10 questions", 10, questions.size)

            for (q in questions) {
                // Must not match generic banned regex
                val isBannedTemplate = TopicKnowledgeEngine.BANNED_GENERIC_PATTERNS.any { it.containsMatchIn(q.questionText) }
                assertFalse("Question must not be a banned generic template: '${q.questionText}' for topic '$topic'", isBannedTemplate)

                // Must not use banned placeholder distractors like 'Methodological Innovations' or 'The Ban on Research'
                for (opt in q.options) {
                    val isBannedDistractor = opt.trim().lowercase() in TopicKnowledgeEngine.BANNED_GENERIC_DISTRACTORS
                    assertFalse("Option '$opt' in topic '$topic' is a forbidden generic distractor", isBannedDistractor)
                }

                // Exactly 4 distinct options
                assertEquals("Question '${q.questionText}' must have exactly 4 options", 4, q.options.size)
                assertEquals("Options must be unique within question '${q.questionText}'", 4, q.options.distinct().size)

                // Valid correctOptionIndex and non-empty explanation
                assertTrue("correctOptionIndex must be 0..3", q.correctOptionIndex in 0..3)
                assertTrue("Explanation must be present for '${q.questionText}'", q.explanation.isNotBlank())
                assertNotNull("Correct answer string must exist", q.correctAnswer)
                assertTrue("Correct answer must match an option", q.options.contains(q.correctAnswer))
            }
        }
    }

    @Test
    fun testNoDuplicatesAndCorrectOptionIndicesAreDistributed() = runBlocking {
        val questions = service.generateQuizForTopic("Quantum Physics")
        assertEquals(10, questions.size)

        // No duplicate question texts
        val texts = questions.map { it.questionText.trim().lowercase() }
        assertEquals("All 10 question texts must be unique", 10, texts.toSet().size)

        // No duplicate option sets
        val optionSets = questions.map { it.options.map { o -> o.lowercase() }.toSet() }
        assertEquals("All 10 option sets must be unique", 10, optionSets.toSet().size)

        // Correct answer positions must vary across 0, 1, 2, 3 (not all at index 0)
        val distinctIndices = questions.map { it.correctOptionIndex }.toSet()
        assertTrue("Correct option indices must vary across multiple positions (found ${distinctIndices.size})", distinctIndices.size >= 2)
    }

    @Test
    fun testJsonParsingAndSanitizerRejectsGenericTemplatesAndPreservesValidContent() {
        val rawJsonWithGenericTemplate = """
            {
              "questions": [
                {
                  "questionText": "Which milestone significantly transformed the study of World War II?",
                  "options": ["Methodological Innovations", "The Ban on Research", "Stagnation of Ideas", "Complete Disregard of Facts"],
                  "correctOptionIndex": 0,
                  "explanation": "Generic explanation."
                },
                {
                  "questionText": "Which event brought the US into World War II in 1941?",
                  "options": ["Attack on Pearl Harbor", "Battle of Britain", "Invasion of Poland", "Sinking of Lusitania"],
                  "correctOptionIndex": 0,
                  "explanation": "The Pearl Harbor attack led the US to declare war on Japan."
                }
              ]
            }
        """.trimIndent()

        val parsed = service.parseAndValidateQuestionsJson(rawJsonWithGenericTemplate, "World War II")
        // The first generic question MUST be rejected, while the valid factual question is kept
        assertEquals(1, parsed.size)
        assertEquals("Which event brought the US into World War II in 1941?", parsed[0].questionText)

        // Sanitizer tops it up to 10 using topic knowledge engine
        val sanitized = service.sanitizeAndEnforceDiversity(parsed, "World War II")
        assertEquals(10, sanitized.size)
        for (q in sanitized) {
            assertEquals(4, q.options.size)
            assertTrue(q.explanation.isNotBlank())
        }
    }
}
