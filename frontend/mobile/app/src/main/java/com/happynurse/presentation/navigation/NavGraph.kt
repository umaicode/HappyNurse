// 폰 앱 네비게이션 그래프 — login → main(4탭) + 모달 라우트(상세/NFC 플로우)
package com.happynurse.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.ui.unit.IntSize
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
import com.happynurse.presentation.screens.ivtimer.IVTimerActiveScreen
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
    // 토큰 결정 후 양방향 진입 분기. null = 결정 전(빈 화면 유지), true → MAIN, false → LOGIN
    LaunchedEffect(loggedIn) {
        when (loggedIn) {
            null -> Unit
            true -> if (navController.currentDestination?.route != NavRoutes.MAIN) {
                navController.navigate(NavRoutes.MAIN) { popUpTo(0) { inclusive = true } }
            }
            false -> if (navController.currentDestination?.route != NavRoutes.LOGIN) {
                navController.navigate(NavRoutes.LOGIN) { popUpTo(0) { inclusive = true } }
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
        sizeTransform = { SizeTransform(clip = false) { _, _ -> snap<IntSize>() } },
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
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
            sizeTransform = { SizeTransform(clip = false) { _, _ -> snap<IntSize>() } },
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
        composable(
            route = NavRoutes.NFC_PATIENT,
            arguments = listOf(
                navArgument("token") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { entry ->
            val token = entry.arguments?.getString("token")
            NfcPatientScreen(
                token = token,
                onClose = { navController.popBackStack() },
                onLog = { patientId, encounterId ->
                    navController.navigate(NavRoutes.logEntry(patientId, encounterId))
                },
                onDrug = { patientId, encounterId ->
                    navController.navigate(NavRoutes.drugEntry(patientId, encounterId))
                },
                onIv = {
                    navController.navigate(NavRoutes.ivTimerActive())
                },
            )
        }
        composable(
            route = NavRoutes.LOG_ENTRY,
            arguments = listOf(
                navArgument("patientId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("encounterId") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { entry ->
            val patientId = entry.arguments?.getLong("patientId") ?: -1L
            val encounterId = entry.arguments?.getLong("encounterId") ?: -1L
            LogEntryScreen(
                patientId = patientId,
                encounterId = encounterId,
                onClose = { navController.popBackStack() },
            )
        }
        composable(
            route = NavRoutes.DRUG_ENTRY,
            arguments = listOf(
                navArgument("patientId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("encounterId") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { entry ->
            val patientId = entry.arguments?.getLong("patientId") ?: -1L
            val encounterId = entry.arguments?.getLong("encounterId") ?: -1L
            DrugEntryScreen(
                patientId = patientId,
                encounterId = encounterId,
                onClose = { navController.popBackStack() },
                onTimer = { encId, orderIds ->
                    navController.navigate(NavRoutes.ivTimerSetup(encId, orderIds))
                },
            )
        }
        composable(
            route = NavRoutes.IV_TIMER_SETUP,
            arguments = listOf(
                navArgument("encounterId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("orderIds") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { entry ->
            val encId = entry.arguments?.getLong("encounterId") ?: -1L
            val ids = entry.arguments?.getString("orderIds")
                ?.split(",")
                ?.mapNotNull { it.toLongOrNull() }
                ?: emptyList()
            IVTimerSetupScreen(
                encounterId = encId,
                medicationOrderIds = ids,
                onClose = { navController.popBackStack() },
                onActive = { ivInfusionId ->
                    navController.navigate(NavRoutes.ivTimerActive(ivInfusionId)) {
                        popUpTo(NavRoutes.IV_TIMER_SETUP) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = NavRoutes.IV_TIMER_ACTIVE,
            arguments = listOf(
                navArgument("ivInfusionId") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { entry ->
            val ivInfusionId = entry.arguments?.getLong("ivInfusionId") ?: -1L
            IVTimerActiveScreen(
                ivInfusionId = ivInfusionId,
                onClose = { navController.popBackStack() },
            )
        }
    }
}
