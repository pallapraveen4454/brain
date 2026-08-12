package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.screens.AiQuizGeneratorScreen
import com.example.ui.screens.AvatarShopScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.ResetPasswordScreen
import com.example.ui.screens.SplashScreen
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.HomeViewModel
import com.example.viewmodel.QuizViewModel

@Composable
fun BrainQuizNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = ScreenRoute.Splash.route,
        modifier = modifier
    ) {
        composable(
            route = ScreenRoute.Splash.route,
            enterTransition = { fadeIn(animationSpec = tween(500)) },
            exitTransition = { fadeOut(animationSpec = tween(500)) }
        ) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(ScreenRoute.Login.route) {
                        popUpTo(ScreenRoute.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    homeViewModel.loadUserProfile()
                    navController.navigate(ScreenRoute.Home.route) {
                        popUpTo(ScreenRoute.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = ScreenRoute.Login.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(400)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(400)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToHome = {
                    homeViewModel.loadUserProfile()
                    navController.navigate(ScreenRoute.Home.route) {
                        popUpTo(ScreenRoute.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = ScreenRoute.Home.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(400)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(400)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToQuiz = { categoryId ->
                    navController.navigate(ScreenRoute.Quiz.createRoute(categoryId))
                },
                onNavigateToAiGenerator = {
                    navController.navigate(ScreenRoute.AiQuizGenerator.route)
                },
                onNavigateToAvatarShop = {
                    navController.navigate(ScreenRoute.AvatarShop.route)
                },
                onNavigateToLogin = {
                    authViewModel.signOut(context) {
                        homeViewModel.signOut()
                        navController.navigate(ScreenRoute.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(
            route = ScreenRoute.AvatarShop.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(400)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(400)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) {
            val uiState = homeViewModel.uiState.collectAsState().value
            AvatarShopScreen(
                userCoins = uiState.coins,
                equippedAvatarId = uiState.avatarId,
                unlockedAvatars = uiState.unlockedAvatars,
                onBuyAvatar = { avatarId, price ->
                    homeViewModel.buyAvatar(avatarId, price)
                },
                onEquipAvatar = { avatarId ->
                    homeViewModel.equipAvatar(avatarId)
                },
                onNavigateToQuiz = {
                    navController.navigate(ScreenRoute.Home.route) {
                        popUpTo(ScreenRoute.Home.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = ScreenRoute.AiQuizGenerator.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(400)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(400)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) {
            AiQuizGeneratorScreen(
                onStartAiQuiz = { topicRoute ->
                    navController.navigate(ScreenRoute.Quiz.createRoute(topicRoute))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = ScreenRoute.Quiz.route,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType }),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(400)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(400)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: "gk"
            val quizViewModel: QuizViewModel = viewModel()
            QuizScreen(
                categoryId = categoryId,
                viewModel = quizViewModel,
                onNavigateBack = {
                    homeViewModel.loadUserProfile()
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = ScreenRoute.ResetPassword.route,
            arguments = listOf(navArgument("oobCode") { type = NavType.StringType }),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(400)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(400)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) { backStackEntry ->
            val oobCode = backStackEntry.arguments?.getString("oobCode") ?: ""
            ResetPasswordScreen(
                oobCode = oobCode,
                authViewModel = authViewModel,
                onNavigateToLogin = {
                    authViewModel.clearResetPasswordState()
                    navController.navigate(ScreenRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

