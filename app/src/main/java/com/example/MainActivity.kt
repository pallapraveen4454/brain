package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.ui.localization.EnglishStrings
import com.example.ui.localization.LocalAppStrings
import com.example.ui.navigation.BrainQuizNavGraph
import com.example.ui.navigation.ScreenRoute
import com.example.ui.theme.BrainQuizAITheme
import com.example.utils.BackgroundMusicPlayer

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialNavigateTo = intent?.getStringExtra("navigate_to")

        setContent {
            CompositionLocalProvider(
                LocalAppStrings provides EnglishStrings
            ) {
                BrainQuizAITheme {
                    val navController = rememberNavController()

                    LaunchedEffect(initialNavigateTo) {
                        when (initialNavigateTo) {
                            "daily_challenge" -> navController.navigate(ScreenRoute.Quiz.createRoute("daily"))
                            "quiz" -> navController.navigate(ScreenRoute.Quiz.createRoute("quick"))
                            "achievements" -> navController.navigate(ScreenRoute.Home.route)
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

    override fun onStop() {
        super.onStop()
        BackgroundMusicPlayer.stop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
