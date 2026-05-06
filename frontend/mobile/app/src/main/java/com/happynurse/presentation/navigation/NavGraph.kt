// 폰 앱 네비게이션 그래프 — login → main(4탭) + 모달 라우트(상세/NFC 플로우)
package com.happynurse.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.happynurse.presentation.screens.drugentry.DrugEntryScreen
import com.happynurse.presentation.screens.ivtimer.IVTimerSetupScreen
import com.happynurse.presentation.screens.login.LoginScreen
import com.happynurse.presentation.screens.logentry.LogEntryScreen
import com.happynurse.presentation.screens.main.MainScaffold
import com.happynurse.presentation.screens.nfc.NfcPatientScreen
import com.happynurse.presentation.screens.patientdetail.PatientDetailScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val loggedIn by sessionViewModel.isLoggedIn.collectAsStateWithLifecycle()
    LaunchedEffect(loggedIn) {
        if (!loggedIn && navController.currentDestination?.route != NavRoutes.LOGIN) {
            navController.navigate(NavRoutes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    NavHost(
        navController = navController,
        startDestination = NavRoutes.LOGIN,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable(NavRoutes.LOGIN) {
            LoginScreen(onLoggedIn = {
                navController.navigate(NavRoutes.MAIN) {
                    popUpTo(NavRoutes.LOGIN) { inclusive = true }
                }
            })
        }
        composable(NavRoutes.MAIN) {
            MainScaffold(
                onOpenPatient = { id -> navController.navigate(NavRoutes.patientDetail(id)) },
                onOpenNFC = { navController.navigate(NavRoutes.NFC_PATIENT) },
                onLogout = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.MAIN) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = NavRoutes.PATIENT_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: ""
            PatientDetailScreen(
                patientId = id,
                onBack = { navController.popBackStack() },
                onSelectPatient = { newId ->
                    navController.navigate(NavRoutes.patientDetail(newId)) {
                        popUpTo(NavRoutes.PATIENT_DETAIL) { inclusive = true }
                    }
                },
            )
        }
        composable(NavRoutes.NFC_PATIENT) {
            NfcPatientScreen(
                onClose = { navController.popBackStack() },
                onLog = { navController.navigate(NavRoutes.LOG_ENTRY) },
                onDrug = { navController.navigate(NavRoutes.DRUG_ENTRY) },
            )
        }
        composable(NavRoutes.LOG_ENTRY) {
            LogEntryScreen(onClose = { navController.popBackStack() })
        }
        composable(NavRoutes.DRUG_ENTRY) {
            DrugEntryScreen(
                onClose = { navController.popBackStack() },
                onTimer = { navController.navigate(NavRoutes.IV_TIMER_SETUP) },
            )
        }
        composable(NavRoutes.IV_TIMER_SETUP) {
            IVTimerSetupScreen(onClose = { navController.popBackStack() })
        }
    }
}
