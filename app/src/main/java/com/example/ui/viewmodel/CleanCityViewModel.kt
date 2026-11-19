package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CleanCityRepository
import com.example.data.SubmittedComplaintResult
import com.example.model.Complaint
import com.example.model.ComplaintCategory
import com.example.model.ComplaintStatus
import com.example.model.StatusHistoryEntry
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.UserRole
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CleanCityViewModel(
    private val repository: CleanCityRepository,
    private val currentUserId: String,
    private val firebaseUser: FirebaseUser,
    initialPortalRole: UserRole
) : ViewModel() {

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _adminCheckDone = MutableStateFlow(false)
    val adminCheckDone: StateFlow<Boolean> = _adminCheckDone.asStateFlow()

    private val _activeRoleMode = MutableStateFlow(UserRole.CITIZEN)
    val activeRoleMode: StateFlow<UserRole> = _activeRoleMode.asStateFlow()

    private val _actionLoading = MutableStateFlow(false)
    val actionLoading: StateFlow<Boolean> = _actionLoading.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _actionSuccess = MutableStateFlow<String?>(null)
    val actionSuccess: StateFlow<String?> = _actionSuccess.asStateFlow()

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing profile", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load user profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val citizenComplaintsState: StateFlow<UiState<List<Complaint>>> =
        repository.observeCitizenComplaints(currentUserId)
            .map<List<Complaint>, UiState<List<Complaint>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing citizen complaints", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load your complaints"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val adminComplaintsState: StateFlow<UiState<List<Complaint>>> =
        _isAdmin
            .flatMapLatest { admin ->
                if (!admin) {
                    flowOf<UiState<List<Complaint>>>(UiState.Success(emptyList()))
                } else {
                    repository.observeAllComplaints()
                        .map<List<Complaint>, UiState<List<Complaint>>> { UiState.Success(it) }
                        .catch { error ->
                            val firestoreError = generateSequence(error) { it.cause }
                                .filterIsInstance<FirebaseFirestoreException>()
                                .firstOrNull()
                            if (firestoreError == null && error is CancellationException) throw error
                            val rootError = firestoreError ?: error
                            Log.w(TAG, "Error observing all complaints for admin", rootError)
                            emit(UiState.Error(rootError.message ?: "Failed to load city complaints"))
                        }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    init {
        viewModelScope.launch {
            val adminVerified = repository.checkIsAdmin(firebaseUser)
            _isAdmin.value = adminVerified
            if (adminVerified && initialPortalRole == UserRole.ADMIN) {
                _activeRoleMode.value = UserRole.ADMIN
            } else {
                _activeRoleMode.value = UserRole.CITIZEN
            }
            _adminCheckDone.value = true
        }
    }

    fun observeComplaintDetail(complaintId: String): StateFlow<UiState<Complaint?>> {
        return repository.observeComplaintById(complaintId)
            .map<Complaint?, UiState<Complaint?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing complaint $complaintId", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load complaint details"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )
    }

    fun switchRoleMode(targetRole: UserRole) {
        if (targetRole == UserRole.ADMIN && !_isAdmin.value) {
            _actionError.value = "This Google account is not authorized as a CleanCity Admin."
            return
        }
        _activeRoleMode.value = targetRole
    }

    fun completeCitizenRegistration(
        name: String,
        phone: String,
        ward: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _actionLoading.value = true
            _actionError.value = null
            val email = firebaseUser.email ?: "citizen@cleancity.app"
            val result = repository.saveCitizenProfile(name, email, phone, ward)
            _actionLoading.value = false
            result.onSuccess {
                _actionSuccess.value = "Citizen profile registered!"
                onSuccess()
            }.onFailure { err ->
                _actionError.value = err.message ?: "Failed to save profile."
            }
        }
    }

    fun updateProfile(
        name: String,
        phone: String,
        ward: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _actionLoading.value = true
            _actionError.value = null
            val result = repository.updateCitizenProfile(currentUserId, name, phone, ward)
            _actionLoading.value = false
            result.onSuccess {
                _actionSuccess.value = "Profile updated successfully."
                onSuccess()
            }.onFailure { err ->
                _actionError.value = err.message ?: "Failed to update profile."
            }
        }
    }

    fun submitComplaint(
        title: String,
        category: ComplaintCategory,
        description: String,
        locationText: String,
        latitude: Double?,
        longitude: Double?,
        imageBase64: String,
        onSubmitted: (SubmittedComplaintResult) -> Unit
    ) {
        viewModelScope.launch {
            _actionLoading.value = true
            _actionError.value = null
            val profile = (userProfileState.value as? UiState.Success)?.data
            val citizenName = profile?.name?.takeIf { it.isNotBlank() }
                ?: firebaseUser.displayName?.takeIf { it.isNotBlank() }
                ?: "Citizen"
            val citizenEmail = profile?.email?.takeIf { it.isNotBlank() }
                ?: firebaseUser.email
                ?: "citizen@cleancity.app"
            val citizenPhone = profile?.phone.orEmpty()

            val result = repository.submitComplaint(
                citizenName = citizenName,
                citizenEmail = citizenEmail,
                citizenPhone = citizenPhone,
                title = title,
                category = category,
                description = description,
                locationText = locationText,
                latitude = latitude,
                longitude = longitude,
                imageBase64 = imageBase64
            )
            _actionLoading.value = false
            result.onSuccess { submitted ->
                onSubmitted(submitted)
            }.onFailure { err ->
                _actionError.value = err.message ?: "Failed to submit complaint."
            }
        }
    }

    fun updateComplaintByAdmin(
        complaintId: String,
        currentHistory: List<StatusHistoryEntry>,
        newStatus: ComplaintStatus,
        adminRemarks: String,
        onUpdated: () -> Unit = {}
    ) {
        if (!_isAdmin.value) {
            _actionError.value = "Admin privileges are required to update complaints."
            return
        }
        viewModelScope.launch {
            _actionLoading.value = true
            _actionError.value = null
            val profile = (userProfileState.value as? UiState.Success)?.data
            val adminName = profile?.name?.takeIf { it.isNotBlank() }
                ?: firebaseUser.displayName?.takeIf { it.isNotBlank() }
                ?: "City Admin"

            val result = repository.updateComplaintStatusAndRemarks(
                complaintId = complaintId,
                currentHistory = currentHistory,
                newStatus = newStatus,
                adminRemarks = adminRemarks,
                adminName = adminName
            )
            _actionLoading.value = false
            result.onSuccess {
                _actionSuccess.value = "Complaint status updated to ${newStatus.label}."
                onUpdated()
            }.onFailure { err ->
                _actionError.value = err.message ?: "Failed to update complaint."
            }
        }
    }

    fun seedSampleReports() {
        if (!_isAdmin.value) return
        viewModelScope.launch {
            _actionLoading.value = true
            _actionError.value = null
            val adminName = firebaseUser.displayName ?: "City Inspector"
            val adminEmail = firebaseUser.email ?: CleanCityRepository.BOOTSTRAP_ADMIN_EMAIL
            val result = repository.seedSampleReportsForAdmin(adminName, adminEmail)
            _actionLoading.value = false
            result.onSuccess { count ->
                _actionSuccess.value = "Loaded $count sample city reports."
            }.onFailure { err ->
                _actionError.value = err.message ?: "Could not load sample reports."
            }
        }
    }

    fun clearMessages() {
        _actionError.value = null
        _actionSuccess.value = null
    }

    private companion object {
        const val TAG = "CleanCityVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
