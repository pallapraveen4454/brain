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
                val apiKey = try {
                    BuildConfig.GEMINI_API_KEY.ifBlank { "AIzaSyAd7H-0cu2afd-sKk8ZM932mMIwoVmSNHk" }
                } catch (e: Exception) {
                    "AIzaSyAd7H-0cu2afd-sKk8ZM932mMIwoVmSNHk"
                }
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:1047242078803:android:com_aistudio_brainquizai_app")
                    .setApiKey(apiKey)
                    .setProjectId("brainquiz-ai")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("BrainQuizApplication", "FirebaseApp initialized successfully")
            }
        } catch (e: Exception) {
            Log.e("BrainQuizApplication", "Failed to initialize FirebaseApp", e)
        }
    }
}
