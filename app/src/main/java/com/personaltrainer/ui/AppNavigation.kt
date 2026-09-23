package com.personaltrainer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.personaltrainer.ui.onboarding.OnboardingScreen
import com.personaltrainer.ui.onboarding.OnboardingViewModel
import com.personaltrainer.ui.today.TodayScreen

/**
 * Root navigation graph.
 *
 * Route hierarchy:
 *   onboarding   — shown once until onboarding is complete
 *   main         — bottom-nav host (today, log, progress, plan, settings)
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val onboardingVm: OnboardingViewModel = hiltViewModel()
    val isOnboardingComplete by onboardingVm.isOnboardingComplete.collectAsState()

    val startDestination = if (isOnboardingComplete == true) Route.MAIN else Route.ONBOARDING

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Route.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Route.MAIN) {
                        popUpTo(Route.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.MAIN) {
            MainScreen()
        }
    }
}

object Route {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val TODAY = "today"
    const val LOG = "log"
    const val PROGRESS = "progress"
    const val PLAN = "plan"
    const val SETTINGS = "settings"
    const val CHAT = "chat"
    const val WORKOUT_ACTIVE = "workout_active/{sessionId}"
    const val LOG_MEAL = "log_meal/{date}/{slot}"
}
