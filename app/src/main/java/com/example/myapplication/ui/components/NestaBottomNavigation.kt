package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.UserRole
import com.example.myapplication.ui.theme.*

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    object Home : BottomNavItem("home", "Home", Icons.Default.Home, Icons.Default.Home)
    object Search : BottomNavItem("search", "Search", Icons.Default.Search, Icons.Default.Search)
    object Saved : BottomNavItem("saved", "Saved", Icons.Outlined.FavoriteBorder, Icons.Default.Favorite)
    object Messages : BottomNavItem("messages", "Messages", Icons.Default.Email, Icons.Default.Email)
    object AgentDashboard : BottomNavItem("agent_dashboard", "Listings", Icons.Default.List, Icons.Default.List)
    object AdminModeration : BottomNavItem("admin_moderation", "Moderate", Icons.Default.Settings, Icons.Default.Settings)
    object Profile : BottomNavItem("profile", "Profile", Icons.Default.Person, Icons.Default.Person)
}

@Composable
fun NestaBottomNavigation(
    currentRoute: String,
    currentUserRole: UserRole,
    onNavigate: (String) -> Unit
) {
    val items = mutableListOf<BottomNavItem>(
        BottomNavItem.Home,
        BottomNavItem.Search,
        BottomNavItem.Saved,
        BottomNavItem.Messages
    )

    when (currentUserRole) {
        UserRole.SEEKER -> items.add(BottomNavItem.Profile)
        UserRole.AGENT -> items.add(BottomNavItem.AgentDashboard)
        UserRole.ADMIN -> items.add(BottomNavItem.AdminModeration)
    }

    NavigationBar(
        containerColor = WarmOffWhite,
        contentColor = DarkCharcoal,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = WarmOffWhite,
                    selectedTextColor = DeepNavy,
                    indicatorColor = DeepNavy,
                    unselectedIconColor = MutedText,
                    unselectedTextColor = MutedText
                )
            )
        }
    }
}
