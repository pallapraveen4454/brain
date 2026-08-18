package com.example.data.database

import android.content.Context
import android.util.Log
import com.example.data.model.QuizQuestion
import java.util.UUID

data class ImportResult(
    val importedCount: Int,
    val skippedDuplicatesCount: Int,
    val failedValidationCount: Int,
    val categoryCounts: Map<String, Int>,
    val importedQuestions: List<QuestionEntity>,
    val validationErrors: List<String>
)

data class ValidationResult(
    val isValid: Boolean,
    val normalizedQuestion: QuestionEntity?,
    val error: String? = null
)

class QuestionImportManager(private val context: Context? = null) {

    private val dbManager = CategoryDatabaseManager(context)
    private val validCategoryKeys = setOf("gk", "science", "sports", "history", "movies", "tech", "geo", "math")

    /**
     * Validates a question against all required business rules:
     * 1. Valid Category
     * 2. No duplicate answer options
     * 3. Correct answer must match one option
     * 4. No duplicate question text or duplicate ID
     */
    suspend fun validateQuestion(question: QuestionEntity, checkDatabaseDuplicates: Boolean = false): ValidationResult {
        // 1. Validate Category
        val normCategory = dbManager.normalizeCategoryKey(question.categoryId)
        if (normCategory !in validCategoryKeys) {
            return ValidationResult(false, null, "Invalid category: '${question.categoryId}'")
        }

        // Validate non-empty fields
        if (question.questionText.isBlank()) {
            return ValidationResult(false, null, "Question text cannot be blank")
        }

        val options = listOf(question.optionA, question.optionB, question.optionC, question.optionD)
        if (options.any { it.isBlank() }) {
            return ValidationResult(false, null, "All four options (A, B, C, D) must be non-blank")
        }

        // 2. Validate No Duplicate Answer Options
        val normalizedOptions = options.map { it.trim().lowercase() }
        if (normalizedOptions.distinct().size < 4) {
            return ValidationResult(false, null, "Answer options must all be distinct: $options")
        }

        // 3. Validate Correct Answer Matches One Option
        val correctTrimmed = question.correctAnswer.trim()
        val matchDirect = options.any { it.trim().equals(correctTrimmed, ignoreCase = true) }
        val isOptionKey = when (correctTrimmed.uppercase()) {
            "A", "OPTION A", "0" -> true
            "B", "OPTION B", "1" -> true
            "C", "OPTION C", "2" -> true
            "D", "OPTION D", "3" -> true
            else -> false
        }

        if (!matchDirect && !isOptionKey) {
            return ValidationResult(false, null, "Correct answer '$correctTrimmed' does not match any of the 4 options")
        }

        // Determine clear correct answer string if given as key (e.g., "A" -> optionA)
        val resolvedCorrectAnswer = if (matchDirect) {
            correctTrimmed
        } else {
            when (correctTrimmed.uppercase()) {
                "A", "OPTION A", "0" -> question.optionA
                "B", "OPTION B", "1" -> question.optionB
                "C", "OPTION C", "2" -> question.optionC
                "D", "OPTION D", "3" -> question.optionD
                else -> correctTrimmed
            }
        }

        // Normalize ID if blank
        val resolvedId = if (question.id.isBlank()) {
            "gen_${normCategory}_${UUID.randomUUID().toString().take(8)}"
        } else {
            question.id.trim()
        }

        // 4. Validate No Duplicate Question in Destination Database if requested
        if (checkDatabaseDuplicates) {
            val db = dbManager.getDatabaseForCategory(normCategory)
                ?: return ValidationResult(false, null, "Database not available for category $normCategory")

            val dao = db.questionDao()
            if (dao.existsByText(question.questionText.trim()) || (question.id.isNotBlank() && dao.existsById(question.id.trim()))) {
                return ValidationResult(false, null, "Duplicate question text already exists in category '$normCategory'")
            }
        }

        val finalQuestion = question.copy(
            id = resolvedId,
            categoryId = normCategory,
            questionText = question.questionText.trim(),
            optionA = question.optionA.trim(),
            optionB = question.optionB.trim(),
            optionC = question.optionC.trim(),
            optionD = question.optionD.trim(),
            correctAnswer = resolvedCorrectAnswer,
            explanation = question.explanation.trim(),
            difficulty = if (question.difficulty.trim().isBlank()) "Medium" else question.difficulty.trim()
        )

        return ValidationResult(true, finalQuestion)
    }

