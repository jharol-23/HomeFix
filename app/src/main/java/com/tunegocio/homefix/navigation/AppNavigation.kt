package com.tunegocio.homefix.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tunegocio.homefix.ui.auth.SplashScreen
import com.tunegocio.homefix.ui.auth.LoginScreen
import com.tunegocio.homefix.ui.auth.RegisterScreen

import com.tunegocio.homefix.ui.client.HomeClientScreen
import com.tunegocio.homefix.ui.technician.HomeTechnicianScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        // Auth
        composable(Routes.SPLASH) {
            SplashScreen(navController = navController)
        }
        composable(Routes.LOGIN) {
            LoginScreen(navController = navController)
        }
        composable(Routes.REGISTER) {
            RegisterScreen(navController = navController)
        }

        // Cliente — se agregarán después
        composable(Routes.HOME_CLIENT) {
            HomeClientScreen(navController = navController)
        }

        // Técnico — se agregarán después
        composable(Routes.HOME_TECHNICIAN) {
            HomeTechnicianScreen(navController = navController)
        }

    }
}