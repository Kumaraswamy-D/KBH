package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Complaint
import com.example.data.model.HostelConstants
import com.example.data.model.Room
import com.example.data.model.StudentProfile
import com.example.data.repository.HostelRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

import kotlinx.coroutines.tasks.await

class HostelRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun testCreateAndReadStudentProfile() = runBlocking {
    val uid = signInTestUser("alice.student@example.com")
    val repository = HostelRepository(firestore, auth)
    repository.saveUserRecord(com.example.data.model.UserRecord(id = uid, email = "alice.student@example.com", name = "Alice Sharma", role = "student"))

    val profile = StudentProfile(
      userId = uid,
      fullName = "Alice Sharma",
      email = "alice.student@example.com",
      mobileNumber = "9876543210",
      academicYear = HostelConstants.YEAR_FIRST,
      branch = "Computer Science & Engineering",
      roomAllocationStatus = "unallocated",
      block = "",
      roomNumber = "",
      photoUrl = ""
    )

    withTimeout(10000L) {
      repository.saveStudentProfile(profile)
    }

    val retrieved = withTimeout(10000L) {
      val doc = firestore.collection("studentProfiles").document(uid).get().await()
      doc.toObject(StudentProfile::class.java)
    }

    assertNotNull(retrieved)
    assertEquals("Alice Sharma", retrieved?.fullName)
    assertEquals(HostelConstants.YEAR_FIRST, retrieved?.academicYear)
  }

  @Test
  fun testRoomAllocationTransaction() = runBlocking {
    // First sign in as admin to create the room inventory
    val adminUid = signInTestUser("admin.hostel@example.com")
    val adminRepo = HostelRepository(firestore, auth)
    adminRepo.saveUserRecord(com.example.data.model.UserRecord(id = adminUid, email = "admin.hostel@example.com", name = "Admin", role = "admin"))

    val room = Room(
      id = "Hemavati Block_101",
      block = HostelConstants.BLOCK_HEMAVATI,
      roomNumber = "101",
      capacity = 2L,
      occupiedBeds = 0L,
      studentIds = emptyList(),
      isAvailable = true
    )
    withTimeout(10000L) {
      adminRepo.createOrUpdateRoom(room)
    }

    // Now sign in as student Bob to test student room booking
    val uid = signInTestUser("bob.student@example.com")
    val repository = HostelRepository(firestore, auth)
    repository.saveUserRecord(com.example.data.model.UserRecord(id = uid, email = "bob.student@example.com", name = "Bob Patel", role = "student"))

    val profile = StudentProfile(
      userId = uid,
      fullName = "Bob Patel",
      email = "bob.student@example.com",
      mobileNumber = "9123456780",
      academicYear = HostelConstants.YEAR_FIRST,
      branch = "Information Science & Engineering",
      roomAllocationStatus = "unallocated",
      block = "",
      roomNumber = "",
      photoUrl = ""
    )
    withTimeout(10000L) {
      repository.saveStudentProfile(profile)
    }

    val result = withTimeout(10000L) {
      repository.allocateRoomTransaction(uid, "Hemavati Block_101")
    }

    assertTrue(result.isSuccess)

    // Verify bed occupied is now 1
    val updatedRoomDoc = withTimeout(10000L) {
      firestore.collection("rooms").document("Hemavati Block_101").get().await()
    }
    val updatedRoom = updatedRoomDoc.toObject(Room::class.java)
    assertEquals(1L, updatedRoom?.occupiedBeds)
    assertTrue(updatedRoom?.studentIds?.contains(uid) == true)
  }

  @Test
  fun testSubmitComplaint() = runBlocking {
    val uid = signInTestUser("charlie@example.com")
    val repository = HostelRepository(firestore, auth)

    val complaint = Complaint(
      studentId = uid,
      studentName = "Charlie Rao",
      academicYear = HostelConstants.YEAR_FIRST,
      branch = "ECE",
      block = HostelConstants.BLOCK_HEMAVATI,
      roomNumber = "101",
      mobileNumber = "9988776655",
      category = "Electricity",
      title = "Fan not working",
      description = "Ceiling fan is spinning slowly and making noise.",
      photoUrl = ""
    )

    val result = withTimeout(10000L) {
      repository.submitComplaint(complaint)
    }

    assertTrue(result.isSuccess)
    assertNotNull(result.getOrNull())
  }
}
