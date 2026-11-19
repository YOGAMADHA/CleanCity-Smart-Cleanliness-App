package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.Complaint
import com.example.model.ComplaintStatus
import com.example.model.StatusHistoryEntry
import com.example.model.UiState
import com.example.ui.components.ComplaintImageView
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.StatusBadge
import com.example.ui.components.categoryIcon
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestPrimary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSurface
import com.example.util.formatMillis
import com.example.util.formatTimestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintDetailsScreen(
    complaintState: UiState<Complaint?>,
    isAdminMode: Boolean,
    isUpdating: Boolean,
    actionError: String?,
    actionSuccess: String?,
    onBack: () -> Unit,
    onAdminUpdate: (
        complaintId: String,
        history: List<StatusHistoryEntry>,
        newStatus: ComplaintStatus,
        remarks: String
    ) -> Unit
) {
    var showFullImage by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ticket Details") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("details_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (complaintState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldPrimary)
                    }
                }

                is UiState.Error -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        EmptyStatePanel(
                            title = "Error loading complaint",
                            subtitle = complaintState.message,
                            icon = Icons.Rounded.History
                        )
                    }
                }

                is UiState.Success -> {
                    val complaint = complaintState.data
                    if (complaint == null) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            EmptyStatePanel(
                                title = "Complaint not found",
                                subtitle = "This complaint may have been removed.",
                                icon = Icons.Rounded.History
                            )
                        }
                    } else {
                        var selectedStatus by remember(complaint.id, complaint.status) {
                            mutableStateOf(complaint.statusEnum)
                        }
                        var remarksInput by remember(complaint.id, complaint.adminRemarks) {
                            mutableStateOf(complaint.adminRemarks)
                        }
                        var localValidationError by remember { mutableStateOf<String?>(null) }

                        if (showFullImage && complaint.imageBase64.isNotBlank()) {
                            Dialog(onDismissRequest = { showFullImage = false }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.Black)
                                ) {
                                    ComplaintImageView(
                                        base64 = complaint.imageBase64,
                                        category = complaint.categoryEnum,
                                        contentDescription = "Full photo for ${complaint.title}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(420.dp)
                                    )
                                    IconButton(
                                        onClick = { showFullImage = false },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Close photo",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Photo Banner (tappable for full view if photo exists)
                            ComplaintImageView(
                                base64 = complaint.imageBase64,
                                category = complaint.categoryEnum,
                                contentDescription = "Complaint photo for ${complaint.title}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(enabled = complaint.imageBase64.isNotBlank()) {
                                        showFullImage = true
                                    }
                            )

                            // 3-Step Visual Progress Stepper
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                StatusProgressStepper(
                                    currentStatus = complaint.statusEnum,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            // Core Details Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = MintContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = complaint.complaintCode.ifBlank { complaint.id },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ForestPrimary,
                                                modifier = Modifier
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                                    .testTag("detail_complaint_code")
                                            )
                                        }
                                        StatusBadge(
                                            status = complaint.statusEnum,
                                            modifier = Modifier.testTag("detail_status_badge")
                                        )
                                    }

                                    Text(
                                        text = complaint.title,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = categoryIcon(complaint.categoryEnum),
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = complaint.categoryEnum.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = EmeraldPrimary
                                        )
                                    }

                                    Text(
                                        text = complaint.description,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                                    DetailInfoRow(
                                        icon = Icons.Rounded.LocationOn,
                                        label = "Location",
                                        value = complaint.locationText
                                    )
                                    DetailInfoRow(
                                        icon = Icons.Rounded.Schedule,
                                        label = "Submitted",
                                        value = formatTimestamp(complaint.createdAt)
                                    )
                                    DetailInfoRow(
                                        icon = Icons.Rounded.Person,
                                        label = "Citizen",
                                        value = complaint.citizenName
                                    )
                                    if (isAdminMode) {
                                        DetailInfoRow(
                                            icon = Icons.Rounded.Email,
                                            label = "Email",
                                            value = complaint.citizenEmail
                                        )
                                        if (complaint.citizenPhone.isNotBlank()) {
                                            DetailInfoRow(
                                                icon = Icons.Rounded.Phone,
                                                label = "Phone",
                                                value = complaint.citizenPhone
                                            )
                                        }
                                    }
                                }
                            }

                            // Official Admin Remarks Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MintSurface),
                                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.28f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Municipal Admin Remarks",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = ForestPrimary
                                    )
                                    Text(
                                        text = complaint.adminRemarks.ifBlank {
                                            "No remarks posted yet. City dispatch will update this section once reviewed."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("detail_admin_remarks")
                                    )
                                }
                            }

                            // Admin Action Panel
                            if (isAdminMode) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.4f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.AdminPanelSettings,
                                                contentDescription = null,
                                                tint = EmeraldPrimary
                                            )
                                            Text(
                                                text = "Manage Ticket Status",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = ForestPrimary
                                            )
                                        }

                                        SingleChoiceSegmentedButtonRow(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            ComplaintStatus.entries.forEachIndexed { index, status ->
                                                SegmentedButton(
                                                    selected = selectedStatus == status,
                                                    onClick = { selectedStatus = status },
                                                    shape = SegmentedButtonDefaults.itemShape(
                                                        index = index,
                                                        count = ComplaintStatus.entries.size
                                                    ),
                                                    modifier = Modifier.testTag("admin_status_${status.name.lowercase()}")
                                                ) {
                                                    Text(
                                                        text = status.label,
                                                        maxLines = 1,
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = remarksInput,
                                            onValueChange = {
                                                remarksInput = it
                                                localValidationError = null
                                            },
                                            label = { Text("Official Remarks / Action Taken") },
                                            placeholder = { Text("e.g. Sanitation crew dispatched; area cleared.") },
                                            minLines = 2,
                                            maxLines = 4,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("admin_remarks_input"),
                                            shape = RoundedCornerShape(14.dp)
                                        )

                                        val err = localValidationError ?: actionError
                                        if (!err.isNullOrBlank()) {
                                            Text(
                                                text = err,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        if (!actionSuccess.isNullOrBlank()) {
                                            Surface(
                                                color = MintContainer,
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(
                                                    text = actionSuccess,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = ForestPrimary,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp)
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                if (remarksInput.trim().length > 500) {
                                                    localValidationError = "Remarks must be 500 characters or fewer."
                                                } else {
                                                    onAdminUpdate(
                                                        complaint.id,
                                                        complaint.statusHistory,
                                                        selectedStatus,
                                                        remarksInput.trim()
                                                    )
                                                }
                                            },
                                            enabled = !isUpdating,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .testTag("admin_update_complaint_button"),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = EmeraldPrimary,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            if (isUpdating) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    color = Color.White,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Text("Update Status & Remarks")
                                            }
                                        }
                                    }
                                }
                            }

                            // Status History Timeline Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Activity Timeline",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    val history = complaint.statusHistory.ifEmpty {
                                        listOf(
                                            StatusHistoryEntry(
                                                status = complaint.status,
                                                remark = "Complaint logged",
                                                updatedBy = complaint.citizenName,
                                                timestampMillis = complaint.createdAt?.toDate()?.time ?: 0L
                                            )
                                        )
                                    }

                                    history.reversed().forEachIndexed { idx, entry ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (idx == 0) EmeraldPrimary else MaterialTheme.colorScheme.outline
                                                        )
                                                )
                                                if (idx < history.lastIndex) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(2.dp)
                                                            .height(38.dp)
                                                            .background(MaterialTheme.colorScheme.outline)
                                                    )
                                                }
                                            }

                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = entry.status,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = ForestPrimary
                                                    )
                                                    Text(
                                                        text = formatMillis(entry.timestampMillis),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (entry.remark.isNotBlank()) {
                                                    Text(
                                                        text = entry.remark,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                if (entry.updatedBy.isNotBlank()) {
                                                    Text(
                                                        text = "By ${entry.updatedBy}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusProgressStepper(
    currentStatus: ComplaintStatus,
    modifier: Modifier = Modifier
) {
    val activeStep = when (currentStatus) {
        ComplaintStatus.REPORTED -> 0
        ComplaintStatus.IN_PROGRESS -> 1
        ComplaintStatus.RESOLVED -> 2
    }
    val steps = ComplaintStatus.entries

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, status ->
            val completedOrCurrent = index <= activeStep
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (completedOrCurrent) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < activeStep || currentStatus == ComplaintStatus.RESOLVED) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (completedOrCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = status.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (completedOrCurrent) ForestPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (index == activeStep) FontWeight.Bold else FontWeight.Normal
                )
            }
            if (index < steps.lastIndex) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(3.dp)
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (index < activeStep) EmeraldPrimary else MaterialTheme.colorScheme.outline
                        )
                )
            }
        }
    }
}

@Composable
private fun DetailInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = EmeraldPrimary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
