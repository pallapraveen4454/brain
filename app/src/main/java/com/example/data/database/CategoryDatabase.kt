package com.example.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [QuestionEntity::class], version = 1, exportSchema = false)
abstract class CategoryDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
}
