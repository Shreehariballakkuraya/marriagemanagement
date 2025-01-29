package com.hari.management.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hari.management.screens.*
import com.hari.management.viewmodel.GuestViewModel

@Composable
fun NavigationGraph(navController: NavHostController, viewModel: GuestViewModel) {
    NavHost(
        navController = navController,
        startDestination = Screen.GuestList.route
    ) {
        composable(Screen.GuestList.route) {
            GuestListScreen(navController, viewModel)
        }
        
        composable(Screen.AddGuest.route) {
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
            SettingsScreen()
        }
    }
} 