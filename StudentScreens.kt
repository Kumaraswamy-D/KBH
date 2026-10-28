package com.example.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bill
import com.example.data.model.Complaint
import com.example.data.model.HostelConstants
import com.example.data.model.NotificationItem
import com.example.data.model.Payment
import com.example.data.model.Room
import com.example.data.model.StudentProfile
import com.example.data.repository.HostelRepository
import com.example.ui.components.AvatarInitials
import com.example.ui.components.MetricStatCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NavyLight
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import kotlinx.coroutines.launch

// -----------------------------------------------------------------------------
// 1. Student Dashboard Screen
// -----------------------------------------------------------------------------

@Composable
fun StudentDashboardScreen(
    studentProfile: StudentProfile?,
    repository: HostelRepository,
    onNavigateToProfile: () -> Unit,
    onNavigateToRoomSelection: () -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToComplaints: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uid = studentProfile?.userId ?: ""
    val bills by repository.observeBillsForStudent(uid).collectAsState(initial = emptyList())
    val complaints by repository.observeStudentComplaints(uid).collectAsState(initial = emptyList())
    val notifications by repository.observeNotifications(uid).collectAsState(initial = emptyList())

    // Calculations
    val roomFeePending = bills.filter { it.billType == "Room Fee" }.sumOf { it.balanceAmount }
    val messFeePending = bills.filter { it.billType == "Mess Fee" }.sumOf { it.balanceAmount }
    val totalPending = roomFeePending + messFeePending

    var roommates by remember { mutableStateOf<List<StudentProfile>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(studentProfile?.block, studentProfile?.roomNumber) {
        if (studentProfile?.roomAllocationStatus == "allocated") {
            val roomId = "${studentProfile.block}_${studentProfile.roomNumber}"
            val roomDoc = repository.db.collection("rooms").document(roomId).get()
            roomDoc.addOnSuccessListener { snap ->
                val room = snap.toObject(Room::class.java)
                val otherIds = room?.studentIds?.filter { it != uid } ?: emptyList()
                if (otherIds.isNotEmpty()) {
                    coroutineScope.launch {
                        roommates = repository.getStudentProfiles(otherIds)
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcome Card
        Card(
            colors = CardDefaults.cardColors(containerColor = NavyPrimary),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(20.dp)
            ) {
                AvatarInitials(
                    name = studentProfile?.fullName ?: "Student",
                    sizeDp = 56,
                    backgroundColor = AmberAccent
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Welcome back,",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = studentProfile?.fullName?.ifBlank { "Hostel Resident" } ?: "Hostel Resident",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (studentProfile != null && studentProfile.branch.isNotBlank())
                            "${studentProfile.academicYear} • ${studentProfile.branch}"
                        else "Please complete your profile",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Incomplete profile warning banner
        if (studentProfile == null || !studentProfile.isComplete()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AmberContainer),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToProfile() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = AmberAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Profile Incomplete",
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Mandatory details must be filled before room selection.",
                            color = Slate700,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(onClick = onNavigateToProfile) {
                        Text("Complete", color = AmberAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Room Status Card
        SectionHeader(title = "Room Allocation", subtitle = "Your accommodation details")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (studentProfile?.roomAllocationStatus == "allocated") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = studentProfile.block,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                            Text(
                                text = "Room No: ${studentProfile.roomNumber}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900
                            )
                        }
                        StatusBadge(status = "Allocated")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Slate200)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Roommate Info Section
                    Text(
                        text = "Roommate Information",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (roommates.isNotEmpty()) {
                        val mate = roommates.first()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarInitials(name = mate.fullName, sizeDp = 40, backgroundColor = NavyLight)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = mate.fullName,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "${mate.academicYear} • ${mate.branch}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate600
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Currently, no roommate is assigned to this bed (1/2 beds occupied).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "No Room Allocated",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "Choose a room from your academic block to secure your bed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate600
                            )
                        }
                        Button(
                            onClick = onNavigateToRoomSelection,
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("select_room_button")
                        ) {
                            Text("Select Room")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Financial Summary Section
        SectionHeader(title = "Bills & Fees", subtitle = "Outstanding balances & payments")
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MetricStatCard(
                title = "Room Fee Due",
                value = "₹${roomFeePending.toInt()}",
                subtitle = "Hostel stay",
                icon = Icons.Default.Apartment,
                iconTint = NavyPrimary,
                modifier = Modifier.weight(1f)
            )
            MetricStatCard(
                title = "Mess Fee Due",
                value = "₹${messFeePending.toInt()}",
                subtitle = "Food & dining",
                icon = Icons.Default.Restaurant,
                iconTint = AmberAccent,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Total Outstanding Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate600
                    )
                    Text(
                        text = "₹${totalPending.toInt()}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (totalPending > 0) RoseError else EmeraldSuccess
                    )
                }
                Button(
                    onClick = onNavigateToBills,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pay / View Bills")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Complaints Overview Section
        SectionHeader(title = "Recent Complaints", subtitle = "Hostel issues & maintenance")
        Spacer(modifier = Modifier.height(8.dp))

        if (complaints.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "No complaints filed.",
                        color = Slate600,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(onClick = onNavigateToComplaints) {
                        Text("File Complaint")
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                complaints.take(3).forEach { cmp ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cmp.title,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "${cmp.complaintRef} • ${cmp.category}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate600
                                )
                            }
                            StatusBadge(status = cmp.status)
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 2. Student Profile Screen
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileScreen(
    currentProfile: StudentProfile?,
    repository: HostelRepository,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fullName by remember(currentProfile) { mutableStateOf(currentProfile?.fullName ?: "") }
    var mobileNumber by remember(currentProfile) { mutableStateOf(currentProfile?.mobileNumber ?: "") }
    var selectedYear by remember(currentProfile) {
        mutableStateOf(currentProfile?.academicYear?.ifBlank { HostelConstants.YEAR_FIRST } ?: HostelConstants.YEAR_FIRST)
    }
    var selectedBranch by remember(currentProfile) {
        mutableStateOf(currentProfile?.branch?.ifBlank { HostelConstants.BRANCHES.first() } ?: HostelConstants.BRANCHES.first())
    }

    var yearExpanded by remember { mutableStateOf(false) }
    var branchExpanded by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        SectionHeader(
            title = "Resident Profile",
            subtitle = "Update mandatory student details for hostel records"
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Avatar Preview
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(80.dp)
                .clip(CircleShape)
                .background(NavyPrimary)
        ) {
            AvatarInitials(name = fullName.ifBlank { "Student" }, sizeDp = 76)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Full Name
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name *") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NavyPrimary) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("student_fullname_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Mobile Number
        OutlinedTextField(
            value = mobileNumber,
            onValueChange = { mobileNumber = it },
            label = { Text("Mobile Number (+91) *") },
            placeholder = { Text("e.g. 9876543210") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = NavyPrimary) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("student_mobile_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Academic Year Dropdown
        ExposedDropdownMenuBox(
            expanded = yearExpanded,
            onExpandedChange = { yearExpanded = !yearExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedYear,
                onValueChange = {},
                readOnly = true,
                label = { Text("Academic Year *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                leadingIcon = { Icon(Icons.Default.School, contentDescription = null, tint = NavyPrimary) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = yearExpanded,
                onDismissRequest = { yearExpanded = false }
            ) {
                HostelConstants.ACADEMIC_YEARS.forEach { year ->
                    val block = HostelConstants.getBlockForYear(year)
                    DropdownMenuItem(
                        text = { Text("$year (Allocated to $block)") },
                        onClick = {
                            selectedYear = year
                            yearExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Assigned Hostel Block: ${HostelConstants.getBlockForYear(selectedYear)}",
            style = MaterialTheme.typography.labelSmall,
            color = AmberAccent,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Branch / Department Dropdown
        ExposedDropdownMenuBox(
            expanded = branchExpanded,
            onExpandedChange = { branchExpanded = !branchExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedBranch,
                onValueChange = {},
                readOnly = true,
                label = { Text("Department / Branch *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchExpanded) },
                leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null, tint = NavyPrimary) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = branchExpanded,
                onDismissRequest = { branchExpanded = false }
            ) {
                HostelConstants.BRANCHES.forEach { branch ->
                    DropdownMenuItem(
                        text = { Text(branch) },
                        onClick = {
                            selectedBranch = branch
                            branchExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Room Status display
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate100),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Room Allocation Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate600
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (currentProfile?.roomAllocationStatus == "allocated")
                            "${currentProfile.block} • Room ${currentProfile.roomNumber}"
                        else "Not yet allocated",
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    StatusBadge(status = currentProfile?.roomAllocationStatus ?: "unallocated")
                }
            }
        }

        if (message != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                color = if (isError) RoseError.copy(alpha = 0.1f) else EmeraldSuccess.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message!!,
                    color = if (isError) RoseError else EmeraldSuccess,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (fullName.isBlank()) {
                    isError = true
                    message = "Full Name cannot be blank."
                    return@Button
                }
                if (mobileNumber.isBlank() || mobileNumber.filter { it.isDigit() }.length < 10) {
                    isError = true
                    message = "Please enter a valid 10-digit mobile number."
                    return@Button
                }

                isSaving = true
                message = null

                coroutineScope.launch {
                    try {
                        val uid = repository.requireUserId()
                        val updated = StudentProfile(
                            id = uid,
                            userId = uid,
                            fullName = fullName.trim(),
                            email = currentProfile?.email ?: repository.auth.currentUser?.email ?: "",
                            mobileNumber = mobileNumber.trim(),
                            academicYear = selectedYear,
                            branch = selectedBranch,
                            roomAllocationStatus = currentProfile?.roomAllocationStatus ?: "unallocated",
                            block = currentProfile?.block ?: "",
                            roomNumber = currentProfile?.roomNumber ?: "",
                            photoUrl = currentProfile?.photoUrl ?: ""
                        )
                        repository.saveStudentProfile(updated)
                        isSaving = false
                        isError = false
                        message = "Profile saved successfully!"
                        onSaveSuccess()
                    } catch (e: Exception) {
                        isSaving = false
                        isError = true
                        message = "Failed to save profile: ${e.message}"
                    }
                }
            },
            enabled = !isSaving,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_profile_button")
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Text("Save Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 3. Student Room Selection Screen
// -----------------------------------------------------------------------------

@Composable
fun StudentRoomSelectionScreen(
    studentProfile: StudentProfile?,
    repository: HostelRepository,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedRoomForDetails by remember { mutableStateOf<Room?>(null) }
    var existingRoommate by remember { mutableStateOf<StudentProfile?>(null) }
    var isBooking by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // Check mandatory profile completion
    if (studentProfile == null || !studentProfile.isComplete()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ReportProblem,
                contentDescription = null,
                tint = AmberAccent,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Profile Completion Required",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You must complete your Full Name, Mobile Number, Academic Year, and Department before you can browse and select a room.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate600
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onNavigateToProfile,
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Complete Profile Now")
            }
        }
        return
    }

    // If already allocated
    if (studentProfile.roomAllocationStatus == "allocated") {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = EmeraldSuccess,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Room Already Allocated",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You are currently allocated to Room ${studentProfile.roomNumber} in ${studentProfile.block}.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate600
            )
        }
        return
    }

    val eligibleBlock = HostelConstants.getBlockForYear(studentProfile.academicYear)
    val rooms by repository.observeRooms(eligibleBlock).collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SectionHeader(
            title = "Room Selection",
            subtitle = "Eligible rooms in $eligibleBlock for ${studentProfile.academicYear}"
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            color = NavyPrimary.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = NavyPrimary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Each room has a strict 2-student capacity. Tap any room with an open bed to inspect or confirm allocation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NavyPrimary
                )
            }
        }

        if (actionMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = if (isError) RoseError.copy(alpha = 0.1f) else EmeraldSuccess.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = actionMessage!!,
                    color = if (isError) RoseError else EmeraldSuccess,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (rooms.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NavyPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading available rooms in $eligibleBlock...", color = Slate600)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(rooms) { room ->
                    val isFull = room.occupiedBeds >= 2L || room.studentIds.size >= 2
                    val availableBeds = (2L - room.occupiedBeds).coerceAtLeast(0L)
                    val statusText = when {
                        !room.isAvailable -> "Unavailable"
                        isFull -> "Fully Occupied"
                        availableBeds == 1L -> "1 Bed Available"
                        else -> "2 Beds Available"
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isFull && room.isAvailable) {
                                selectedRoomForDetails = room
                                if (room.studentIds.isNotEmpty()) {
                                    coroutineScope.launch {
                                        val mates = repository.getStudentProfiles(room.studentIds)
                                        existingRoommate = mates.firstOrNull()
                                    }
                                } else {
                                    existingRoommate = null
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Room ${room.roomNumber}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "${room.block} • Capacity: 2",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate600
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(status = statusText)
                                Spacer(modifier = Modifier.width(8.dp))
                                if (!isFull && room.isAvailable) {
                                    Button(
                                        onClick = {
                                            selectedRoomForDetails = room
                                            if (room.studentIds.isNotEmpty()) {
                                                coroutineScope.launch {
                                                    val mates = repository.getStudentProfiles(room.studentIds)
                                                    existingRoommate = mates.firstOrNull()
                                                }
                                            } else {
                                                existingRoommate = null
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Select")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Step 4 & 5 Confirmation & Roommate Popup
    if (selectedRoomForDetails != null) {
        val room = selectedRoomForDetails!!
        AlertDialog(
            onDismissRequest = {
                if (!isBooking) selectedRoomForDetails = null
            },
            title = {
                Text(
                    text = "Confirm Room Allocation",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "You are selecting Room ${room.roomNumber} in ${room.block}.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (existingRoommate != null) {
                        Surface(
                            color = Slate100,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Existing Roommate:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Slate600
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AvatarInitials(name = existingRoommate!!.fullName, sizeDp = 36)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = existingRoommate!!.fullName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "${existingRoommate!!.academicYear} • ${existingRoommate!!.branch}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate600
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "This room is currently empty. You will be the first resident.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isBooking = true
                        coroutineScope.launch {
                            val res = repository.allocateRoomTransaction(studentProfile.userId, room.id)
                            isBooking = false
                            if (res.isSuccess) {
                                isError = false
                                actionMessage = "Successfully allocated Room ${room.roomNumber} in ${room.block}!"
                                selectedRoomForDetails = null
                            } else {
                                isError = true
                                actionMessage = "Allocation failed: ${res.exceptionOrNull()?.message}"
                            }
                        }
                    },
                    enabled = !isBooking,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    if (isBooking) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Confirm Allocation")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedRoomForDetails = null },
                    enabled = !isBooking
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// 4. Student Bills and Payments Screen
// -----------------------------------------------------------------------------

@Composable
fun StudentBillsScreen(
    studentProfile: StudentProfile?,
    repository: HostelRepository,
    modifier: Modifier = Modifier
) {
    val uid = studentProfile?.userId ?: ""
    val bills by repository.observeBillsForStudent(uid).collectAsState(initial = emptyList())
    val payments by repository.observePayments(uid).collectAsState(initial = emptyList())

    var selectedBillForPayment by remember { mutableStateOf<Bill?>(null) }
    var isProcessingPayment by remember { mutableStateOf(false) }
    var paymentSuccessDialog by remember { mutableStateOf<Payment?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SectionHeader(
            title = "Bills & Payment History",
            subtitle = "Official Room and Mess Fee statements"
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (errorMessage != null) {
            Surface(
                color = RoseError.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage!!,
                    color = RoseError,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (bills.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = "No fee bills currently issued for your account.",
                    color = Slate600,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(bills) { bill ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = bill.billType,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary
                                    )
                                    Text(
                                        text = "Ref: ${bill.billRef} • ${bill.billingPeriod}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate600
                                    )
                                }
                                StatusBadge(status = bill.paymentStatus)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Slate200)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text("Total Amount", style = MaterialTheme.typography.labelSmall, color = Slate600)
                                    Text("₹${bill.totalAmount.toInt()}", fontWeight = FontWeight.SemiBold, color = Slate900)
                                }
                                Column {
                                    Text("Paid Amount", style = MaterialTheme.typography.labelSmall, color = Slate600)
                                    Text("₹${bill.amountPaid.toInt()}", fontWeight = FontWeight.SemiBold, color = EmeraldSuccess)
                                }
                                Column {
                                    Text("Balance Due", style = MaterialTheme.typography.labelSmall, color = Slate600)
                                    Text("₹${bill.balanceAmount.toInt()}", fontWeight = FontWeight.Bold, color = if (bill.balanceAmount > 0) RoseError else Slate900)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Due: ${bill.dueDate}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate600
                                )
                                if (bill.balanceAmount > 0) {
                                    Button(
                                        onClick = { selectedBillForPayment = bill },
                                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Pay Now (₹${bill.balanceAmount.toInt()})")
                                    }
                                }
                            }
                        }
                    }
                }

                // Payment History Section
                if (payments.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        SectionHeader(title = "Payment Receipts", subtitle = "Verified transactions")
                    }
                    items(payments) { pmt ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate100),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Receipt #${pmt.receiptRef}",
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "${pmt.paymentDate} • ${pmt.paymentMethod}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate600
                                    )
                                    if (pmt.transactionRef.isNotBlank()) {
                                        Text(
                                            text = "Txn: ${pmt.transactionRef}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate600
                                        )
                                    }
                                }
                                Text(
                                    text = "₹${pmt.amount.toInt()}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldSuccess,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Pay Now Checkout Dialog
    if (selectedBillForPayment != null) {
        val bill = selectedBillForPayment!!
        var paymentMethod by remember { mutableStateOf("UPI (GPay / PhonePe / Paytm)") }

        AlertDialog(
            onDismissRequest = {
                if (!isProcessingPayment) selectedBillForPayment = null
            },
            title = {
                Text(
                    text = "Online Payment Checkout",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Pay outstanding balance for ${bill.billType} (${bill.billingPeriod})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Amount Payable: ₹${bill.balanceAmount.toInt()}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Select Indian Payment Method:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    val methods = listOf(
                        "UPI (GPay / PhonePe / Paytm)",
                        "Net Banking (SBI / HDFC / ICICI)",
                        "Debit / Credit Card"
                    )

                    methods.forEach { method ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { paymentMethod = method }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = method, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessingPayment = true
                        val txnRef = "PAY-" + System.currentTimeMillis().toString().takeLast(10)
                        coroutineScope.launch {
                            val res = repository.processOnlinePayment(
                                billId = bill.id,
                                amount = bill.balanceAmount,
                                gatewayOrderId = txnRef,
                                paymentMethod = paymentMethod
                            )
                            isProcessingPayment = false
                            if (res.isSuccess) {
                                selectedBillForPayment = null
                                paymentSuccessDialog = res.getOrNull()
                            } else {
                                errorMessage = "Payment gateway error: ${res.exceptionOrNull()?.message}"
                            }
                        }
                    },
                    enabled = !isProcessingPayment,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    if (isProcessingPayment) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Pay ₹${bill.balanceAmount.toInt()}")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedBillForPayment = null },
                    enabled = !isProcessingPayment
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Payment Success & Digital Receipt Dialog
    if (paymentSuccessDialog != null) {
        val pmt = paymentSuccessDialog!!
        AlertDialog(
            onDismissRequest = { paymentSuccessDialog = null },
            icon = {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(48.dp))
            },
            title = {
                Text("Payment Successful!", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Your payment has been verified and recorded to the hostel accounts ledger.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Official Receipt: #${pmt.receiptRef}", fontWeight = FontWeight.Bold)
                            Text("Amount: ₹${pmt.amount.toInt()}")
                            Text("Method: ${pmt.paymentMethod}")
                            Text("Date: ${pmt.paymentDate}")
                            Text("Txn Ref: ${pmt.transactionRef}")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { paymentSuccessDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("Done")
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// 5. Student Complaints Screen
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentComplaintsScreen(
    studentProfile: StudentProfile?,
    repository: HostelRepository,
    modifier: Modifier = Modifier
) {
    val uid = studentProfile?.userId ?: ""
    val complaints by repository.observeStudentComplaints(uid).collectAsState(initial = emptyList())

    var showNewComplaintDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(HostelConstants.COMPLAINT_CATEGORIES.first()) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewComplaintDialog = true },
                containerColor = NavyPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Complaint")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            SectionHeader(
                title = "Complaints & Grievances",
                subtitle = "Track maintenance and service requests"
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (resultMessage != null) {
                Surface(
                    color = if (isError) RoseError.copy(alpha = 0.1f) else EmeraldSuccess.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = resultMessage!!,
                        color = if (isError) RoseError else EmeraldSuccess,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (complaints.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ReportProblem, contentDescription = null, tint = Slate600, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No complaints submitted.",
                            color = Slate600,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the + button to file a new maintenance request.",
                            color = Slate600,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(complaints) { cmp ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(
                                            text = cmp.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "Ref: ${cmp.complaintRef} • ${cmp.category}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate600
                                        )
                                    }
                                    StatusBadge(status = cmp.status)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = cmp.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate700
                                )

                                if (cmp.adminResponse.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        color = Slate100,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "Hostel Admin Response:",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NavyPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = cmp.adminResponse,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Slate800
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // New Complaint Submission Dialog
    if (showNewComplaintDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSubmitting) showNewComplaintDialog = false
            },
            title = { Text("File New Complaint", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            HostelConstants.COMPLAINT_CATEGORIES.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Complaint Title") },
                        placeholder = { Text("Brief summary of issue") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Detailed Description") },
                        placeholder = { Text("Describe location and problem details...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isBlank() || description.isBlank()) {
                            isError = true
                            resultMessage = "Title and description are required."
                            return@Button
                        }
                        isSubmitting = true
                        coroutineScope.launch {
                            val newCmp = Complaint(
                                studentId = uid,
                                studentName = studentProfile?.fullName ?: "Resident",
                                academicYear = studentProfile?.academicYear ?: "",
                                branch = studentProfile?.branch ?: "",
                                block = studentProfile?.block ?: "",
                                roomNumber = studentProfile?.roomNumber ?: "",
                                mobileNumber = studentProfile?.mobileNumber ?: "",
                                category = selectedCategory,
                                title = title.trim(),
                                description = description.trim(),
                                photoUrl = ""
                            )
                            val res = repository.submitComplaint(newCmp)
                            isSubmitting = false
                            if (res.isSuccess) {
                                isError = false
                                resultMessage = "Complaint ${res.getOrNull()} submitted successfully."
                                title = ""
                                description = ""
                                showNewComplaintDialog = false
                            } else {
                                isError = true
                                resultMessage = "Failed: ${res.exceptionOrNull()?.message}"
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Submit")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNewComplaintDialog = false },
                    enabled = !isSubmitting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// 6. Student Notifications Screen
// -----------------------------------------------------------------------------

@Composable
fun StudentNotificationsScreen(
    studentProfile: StudentProfile?,
    repository: HostelRepository,
    modifier: Modifier = Modifier
) {
    val uid = studentProfile?.userId ?: ""
    val notifications by repository.observeNotifications(uid).collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SectionHeader(
            title = "Notifications & Reminders",
            subtitle = "Official alerts, room allocations, and bill reminders"
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (notifications.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text("No notifications received yet.", color = Slate600)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(notifications) { notif ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (notif.isRead) MaterialTheme.colorScheme.surface else Color(0xFFF0FDF4)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!notif.isRead && notif.id.isNotBlank()) {
                                    coroutineScope.launch { repository.markNotificationAsRead(notif.id) }
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(
                                imageVector = when (notif.type) {
                                    "BILL" -> Icons.Default.Payment
                                    "ALLOCATION" -> Icons.Default.MeetingRoom
                                    "COMPLAINT" -> Icons.Default.ReportProblem
                                    else -> Icons.Default.Notifications
                                },
                                contentDescription = null,
                                tint = if (notif.type == "BILL") AmberAccent else NavyPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = notif.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate900
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = notif.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate700
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
