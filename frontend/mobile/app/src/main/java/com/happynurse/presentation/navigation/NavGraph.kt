package com.happynurse.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.happynurse.presentation.screens.handover.HandoverScreen
import com.happynurse.presentation.screens.journal.JournalScreen
import com.happynurse.presentation.screens.login.LoginScreen
import com.happynurse.presentation.screens.mypage.MyPageScreen
import com.happynurse.presentation.screens.nfc.NfcWriteScreen
import com.happynurse.presentation.screens.notification.NotificationSettingScreen
import com.happynurse.presentation.screens.order.DoctorOrderScreen
import com.happynurse.presentation.screens.patient.PatientDetailScreen
import com.happynurse.presentation.screens.patient.PatientListScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "login"
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
    }
}
