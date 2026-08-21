package com.example.data.database

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.BrainQuizApplication
import java.util.concurrent.ConcurrentHashMap

class CategoryDatabaseManager(private val context: Context? = null) {

    companion object {
        private val dbMap = ConcurrentHashMap<String, CategoryDatabase>()

        fun closeAndClearAll() {
            dbMap.values.forEach { try { it.close() } catch (_: Exception) {} }
            dbMap.clear()
        }

        fun closeAndClear(categoryKey: String) {
            val norm = categoryKey.lowercase().trim()
            val db = dbMap.remove(norm)
            try { db?.close() } catch (_: Exception) {}
        }
    }

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
            Room.databaseBuilder(
                ctx.applicationContext,
                CategoryDatabase::class.java,
                dbName
            )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .allowMainThreadQueries() // Allows fast synchronous count lookup during UI setup
            .build()
        }
    }

    /**
     * Returns all supported canonical category keys.
     */
    fun getAllCategoryKeys(): List<String> {
        return listOf("gk", "science", "sports", "history", "movies", "tech", "geo", "math")
    }

    fun ensureCategoryPopulated(categoryKey: String, db: CategoryDatabase? = null) {
        val normKey = normalizeCategoryKey(categoryKey)
        val targetDb = db ?: getDatabaseForCategory(normKey) ?: return
        try {
            val seedQuestions = DefaultQuestionSeeds.getSeedsForCategory(normKey)
            val currentCount = targetDb.questionDao().getQuestionCountSync()
            if (currentCount < seedQuestions.size) {
                targetDb.questionDao().insertQuestionsSync(seedQuestions)
                Log.d("CategoryDatabaseManager", "Synced ${seedQuestions.size} seed questions into category_$normKey.db (previously $currentCount)")
            }
        } catch (e: Exception) {
            Log.e("CategoryDatabaseManager", "Error ensuring population for category_$normKey", e)
        }
    }
}
