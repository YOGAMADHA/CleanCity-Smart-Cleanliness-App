package com.example.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.CleanCityRepository
import com.example.model.ComplaintCategory
import com.example.model.UiState
import com.example.model.UserRole
import com.example.ui.screens.AdminComplaintsScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CitizenHomeScreen
import com.example.ui.screens.CitizenRegistrationScreen
import com.example.ui.screens.ComplaintDetailsScreen
import com.example.ui.screens.MyComplaintsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReportComplaintScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestPrimary
import com.example.ui.theme.MintContainer
import com.example.ui.viewmodel.CleanCityViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

object CleanCityRoutes {
    const val ROUTE_CITIZEN_HOME = "citizen_home"
    const val ROUTE_MY_COMPLAINTS = "my_complaints"
    const val ROUTE_REPORT_COMPLAINT = "report_complaint?category={category}"
    const val ROUTE_ADMIN_DASHBOARD = "admin_dashboard"
    const val ROUTE_ADMIN_COMPLAINTS = "admin_complaints"
    const val ROUTE_PROFILE = "profile"
    const val ROUTE_COMPLAINT_DETAILS = "complaint_details/{complaintId}"

    fun reportComplaint(categoryName: String? = null): String =
        if (categoryName.isNullOrBlank()) "report_complaint"
        else "report_complaint?category=$categoryName"

    fun complaintDetails(complaintId: String): String = "complaint_details/$complaintId"
}

