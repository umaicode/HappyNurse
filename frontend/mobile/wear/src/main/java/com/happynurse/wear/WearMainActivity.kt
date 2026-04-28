// 워치 단일 Activity — SwipeDismissable NavController로 WearNavGraph 호스팅
package com.happynurse.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.happynurse.wear.presentation.navigation.WearNavGraph
import com.happynurse.wear.presentation.theme.HappyNurseWearTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WearMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HappyNurseWearTheme {
                // rememberSwipeDismissableNavController()는 NavHostController를 반환
                val navController = rememberSwipeDismissableNavController()
                WearNavGraph(navController = navController)
            }
        }
    }
}
