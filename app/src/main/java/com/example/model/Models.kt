package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ComplaintCategory(val label: String) {
    GARBAGE_OVERFLOW("Garbage Overflow"),
    ROADSIDE_WASTE("Roadside Waste"),
    ILLEGAL_DUMPING("Illegal Dumping"),
    DRAINAGE_ISSUES("Drainage Issues"),
    PUBLIC_AREA_CLEANLINESS("Public Area Cleanliness"),
    OTHER("Other");

    companion object {
        fun fromLabel(raw: String): ComplaintCategory {
            return entries.firstOrNull { it.label == raw || it.name == raw } ?: OTHER
        }
    }
}

enum class ComplaintStatus(val label: String) {
    REPORTED("Reported"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved");

    companion object {
        fun fromLabel(raw: String): ComplaintStatus {
            return entries.firstOrNull { it.label == raw || it.name == raw } ?: REPORTED
        }
    }
}

enum class UserRole(val key: String) {
    CITIZEN("citizen"),
    ADMIN("admin");

    companion object {
        fun fromKey(raw: String): UserRole {
            return entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: CITIZEN
        }
    }
}

@Keep
@IgnoreExtraProperties
data class StatusHistoryEntry(
    val status: String = ComplaintStatus.REPORTED.label,
    val remark: String = "",
    val updatedBy: String = "",
    val timestampMillis: Long = 0L
)

@Keep
@IgnoreExtraProperties
data class Complaint(
    @DocumentId val id: String = "",
    val complaintCode: String = "",
    val userId: String = "",
    val citizenName: String = "",
    val citizenEmail: String = "",
    val citizenPhone: String = "",
    val title: String = "",
    val category: String = ComplaintCategory.OTHER.label,
    val description: String = "",
    val locationText: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageBase64: String = "",
    val status: String = ComplaintStatus.REPORTED.label,
    val adminRemarks: String = "",
    val statusHistory: List<StatusHistoryEntry> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: ComplaintCategory get() = ComplaintCategory.fromLabel(category)
    val statusEnum: ComplaintStatus get() = ComplaintStatus.fromLabel(status)
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val ward: String = "",
    val role: String = UserRole.CITIZEN.key,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
