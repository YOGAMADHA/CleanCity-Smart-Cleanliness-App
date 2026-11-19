package com.example.data

import android.content.Context
import com.example.R
import com.example.model.Complaint
import com.example.model.ComplaintCategory
import com.example.model.ComplaintStatus
import com.example.model.StatusHistoryEntry
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID

data class SubmittedComplaintResult(
    val documentId: String,
    val complaintCode: String
)

class CleanCityRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val BOOTSTRAP_ADMIN_EMAIL = "yogamadha126@gmail.com"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_ADMINS = "admins"
        private const val COLLECTION_COMPLAINTS = "complaints"
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun checkIsAdmin(user: FirebaseUser): Boolean {
        if (user.isEmailVerified && user.email.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)) {
            return true
        }
        return try {
            db.collection(COLLECTION_ADMINS).document(user.uid).get().await().exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "$COLLECTION_ADMINS/${user.uid}")
            false
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "$COLLECTION_USERS/$userId"
        emitAll(
            db.collection(COLLECTION_USERS).document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveCitizenProfile(
        name: String,
        email: String,
        phone: String,
        ward: String
    ): Result<UserProfile> {
        var docPath = COLLECTION_USERS
        return try {
            val uid = requireUserId()
            val trimmedName = name.trim()
            val trimmedEmail = email.trim()
            val trimmedPhone = phone.trim()
            val trimmedWard = ward.trim()

            require(trimmedName.length in 1..80) { "Name must be between 1 and 80 characters." }
            require(trimmedEmail.length in 3..120) { "Valid email is required." }
            require(trimmedPhone.length in 7..20) { "Phone number must be between 7 and 20 characters." }
            require(trimmedWard.length <= 80) { "Ward/Zone must be 80 characters or fewer." }

            val docRef = db.collection(COLLECTION_USERS).document(uid)
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "name" to trimmedName,
                "email" to trimmedEmail,
                "phone" to trimmedPhone,
                "ward" to trimmedWard,
                "role" to "citizen",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(
                UserProfile(
                    id = uid,
                    userId = uid,
                    name = trimmedName,
                    email = trimmedEmail,
                    phone = trimmedPhone,
                    ward = trimmedWard,
                    role = "citizen"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCitizenProfile(
        userId: String,
        name: String,
        phone: String,
        ward: String
    ): Result<Unit> {
        var docPath = "$COLLECTION_USERS/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val trimmedName = name.trim()
            val trimmedPhone = phone.trim()
            val trimmedWard = ward.trim()
            require(trimmedName.length in 1..80) { "Name must be between 1 and 80 characters." }
            require(trimmedPhone.length in 7..20) { "Phone number must be between 7 and 20 characters." }
            require(trimmedWard.length <= 80) { "Ward/Zone must be 80 characters or fewer." }

            val docRef = db.collection(COLLECTION_USERS).document(uid)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "name" to trimmedName,
                    "phone" to trimmedPhone,
                    "ward" to trimmedWard,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeCitizenComplaints(userId: String): Flow<List<Complaint>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        emitAll(
            db.collection(COLLECTION_COMPLAINTS)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Complaint::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, COLLECTION_COMPLAINTS)
                    }
                    throw error
                }
        )
    }

    fun observeAllComplaints(): Flow<List<Complaint>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection(COLLECTION_COMPLAINTS)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Complaint::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, COLLECTION_COMPLAINTS)
                    }
                    throw error
                }
        )
    }

    fun observeComplaintById(complaintId: String): Flow<Complaint?> = flow {
        val uid = requireUserId()
        require(complaintId.isNotBlank()) { "Complaint ID must not be blank" }
        val path = "$COLLECTION_COMPLAINTS/$complaintId"
        emitAll(
            db.collection(COLLECTION_COMPLAINTS).document(complaintId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            Complaint::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun submitComplaint(
        citizenName: String,
        citizenEmail: String,
        citizenPhone: String,
        title: String,
        category: ComplaintCategory,
        description: String,
        locationText: String,
        latitude: Double?,
        longitude: Double?,
        imageBase64: String
    ): Result<SubmittedComplaintResult> {
        var docPath = COLLECTION_COMPLAINTS
        return try {
            val uid = requireUserId()
            val trimmedTitle = title.trim()
            val trimmedDesc = description.trim()
            val trimmedLoc = locationText.trim()
            val safeName = citizenName.trim().ifBlank { "Citizen" }.take(80)
            val safeEmail = citizenEmail.trim().ifBlank { "citizen@cleancity.app" }.take(120)
            val safePhone = citizenPhone.trim().take(20)

            require(trimmedTitle.length in 3..120) { "Title must be between 3 and 120 characters." }
            require(trimmedDesc.length in 5..1000) { "Description must be between 5 and 1000 characters." }
            require(trimmedLoc.length in 2..200) { "Location must be between 2 and 200 characters." }
            require(imageBase64.length <= 350_000) { "Attached photo exceeds maximum allowed size." }

            val year = Calendar.getInstance().get(Calendar.YEAR)
            val codeSuffix = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
            val complaintCode = "CC-$year-$codeSuffix"

            val docRef = db.collection(COLLECTION_COMPLAINTS).document()
            docPath = docRef.path

            val initialHistory = listOf(
                mapOf(
                    "status" to ComplaintStatus.REPORTED.label,
                    "remark" to "Complaint registered by citizen.",
                    "updatedBy" to safeName,
                    "timestampMillis" to System.currentTimeMillis()
                )
            )

            val payload = mapOf(
                "complaintCode" to complaintCode,
                "userId" to uid,
                "citizenName" to safeName,
                "citizenEmail" to safeEmail,
                "citizenPhone" to safePhone,
                "title" to trimmedTitle,
                "category" to category.label,
                "description" to trimmedDesc,
                "locationText" to trimmedLoc,
                "latitude" to latitude,
                "longitude" to longitude,
                "imageBase64" to imageBase64,
                "status" to ComplaintStatus.REPORTED.label,
                "adminRemarks" to "",
                "statusHistory" to initialHistory,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(SubmittedComplaintResult(docRef.id, complaintCode))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateComplaintStatusAndRemarks(
        complaintId: String,
        currentHistory: List<StatusHistoryEntry>,
        newStatus: ComplaintStatus,
        adminRemarks: String,
        adminName: String
    ): Result<Unit> {
        var docPath = "$COLLECTION_COMPLAINTS/$complaintId"
        return try {
            requireUserId()
            require(complaintId.isNotBlank()) { "Complaint ID must not be blank." }
            val trimmedRemark = adminRemarks.trim()
            require(trimmedRemark.length <= 500) { "Remarks must be 500 characters or fewer." }

            val entryRemark = trimmedRemark.ifBlank {
                "Status updated to ${newStatus.label}"
            }
            val updatedHistory = (currentHistory.takeLast(28) + StatusHistoryEntry(
                status = newStatus.label,
                remark = entryRemark,
                updatedBy = adminName.trim().ifBlank { "City Admin" }.take(80),
                timestampMillis = System.currentTimeMillis()
            )).map { entry ->
                mapOf(
                    "status" to entry.status,
                    "remark" to entry.remark,
                    "updatedBy" to entry.updatedBy,
                    "timestampMillis" to entry.timestampMillis
                )
            }

            val docRef = db.collection(COLLECTION_COMPLAINTS).document(complaintId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to newStatus.label,
                    "adminRemarks" to trimmedRemark,
                    "statusHistory" to updatedHistory,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun seedSampleReportsForAdmin(
        adminName: String,
        adminEmail: String
    ): Result<Int> {
        var docPath = COLLECTION_COMPLAINTS
        return try {
            val uid = requireUserId()
            val year = Calendar.getInstance().get(Calendar.YEAR)
            val nowMillis = System.currentTimeMillis()
            val safeName = adminName.ifBlank { "Civic Inspector" }.take(80)
            val safeEmail = adminEmail.ifBlank { BOOTSTRAP_ADMIN_EMAIL }.take(120)

            val samples = listOf(
                Triple(
                    "Overflowing Smart Bin at Central Plaza",
                    ComplaintCategory.GARBAGE_OVERFLOW,
                    "Central Plaza Metro Exit 2, Ward 4"
                ) to "Public twin-bin receptacle has reached capacity after weekend market traffic and requires immediate compaction pickup.",
                Triple(
                    "Blocked Storm Drain Near Park Avenue",
                    ComplaintCategory.DRAINAGE_ISSUES,
                    "Park Avenue & 12th Cross, Ward 2"
                ) to "Plastic debris and fallen leaves are blocking the stormwater grate, causing minor pooling near the pedestrian crossing.",
                Triple(
                    "Construction Debris Dumped on Sidewalk",
                    ComplaintCategory.ILLEGAL_DUMPING,
                    "Greenbelt Service Road, Ward 7"
                ) to "Unattended rubble bags and broken tiles left along the cycle path overnight. Needs municipal haulage crew."
            )

            val batch = db.batch()
            samples.forEachIndexed { index, (meta, desc) ->
                val (title, category, location) = meta
                val docRef = db.collection(COLLECTION_COMPLAINTS).document()
                docPath = docRef.path
                val code = "CC-$year-DEMO${index + 1}"
                val payload = mapOf(
                    "complaintCode" to code,
                    "userId" to uid,
                    "citizenName" to safeName,
                    "citizenEmail" to safeEmail,
                    "citizenPhone" to "+1 555-0140",
                    "title" to title,
                    "category" to category.label,
                    "description" to desc,
                    "locationText" to location,
                    "imageBase64" to "",
                    "status" to ComplaintStatus.REPORTED.label,
                    "adminRemarks" to "",
                    "statusHistory" to listOf(
                        mapOf(
                            "status" to ComplaintStatus.REPORTED.label,
                            "remark" to "Sample city report created for demonstration.",
                            "updatedBy" to safeName,
                            "timestampMillis" to (nowMillis - (index * 3_600_000L))
                        )
                    ),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, payload)
            }
            batch.commit().await()
            Result.success(samples.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }
}
