package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserRecord(
    @DocumentId val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "student", // "student" or "admin"
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudentProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val mobileNumber: String = "",
    val academicYear: String = "First Year", // "First Year", "Second Year", "Third Year", "Fourth Year"
    val branch: String = "",
    val roomAllocationStatus: String = "unallocated", // "unallocated" or "allocated"
    val block: String = "", // "Hemavati Block", "Kaveri Block", "MCA Block"
    val roomNumber: String = "",
    val photoUrl: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isComplete(): Boolean {
        return fullName.isNotBlank() &&
                mobileNumber.isNotBlank() &&
                academicYear.isNotBlank() &&
                branch.isNotBlank()
    }
}

@IgnoreExtraProperties
data class Room(
    @DocumentId val id: String = "", // e.g., "Hemavati_101"
    val block: String = "Hemavati Block",
    val roomNumber: String = "",
    val capacity: Long = 2L,
    val occupiedBeds: Long = 0L,
    val studentIds: List<String> = emptyList(),
    val isAvailable: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class RoomAllocation(
    @DocumentId val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val academicYear: String = "",
    val branch: String = "",
    val block: String = "",
    val roomNumber: String = "",
    @ServerTimestamp val allocatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Bill(
    @DocumentId val id: String = "",
    val billRef: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val billType: String = "Room Fee", // "Room Fee", "Mess Fee"
    val billingPeriod: String = "",
    val totalAmount: Double = 0.0,
    val amountPaid: Double = 0.0,
    val balanceAmount: Double = 0.0,
    val dueDate: String = "",
    val paymentStatus: String = "Pending", // "Pending", "Partially Paid", "Paid", "Overdue"
    val remarks: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Payment(
    @DocumentId val id: String = "",
    val receiptRef: String = "",
    val billId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val billType: String = "",
    val amount: Double = 0.0,
    val paymentDate: String = "",
    val paymentMethod: String = "Cash", // "Cash", "Bank Transfer", "UPI", "Online"
    val transactionRef: String = "",
    val remarks: String = "",
    val status: String = "Verified", // "Verified", "Pending", "Failed"
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Complaint(
    @DocumentId val id: String = "",
    val complaintRef: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val academicYear: String = "",
    val branch: String = "",
    val block: String = "",
    val roomNumber: String = "",
    val mobileNumber: String = "",
    val category: String = "Room maintenance", // "Room maintenance", "Electricity", "Water supply", "Bathroom", "Cleaning", "Mess food", "Furniture", "Other"
    val title: String = "",
    val description: String = "",
    val photoUrl: String = "",
    val status: String = "Submitted", // "Submitted", "In Progress", "Resolved", "Rejected"
    val adminResponse: String = "",
    @ServerTimestamp val submittedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SmsReminder(
    @DocumentId val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val mobileNumber: String = "",
    val billId: String = "",
    val billType: String = "",
    val amount: Double = 0.0,
    val dueDate: String = "",
    val message: String = "",
    val status: String = "Sent", // "Sent", "Failed", "Pending", "Delivered"
    val providerResponse: String = "",
    @ServerTimestamp val sentAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val recipientId: String = "", // student UID, "all", or "admin"
    val title: String = "",
    val message: String = "",
    val type: String = "GENERAL", // "BILL", "COMPLAINT", "ALLOCATION", "GENERAL"
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

object HostelConstants {
    const val BLOCK_HEMAVATI = "Hemavati Block"
    const val BLOCK_KAVERI = "Kaveri Block"
    const val BLOCK_MCA = "MCA Block"

    const val YEAR_FIRST = "First Year"
    const val YEAR_SECOND = "Second Year"
    const val YEAR_THIRD = "Third Year"
    const val YEAR_FOURTH = "Fourth Year"

    val ACADEMIC_YEARS = listOf(YEAR_FIRST, YEAR_SECOND, YEAR_THIRD, YEAR_FOURTH)

    val BRANCHES = listOf(
        "Computer Science & Engineering",
        "Information Science & Engineering",
        "Electronics & Communication Engineering",
        "Mechanical Engineering",
        "Civil Engineering",
        "Electrical & Electronics Engineering",
        "Master of Computer Applications (MCA)"
    )

    val COMPLAINT_CATEGORIES = listOf(
        "Room maintenance",
        "Electricity",
        "Water supply",
        "Bathroom",
        "Cleaning",
        "Mess food",
        "Furniture",
        "Other"
    )

    fun getBlockForYear(year: String): String {
        return when (year) {
            YEAR_FIRST -> BLOCK_HEMAVATI
            YEAR_SECOND, YEAR_THIRD -> BLOCK_KAVERI
            YEAR_FOURTH -> BLOCK_MCA
            else -> BLOCK_HEMAVATI
        }
    }
}
