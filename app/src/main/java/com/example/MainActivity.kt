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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.NoInternetOverlay
import com.example.ui.localization.EnglishStrings
import com.example.ui.localization.LocalAppStrings
import com.example.ui.navigation.BrainQuizNavGraph
import com.example.ui.navigation.ScreenRoute
import com.example.ui.theme.BrainQuizAITheme
import com.example.utils.NetworkConnectivityObserver
import com.example.utils.NotificationHelper
import com.example.utils.RewardedAdManager

class MainActivity : ComponentActivity() {

    private val deepLinkDestination = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        deepLinkDestination.value = processDeepLinkIntent(intent)

        setContent {
            CompositionLocalProvider(
                LocalAppStrings provides EnglishStrings
            ) {
                BrainQuizAITheme {
                    val context = LocalContext.current
                    val navController = rememberNavController()
                    val networkObserver = remember { NetworkConnectivityObserver.getInstance(context) }
                    val isOnline by networkObserver.isOnline.collectAsState()

                    val targetRoute = deepLinkDestination.value
                    LaunchedEffect(targetRoute) {
                        if (targetRoute != null) {
                            when {
                                targetRoute.startsWith("reset_password_screen") -> navController.navigate(targetRoute)
                                targetRoute == "daily_challenge" -> navController.navigate(ScreenRoute.Quiz.createRoute("daily"))
                                targetRoute == "quiz" -> navController.navigate(ScreenRoute.Quiz.createRoute("quick"))
                                targetRoute == "achievements" -> navController.navigate(ScreenRoute.Home.route)
                                else -> navController.navigate(targetRoute)
                            }
                            deepLinkDestination.value = null
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        BrainQuizNavGraph(
                            navController = navController,
                            modifier = Modifier.fillMaxSize()
                        )

                        NoInternetOverlay(
                            isOnline = isOnline,
                            onRetry = { networkObserver.refresh() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val dest = processDeepLinkIntent(intent)
        if (dest != null) {
            deepLinkDestination.value = dest
        }
    }

    private fun processDeepLinkIntent(intent: Intent?): String? {
        if (intent == null) return null

        val navigateToExtra = intent.getStringExtra("navigate_to")
        if (navigateToExtra != null) return navigateToExtra

        val data: android.net.Uri = intent.data ?: return null

        // Check for explicit post-reset success or login redirect
        val isPostReset = data.getBooleanQueryParameter("postReset", false) ||
                data.getQueryParameter("postReset") == "true" ||
                data.getQueryParameter("status") == "success"

        if (isPostReset || data.host == "login") {
            return ScreenRoute.Login.route
        }

        var mode = data.getQueryParameter("mode")
        var oobCode = data.getQueryParameter("oobCode")

        if (oobCode.isNullOrBlank()) {
            val nestedLink = data.getQueryParameter("link")
            if (!nestedLink.isNullOrBlank()) {
                val nestedUri = android.net.Uri.parse(nestedLink)
                if (nestedUri.getBooleanQueryParameter("postReset", false) ||
                    nestedUri.getQueryParameter("postReset") == "true" ||
                    nestedUri.getQueryParameter("status") == "success") {
                    return ScreenRoute.Login.route
                }
                mode = mode ?: nestedUri.getQueryParameter("mode")
                oobCode = nestedUri.getQueryParameter("oobCode")
            }
        }

        if (!oobCode.isNullOrBlank()) {
            if (mode == null || mode == "resetPassword") {
                return ScreenRoute.ResetPassword.createRoute(oobCode)
            }
        }

        if (data.scheme == "brainquizai") {
            return ScreenRoute.Login.route
        }

        return null
    }
}
