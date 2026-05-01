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

import com.tunegocio.homefix.ui.client.NewRequestScreen

import com.tunegocio.homefix.ui.technician.RequestDetailScreen

import com.tunegocio.homefix.ui.shared.ProfileScreen
import com.tunegocio.homefix.ui.shared.SettingsScreen
import com.tunegocio.homefix.ui.client.TechnicianListScreen

import com.tunegocio.homefix.ui.client.RequestTrackingScreen

import com.tunegocio.homefix.ui.shared.RatingScreen
import com.tunegocio.homefix.ui.shared.HistoryScreen
import com.tunegocio.homefix.ui.technician.EarningsScreen

import com.tunegocio.homefix.ui.auth.VerificarEmailScreen
import com.tunegocio.homefix.ui.auth.OlvideContrasenaScreen


@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH


    ) {

        // Dentro del NavHost agrega:
        composable(Routes.NEW_REQUEST) {
            NewRequestScreen(navController = navController)
        }


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

        // Dentro del NavHost agrega:
        composable(Routes.REQUEST_DETAIL) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            RequestDetailScreen(
                navController = navController,
                requestId = requestId
            )
        }



        composable(Routes.PROFILE) {
            ProfileScreen(navController = navController)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController)
        }
        composable(Routes.EARNINGS) {
            // Por ahora pantalla vacía — la haremos después
        }
        composable(Routes.HISTORY) {
            // Por ahora pantalla vacía — la haremos después
        }

        // LO HACE ASHLEY
        composable(Routes.TECHNICIAN_LIST) {  // LO HARA ASHLEY
            TechnicianListScreen(navController = navController)
        }

        composable(Routes.REQUEST_TRACKING) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            RequestTrackingScreen(
                navController = navController,
                requestId = requestId
            )
        }

        // DESPUES DE MEJORAR REGISTRO SON ESTAS VISTAS

        composable(Routes.RATING) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            RatingScreen(navController = navController, requestId = requestId)
        }

        composable(Routes.HISTORY) {
            HistoryScreen(navController = navController)
        }

        composable(Routes.EARNINGS) {
            EarningsScreen(navController = navController)
        }

        // VERFICAR EN EL CORREO Y RECUPERACION DE CONTRASENA

        composable(Routes.VERIFICAR_EMAIL) {
            VerificarEmailScreen(navController = navController)
        }
        composable(Routes.OLVIDE_CONTRASENA) {
            OlvideContrasenaScreen(navController = navController)
        }


    }
}