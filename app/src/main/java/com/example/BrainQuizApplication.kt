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
                val app = FirebaseApp.initializeApp(this)
                if (app != null) {
                    Log.d("BrainQuizApplication", "FirebaseApp initialized automatically from google-services.json successfully: ${app.options.projectId}")
                } else {
                    val optionsFromRes = FirebaseOptions.fromResource(this)
                    if (optionsFromRes != null) {
                        FirebaseApp.initializeApp(this, optionsFromRes)
                        Log.d("BrainQuizApplication", "FirebaseApp initialized from resource options successfully")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BrainQuizApplication", "Failed to initialize FirebaseApp", e)
        }
    }
}
