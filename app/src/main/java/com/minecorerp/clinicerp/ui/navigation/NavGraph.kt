package com.minecorerp.clinicerp.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.minecorerp.clinicerp.ui.screens.calendar.CalendarScreen
import com.minecorerp.clinicerp.ui.screens.contacts.ContactsScreen
import com.minecorerp.clinicerp.ui.screens.dashboard.DashboardScreen
import com.minecorerp.clinicerp.ui.screens.dashboard.DashboardViewModel
import com.minecorerp.clinicerp.ui.screens.login.LoginScreen
import com.minecorerp.clinicerp.ui.screens.profile.ProfileScreen
import com.minecorerp.clinicerp.ui.theme.*
import com.minecorerp.clinicerp.ui.components.ClinicLogo

sealed class Screen(val route: String, val title: String = "", val icon: @Composable () -> Unit = {}) {
    object Login : Screen("login")
    object Main : Screen("main")
    object Profile : Screen("profile")
    
    // Bottom Tab Screens
    object Dashboard : Screen("dashboard", "المتابعة", { Icon(Icons.Default.Home, contentDescription = null) })
    object Calendar : Screen("calendar", "المواعيد", { Icon(Icons.Default.DateRange, contentDescription = null) })
    object Contacts : Screen("contacts", "جهات الاتصال", { Icon(Icons.Default.Group, contentDescription = null) })
}

@Composable
fun ClinicErpNavGraph() {
    val rootNavController = rememberNavController()
    val viewModel: DashboardViewModel = hiltViewModel()
    
    NavHost(navController = rootNavController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    viewModel.statusMessage = null
                    viewModel.loadData()
                    rootNavController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Main.route) {
            MainScaffold(
                viewModel = viewModel,
                onLogout = {
                    rootNavController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onNavigateToProfile = {
                    rootNavController.navigate(Screen.Profile.route)
                }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = viewModel,
                onBack = { rootNavController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    viewModel: DashboardViewModel,
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val items = listOf(Screen.Dashboard, Screen.Calendar, Screen.Contacts)
    var showMenu by remember { mutableStateOf(false) }
    val currentScreen = items.find { it.route == currentDestination?.route } ?: Screen.Dashboard

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentScreen.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "المزيد") }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("الحساب والإعدادات") }, onClick = { showMenu = false; onNavigateToProfile() })
                            DropdownMenuItem(text = { Text("تسجيل الخروج") }, enabled = !viewModel.isSaving, onClick = { showMenu = false; viewModel.logout(onLogout) })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar(tonalElevation = 0.dp, containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary) {
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = screen.icon,
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(when {
                        viewModel.isLoading -> "جارٍ تحديث البيانات…"
                        viewModel.error != null -> "التحديث غير مكتمل"
                        viewModel.lastRefreshAt != null -> "آخر تحديث ${viewModel.lastRefreshAt!!.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}"
                        else -> "لم يكتمل تحديث البيانات بعد"
                    }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            viewModel.statusMessage?.let { message ->
                Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Text(message, Modifier.weight(1f).padding(8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        IconButton(onClick = { viewModel.statusMessage = null }) { Icon(Icons.Default.Close, "إغلاق التأكيد") }
                    }
                }
            }
            viewModel.error?.let { message ->
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(onClick = { viewModel.loadData() }) { Text("إعادة المحاولة") }
                        IconButton(onClick = { viewModel.error = null }) { Icon(Icons.Default.Close, "إغلاق") }
                    }
                }
            }
        NavHost(
            navController = navController, 
            startDestination = Screen.Dashboard.route, 
            modifier = Modifier.weight(1f)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(viewModel = viewModel)
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(viewModel = viewModel)
            }
            composable(Screen.Contacts.route) {
                ContactsScreen(viewModel = viewModel)
            }
        }
        }
    }
}
