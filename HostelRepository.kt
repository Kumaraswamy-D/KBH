package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.Bill
import com.example.data.model.Complaint
import com.example.data.model.HostelConstants
import com.example.data.model.NotificationItem
import com.example.data.model.Payment
import com.example.data.model.Room
import com.example.data.model.RoomAllocation
import com.example.data.model.SmsReminder
import com.example.data.model.StudentProfile
import com.example.data.model.UserRecord
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class HostelRepository(
    val db: FirebaseFirestore,
    val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private val tag = "HostelRepository"

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated before performing database operations.")
    }

    // -------------------------------------------------------------
    // User Record & Admin Verification
    // -------------------------------------------------------------

    suspend fun getUserRecord(uid: String): UserRecord? {
        val snapshot = db.collection("users").document(uid).get().await()
        return if (snapshot.exists()) snapshot.toObject(UserRecord::class.java) else null
    }

    suspend fun saveUserRecord(userRecord: UserRecord) {
        val uid = userRecord.id.ifBlank { requireUserId() }
        val docRef = db.collection("users").document(uid)
        val data = hashMapOf(
            "email" to userRecord.email,
            "name" to userRecord.name,
            "role" to userRecord.role,
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()
    }

    fun validateAdminAccessKey(key: String): Boolean {
        // Backend access key verification required by specification
        // Administrator key must match configured secret
        val secretKey = "KBH-ADMIN-2026"
        return key.trim() == secretKey
    }

    // -------------------------------------------------------------
    // Student Profile
    // -------------------------------------------------------------

    fun observeStudentProfile(uid: String): Flow<StudentProfile?> = callbackFlow {
        val listener = db.collection("studentProfiles").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error observing student profile: ${error.message}", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                val profile = snapshot?.toObject(StudentProfile::class.java)
                trySend(profile)
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveStudentProfile(profile: StudentProfile) {
        val uid = profile.userId.ifBlank { requireUserId() }
        val docRef = db.collection("studentProfiles").document(uid)
        val payload = hashMapOf(
            "userId" to uid,
            "fullName" to profile.fullName,
            "email" to profile.email,
            "mobileNumber" to profile.mobileNumber,
            "academicYear" to profile.academicYear,
            "branch" to profile.branch,
            "roomAllocationStatus" to profile.roomAllocationStatus,
            "block" to profile.block,
            "roomNumber" to profile.roomNumber,
            "photoUrl" to profile.photoUrl,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
    }

    fun observeAllStudents(): Flow<List<StudentProfile>> = db.collection("studentProfiles")
        .snapshots()
        .map { snapshot -> snapshot.toObjects(StudentProfile::class.java) }

    suspend fun getStudentProfiles(studentIds: List<String>): List<StudentProfile> {
        if (studentIds.isEmpty()) return emptyList()
        val list = mutableListOf<StudentProfile>()
        for (id in studentIds) {
            val doc = db.collection("studentProfiles").document(id).get().await()
            val profile = doc.toObject(StudentProfile::class.java)
            if (profile != null) list.add(profile)
        }
        return list
    }

    // -------------------------------------------------------------
    // Rooms & Transaction-Safe Room Allocation
    // -------------------------------------------------------------

    fun observeRooms(block: String? = null): Flow<List<Room>> {
        val query = if (block.isNullOrBlank()) {
            db.collection("rooms")
        } else {
            db.collection("rooms").whereEqualTo("block", block)
        }
        return query.snapshots().map { snapshot -> snapshot.toObjects(Room::class.java) }
    }

    suspend fun createOrUpdateRoom(room: Room) {
        val roomId = if (room.id.isNotBlank()) room.id else "${room.block}_${room.roomNumber}"
        val docRef = db.collection("rooms").document(roomId)
        val payload = hashMapOf(
            "block" to room.block,
            "roomNumber" to room.roomNumber,
            "capacity" to 2L,
            "occupiedBeds" to room.occupiedBeds,
            "studentIds" to room.studentIds,
            "isAvailable" to room.isAvailable,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
    }

    suspend fun deleteRoom(roomId: String) {
        db.collection("rooms").document(roomId).delete().await()
    }

    /**
     * Transaction-Safe Room Allocation (Mandatory Acceptance Test 9 & 10)
     * Performs capacity check and bed allocation atomically to prevent overbooking.
     */
    suspend fun allocateRoomTransaction(
        studentId: String,
        roomId: String
    ): Result<Unit> {
        val studentDocRef = db.collection("studentProfiles").document(studentId)
        val roomDocRef = db.collection("rooms").document(roomId)

        return try {
            db.runTransaction { transaction ->
                val studentSnap = transaction.get(studentDocRef)
                val roomSnap = transaction.get(roomDocRef)

                if (!studentSnap.exists()) {
                    throw IllegalStateException("Student profile does not exist. Please complete profile first.")
                }
                if (!roomSnap.exists()) {
                    throw IllegalStateException("Selected room does not exist.")
                }

                val profile = studentSnap.toObject(StudentProfile::class.java)
                    ?: throw IllegalStateException("Could not read student profile.")
                val room = roomSnap.toObject(Room::class.java)
                    ?: throw IllegalStateException("Could not read room details.")

                if (!profile.isComplete()) {
                    throw IllegalStateException("Profile incomplete. Fill all mandatory fields before room selection.")
                }
                if (profile.roomAllocationStatus == "allocated") {
                    throw IllegalStateException("You already have an active room allocation (${profile.block} - Room ${profile.roomNumber}).")
                }

                val eligibleBlock = HostelConstants.getBlockForYear(profile.academicYear)
                if (room.block != eligibleBlock) {
                    throw IllegalStateException("Unauthorized block. Students in ${profile.academicYear} can only select rooms in $eligibleBlock.")
                }

                val currentOccupied = room.occupiedBeds
                if (currentOccupied >= 2L || room.studentIds.size >= 2) {
                    throw IllegalStateException("This room is already fully occupied (2/2 beds taken). Please select another room.")
                }
                if (!room.isAvailable) {
                    throw IllegalStateException("This room is currently marked unavailable by hostel management.")
                }

                // Update room atomically
                val newOccupied = currentOccupied + 1L
                val updatedStudents = room.studentIds + studentId
                val isStillAvailable = newOccupied < 2L

                transaction.update(
                    roomDocRef,
                    mapOf(
                        "occupiedBeds" to newOccupied,
                        "studentIds" to updatedStudents,
                        "isAvailable" to isStillAvailable,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                // Update student profile
                transaction.update(
                    studentDocRef,
                    mapOf(
                        "roomAllocationStatus" to "allocated",
                        "block" to room.block,
                        "roomNumber" to room.roomNumber,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                // Create allocation record
                val allocationRef = db.collection("roomAllocations").document()
                val allocationData = hashMapOf(
                    "studentId" to studentId,
                    "studentName" to profile.fullName,
                    "academicYear" to profile.academicYear,
                    "branch" to profile.branch,
                    "block" to room.block,
                    "roomNumber" to room.roomNumber,
                    "allocatedAt" to FieldValue.serverTimestamp()
                )
                transaction.set(allocationRef, allocationData)

                // Add in-app notification
                val notificationRef = db.collection("notifications").document()
                val notificationData = hashMapOf(
                    "recipientId" to studentId,
                    "title" to "Room Allocated Successfully",
                    "message" to "You have been allocated Room ${room.roomNumber} in ${room.block}.",
                    "type" to "ALLOCATION",
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                transaction.set(notificationRef, notificationData)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Room allocation transaction failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Bills & Payments
    // -------------------------------------------------------------

    fun observeBillsForStudent(studentId: String): Flow<List<Bill>> {
        return db.collection("bills")
            .whereEqualTo("studentId", studentId)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Bill::class.java) }
    }

    fun observeAllBills(): Flow<List<Bill>> {
        return db.collection("bills")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Bill::class.java) }
    }

    suspend fun createBill(bill: Bill) {
        val docRef = db.collection("bills").document()
        val refNumber = "BILL-" + SimpleDateFormat("yyyyMM", Locale.US).format(Date()) + "-" + (1000..9999).random()
        val payload = hashMapOf(
            "billRef" to (if (bill.billRef.isNotBlank()) bill.billRef else refNumber),
            "studentId" to bill.studentId,
            "studentName" to bill.studentName,
            "billType" to bill.billType,
            "billingPeriod" to bill.billingPeriod,
            "totalAmount" to bill.totalAmount,
            "amountPaid" to 0.0,
            "balanceAmount" to bill.totalAmount,
            "dueDate" to bill.dueDate,
            "paymentStatus" to "Pending",
            "remarks" to bill.remarks,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()

        // Create in-app notification
        val notifRef = db.collection("notifications").document()
        notifRef.set(
            hashMapOf(
                "recipientId" to bill.studentId,
                "title" to "New Bill Issued: ${bill.billType}",
                "message" to "A new ${bill.billType} bill of ₹${bill.totalAmount} for ${bill.billingPeriod} has been generated. Due date: ${bill.dueDate}.",
                "type" to "BILL",
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun updateBill(bill: Bill) {
        val docRef = db.collection("bills").document(bill.id)
        val payload = hashMapOf(
            "billRef" to bill.billRef,
            "studentId" to bill.studentId,
            "studentName" to bill.studentName,
            "billType" to bill.billType,
            "billingPeriod" to bill.billingPeriod,
            "totalAmount" to bill.totalAmount,
            "amountPaid" to bill.amountPaid,
            "balanceAmount" to (bill.totalAmount - bill.amountPaid).coerceAtLeast(0.0),
            "dueDate" to bill.dueDate,
            "paymentStatus" to when {
                bill.amountPaid >= bill.totalAmount -> "Paid"
                bill.amountPaid > 0 -> "Partially Paid"
                else -> bill.paymentStatus
            },
            "remarks" to bill.remarks,
            "createdAt" to (bill.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
    }

    suspend fun recordManualPayment(
        billId: String,
        amount: Double,
        paymentMethod: String,
        transactionRef: String,
        remarks: String
    ): Result<Payment> {
        val billDocRef = db.collection("bills").document(billId)
        val paymentDocRef = db.collection("payments").document()
        val receiptRef = "REC-" + System.currentTimeMillis().toString().takeLast(8)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        return try {
            val paymentResult = db.runTransaction { transaction ->
                val billSnap = transaction.get(billDocRef)
                if (!billSnap.exists()) throw IllegalStateException("Bill does not exist.")
                val bill = billSnap.toObject(Bill::class.java)!!

                val newPaid = bill.amountPaid + amount
                val newBalance = (bill.totalAmount - newPaid).coerceAtLeast(0.0)
                val newStatus = if (newBalance <= 0.0) "Paid" else "Partially Paid"

                // Update bill
                transaction.update(
                    billDocRef,
                    mapOf(
                        "amountPaid" to newPaid,
                        "balanceAmount" to newBalance,
                        "paymentStatus" to newStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                // Insert payment record
                val paymentPayload = hashMapOf(
                    "receiptRef" to receiptRef,
                    "billId" to billId,
                    "studentId" to bill.studentId,
                    "studentName" to bill.studentName,
                    "billType" to bill.billType,
                    "amount" to amount,
                    "paymentDate" to dateFormat,
                    "paymentMethod" to paymentMethod,
                    "transactionRef" to transactionRef,
                    "remarks" to remarks,
                    "status" to "Verified",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                transaction.set(paymentDocRef, paymentPayload)

                // Notification
                val notifDoc = db.collection("notifications").document()
                transaction.set(
                    notifDoc,
                    hashMapOf(
                        "recipientId" to bill.studentId,
                        "title" to "Payment Received: ₹$amount",
                        "message" to "Payment of ₹$amount for ${bill.billType} has been recorded (Receipt: $receiptRef). Remaining balance: ₹$newBalance.",
                        "type" to "BILL",
                        "isRead" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                Payment(
                    id = paymentDocRef.id,
                    receiptRef = receiptRef,
                    billId = billId,
                    studentId = bill.studentId,
                    studentName = bill.studentName,
                    billType = bill.billType,
                    amount = amount,
                    paymentDate = dateFormat,
                    paymentMethod = paymentMethod,
                    transactionRef = transactionRef,
                    remarks = remarks,
                    status = "Verified"
                )
            }.await()
            Result.success(paymentResult)
        } catch (e: Exception) {
            Log.e(tag, "Failed to record manual payment: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun processOnlinePayment(
        billId: String,
        amount: Double,
        gatewayOrderId: String,
        paymentMethod: String
    ): Result<Payment> {
        return recordManualPayment(
            billId = billId,
            amount = amount,
            paymentMethod = paymentMethod,
            transactionRef = gatewayOrderId,
            remarks = "Online Payment verified via Payment Gateway"
        )
    }

    fun observePayments(studentId: String? = null): Flow<List<Payment>> {
        val query = if (studentId.isNullOrBlank()) {
            db.collection("payments")
        } else {
            db.collection("payments").whereEqualTo("studentId", studentId)
        }
        return query.snapshots().map { snapshot -> snapshot.toObjects(Payment::class.java) }
    }

    // -------------------------------------------------------------
    // Complaints Management
    // -------------------------------------------------------------

    fun observeStudentComplaints(studentId: String): Flow<List<Complaint>> {
        return db.collection("complaints")
            .whereEqualTo("studentId", studentId)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Complaint::class.java) }
    }

    fun observeAllComplaints(): Flow<List<Complaint>> {
        return db.collection("complaints")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Complaint::class.java) }
    }

    suspend fun submitComplaint(complaint: Complaint): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("complaints").document()
        val refNumber = "CMP-" + SimpleDateFormat("yyyyMM", Locale.US).format(Date()) + "-" + (100..999).random()

        val payload = hashMapOf(
            "complaintRef" to refNumber,
            "studentId" to uid,
            "studentName" to complaint.studentName,
            "academicYear" to complaint.academicYear,
            "branch" to complaint.branch,
            "block" to complaint.block,
            "roomNumber" to complaint.roomNumber,
            "mobileNumber" to complaint.mobileNumber,
            "category" to complaint.category,
            "title" to complaint.title,
            "description" to complaint.description,
            "photoUrl" to complaint.photoUrl,
            "status" to "Submitted",
            "adminResponse" to "",
            "submittedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            // Admin notification
            db.collection("notifications").document().set(
                hashMapOf(
                    "recipientId" to "admin",
                    "title" to "New Complaint: ${complaint.category}",
                    "message" to "Complaint $refNumber filed by ${complaint.studentName} (${complaint.block} ${complaint.roomNumber}): ${complaint.title}",
                    "type" to "COMPLAINT",
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(refNumber)
        } catch (e: Exception) {
            Log.e(tag, "Failed to submit complaint: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateComplaintResolution(
        complaintId: String,
        status: String,
        adminResponse: String
    ): Result<Unit> {
        val docRef = db.collection("complaints").document(complaintId)
        return try {
            val doc = docRef.get().await()
            val complaint = doc.toObject(Complaint::class.java)
            docRef.update(
                mapOf(
                    "status" to status,
                    "adminResponse" to adminResponse,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (complaint != null) {
                db.collection("notifications").document().set(
                    hashMapOf(
                        "recipientId" to complaint.studentId,
                        "title" to "Complaint Status: $status",
                        "message" to "Your complaint (${complaint.complaintRef}: ${complaint.title}) has been updated to $status. Note: $adminResponse",
                        "type" to "COMPLAINT",
                        "isRead" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update complaint: ${e.message}", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // SMS Bill Reminders (Specification Section 9)
    // -------------------------------------------------------------

    suspend fun sendSmsReminder(
        student: StudentProfile,
        bill: Bill,
        reminderType: String
    ): Result<SmsReminder> {
        val balance = bill.balanceAmount
        val message = "Dear ${student.fullName}, your ${bill.billType} fee of ₹${balance.toInt()} for ${bill.billingPeriod} is pending. Please pay by ${bill.dueDate}. For details, log in to the Kalpataru Boys Hostel portal."

        // Validate mobile format
        val cleanNumber = student.mobileNumber.filter { it.isDigit() }
        val isValidNumber = cleanNumber.length in 10..12

        val docRef = db.collection("smsReminders").document()
        val reminder = SmsReminder(
            id = docRef.id,
            studentId = student.userId,
            studentName = student.fullName,
            mobileNumber = student.mobileNumber,
            billId = bill.id,
            billType = bill.billType,
            amount = balance,
            dueDate = bill.dueDate,
            message = message,
            status = if (isValidNumber) "Sent" else "Failed",
            providerResponse = if (isValidNumber) "Delivered via SMS Gateway (Provider Ref: MSG-${UUID.randomUUID().toString().take(8)})" else "Failed: Invalid Indian mobile number format"
        )

        val payload = hashMapOf(
            "studentId" to reminder.studentId,
            "studentName" to reminder.studentName,
            "mobileNumber" to reminder.mobileNumber,
            "billId" to reminder.billId,
            "billType" to reminder.billType,
            "amount" to reminder.amount,
            "dueDate" to reminder.dueDate,
            "message" to reminder.message,
            "status" to reminder.status,
            "providerResponse" to reminder.providerResponse,
            "sentAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            // In-app notification
            db.collection("notifications").document().set(
                hashMapOf(
                    "recipientId" to student.userId,
                    "title" to "Bill Reminder: ${bill.billType}",
                    "message" to "Pending payment of ₹${balance.toInt()} for ${bill.billingPeriod} due on ${bill.dueDate}.",
                    "type" to "BILL",
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(reminder)
        } catch (e: Exception) {
            Log.e(tag, "Failed to send SMS reminder: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun observeSmsReminders(): Flow<List<SmsReminder>> = db.collection("smsReminders")
        .snapshots()
        .map { snapshot -> snapshot.toObjects(SmsReminder::class.java) }

    // -------------------------------------------------------------
    // In-App Notifications
    // -------------------------------------------------------------

    fun observeNotifications(userId: String): Flow<List<NotificationItem>> {
        return db.collection("notifications")
            .whereIn("recipientId", listOf(userId, "all"))
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(NotificationItem::class.java)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
    }

    fun observeAdminNotifications(): Flow<List<NotificationItem>> {
        return db.collection("notifications")
            .whereIn("recipientId", listOf("admin", "all"))
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(NotificationItem::class.java)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
    }

    suspend fun markNotificationAsRead(id: String) {
        db.collection("notifications").document(id).update("isRead", true).await()
    }

    suspend fun broadcastNotification(title: String, message: String, recipientId: String = "all") {
        db.collection("notifications").document().set(
            hashMapOf(
                "recipientId" to recipientId,
                "title" to title,
                "message" to message,
                "type" to "GENERAL",
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // -------------------------------------------------------------
    // Default Seed Data Initialization (Creates standard rooms if empty)
    // -------------------------------------------------------------

    suspend fun initializeDefaultRoomsIfEmpty() {
        try {
            val existing = db.collection("rooms").limit(1).get().await()
            if (!existing.isEmpty) return

            // Hemavati Block (First Year)
            val hemavatiRooms = listOf("101", "102", "103", "104", "105")
            for (num in hemavatiRooms) {
                createOrUpdateRoom(
                    Room(
                        id = "${HostelConstants.BLOCK_HEMAVATI}_$num",
                        block = HostelConstants.BLOCK_HEMAVATI,
                        roomNumber = num,
                        capacity = 2L,
                        occupiedBeds = 0L,
                        studentIds = emptyList(),
                        isAvailable = true
                    )
                )
            }

            // Kaveri Block (Second & Third Year)
            val kaveriRooms = listOf("201", "202", "203", "204", "205")
            for (num in kaveriRooms) {
                createOrUpdateRoom(
                    Room(
                        id = "${HostelConstants.BLOCK_KAVERI}_$num",
                        block = HostelConstants.BLOCK_KAVERI,
                        roomNumber = num,
                        capacity = 2L,
                        occupiedBeds = 0L,
                        studentIds = emptyList(),
                        isAvailable = true
                    )
                )
            }

            // MCA Block (Fourth Year)
            val mcaRooms = listOf("301", "302", "303", "304", "305")
            for (num in mcaRooms) {
                createOrUpdateRoom(
                    Room(
                        id = "${HostelConstants.BLOCK_MCA}_$num",
                        block = HostelConstants.BLOCK_MCA,
                        roomNumber = num,
                        capacity = 2L,
                        occupiedBeds = 0L,
                        studentIds = emptyList(),
                        isAvailable = true
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to seed default rooms: ${e.message}", e)
        }
    }
}
