package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.ui.localization.EnglishStrings
import com.example.ui.localization.LocalAppStrings
import com.example.ui.navigation.BrainQuizNavGraph
import com.example.ui.navigation.ScreenRoute
import com.example.ui.theme.BrainQuizAITheme
import com.example.utils.BackgroundMusicPlayer
import com.example.utils.NotificationHelper

class MainActivity : ComponentActivity() {

    private val deepLinkDestination = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        deepLinkDestination.value = intent?.getStringExtra("navigate_to")

        setContent {
            CompositionLocalProvider(
                LocalAppStrings provides EnglishStrings
            ) {
                BrainQuizAITheme {
                    val navController = rememberNavController()

                    val targetRoute = deepLinkDestination.value
                    LaunchedEffect(targetRoute) {
                        if (targetRoute != null) {
                            when (targetRoute) {
                                "daily_challenge" -> navController.navigate(ScreenRoute.Quiz.createRoute("daily"))
                                "quiz" -> navController.navigate(ScreenRoute.Quiz.createRoute("quick"))
                                "achievements" -> navController.navigate(ScreenRoute.Home.route)
                            }
                            deepLinkDestination.value = null
                        }
                    }

                    BrainQuizNavGraph(
                        navController = navController,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        BackgroundMusicPlayer.updateMusicState(this)
    }

    override fun onResume() {
        super.onResume()
        BackgroundMusicPlayer.resume(this)
    }

    override fun onPause() {
        super.onPause()
        BackgroundMusicPlayer.pause()
    }

    override fun onStop() {
        super.onStop()
        BackgroundMusicPlayer.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        BackgroundMusicPlayer.release()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val navigateTo = intent.getStringExtra("navigate_to")
        if (navigateTo != null) {
            deepLinkDestination.value = navigateTo
        }
    }
}
