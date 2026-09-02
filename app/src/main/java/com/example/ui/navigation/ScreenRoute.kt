package com.example.ui.navigation

sealed class ScreenRoute(val route: String) {
    object Splash : ScreenRoute("splash_screen")
    object Login : ScreenRoute("login_screen")
    object Home : ScreenRoute("home_screen")
    object AiQuizGenerator : ScreenRoute("ai_quiz_generator_screen")
    object AiQuickAnswer : ScreenRoute("ai_quick_answer_screen")
    object AvatarShop : ScreenRoute("avatar_shop_screen")
    object Quiz : ScreenRoute("quiz_screen/{categoryId}") {
        fun createRoute(categoryId: String) = "quiz_screen/$categoryId"
    }
    object ForgotPassword : ScreenRoute("forgot_password_screen")
    object ResetPassword : ScreenRoute("reset_password_screen/{oobCode}") {
        fun createRoute(oobCode: String) = "reset_password_screen/$oobCode"
    }
}

