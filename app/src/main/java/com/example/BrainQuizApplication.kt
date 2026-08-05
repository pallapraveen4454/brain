package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class BrainQuizApplication : Application() {

    companion object {
        lateinit var instance: BrainQuizApplication
            private set

        fun ensureFirebaseInitialized(context: Context) {
            try {
                val apps = FirebaseApp.getApps(context)
                if (apps.isEmpty()) {
                    Log.d("BrainQuizApplication", "ensureFirebaseInitialized: No FirebaseApp found. Attempting initializeApp(context)...")
                    var app = try { FirebaseApp.initializeApp(context) } catch (e: Exception) { null }
                    if (app == null || FirebaseApp.getApps(context).isEmpty()) {
                        Log.w("BrainQuizApplication", "Auto initializeApp returned null. Trying FirebaseOptions.fromResource(context)...")
                        val optionsFromRes = try { FirebaseOptions.fromResource(context) } catch (e: Exception) { null }
                        if (optionsFromRes != null) {
                            app = try { FirebaseApp.initializeApp(context, optionsFromRes) } catch (e: Exception) { null }
                        }
                    }
                    if (FirebaseApp.getApps(context).isEmpty()) {
                        Log.w("BrainQuizApplication", "Resource initialization unavailable. Using fallback explicit FirebaseOptions builder...")
                        val explicitOptions = FirebaseOptions.Builder()
                            .setApplicationId("1:106236832575:android:8bb30cbfcabc48ffdfc18a")
                            .setApiKey("AIzaSyApStHvA17YLLkNv-H75VIOJjCvPMr1azM")
                            .setGcmSenderId("106236832575")
                            .setProjectId("brainquiz-ai-app")
                            .setStorageBucket("brainquiz-ai-app.firebasestorage.app")
                            .build()
                        app = FirebaseApp.initializeApp(context, explicitOptions)
                    }
                    val activeApp = FirebaseApp.getInstance()
                    val runtimeApiKey = activeApp.options.apiKey ?: ""
                    val googleServicesKey = "AIzaSyApStHvA17YLLkNv-H75VIOJjCvPMr1azM"
                    val isFromGoogleServicesJson = (runtimeApiKey == googleServicesKey)
                    val keyMasked = if (runtimeApiKey.length > 8) "${runtimeApiKey.take(6)}...${runtimeApiKey.takeLast(4)}" else runtimeApiKey
                    Log.d("FirebaseAuthCheck", "Firebase Auth Runtime API Key: $keyMasked (Source: ${if (isFromGoogleServicesJson) "google-services.json" else "other source"})")
                    Log.d("BrainQuizApplication", "ensureFirebaseInitialized: SUCCESS -> app=${activeApp.name}, projectId=${activeApp.options.projectId}")
                } else {
                    val defaultApp = FirebaseApp.getInstance()
                    val runtimeApiKey = defaultApp.options.apiKey ?: ""
                    val googleServicesKey = "AIzaSyApStHvA17YLLkNv-H75VIOJjCvPMr1azM"
                    val isFromGoogleServicesJson = (runtimeApiKey == googleServicesKey)
                    val keyMasked = if (runtimeApiKey.length > 8) "${runtimeApiKey.take(6)}...${runtimeApiKey.takeLast(4)}" else runtimeApiKey
                    Log.d("FirebaseAuthCheck", "Firebase Auth Runtime API Key: $keyMasked (Source: ${if (isFromGoogleServicesJson) "google-services.json" else "other source"})")
                    Log.d("BrainQuizApplication", "ensureFirebaseInitialized: Already initialized (${apps.size} app(s)) -> projectId=${defaultApp.options.projectId}")
                }
            } catch (e: Exception) {
                Log.e("BrainQuizApplication", "ensureFirebaseInitialized failed: [${e.javaClass.name}] ${e.message}", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d("BrainQuizApplication", "Application onCreate() - Initializing FirebaseApp...")
        ensureFirebaseInitialized(this)

        try {
            com.example.utils.NotificationHelper.createChannels(this)
            com.example.utils.NotificationHelper.syncReminders(this)
        } catch (e: Exception) {
            Log.e("BrainQuizApplication", "Failed to initialize notifications", e)
        }
    }
}