private data class NavTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun CleanCityApp(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var showSplash by remember { mutableStateOf(true) }
    var selectedPortal by remember { mutableStateOf(UserRole.CITIZEN) }
    var authErrorBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
        delay(850L)
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
        return
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            selectedPortal = selectedPortal,
            onPortalSelected = { portal ->
                selectedPortal = portal
                authErrorBanner = null
            },
            onAuthSuccess = {
                authErrorBanner = null
            },
            externalError = authErrorBanner
        )
    } else {
        key(user.uid) {
            AuthenticatedCleanCityShell(
                user = user,
                initialPortalRole = selectedPortal,
                onSignOutRequested = { message ->
                    authErrorBanner = message
                    signOut(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedCleanCityShell(
    user: FirebaseUser,
    initialPortalRole: UserRole,
    onSignOutRequested: (String?) -> Unit
) {
    val viewModel: CleanCityViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                CleanCityViewModel(
                    repository = CleanCityRepository(db),
                    currentUserId = user.uid,
                    firebaseUser = user,
                    initialPortalRole = initialPortalRole
                )
            }
        }
    )

    val adminCheckDone by viewModel.adminCheckDone.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val activeRoleMode by viewModel.activeRoleMode.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val citizenComplaintsState by viewModel.citizenComplaintsState.collectAsStateWithLifecycle()
    val adminComplaintsState by viewModel.adminComplaintsState.collectAsStateWithLifecycle()
    val actionLoading by viewModel.actionLoading.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val actionSuccess by viewModel.actionSuccess.collectAsStateWithLifecycle()

    var bypassAdminDeniedPrompt by remember { mutableStateOf(false) }

    if (!adminCheckDone || userProfileState is UiState.Loading) {
        SplashScreen()
        return
    }

    if (initialPortalRole == UserRole.ADMIN && !isAdmin && !bypassAdminDeniedPrompt) {
        AdminAccessNoticeScreen(
            email = user.email.orEmpty(),
            onContinueAsCitizen = { bypassAdminDeniedPrompt = true },
            onSignOut = {
                onSignOutRequested("Please sign in with an authorized Admin Google account to access Admin Portal.")
            }
        )
        return
    }

    val profileData = (userProfileState as? UiState.Success)?.data
    if (activeRoleMode == UserRole.CITIZEN && profileData == null && userProfileState is UiState.Success) {
        CitizenRegistrationScreen(
            initialName = user.displayName.orEmpty(),
            verifiedEmail = user.email ?: "citizen@cleancity.app",
            isLoading = actionLoading,
            errorMessage = actionError,
            onRegister = { name, phone, ward ->
                viewModel.completeCitizenRegistration(name, phone, ward)
            },
            onSignOutClick = { onSignOutRequested(null) }
        )
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionSuccess, actionError) {
        val msg = actionSuccess ?: actionError
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val citizenTabs = remember {
        listOf(
            NavTabItem(
                route = CleanCityRoutes.ROUTE_CITIZEN_HOME,
                label = "Home",
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                testTag = "nav_citizen_home"
            ),
            NavTabItem(
                route = CleanCityRoutes.ROUTE_MY_COMPLAINTS,
                label = "My Complaints",
                selectedIcon = Icons.Filled.Assignment,
                unselectedIcon = Icons.Outlined.Assignment,
                testTag = "nav_my_complaints"
            ),
            NavTabItem(
                route = CleanCityRoutes.ROUTE_PROFILE,
                label = "Profile",
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person,
                testTag = "nav_profile"
            )
        )
    }

    val adminTabs = remember {
        listOf(
            NavTabItem(
                route = CleanCityRoutes.ROUTE_ADMIN_DASHBOARD,
                label = "Dashboard",
                selectedIcon = Icons.Filled.Dashboard,
                unselectedIcon = Icons.Outlined.Dashboard,
                testTag = "nav_admin_dashboard"
            ),
            NavTabItem(
                route = CleanCityRoutes.ROUTE_ADMIN_COMPLAINTS,
                label = "Complaints",
                selectedIcon = Icons.Filled.ListAlt,
                unselectedIcon = Icons.Outlined.ListAlt,
                testTag = "nav_admin_complaints"
            ),
            NavTabItem(
                route = CleanCityRoutes.ROUTE_PROFILE,
                label = "Profile",
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person,
                testTag = "nav_profile"
            )
        )
    }

    val activeTabs = if (activeRoleMode == UserRole.ADMIN) adminTabs else citizenTabs
    val showNavigationChrome = activeTabs.any { it.route == currentRoute }

    // Switch root destination cleanly when workspace role mode toggles
    LaunchedEffect(activeRoleMode) {
        val targetHome = if (activeRoleMode == UserRole.ADMIN) {
            CleanCityRoutes.ROUTE_ADMIN_DASHBOARD
        } else {
            CleanCityRoutes.ROUTE_CITIZEN_HOME
        }
        val current = navController.currentBackStackEntry?.destination?.route
        if (current != null && current != targetHome && current != CleanCityRoutes.ROUTE_PROFILE) {
            navController.navigate(targetHome) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val startDestination = if (activeRoleMode == UserRole.ADMIN) {
        CleanCityRoutes.ROUTE_ADMIN_DASHBOARD
    } else {
        CleanCityRoutes.ROUTE_CITIZEN_HOME
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showNavigationChrome && !useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        activeTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != tab.route) {
                                        navController.navigate(tab.route) {
                                            popUpTo(startDestination) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ForestPrimary,
                                    selectedTextColor = ForestPrimary,
                                    indicatorColor = MintContainer
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showNavigationChrome && useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        activeTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != tab.route) {
                                        navController.navigate(tab.route) {
                                            popUpTo(startDestination) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .then(if (showNavigationChrome) Modifier.statusBarsPadding() else Modifier)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable(CleanCityRoutes.ROUTE_CITIZEN_HOME) {
                            CitizenHomeScreen(
                                userProfile = profileData,
                                fallbackName = user.displayName.orEmpty(),
                                complaintsState = citizenComplaintsState,
                                onReportComplaintClick = { preselectedCategory ->
                                    navController.navigate(
                                        CleanCityRoutes.reportComplaint(preselectedCategory?.name)
                                    )
                                },
                                onViewAllClick = {
                                    navController.navigate(CleanCityRoutes.ROUTE_MY_COMPLAINTS) {
                                        popUpTo(CleanCityRoutes.ROUTE_CITIZEN_HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onComplaintClick = { id ->
                                    navController.navigate(CleanCityRoutes.complaintDetails(id))
                                }
                            )
                        }

                        composable(CleanCityRoutes.ROUTE_MY_COMPLAINTS) {
                            MyComplaintsScreen(
                                complaintsState = citizenComplaintsState,
                                onComplaintClick = { id ->
                                    navController.navigate(CleanCityRoutes.complaintDetails(id))
                                },
                                onReportComplaintClick = {
                                    navController.navigate(CleanCityRoutes.reportComplaint())
                                }
                            )
                        }

                        composable(
                            route = CleanCityRoutes.ROUTE_REPORT_COMPLAINT,
                            arguments = listOf(
                                navArgument("category") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { entry ->
                            val categoryArg = entry.arguments?.getString("category")
                            val initialCategory = ComplaintCategory.entries
                                .firstOrNull { it.name == categoryArg }
                                ?: ComplaintCategory.GARBAGE_OVERFLOW

                            ReportComplaintScreen(
                                initialCategory = initialCategory,
                                isLoading = actionLoading,
                                externalError = actionError,
                                onBack = { navController.popBackStack() },
                                onSubmit = { title, category, desc, loc, lat, lon, img, onSuccess ->
                                    viewModel.submitComplaint(
                                        title = title,
                                        category = category,
                                        description = desc,
                                        locationText = loc,
                                        latitude = lat,
                                        longitude = lon,
                                        imageBase64 = img,
                                        onSubmitted = onSuccess
                                    )
                                },
                                onViewSubmittedComplaint = { docId ->
                                    navController.navigate(CleanCityRoutes.complaintDetails(docId)) {
                                        popUpTo(CleanCityRoutes.ROUTE_REPORT_COMPLAINT) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(CleanCityRoutes.ROUTE_ADMIN_DASHBOARD) {
                            AdminDashboardScreen(
                                adminName = profileData?.name?.takeIf { it.isNotBlank() }
                                    ?: user.displayName
                                    ?: "Admin",
                                complaintsState = adminComplaintsState,
                                isSeeding = actionLoading,
                                onManageAllClick = {
                                    navController.navigate(CleanCityRoutes.ROUTE_ADMIN_COMPLAINTS) {
                                        popUpTo(CleanCityRoutes.ROUTE_ADMIN_DASHBOARD) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onComplaintClick = { id ->
                                    navController.navigate(CleanCityRoutes.complaintDetails(id))
                                },
                                onSeedSampleReports = { viewModel.seedSampleReports() }
                            )
                        }

                        composable(CleanCityRoutes.ROUTE_ADMIN_COMPLAINTS) {
                            AdminComplaintsScreen(
                                complaintsState = adminComplaintsState,
                                onComplaintClick = { id ->
                                    navController.navigate(CleanCityRoutes.complaintDetails(id))
                                }
                            )
                        }

                        composable(CleanCityRoutes.ROUTE_PROFILE) {
                            ProfileScreen(
                                userProfile = profileData,
                                fallbackName = user.displayName.orEmpty(),
                                fallbackEmail = user.email.orEmpty(),
                                isAdminUser = isAdmin,
                                activeRoleMode = activeRoleMode,
                                isSaving = actionLoading,
                                onUpdateProfile = { name, phone, ward, onDone ->
                                    viewModel.updateProfile(name, phone, ward, onDone)
                                },
                                onSwitchRoleMode = { targetRole ->
                                    viewModel.switchRoleMode(targetRole)
                                    val dest = if (targetRole == UserRole.ADMIN) {
                                        CleanCityRoutes.ROUTE_ADMIN_DASHBOARD
                                    } else {
                                        CleanCityRoutes.ROUTE_CITIZEN_HOME
                                    }
                                    navController.navigate(dest) {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onLogout = { onSignOutRequested(null) }
                            )
                        }

                        composable(
                            route = CleanCityRoutes.ROUTE_COMPLAINT_DETAILS,
                            arguments = listOf(navArgument("complaintId") { type = NavType.StringType })
                        ) { entry ->
                            val complaintId = entry.arguments?.getString("complaintId").orEmpty()
                            val detailFlow = remember(complaintId) {
                                viewModel.observeComplaintDetail(complaintId)
                            }
                            val complaintDetailState by detailFlow.collectAsStateWithLifecycle()

                            ComplaintDetailsScreen(
                                complaintState = complaintDetailState,
                                isAdminMode = activeRoleMode == UserRole.ADMIN && isAdmin,
                                isUpdating = actionLoading,
                                actionError = actionError,
                                actionSuccess = actionSuccess,
                                onBack = { navController.popBackStack() },
                                onAdminUpdate = { id, history, status, remarks ->
                                    viewModel.updateComplaintByAdmin(id, history, status, remarks)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAccessNoticeScreen(
    email: String,
    onContinueAsCitizen: () -> Unit,
    onSignOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.widthIn(max = 480.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.GppBad,
                    contentDescription = "Admin access required",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Admin Access Required",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "The account \"$email\" is not registered as a CleanCity Administrator in Cloud Firestore.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onContinueAsCitizen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("continue_as_citizen_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Continue as Citizen")
                }
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_denied_signout_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Sign In With Another Account")
                }
            }
        }
    }
}
