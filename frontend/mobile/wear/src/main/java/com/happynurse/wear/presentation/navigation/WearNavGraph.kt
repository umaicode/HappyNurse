// 워치 앱 네비게이션 — MAIN 단일 라우트 (메인 안에서 HorizontalPager 로 알림/녹음 페이지 전환)
package com.happynurse.wear.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import com.happynurse.wear.presentation.screens.main.MainScreen

object WearRoute {
    const val MAIN = "main"
}

@Composable
fun WearNavGraph(navController: NavHostController) {
    SwipeDismissableNavHost(
        navController = navController,
        startDestination = WearRoute.MAIN,
    ) {
        composable(WearRoute.MAIN) { MainScreen() }
    }
}
