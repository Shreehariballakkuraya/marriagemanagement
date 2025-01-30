package com.hari.management.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hari.management.screens.*
import com.hari.management.viewmodel.GuestViewModel

@Composable
fun NavigationGraph(navController: NavHostController, viewModel: GuestViewModel) {
    var showSplash by remember { mutableStateOf(true) }

    NavHost(
        navController = navController,
        startDestination = if (showSplash) Screen.Splash.route else Screen.GuestList.route
    ) {
        composable(
            route = Screen.Splash.route,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            SplashScreen(
                onSplashComplete = {
                    showSplash = false
                    navController.navigate(Screen.GuestList.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.GuestList.route,
            enterTransition = { 
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(500)
                ) + fadeIn()
            }
        ) {
            GuestListScreen(navController, viewModel)
        }
        
        composable(
            route = Screen.AddGuest.route,
            enterTransition = { 
                slideInHorizontally(initialOffsetX = { it }) + fadeIn() 
            },
            exitTransition = { 
                slideOutHorizontally(targetOffsetX = { it }) + fadeOut() 
            }
        ) {
            AddGuestScreen(navController, viewModel)
        }
        
        composable(
            route = Screen.GuestDetail.route,
            arguments = listOf(
                navArgument("guestId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            GuestDetailScreen(
                navController,
                viewModel,
                backStackEntry.arguments?.getInt("guestId") ?: return@composable
            )
        }
        
        composable(Screen.ManageCategories.route) {
            ManageCategoriesScreen(navController, viewModel)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable(Screen.EmailSettings.route) {
            EmailSettingsScreen(navController)
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen(navController, viewModel)
        }

        composable(Screen.BulkOperations.route) {
            BulkOperationsScreen(navController, viewModel)
        }

        composable(Screen.AdminSettings.route) {
            AdminSettingsScreen(navController)
        }
    }
} 