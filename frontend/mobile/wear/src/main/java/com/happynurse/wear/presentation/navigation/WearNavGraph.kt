// WearNavGraph — 워치 NavGraph. SwipeDismissableNavHost 위에 화면 라우트 구성.
// 녹음(s11)은 RecordScreen 내 인라인 처리로 바뀌었으므로 SttRecording 라우트 없음.
// s09/s13 풀스크린 알람은 Activity 로 별도 진입하므로 NavGraph 에 포함하지 않는다.
package com.happynurse.wear.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import com.happynurse.wear.data.model.MockData
import com.happynurse.wear.data.notification.WearSttCreatePayload
import com.happynurse.wear.presentation.screens.detail.IvProgressScreen
import com.happynurse.wear.presentation.screens.detail.SttTimerDetailScreen
import com.happynurse.wear.presentation.screens.pager.HomeRecordPager
import com.happynurse.wear.presentation.screens.stt.SttResultScreen
import com.happynurse.wear.presentation.screens.stt.SttResultViewModel

@Composable
fun WearNavGraph(navController: NavHostController) {
    SwipeDismissableNavHost(
        navController = navController,
        startDestination = WearRoute.HomePager.path,
    ) {
        composable(WearRoute.HomePager.path) {
            HomeRecordPager(
                onIvClick = { iv -> navController.navigate(WearRoute.IvProgress.build(iv.ivInfusionId)) },
                onSttClick = { stt -> navController.navigate(WearRoute.SttTimerDetail.build(stt.sttTimerId)) },
                onReqClick = { /* TODO: 환자 상세 화면 — 추후 정의 */ },
                onRecordingComplete = {
                    navController.navigate(WearRoute.SttResult.path) {
                        popUpTo(WearRoute.HomePager.path)
                    }
                },
            )
        }
        composable(WearRoute.SttResult.path) {
            val sample = MockData.sttList.first()
            val viewModel: SttResultViewModel = hiltViewModel()
            SttResultScreen(
                patientName = sample.patientName,
                roomBed = sample.patientRoomBed,
                timeDisplay = "30:00",
                sttText = sample.sttText,
                highlightStart = sample.highlightStart,
                highlightEnd = sample.highlightEnd,
                onConfirm = {
                    // 폰에 STT 타이머 등록 위임 (백엔드 timer/reminder API)
                    viewModel.submitToPhone(
                        WearSttCreatePayload(
                            patientName = sample.patientName,
                            sttText = sample.sttText,
                            contentSummary = sample.contentSummary,
                            targetEpochMillis = System.currentTimeMillis() + sample.remainingSec * 1000L,
                        ),
                    )
                    navController.navigate(WearRoute.SttTimerDetail.build(sample.sttTimerId)) {
                        popUpTo(WearRoute.HomePager.path)
                    }
                },
            )
        }
        composable(
            route = WearRoute.IvProgress.path,
            arguments = listOf(navArgument(WearRoute.IvProgress.ARG_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong(WearRoute.IvProgress.ARG_ID) ?: -1L
            val iv = remember(id) {
                MockData.ivList.firstOrNull { it.ivInfusionId == id } ?: MockData.ivList.first()
            }
            IvProgressScreen(iv = iv, onBack = { navController.popBackStack() })
        }
        composable(
            route = WearRoute.SttTimerDetail.path,
            arguments = listOf(navArgument(WearRoute.SttTimerDetail.ARG_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString(WearRoute.SttTimerDetail.ARG_ID).orEmpty()
            val stt = remember(id) {
                MockData.sttList.firstOrNull { it.sttTimerId == id } ?: MockData.sttList.first()
            }
            SttTimerDetailScreen(stt = stt, onBack = { navController.popBackStack() })
        }
    }
}
