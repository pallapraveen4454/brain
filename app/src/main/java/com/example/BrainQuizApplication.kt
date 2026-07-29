package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class BrainQuizApplication : Application() {

    companion object {
        lateinit var instance: BrainQuizApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:1047242078803:android:brainquizai")
                    .setApiKey("AIzaSyDummyKeyForInitializationFallback")
                    .setProjectId("brainquiz-ai")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("BrainQuizApplication", "FirebaseApp initialized with fallback options")
            }
        } catch (e: Exception) {
            Log.e("BrainQuizApplication", "Failed to initialize FirebaseApp", e)
        }
    }
}
