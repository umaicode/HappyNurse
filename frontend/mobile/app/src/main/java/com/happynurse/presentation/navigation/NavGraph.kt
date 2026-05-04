// 폰 앱 네비게이션 그래프 — login을 시작점으로 라우트 정의 (NFC 진입은 별도 라우트)
package com.happynurse.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.happynurse.presentation.screens.handover.HandoverScreen
import com.happynurse.presentation.screens.journal.JournalScreen
import com.happynurse.presentation.screens.login.LoginScreen
import com.happynurse.presentation.screens.mypage.MyPageScreen
import com.happynurse.presentation.screens.nfc.NfcEntryScreen
import com.happynurse.presentation.screens.nfc.NfcWriteScreen
import com.happynurse.presentation.screens.notification.NotificationSettingScreen
import com.happynurse.presentation.screens.order.DoctorOrderScreen
import com.happynurse.presentation.screens.patient.PatientDetailScreen
import com.happynurse.presentation.screens.patient.PatientListScreen
import kotlinx.coroutines.delay

@Composable
fun NavGraph(
    navController: NavHostController,
    nfcToken: String? = null,
) {
    NavHost(
        navController = navController,
        startDestination = "login",
    ) {
        composable("login") { LoginScreen(navController) }
        composable("patient_list") { PatientListScreen(navController) }
        composable("patient_detail/{id}") { PatientDetailScreen(navController) }
        composable("journal") { JournalScreen(navController) }
        composable("journal/{patientId}") { JournalScreen(navController) }
        composable("handover") { HandoverScreen(navController) }
        composable("doctor_order") { DoctorOrderScreen(navController) }
        composable("doctor_order/{patientId}") { DoctorOrderScreen(navController) }
        composable("mypage") { MyPageScreen(navController) }
        composable("notification_setting") { NotificationSettingScreen(navController) }
        composable("nfc_write") { NfcWriteScreen(navController) }
        composable(
            route = "nfc_entry/{token}",
            arguments = listOf(navArgument("token") { type = NavType.StringType }),
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: ""
            NfcEntryScreen(navController, token)
        }
    }

    // NavHost 초기화 후 NFC 진입 라우트로 이동 (호출부 인텐트에서 token 받았을 때)
    LaunchedEffect(nfcToken) {
        if (nfcToken != null) {
            delay(100)
            navController.navigate("nfc_entry/$nfcToken")
        }
    }
}
