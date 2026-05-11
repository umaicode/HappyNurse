// WearNavGraph — 워치 NavGraph. SwipeDismissableNavHost 위에 화면 라우트 구성.
// 녹음(RecordScreen) 과 결과 확인(SttResultScreen) 은 같은 RecordViewModel 인스턴스를 공유해야 하므로
// Activity ViewModelStoreOwner 를 통해 hiltViewModel 을 가져온다.
package com.happynurse.wear.presentation.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import com.happynurse.wear.presentation.screens.detail.IvProgressScreen
import com.happynurse.wear.presentation.screens.detail.SttTimerDetailScreen
import com.happynurse.wear.presentation.screens.home.HomeViewModel
import com.happynurse.wear.presentation.screens.pager.HomeRecordPager
import com.happynurse.wear.presentation.screens.record.RecordViewModel
import com.happynurse.wear.presentation.screens.stt.SttResultScreen

@Composable
fun WearNavGraph(
    navController: NavHostController,
    autoStartRecord: Boolean = false,
    onAutoStartConsumed: () -> Unit = {},
) {
    val activity = LocalContext.current as ComponentActivity
    val recordViewModel: RecordViewModel = hiltViewModel(viewModelStoreOwner = activity)

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = WearRoute.HomePager.path,
    ) {
        composable(WearRoute.HomePager.path) {
            HomeRecordPager(
                recordViewModel = recordViewModel,
                onIvClick = { iv -> navController.navigate(WearRoute.IvProgress.build(iv.ivInfusionId)) },
                onSttClick = { stt -> navController.navigate(WearRoute.SttTimerDetail.build(stt.sttTimerId)) },
                onShowResult = {
                    navController.navigate(WearRoute.SttResult.path) {
                        launchSingleTop = true
                    }
                },
                autoStartRecord = autoStartRecord,
                onAutoStartConsumed = onAutoStartConsumed,
            )
        }
        composable(WearRoute.SttResult.path) {
            SttResultScreen(
                viewModel = recordViewModel,
                onSubmitted = {
                    navController.navigate(WearRoute.HomePager.path) {
                        popUpTo(WearRoute.HomePager.path) { inclusive = true }
                    }
                },
                onCancel = {
                    navController.popBackStack(WearRoute.HomePager.path, inclusive = false)
                },
            )
        }
        composable(
            route = WearRoute.IvProgress.path,
            arguments = listOf(navArgument(WearRoute.IvProgress.ARG_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong(WearRoute.IvProgress.ARG_ID) ?: -1L
            val homeViewModel: HomeViewModel = hiltViewModel()
            val state by homeViewModel.state.collectAsStateWithLifecycle()
            val iv = state.ivList.firstOrNull { it.ivInfusionId == id }
            if (iv != null) {
                IvProgressScreen(iv = iv, onBack = { navController.popBackStack() })
            }
        }
        composable(
            route = WearRoute.SttTimerDetail.path,
            arguments = listOf(navArgument(WearRoute.SttTimerDetail.ARG_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString(WearRoute.SttTimerDetail.ARG_ID).orEmpty()
            val homeViewModel: HomeViewModel = hiltViewModel()
            val state by homeViewModel.state.collectAsStateWithLifecycle()
            val stt = state.sttList.firstOrNull { it.sttTimerId == id }
            if (stt != null) {
                SttTimerDetailScreen(stt = stt, onBack = { navController.popBackStack() })
            }
        }
    }
}
