package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.Payment
import com.example.data.model.Room
import com.example.data.model.SmsReminder
import com.example.data.model.StudentProfile
import com.example.data.repository.HostelRepository
import com.example.ui.components.AvatarInitials
import com.example.ui.components.MetricStatCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// -----------------------------------------------------------------------------
// 1. Administrator Dashboard Screen
// -----------------------------------------------------------------------------

@Composable
fun AdminDashboardScreen(
    repository: HostelRepository,
    modifier: Modifier = Modifier
) {
    val students by repository.observeAllStudents().collectAsState(initial = emptyList())
    val rooms by repository.observeRooms().collectAsState(initial = emptyList())
    val bills by repository.observeAllBills().collectAsState(initial = emptyList())
    val payments by repository.observePayments().collectAsState(initial = emptyList())
    val complaints by repository.observeAllComplaints().collectAsState(initial = emptyList())

    // Metrics calculations
    val totalStudents = students.size
    val firstYearCount = students.count { it.academicYear == HostelConstants.YEAR_FIRST }
    val secondYearCount = students.count { it.academicYear == HostelConstants.YEAR_SECOND }
    val thirdYearCount = students.count { it.academicYear == HostelConstants.YEAR_THIRD }
    val fourthYearCount = students.count { it.academicYear == HostelConstants.YEAR_FOURTH }

    val totalRooms = rooms.size
    val occupiedBeds = rooms.sumOf { it.occupiedBeds }
    val totalCapacity = totalRooms * 2L
    val availableBeds = (totalCapacity - occupiedBeds).coerceAtLeast(0L)
    val fullyOccupiedRooms = rooms.count { it.occupiedBeds >= 2L }

    val pendingRoomFee = bills.filter { it.billType == "Room Fee" }.sumOf { it.balanceAmount }
    val pendingMessFee = bills.filter { it.billType == "Mess Fee" }.sumOf { it.balanceAmount }
    val totalPaymentsReceived = payments.sumOf { it.amount }
    val unresolvedComplaints = complaints.count { it.status == "Submitted" || it.status == "In Progress" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SectionHeader(
            title = "Administrator Overview",
            subtitle = "Kalpataru Boys Hostel Central Operations"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Student Resident Stats
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricStatCard(
                title = "Total Students",
                value = "$totalStudents",
                subtitle = "Registered residents",
                icon = Icons.Default.Group,
                iconTint = NavyPrimary,
                modifier = Modifier.weight(1f)
            )
            MetricStatCard(
                title = "Unresolved Issues",
                value = "$unresolvedComplaints",
                subtitle = "Active complaints",
                icon = Icons.Default.ReportProblem,
                iconTint = AmberAccent,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Room & Bed Inventory Stats
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricStatCard(
                title = "Occupied Beds",
                value = "$occupiedBeds / $totalCapacity",
                subtitle = "$fullyOccupiedRooms rooms full",
                icon = Icons.Default.MeetingRoom,
                iconTint = NavyPrimary,
                modifier = Modifier.weight(1f)
            )
            MetricStatCard(
                title = "Available Beds",
                value = "$availableBeds",
                subtitle = "Across all 3 blocks",
                icon = Icons.Default.Apartment,
                iconTint = EmeraldSuccess,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Financial Overview
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricStatCard(
                title = "Pending Room Fee",
                value = "₹${pendingRoomFee.toInt()}",
                subtitle = "Hostel rent dues",
                icon = Icons.Default.Payment,
                iconTint = RoseError,
                modifier = Modifier.weight(1f)
            )
            MetricStatCard(
                title = "Pending Mess Fee",
                value = "₹${pendingMessFee.toInt()}",
                subtitle = "Dining fee dues",
                icon = Icons.Default.Payment,
                iconTint = AmberAccent,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Students by Academic Year",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("1st Year (Hemavati)", style = MaterialTheme.typography.labelSmall, color = Slate600)
                        Text("$firstYearCount students", fontWeight = FontWeight.Bold, color = Slate900)
                    }
                    Column {
                        Text("2nd Year (Kaveri)", style = MaterialTheme.typography.labelSmall, color = Slate600)
                        Text("$secondYearCount students", fontWeight = FontWeight.Bold, color = Slate900)
                    }
                    Column {
                        Text("3rd Year (Kaveri)", style = MaterialTheme.typography.labelSmall, color = Slate600)
                        Text("$thirdYearCount students", fontWeight = FontWeight.Bold, color = Slate900)
                    }
                    Column {
                        Text("4th Year (MCA)", style = MaterialTheme.typography.labelSmall, color = Slate600)
                        Text("$fourthYearCount students", fontWeight = FontWeight.Bold, color = Slate900)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Slate100),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(16.dp)
            ) {
                Column {
                    Text("Total Collections Received", style = MaterialTheme.typography.bodyMedium, color = Slate600)
                    Text("₹${totalPaymentsReceived.toInt()}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                }
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(36.dp))
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 2. Administrator Student Management Screen
// -----------------------------------------------------------------------------

@Composable
fun AdminStudentManagementScreen(
    repository: HostelRepository,
    modifier: Modifier = Modifier
) {
    val students by repository.observeAllStudents().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedYearFilter by remember { mutableStateOf("All") }
    var inspectStudent by remember { mutableStateOf<StudentProfile?>(null) }

    val filteredStudents = students.filter { s ->
        val matchesSearch = s.fullName.contains(searchQuery, ignoreCase = true) ||
                s.mobileNumber.contains(searchQuery) ||
                s.roomNumber.contains(searchQuery)
        val matchesYear = selectedYearFilter == "All" || s.academicYear == selectedYearFilter
        matchesSearch && matchesYear
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SectionHeader(
            title = "Student Resident Management",
            subtitle = "Organized by academic year and block assignments"
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by student name, room or phone...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Year filter chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("All", "1st Year", "2nd Year", "3rd Year", "4th Year").forEach { chip ->
                val fullYear = when (chip) {
                    "1st Year" -> HostelConstants.YEAR_FIRST
                    "2nd Year" -> HostelConstants.YEAR_SECOND
                    "3rd Year" -> HostelConstants.YEAR_THIRD
                    "4th Year" -> HostelConstants.YEAR_FOURTH
                    else -> "All"
                }
                val isSelected = selectedYearFilter == fullYear

                Surface(
                    color = if (isSelected) NavyPrimary else Slate100,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clickable { selectedYearFilter = fullYear }
                ) {
                    Text(
                        text = chip,
                        color = if (isSelected) Color.White else Slate700,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredStudents.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxWidth()) {
                Text("No student records found.", color = Slate600)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredStudents) { student ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { inspectStudent = student }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            AvatarInitials(name = student.fullName, sizeDp = 44)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.fullName,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "${student.academicYear} • ${student.branch}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate600
                                )
                                Text(
                                    text = if (student.roomAllocationStatus == "allocated")
                                        "Room: ${student.block} - ${student.roomNumber}"
                                    else "Unallocated",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (student.roomAllocationStatus == "allocated") NavyPrimary else AmberAccent
                                )
                            }
                            StatusBadge(status = student.roomAllocationStatus)
                        }
                    }
                }
            }
        }
    }

    // Student Detail Inspector Dialog
    if (inspectStudent != null) {
        val s = inspectStudent!!
        AlertDialog(
            onDismissRequest = { inspectStudent = null },
            title = { Text(s.fullName, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Email: ${s.email}")
                    Text("Phone: ${s.mobileNumber}")
                    Text("Year: ${s.academicYear}")
                    Text("Department: ${s.branch}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (s.roomAllocationStatus == "allocated")
                            "Allocated Room: ${s.block} - Room ${s.roomNumber}"
                        else "Room Allocation: Pending",
                        fontWeight = FontWeight.SemiBold,
                        color = NavyPrimary
                    )
                }
            },
            confirmButton = {
                Button(onClick = { inspectStudent = null }) {
                    Text("Close")
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// 3. Administrator Room Management Screen
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRoomManagementScreen(
    repository: HostelRepository,
    modifier: Modifier = Modifier
) {
    val rooms by repository.observeRooms().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    var showAddRoomDialog by remember { mutableStateOf(false) }
    var selectedBlock by remember { mutableStateOf(HostelConstants.BLOCK_HEMAVATI) }
    var blockExpanded by remember { mutableStateOf(false) }
    var roomNumber by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddRoomDialog = true },
                containerColor = NavyPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Room")
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
                title = "Hostel Room Inventory",
                subtitle = "Manage room numbers and 2-bed occupancy"
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (rooms.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NavyPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading hostel rooms...", color = Slate600)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(rooms) { room ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(
                                            text = "Room ${room.roomNumber}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "${room.block} • Max Capacity: 2",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate600
                                        )
                                    }
                                    StatusBadge(
                                        status = if (!room.isAvailable) "Unavailable"
                                        else if (room.occupiedBeds >= 2L) "Fully Occupied"
                                        else "${room.occupiedBeds}/2 Beds Taken"
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (room.isAvailable) "Available" else "Disabled",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate600
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Switch(
                                            checked = room.isAvailable,
                                            onCheckedChange = { checked ->
                                                coroutineScope.launch {
                                                    repository.createOrUpdateRoom(room.copy(isAvailable = checked))
                                                }
                                            }
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.deleteRoom(room.id)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseError)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Room Dialog
    if (showAddRoomDialog) {
        AlertDialog(
            onDismissRequest = { if (!isAdding) showAddRoomDialog = false },
            title = { Text("Add New Hostel Room", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ExposedDropdownMenuBox(
                        expanded = blockExpanded,
                        onExpandedChange = { blockExpanded = !blockExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedBlock,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Hostel Block") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = blockExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = blockExpanded,
                            onDismissRequest = { blockExpanded = false }
                        ) {
                            listOf(
                                HostelConstants.BLOCK_HEMAVATI,
                                HostelConstants.BLOCK_KAVERI,
                                HostelConstants.BLOCK_MCA
                            ).forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b) },
                                    onClick = {
                                        selectedBlock = b
                                        blockExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = roomNumber,
                        onValueChange = { roomNumber = it },
                        label = { Text("Room Number") },
                        placeholder = { Text("e.g. 106, 206, 306") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Capacity is strictly set to 2 students per room.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate600
                    )

                    if (addError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = addError!!, color = RoseError, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (roomNumber.isBlank()) {
                            addError = "Room number is required."
                            return@Button
                        }
                        isAdding = true
                        addError = null
                        coroutineScope.launch {
                            val newRoom = Room(
                                id = "${selectedBlock}_${roomNumber.trim()}",
                                block = selectedBlock,
                                roomNumber = roomNumber.trim(),
                                capacity = 2L,
                                occupiedBeds = 0L,
                                studentIds = emptyList(),
                                isAvailable = true
                            )
                            repository.createOrUpdateRoom(newRoom)
                            isAdding = false
                            roomNumber = ""
                            showAddRoomDialog = false
                        }
                    },
                    enabled = !isAdding,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("Add Room")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRoomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// 4. Administrator Bills & Payments Management
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBillsManagementScreen(
    repository: HostelRepository,
    modifier: Modifier = Modifier
) {
    val students by repository.observeAllStudents().collectAsState(initial = emptyList())
    val bills by repository.observeAllBills().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    var showAddBillDialog by remember { mutableStateOf(false) }
    var selectedStudent by remember { mutableStateOf<StudentProfile?>(null) }
    var studentDropdownExpanded by remember { mutableStateOf(false) }
    var billType by remember { mutableStateOf("Room Fee") }
    var billingPeriod by remember { mutableStateOf("Semester 1 (2026-27)") }
    var amountStr by remember { mutableStateOf("15000") }
    var dueDate by remember { mutableStateOf("2026-11-30") }
    var remarks by remember { mutableStateOf("Official Hostel Fees") }
    var isCreatingBill by remember { mutableStateOf(false) }

    var recordPaymentDialogBill by remember { mutableStateOf<Bill?>(null) }
    var paymentAmountStr by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var paymentRef by remember { mutableStateOf("") }
    var paymentRemarks by remember { mutableStateOf("") }
    var isRecordingPayment by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (students.isNotEmpty()) selectedStudent = students.first()
                    showAddBillDialog = true
                },
                containerColor = NavyPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Bill")
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
                title = "Bills & Payment Ledger",
                subtitle = "Issue student fee bills and log offline receipts"
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (bills.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Text("No billing records found. Tap + to generate a fee bill.", color = Slate600)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(bills) { bill ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(
                                            text = bill.studentName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "${bill.billType} • ${bill.billingPeriod}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NavyPrimary
                                        )
                                    }
                                    StatusBadge(status = bill.paymentStatus)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Total: ₹${bill.totalAmount.toInt()}", style = MaterialTheme.typography.bodySmall)
                                    Text("Paid: ₹${bill.amountPaid.toInt()}", style = MaterialTheme.typography.bodySmall, color = EmeraldSuccess)
                                    Text("Balance: ₹${bill.balanceAmount.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (bill.balanceAmount > 0) RoseError else Slate900)
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (bill.balanceAmount > 0) {
                                        OutlinedButton(
                                            onClick = {
                                                recordPaymentDialogBill = bill
                                                paymentAmountStr = bill.balanceAmount.toInt().toString()
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Record Payment")
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

    // Add Bill Dialog
    if (showAddBillDialog) {
        AlertDialog(
            onDismissRequest = { if (!isCreatingBill) showAddBillDialog = false },
            title = { Text("Issue Fee Bill", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    // Student selection dropdown
                    ExposedDropdownMenuBox(
                        expanded = studentDropdownExpanded,
                        onExpandedChange = { studentDropdownExpanded = !studentDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedStudent?.fullName ?: "Select Student",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Student Resident") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = studentDropdownExpanded,
                            onDismissRequest = { studentDropdownExpanded = false }
                        ) {
                            students.forEach { st ->
                                DropdownMenuItem(
                                    text = { Text("${st.fullName} (${st.academicYear})") },
                                    onClick = {
                                        selectedStudent = st
                                        studentDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Room Fee", "Mess Fee").forEach { type ->
                            Surface(
                                color = if (billType == type) NavyPrimary else Slate100,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { billType = type }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = type,
                                    color = if (billType == type) Color.White else Slate900,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(10.dp),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = billingPeriod,
                        onValueChange = { billingPeriod = it },
                        label = { Text("Billing Period") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Due Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        if (selectedStudent != null && amt > 0) {
                            isCreatingBill = true
                            coroutineScope.launch {
                                val newBill = Bill(
                                    studentId = selectedStudent!!.userId,
                                    studentName = selectedStudent!!.fullName,
                                    billType = billType,
                                    billingPeriod = billingPeriod,
                                    totalAmount = amt,
                                    dueDate = dueDate,
                                    remarks = remarks
                                )
                                repository.createBill(newBill)
                                isCreatingBill = false
                                showAddBillDialog = false
                            }
                        }
                    },
                    enabled = !isCreatingBill && selectedStudent != null,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("Issue Bill")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBillDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Record Manual Payment Dialog
    if (recordPaymentDialogBill != null) {
        val b = recordPaymentDialogBill!!
        AlertDialog(
            onDismissRequest = { if (!isRecordingPayment) recordPaymentDialogBill = null },
            title = { Text("Record Manual Payment", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Student: ${b.studentName}", fontWeight = FontWeight.SemiBold)
                    Text("Bill: ${b.billType} (${b.billingPeriod})")
                    Text("Outstanding: ₹${b.balanceAmount.toInt()}", color = RoseError, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = paymentAmountStr,
                        onValueChange = { paymentAmountStr = it },
                        label = { Text("Amount Received (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = { paymentMethod = it },
                        label = { Text("Payment Method (Cash / Bank Transfer / UPI)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = paymentRef,
                        onValueChange = { paymentRef = it },
                        label = { Text("Transaction / Receipt Ref") },
                        placeholder = { Text("e.g. UTR12345678 or Cash Slip") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = paymentAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            isRecordingPayment = true
                            coroutineScope.launch {
                                repository.recordManualPayment(
                                    billId = b.id,
                                    amount = amt,
                                    paymentMethod = paymentMethod,
                                    transactionRef = paymentRef,
                                    remarks = paymentRemarks
                                )
                                isRecordingPayment = false
                                recordPaymentDialogBill = null
                            }
                        }
                    },
                    enabled = !isRecordingPayment,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("Confirm Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordPaymentDialogBill = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// 5. Administrator Complaints Management
// -----------------------------------------------------------------------------

@Composable
fun AdminComplaintsScreen(
    repository: HostelRepository,
    modifier: Modifier = Modifier
) {
    val complaints by repository.observeAllComplaints().collectAsState(initial = emptyList())
    var selectedComplaintForAction by remember { mutableStateOf<Complaint?>(null) }
    var responseText by remember { mutableStateOf("") }
    var newStatus by remember { mutableStateOf("In Progress") }
    var isUpdating by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SectionHeader(
            title = "Complaints Management",
            subtitle = "Resolve maintenance and resident issues"
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (complaints.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxWidth()) {
                Text("No complaints submitted by students.", color = Slate600)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedComplaintForAction = cmp
                                responseText = cmp.adminResponse
                                newStatus = if (cmp.status == "Submitted") "In Progress" else cmp.status
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = cmp.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "${cmp.studentName} (${cmp.block} ${cmp.roomNumber}) • ${cmp.category}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate600
                                    )
                                }
                                StatusBadge(status = cmp.status)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = cmp.description, style = MaterialTheme.typography.bodyMedium, color = Slate700)

                            if (cmp.adminResponse.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Response: ${cmp.adminResponse}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NavyPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Complaint Resolution Dialog
    if (selectedComplaintForAction != null) {
        val cmp = selectedComplaintForAction!!
        AlertDialog(
            onDismissRequest = { if (!isUpdating) selectedComplaintForAction = null },
            title = { Text("Update Complaint: ${cmp.complaintRef}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Student: ${cmp.studentName} (Phone: ${cmp.mobileNumber})")
                    Text("Location: ${cmp.block} - Room ${cmp.roomNumber}")
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Status:", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("In Progress", "Resolved", "Rejected").forEach { st ->
                            Surface(
                                color = if (newStatus == st) NavyPrimary else Slate100,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { newStatus = st }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = st,
                                    color = if (newStatus == st) Color.White else Slate900,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = responseText,
                        onValueChange = { responseText = it },
                        label = { Text("Administrator Response / Action Taken") },
                        placeholder = { Text("e.g. Electrician visited and repaired fan.") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isUpdating = true
                        coroutineScope.launch {
                            repository.updateComplaintResolution(cmp.id, newStatus, responseText.trim())
                            isUpdating = false
                            selectedComplaintForAction = null
                        }
                    },
                    enabled = !isUpdating,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("Save Resolution")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedComplaintForAction = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// 6. Administrator SMS Reminders Screen (Specification Section 9)
// -----------------------------------------------------------------------------

@Composable
fun AdminSmsScreen(
    repository: HostelRepository,
    modifier: Modifier = Modifier
) {
    val students by repository.observeAllStudents().collectAsState(initial = emptyList())
    val bills by repository.observeAllBills().collectAsState(initial = emptyList())
    val smsLogs by repository.observeSmsReminders().collectAsState(initial = emptyList())

    val pendingBills = bills.filter { it.balanceAmount > 0 }
    var selectedBillForSms by remember { mutableStateOf<Bill?>(null) }
    var confirmationTargetStudent by remember { mutableStateOf<StudentProfile?>(null) }
    var isSendingSms by remember { mutableStateOf(false) }
    var dispatchStatusMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SectionHeader(
            title = "SMS Fee Reminders",
            subtitle = "Dispatch real SMS notifications for pending fee balances"
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (dispatchStatusMessage != null) {
            Surface(
                color = Slate100,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = dispatchStatusMessage!!,
                    color = NavyPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Text(
            text = "Pending Dues Awaiting Reminders (${pendingBills.size}):",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (pendingBills.isEmpty()) {
            Text("No pending fees currently require reminders.", color = Slate600, style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(pendingBills) { b ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Column {
                                Text(b.studentName, fontWeight = FontWeight.Bold)
                                Text("${b.billType} • Balance: ₹${b.balanceAmount.toInt()}", color = RoseError, style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                onClick = {
                                    val st = students.find { it.userId == b.studentId }
                                    confirmationTargetStudent = st
                                    selectedBillForSms = b
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Send SMS")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SMS Dispatch Logs
        Text(
            text = "SMS Dispatch & Delivery History (${smsLogs.size}):",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (smsLogs.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxWidth()) {
                Text("No SMS reminders sent yet.", color = Slate600)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(smsLogs) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate100),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${log.studentName} (${log.mobileNumber})",
                                    fontWeight = FontWeight.Bold
                                )
                                StatusBadge(status = log.status)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = log.message, style = MaterialTheme.typography.bodySmall, color = Slate700)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Provider status: ${log.providerResponse}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate600
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog before sending SMS (Requirement Section 9)
    if (selectedBillForSms != null && confirmationTargetStudent != null) {
        val b = selectedBillForSms!!
        val s = confirmationTargetStudent!!

        AlertDialog(
            onDismissRequest = { if (!isSendingSms) selectedBillForSms = null },
            title = { Text("Confirm SMS Bill Reminder", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Recipient Student: ${s.fullName}")
                    Text("Mobile Number: ${s.mobileNumber}")
                    Text("Bill Type: ${b.billType}")
                    Text("Outstanding Balance: ₹${b.balanceAmount.toInt()}", fontWeight = FontWeight.Bold, color = RoseError)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Message template:\n\"Dear ${s.fullName}, your ${b.billType} fee of ₹${b.balanceAmount.toInt()} for ${b.billingPeriod} is pending. Please pay by ${b.dueDate}. For details, log in to the Kalpataru Boys Hostel portal.\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSendingSms = true
                        coroutineScope.launch {
                            val res = repository.sendSmsReminder(s, b, b.billType)
                            isSendingSms = false
                            selectedBillForSms = null
                            if (res.isSuccess) {
                                dispatchStatusMessage = "SMS Reminder dispatched: ${res.getOrNull()?.providerResponse}"
                            } else {
                                dispatchStatusMessage = "SMS Dispatch failed: ${res.exceptionOrNull()?.message}"
                            }
                        }
                    },
                    enabled = !isSendingSms,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                ) {
                    if (isSendingSms) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Send SMS Now")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBillForSms = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
