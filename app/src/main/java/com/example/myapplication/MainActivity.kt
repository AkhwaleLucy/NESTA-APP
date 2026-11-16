package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.model.*
import com.example.myapplication.repository.AuthenticationState
import com.example.myapplication.repository.AuthRepository
import com.example.myapplication.repository.NestaRepository
import com.example.myapplication.ui.components.NestaBottomNavigation
import com.example.myapplication.ui.components.NestaTopAppBar
import com.example.myapplication.ui.screens.*
import com.example.myapplication.ui.theme.DeepNavy
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthRepository.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                NestaApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NestaApp(
    authViewModel: AuthViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val authState by authViewModel.authState.collectAsState()

    when (val state = authState) {
        is AuthenticationState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DeepNavy)
            }
        }

        is AuthenticationState.Unauthenticated, is AuthenticationState.Error, is AuthenticationState.EmailVerificationPending -> {
            LoginScreen(
                authViewModel = authViewModel
            )
        }

        is AuthenticationState.Authenticated -> {
            val currentUser = state.user
            val navController = rememberNavController()

            // State Flow Collectors
            val properties by NestaRepository.properties.collectAsState()
            val favoriteIds by NestaRepository.favoritePropertyIds.collectAsState()
            val compareIds by NestaRepository.comparePropertyIds.collectAsState()
            val recentlyViewedIds by NestaRepository.recentlyViewedIds.collectAsState()
            val viewingRequests by NestaRepository.viewingRequests.collectAsState()
            val conversations by NestaRepository.conversations.collectAsState()
            val messagesMap by NestaRepository.messages.collectAsState()
            val reports by NestaRepository.reports.collectAsState()
            val notifications by NestaRepository.notifications.collectAsState()
            val propertyAlerts by NestaRepository.propertyAlerts.collectAsState()

            var showNotificationsDialog by remember { mutableStateOf(false) }
            var activeSearchFilter by remember { mutableStateOf(SearchFilter()) }
            var activeConversationId by remember { mutableStateOf<String?>(null) }

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: "home"

            val favoriteProperties = remember(favoriteIds, properties) {
                properties.filter { favoriteIds.contains(it.id) }
            }

            val compareProperties = remember(compareIds, properties) {
                properties.filter { compareIds.contains(it.id) }
            }

            val recentlyViewedProperties = remember(recentlyViewedIds, properties) {
                recentlyViewedIds.mapNotNull { id -> properties.find { it.id == id } }
            }

            val agentProperties = remember(properties, currentUser) {
                properties.filter { it.ownerId == currentUser.id }
            }

            val unreadNotifsCount = notifications.count { !it.isRead }

            Scaffold(
                topBar = {
                    if (!currentRoute.startsWith("detail") && !currentRoute.startsWith("list_wizard")) {
                        NestaTopAppBar(
                            currentUserRole = currentUser.role,
                            unreadNotificationCount = unreadNotifsCount,
                            onNotificationClick = { showNotificationsDialog = true },
                            onProfileClick = { navController.navigate("profile") },
                            onSearchClick = { navController.navigate("search") }
                        )
                    }
                },
                bottomBar = {
                    if (!currentRoute.startsWith("detail") && !currentRoute.startsWith("list_wizard")) {
                        NestaBottomNavigation(
                            currentRoute = currentRoute,
                            currentUserRole = currentUser.role,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        // 1. HOME SCREEN
                        composable("home") {
                            HomeScreen(
                                properties = properties,
                                favoriteIds = favoriteIds,
                                compareIds = compareIds,
                                onPropertyClick = { prop ->
                                    NestaRepository.addRecentlyViewed(prop.id)
                                    navController.navigate("detail/${prop.id}")
                                },
                                onFavoriteToggle = { id -> NestaRepository.toggleFavorite(id) },
                                onCompareToggle = { id -> NestaRepository.toggleCompare(id) },
                                onSearchSubmit = { query, listingType ->
                                    val parsed = NestaRepository.parseNaturalSearch(query)
                                    activeSearchFilter = if (listingType != null) parsed.copy(listingType = listingType) else parsed
                                    navController.navigate("search")
                                },
                                onListPropertyClick = {
                                    if (currentUser.role == UserRole.AGENT || currentUser.role == UserRole.ADMIN) {
                                        navController.navigate("list_wizard")
                                    } else {
                                        navController.navigate("profile")
                                    }
                                }
                            )
                        }

                        // 2. SEARCH SCREEN
                        composable("search") {
                            SearchScreen(
                                initialFilter = activeSearchFilter,
                                properties = properties,
                                favoriteIds = favoriteIds,
                                compareIds = compareIds,
                                onPropertyClick = { prop ->
                                    NestaRepository.addRecentlyViewed(prop.id)
                                    navController.navigate("detail/${prop.id}")
                                },
                                onFavoriteToggle = { id -> NestaRepository.toggleFavorite(id) },
                                onCompareToggle = { id -> NestaRepository.toggleCompare(id) }
                            )
                        }

                        // 3. PROPERTY DETAIL SCREEN
                        composable(
                            route = "detail/{propertyId}",
                            arguments = listOf(navArgument("propertyId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val propId = backStackEntry.arguments?.getString("propertyId") ?: ""
                            val targetProperty = properties.find { it.id == propId }

                            if (targetProperty == null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Property unavailable or removed.", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { navController.popBackStack() },
                                            colors = ButtonDefaults.buttonColors(containerColor = DeepNavy)
                                        ) {
                                            Text("Go Back")
                                        }
                                    }
                                }
                            } else {
                                PropertyDetailScreen(
                                    property = targetProperty,
                                    isFavorite = favoriteIds.contains(targetProperty.id),
                                    isCompared = compareIds.contains(targetProperty.id),
                                    onFavoriteToggle = { NestaRepository.toggleFavorite(targetProperty.id) },
                                    onCompareToggle = { NestaRepository.toggleCompare(targetProperty.id) },
                                    onBackClick = { navController.popBackStack() },
                                    onRequestViewingSubmit = { req -> NestaRepository.sendViewingRequest(req) },
                                    onReportSubmit = { rep -> NestaRepository.submitReport(rep) },
                                    onStartChat = { prop ->
                                        coroutineScope.launch {
                                            val res = NestaRepository.getOrCreateConversation(prop)
                                            res.fold(
                                                onSuccess = { conv ->
                                                    activeConversationId = conv.id
                                                    navController.navigate("messages")
                                                },
                                                onFailure = { err ->
                                                    // Display failure error toast/alert if self-messaging or network error
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        // 4. COMPARE SCREEN
                        composable("compare") {
                            CompareScreen(
                                compareProperties = compareProperties,
                                onRemoveFromCompare = { id -> NestaRepository.toggleCompare(id) },
                                onPropertyClick = { prop -> navController.navigate("detail/${prop.id}") },
                                onBrowseClick = { navController.navigate("search") }
                            )
                        }

                        // 5. SAVED / FAVORITES SCREEN
                        composable("saved") {
                            FavoritesScreen(
                                favoriteProperties = favoriteProperties,
                                favoriteIds = favoriteIds,
                                compareIds = compareIds,
                                onFavoriteToggle = { id -> NestaRepository.toggleFavorite(id) },
                                onCompareToggle = { id -> NestaRepository.toggleCompare(id) },
                                onPropertyClick = { prop -> navController.navigate("detail/${prop.id}") },
                                onNavigateToCompare = { navController.navigate("compare") },
                                onBrowseClick = { navController.navigate("search") }
                            )
                        }

                        // 6. LIST PROPERTY WIZARD (Agents & Admins Only)
                        composable("list_wizard") {
                            if (currentUser.role == UserRole.AGENT || currentUser.role == UserRole.ADMIN) {
                                ListPropertyWizardScreen(
                                    currentAgent = currentUser,
                                    onBackClick = { navController.popBackStack() },
                                    onSubmitProperty = { newProp ->
                                        NestaRepository.addProperty(newProp)
                                        navController.navigate("agent_dashboard") {
                                            popUpTo("home")
                                        }
                                    }
                                )
                            } else {
                                LaunchedEffect(Unit) {
                                    navController.navigate("profile") { popUpTo("home") }
                                }
                            }
                        }

                        // 7. MESSAGES SCREEN
                        composable("messages") {
                            MessagesScreen(
                                conversations = conversations,
                                messagesMap = messagesMap,
                                currentUserId = currentUser.id,
                                initialConversationId = activeConversationId,
                                onSendMessageAsync = { convId, text -> NestaRepository.sendMessageAsync(convId, text) },
                                onBackToList = { activeConversationId = null }
                            )
                        }

                        // 8. AGENT DASHBOARD SCREEN (Agents Only)
                        composable("agent_dashboard") {
                            if (currentUser.role == UserRole.AGENT || currentUser.role == UserRole.ADMIN) {
                                AgentDashboardScreen(
                                    currentAgent = currentUser,
                                    agentProperties = agentProperties,
                                    viewingRequests = viewingRequests,
                                    onAddListingClick = { navController.navigate("list_wizard") },
                                    onStatusChange = { id, status -> NestaRepository.updatePropertyStatus(id, status) },
                                    onPropertyClick = { prop -> navController.navigate("detail/${prop.id}") }
                                )
                            } else {
                                LaunchedEffect(Unit) {
                                    navController.navigate("profile") { popUpTo("home") }
                                }
                            }
                        }

                        // 9. ADMIN MODERATION SCREEN (Admins Only)
                        composable("admin_moderation") {
                            val pendingAgents by NestaRepository.pendingAgents.collectAsState()
                            if (currentUser.role == UserRole.ADMIN) {
                                AdminModerationScreen(
                                    allProperties = properties,
                                    reports = reports,
                                    pendingAgents = pendingAgents,
                                    onApproveProperty = { id -> NestaRepository.approveProperty(id) },
                                    onRejectProperty = { id, reason -> NestaRepository.rejectProperty(id, reason) },
                                    onToggleVerifyProperty = { id -> NestaRepository.toggleVerifyProperty(id) },
                                    onSuspendProperty = { id -> NestaRepository.suspendProperty(id) },
                                    onApproveAgent = { id -> coroutineScope.launch { NestaRepository.approveAgent(id) } },
                                    onRejectAgent = { id, reason -> coroutineScope.launch { NestaRepository.rejectAgent(id, reason) } },
                                    onPropertyClick = { prop -> navController.navigate("detail/${prop.id}") }
                                )
                            } else {
                                LaunchedEffect(Unit) {
                                    navController.navigate("home") { popUpTo("home") }
                                }
                            }
                        }

                        // 10. PROFILE SCREEN
                        composable("profile") {
                            ProfileScreen(
                                currentUser = currentUser,
                                recentlyViewedProperties = recentlyViewedProperties,
                                propertyAlerts = propertyAlerts,
                                favoriteIds = favoriteIds,
                                compareIds = compareIds,
                                onAddAlertSubmit = { alert -> NestaRepository.addPropertyAlert(alert) },
                                onPropertyClick = { prop -> navController.navigate("detail/${prop.id}") },
                                onFavoriteToggle = { id -> NestaRepository.toggleFavorite(id) },
                                onLogoutClick = {
                                    coroutineScope.launch {
                                        AuthRepository.logout()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Notifications Dialog Overlay
            if (showNotificationsDialog) {
                AlertDialog(
                    onDismissRequest = { showNotificationsDialog = false },
                    title = { Text("Notifications Inbox", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    text = {
                        Column {
                            if (notifications.isEmpty()) {
                                Text("No notifications.", fontSize = 13.sp)
                            } else {
                                notifications.forEach { notif ->
                                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                        Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(notif.message, fontSize = 11.sp)
                                        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showNotificationsDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}
