package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.data.model.StudentProfile
import com.example.data.model.UserRecord
import com.example.data.repository.HostelRepository
import com.example.ui.admin.AdminBillsManagementScreen
import com.example.ui.admin.AdminComplaintsScreen
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.admin.AdminRoomManagementScreen
import com.example.ui.admin.AdminSmsScreen
import com.example.ui.admin.AdminStudentManagementScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.components.AvatarInitials
import com.example.ui.student.StudentBillsScreen
import com.example.ui.student.StudentComplaintsScreen
import com.example.ui.student.StudentDashboardScreen
import com.example.ui.student.StudentNotificationsScreen
import com.example.ui.student.StudentProfileScreen
import com.example.ui.student.StudentRoomSelectionScreen
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.KalpataruHostelTheme
import com.example.ui.theme.NavyLight
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KalpataruHostelTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val repository = remember { HostelRepository(context) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        // Seed initial hostel rooms if empty
        repository.initializeDefaultRoomsIfEmpty()
    }

    if (currentUser == null) {
        AuthScreen(
            repository = repository,
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        AppContent(
            userId = currentUser!!.uid,
            userEmail = currentUser!!.email ?: "",
            repository = repository,
            onSignOut = {
                val credManager = CredentialManager.create(context)
                Firebase.auth.signOut()
                coroutineScope.launch {
                    try {
                        credManager.clearCredentialState(ClearCredentialStateRequest())
                    } catch (_: Exception) {}
                    currentUser = null
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(
    userId: String,
    userEmail: String,
    repository: HostelRepository,
    onSignOut: () -> Unit
) {
    var userRecord by remember { mutableStateOf<UserRecord?>(null) }
    var isLoadingUser by remember { mutableStateOf(true) }
    val studentProfile by repository.observeStudentProfile(userId).collectAsState(initial = null)

    LaunchedEffect(userId) {
        val record = repository.getUserRecord(userId)
        userRecord = record ?: UserRecord(id = userId, email = userEmail, role = "student")
        isLoadingUser = false
    }

    if (isLoadingUser) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = NavyPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Connecting to Hostel Portal...", color = Slate600)
            }
        }
        return
    }

    val role = userRecord?.role ?: "student"
    val isAdmin = role == "admin"

    // Navigation states
    // Student screens: "dashboard", "profile", "rooms", "bills", "complaints", "notifications"
    // Admin screens: "admin_dashboard", "admin_students", "admin_rooms", "admin_bills", "admin_complaints", "admin_sms"
    var currentScreen by remember(isAdmin) {
        mutableStateOf(if (isAdmin) "admin_dashboard" else "dashboard")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "KALPATARU BOYS HOSTEL",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = if (isAdmin) "Administrator Console" else (studentProfile?.fullName ?: "Resident Portal"),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (!isAdmin) {
                // Student Bottom Navigation Bar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == "dashboard",
                        onClick = { currentScreen = "dashboard" },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Home", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "profile",
                        onClick = { currentScreen = "profile" },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "rooms",
                        onClick = { currentScreen = "rooms" },
                        icon = { Icon(Icons.Default.MeetingRoom, contentDescription = "Rooms") },
                        label = { Text("Room", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "bills",
                        onClick = { currentScreen = "bills" },
                        icon = { Icon(Icons.Default.Payment, contentDescription = "Bills") },
                        label = { Text("Bills", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "complaints",
                        onClick = { currentScreen = "complaints" },
                        icon = { Icon(Icons.Default.ReportProblem, contentDescription = "Complaints") },
                        label = { Text("Issues", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "notifications",
                        onClick = { currentScreen = "notifications" },
                        icon = { Icon(Icons.Default.Notifications, contentDescription = "Notices") },
                        label = { Text("Alerts", fontSize = 11.sp) }
                    )
                }
            } else {
                // Admin Bottom Navigation Bar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == "admin_dashboard",
                        onClick = { currentScreen = "admin_dashboard" },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Overview") },
                        label = { Text("Overview", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "admin_students",
                        onClick = { currentScreen = "admin_students" },
                        icon = { Icon(Icons.Default.Group, contentDescription = "Students") },
                        label = { Text("Students", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "admin_rooms",
                        onClick = { currentScreen = "admin_rooms" },
                        icon = { Icon(Icons.Default.MeetingRoom, contentDescription = "Rooms") },
                        label = { Text("Rooms", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "admin_bills",
                        onClick = { currentScreen = "admin_bills" },
                        icon = { Icon(Icons.Default.Payment, contentDescription = "Bills") },
                        label = { Text("Bills", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "admin_complaints",
                        onClick = { currentScreen = "admin_complaints" },
                        icon = { Icon(Icons.Default.ReportProblem, contentDescription = "Complaints") },
                        label = { Text("Issues", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "admin_sms",
                        onClick = { currentScreen = "admin_sms" },
                        icon = { Icon(Icons.Default.Sms, contentDescription = "SMS") },
                        label = { Text("SMS", fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                // Student Portal
                "dashboard" -> StudentDashboardScreen(
                    studentProfile = studentProfile,
                    repository = repository,
                    onNavigateToProfile = { currentScreen = "profile" },
                    onNavigateToRoomSelection = { currentScreen = "rooms" },
                    onNavigateToBills = { currentScreen = "bills" },
                    onNavigateToComplaints = { currentScreen = "complaints" }
                )
                "profile" -> StudentProfileScreen(
                    currentProfile = studentProfile,
                    repository = repository,
                    onSaveSuccess = { currentScreen = "dashboard" }
                )
                "rooms" -> StudentRoomSelectionScreen(
                    studentProfile = studentProfile,
                    repository = repository,
                    onNavigateToProfile = { currentScreen = "profile" }
                )
                "bills" -> StudentBillsScreen(
                    studentProfile = studentProfile,
                    repository = repository
                )
                "complaints" -> StudentComplaintsScreen(
                    studentProfile = studentProfile,
                    repository = repository
                )
                "notifications" -> StudentNotificationsScreen(
                    studentProfile = studentProfile,
                    repository = repository
                )

                // Administrator Portal
                "admin_dashboard" -> AdminDashboardScreen(repository = repository)
                "admin_students" -> AdminStudentManagementScreen(repository = repository)
                "admin_rooms" -> AdminRoomManagementScreen(repository = repository)
                "admin_bills" -> AdminBillsManagementScreen(repository = repository)
                "admin_complaints" -> AdminComplaintsScreen(repository = repository)
                "admin_sms" -> AdminSmsScreen(repository = repository)
                else -> StudentDashboardScreen(
                    studentProfile = studentProfile,
                    repository = repository,
                    onNavigateToProfile = { currentScreen = "profile" },
                    onNavigateToRoomSelection = { currentScreen = "rooms" },
                    onNavigateToBills = { currentScreen = "bills" },
                    onNavigateToComplaints = { currentScreen = "complaints" }
                )
            }
        }
    }
}
