package com.example.ui.navigation

sealed class ScreenRoute(val route: String) {
    object Splash : ScreenRoute("splash_screen")
    object Login : ScreenRoute("login_screen")
    object Home : ScreenRoute("home_screen")
    object AiQuizGenerator : ScreenRoute("ai_quiz_generator_screen")
    object Quiz : ScreenRoute("quiz_screen/{categoryId}") {
        fun createRoute(categoryId: String) = "quiz_screen/$categoryId"
    }
}