    /**
     * Imports a batch of questions into their respective category databases.
     * Routes General Knowledge -> category_gk.db, Science -> category_science.db, etc.
     * Automatically handles duplicate filtering and updates total question counts.
     */
    suspend fun importQuestions(rawQuestions: List<QuestionEntity>): ImportResult {
        var importedCount = 0
        var skippedDuplicatesCount = 0
        var failedValidationCount = 0
        val importedQuestions = mutableListOf<QuestionEntity>()
        val validationErrors = mutableListOf<String>()

        // Group questions by normalized category
        val questionsByCategory = rawQuestions.groupBy { dbManager.normalizeCategoryKey(it.categoryId) }

        for ((categoryKey, questions) in questionsByCategory) {
            if (categoryKey !in validCategoryKeys) {
                failedValidationCount += questions.size
                validationErrors.add("Skipped ${questions.size} questions for invalid category '$categoryKey'")
                continue
            }

            val db = dbManager.getDatabaseForCategory(categoryKey)
            if (db == null) {
                failedValidationCount += questions.size
                validationErrors.add("Database unavailable for category '$categoryKey'")
                continue
            }

            val dao = db.questionDao()
            val validCategoryQuestions = mutableListOf<QuestionEntity>()
            val seenInBatchTexts = mutableSetOf<String>()
            val seenInBatchIds = mutableSetOf<String>()

            for (rawQ in questions) {
                val validation = validateQuestion(rawQ, checkDatabaseDuplicates = true)
                if (!validation.isValid || validation.normalizedQuestion == null) {
                    val errorMsg = validation.error ?: "Unknown error"
                    if (errorMsg.contains("Duplicate question text")) {
                        skippedDuplicatesCount++
                    } else {
                        failedValidationCount++
                    }
                    validationErrors.add("Validation failed for ID '${rawQ.id}': $errorMsg")
                    continue
                }

                val validatedQ = validation.normalizedQuestion
                val textKey = validatedQ.questionText.lowercase()

                // Check against intra-batch duplicates
                if (seenInBatchTexts.contains(textKey) || (validatedQ.id.isNotBlank() && seenInBatchIds.contains(validatedQ.id.lowercase()))) {
                    skippedDuplicatesCount++
                    validationErrors.add("Duplicate question text inside batch skipped: '${validatedQ.questionText}'")
                    continue
                }

                var finalId = validatedQ.id
                if (finalId.isBlank()) {
                    finalId = "gen_${categoryKey}_${UUID.randomUUID().toString().take(8)}"
                }

                val finalQ = validatedQ.copy(id = finalId)
                seenInBatchTexts.add(textKey)
                seenInBatchIds.add(finalId.lowercase())
                validCategoryQuestions.add(finalQ)
            }

            if (validCategoryQuestions.isNotEmpty()) {
                dao.insertQuestions(validCategoryQuestions)
                importedCount += validCategoryQuestions.size
                importedQuestions.addAll(validCategoryQuestions)
                Log.d("QuestionImportManager", "Imported ${validCategoryQuestions.size} questions into category_$categoryKey.db")
            }
        }

        // Fetch updated total question count for all categories
        val updatedCategoryCounts = validCategoryKeys.associateWith { cat ->
            dbManager.getDatabaseForCategory(cat)?.questionDao()?.getQuestionCountSync() ?: 0
        }

        return ImportResult(
            importedCount = importedCount,
            skippedDuplicatesCount = skippedDuplicatesCount,
            failedValidationCount = failedValidationCount,
            categoryCounts = updatedCategoryCounts,
            importedQuestions = importedQuestions,
            validationErrors = validationErrors
        )
    }
}
