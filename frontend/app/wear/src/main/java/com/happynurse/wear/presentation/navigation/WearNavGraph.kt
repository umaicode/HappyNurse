package com.happynurse.wear.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import com.happynurse.wear.presentation.screens.main.MainScreen
import com.happynurse.wear.presentation.screens.nfc.MedicationTagScreen
import com.happynurse.wear.presentation.screens.nfc.PatientTagScreen
import com.happynurse.wear.presentation.screens.notification.NotificationScreen
import com.happynurse.wear.presentation.screens.recording.RecordingScreen

object WearRoute {
    const val MAIN = "main"
    const val PATIENT_TAG = "patient_tag"
    const val MEDICATION_TAG = "medication_tag"
    const val RECORDING = "recording"
    const val NOTIFICATION = "notification"
}

@Composable
fun WearNavGraph(navController: NavHostController) {
    SwipeDismissableNavHost(
        navController = navController,
        startDestination = WearRoute.MAIN
    ) {
        composable(WearRoute.MAIN) { MainScreen(navController) }
        composable(WearRoute.PATIENT_TAG) { PatientTagScreen(navController) }
        composable(WearRoute.MEDICATION_TAG) { MedicationTagScreen(navController) }
        composable(WearRoute.RECORDING) { RecordingScreen(navController) }
        composable(WearRoute.NOTIFICATION) { NotificationScreen(navController) }
    }
}
