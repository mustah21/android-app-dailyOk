package com.example.dailyok.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.navigation.NavHostController
import com.example.dailyok.R

import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.graphics.Color
import com.example.dailyok.ui.theme.ForestGreen
import com.example.dailyok.ui.theme.MintBg
import com.example.dailyok.ui.theme.SecondaryText

/** The pages reachable from the bottom bar, in order. */
private data class NavTab(val route: String, val label: String, val icon: @Composable () -> Painter)

private val tabs = listOf(
    NavTab(Routes.HOME, "Home") { rememberVectorPainter(Icons.Filled.Home) },
    NavTab(Routes.MEDS, "Medicines") { painterResource(R.drawable.ic_nav_meds) },
)

/** Routes that show the bottom bar. Welcome and "I need help" stay full screen. */
val bottomBarRoutes = tabs.map { it.route }.toSet()

/** Bottom navigation: Home, Medicines. Large icons and labels for easy tapping. */
@Composable
fun BottomNavBar(nav: NavHostController, currentRoute: String?) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        tabs.forEach { tab ->
            val isSelected = currentRoute == tab.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { nav.navigateToTab(tab.route) },
                icon = { Icon(tab.icon(), contentDescription = null, modifier = Modifier.size(28.dp)) },
                label = { Text(tab.label, style = MaterialTheme.typography.titleSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ForestGreen,
                    selectedTextColor = ForestGreen,
                    indicatorColor = MintBg,
                    unselectedIconColor = SecondaryText,
                    unselectedTextColor = SecondaryText,
                ),
            )
        }
    }
}

/** Switches tab without piling up screens: Back from any tab returns to Home. */
fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
