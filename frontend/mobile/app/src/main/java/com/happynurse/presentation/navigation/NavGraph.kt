// 폰 앱 네비게이션 그래프 — login → main(4탭) + 모달 라우트(상세/NFC 플로우)
package com.happynurse.presentation.navigation

import androidx.compose.runtime.Composable
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
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = NavRoutes.LOGIN) {
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
            PatientDetailScreen(patientId = id, onBack = { navController.popBackStack() })
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
