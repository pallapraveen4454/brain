package com.example.data.database

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.BrainQuizApplication
import java.util.concurrent.ConcurrentHashMap

class CategoryDatabaseManager(private val context: Context? = null) {

    private val dbMap = ConcurrentHashMap<String, CategoryDatabase>()

    private fun getAppContext(): Context? {
        return context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null }
    }

    /**
     * Normalizes category ID to canonical database key.
     */
    fun normalizeCategoryKey(categoryId: String): String {
        return when (categoryId.lowercase().trim()) {
            "gk", "general knowledge", "generalknowledge" -> "gk"
            "science" -> "science"
            "sports" -> "sports"
            "history" -> "history"
            "movies", "movie" -> "movies"
            "tech", "technology" -> "tech"
            "geo", "geography" -> "geo"
            "math", "mathematics", "maths" -> "math"
            else -> categoryId.lowercase().trim()
        }
    }

    /**
     * Gets or builds the separate Room database instance for a category.
     * Each category resides in its own isolated database file (e.g. category_science.db).
     */
    fun getDatabaseForCategory(categoryId: String): CategoryDatabase? {
        val normKey = normalizeCategoryKey(categoryId)
        val ctx = getAppContext() ?: return null

        return dbMap.getOrPut(normKey) {
            val dbName = "category_$normKey.db"
            val db = Room.databaseBuilder(
                ctx.applicationContext,
                CategoryDatabase::class.java,
                dbName
            )
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries() // Allows fast synchronous count lookup during UI setup
            .build()

            // Initialize default seed questions if category database is empty
            ensureCategoryPopulated(normKey, db)
            db
        }
    }

    /**
     * Returns all supported canonical category keys.
     */
    fun getAllCategoryKeys(): List<String> {
        return listOf("gk", "science", "sports", "history", "movies", "tech", "geo", "math")
    }

    private fun ensureCategoryPopulated(categoryKey: String, db: CategoryDatabase) {
        try {
            val seedQuestions = DefaultQuestionSeeds.getSeedsForCategory(categoryKey)
            val currentCount = db.questionDao().getQuestionCountSync()
            if (currentCount < seedQuestions.size) {
                db.questionDao().insertQuestionsSync(seedQuestions)
                Log.d("CategoryDatabaseManager", "Synced ${seedQuestions.size} seed questions into category_$categoryKey.db (previously $currentCount)")
            }
        } catch (e: Exception) {
            Log.e("CategoryDatabaseManager", "Error ensuring population for category_$categoryKey", e)
        }
    }
}
