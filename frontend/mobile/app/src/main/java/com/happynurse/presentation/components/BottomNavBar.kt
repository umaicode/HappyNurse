// 4개 탭(환자/일지/인계/의사오더)을 가진 하단 네비게이션 바
package com.happynurse.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

enum class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    Patient("patient_list", "환자", Icons.Filled.People),
    Journal("journal", "일지", Icons.Filled.Book),
    Handover("handover", "인계", Icons.Filled.SwapHoriz),
    Order("doctor_order", "의사오더", Icons.AutoMirrored.Filled.Assignment)
}

@Composable
fun BottomNavBar(navController: NavController) {
    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry.value?.destination?.route ?: ""

    NavigationBar {
        BottomTab.values().forEach { tab ->
            val selected = currentRoute.startsWith(tab.route)
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        val target = when (tab) {
                            BottomTab.Journal -> "journal/1"
                            BottomTab.Order -> "doctor_order/1"
                            else -> tab.route
                        }
                        navController.navigate(target) {
                            popUpTo("patient_list") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}
