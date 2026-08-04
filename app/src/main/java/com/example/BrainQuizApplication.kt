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
        Log.d("BrainQuizApplication", "Application onCreate() - Initializing FirebaseApp...")
        try {
            val existingApps = FirebaseApp.getApps(this)
            Log.d("BrainQuizApplication", "FirebaseApp.getApps(this) count = ${existingApps.size}")
            if (existingApps.isEmpty()) {
                val app = FirebaseApp.initializeApp(this)
                if (app != null) {
                    Log.d("BrainQuizApplication", "FirebaseApp.initializeApp(this) SUCCESS -> name: ${app.name}, projectId: ${app.options.projectId}, applicationId: ${app.options.applicationId}, apiKey: ${app.options.apiKey.take(8)}...")
                } else {
                    Log.w("BrainQuizApplication", "FirebaseApp.initializeApp(this) returned null. Attempting FirebaseOptions.fromResource(this)...")
                    val optionsFromRes = FirebaseOptions.fromResource(this)
                    if (optionsFromRes != null) {
                        val initApp = FirebaseApp.initializeApp(this, optionsFromRes)
                        Log.d("BrainQuizApplication", "FirebaseApp initialized via optionsFromRes SUCCESS -> projectId: ${initApp.options.projectId}")
                    } else {
                        Log.e("BrainQuizApplication", "FirebaseOptions.fromResource(this) returned null. Ensure google-services.json is valid in /app directory.")
                    }
                }
            } else {
                val app = FirebaseApp.getInstance()
                Log.d("BrainQuizApplication", "FirebaseApp already initialized -> name: ${app.name}, projectId: ${app.options.projectId}, applicationId: ${app.options.applicationId}")
            }
        } catch (e: Exception) {
            Log.e("BrainQuizApplication", "Failed to initialize FirebaseApp: [${e.javaClass.name}] ${e.message}", e)
        }

        try {
            com.example.utils.NotificationHelper.createChannels(this)
            com.example.utils.NotificationHelper.syncReminders(this)
        } catch (e: Exception) {
            Log.e("BrainQuizApplication", "Failed to initialize notifications", e)
        }
    }
}

