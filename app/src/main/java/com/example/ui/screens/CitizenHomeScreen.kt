package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Complaint
import com.example.model.ComplaintCategory
import com.example.model.ComplaintStatus
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.ComplaintCard
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.KpiStatCard
import com.example.ui.components.categoryIcon
import com.example.ui.components.categoryShortLabel
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestDark
import com.example.ui.theme.ForestPrimary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSurface
import com.example.ui.theme.OutlineGreen
import com.example.ui.theme.StatusReportedBg
import com.example.ui.theme.StatusReportedText
import com.example.ui.theme.StatusResolvedBg
import com.example.ui.theme.StatusResolvedText

@Composable
fun CitizenHomeScreen(
    userProfile: UserProfile?,
    fallbackName: String,
    complaintsState: UiState<List<Complaint>>,
    onReportComplaintClick: (ComplaintCategory?) -> Unit,
    onViewAllClick: () -> Unit,
    onComplaintClick: (String) -> Unit
) {
    val displayName = userProfile?.name?.takeIf { it.isNotBlank() } ?: fallbackName.ifBlank { "Citizen" }
    val wardLabel = userProfile?.ward?.takeIf { it.isNotBlank() } ?: "CleanCity"

    val complaints = (complaintsState as? UiState.Success)?.data.orEmpty()
    val totalCount = complaints.size
    val pendingCount = complaints.count {
        it.statusEnum == ComplaintStatus.REPORTED || it.statusEnum == ComplaintStatus.IN_PROGRESS
    }
    val resolvedCount = complaints.count { it.statusEnum == ComplaintStatus.RESOLVED }
    val recentComplaints = complaints.take(4)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Clean Greeting Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MintContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayName.firstOrNull()?.uppercase() ?: "C",
                                style = MaterialTheme.typography.titleLarge,
                                color = ForestDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(
                                text = "Hi, $displayName",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = wardLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Clean Light Action Card (NO dark card)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MintSurface),
                    border = BorderStroke(1.dp, OutlineGreen)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Report a Cleanliness Issue",
                            style = MaterialTheme.typography.titleLarge,
                            color = ForestDark
                        )
                        Button(
                            onClick = { onReportComplaintClick(null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("report_complaint_cta"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Report Complaint",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // KPI Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard(
                        title = "Total",
                        value = totalCount.toString(),
                        icon = Icons.Rounded.Assignment,
                        accentColor = ForestPrimary,
                        containerColor = MintContainer,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "Pending",
                        value = pendingCount.toString(),
                        icon = Icons.Rounded.PendingActions,
                        accentColor = StatusReportedText,
                        containerColor = StatusReportedBg,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "Resolved",
                        value = resolvedCount.toString(),
                        icon = Icons.Rounded.CheckCircle,
                        accentColor = StatusResolvedText,
                        containerColor = StatusResolvedBg,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Categories
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ComplaintCategory.entries.forEach { cat ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, OutlineGreen),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onReportComplaintClick(cat) }
                                    .testTag("home_quick_category_${cat.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(MintSurface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = categoryIcon(cat),
                                            contentDescription = cat.label,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = categoryShortLabel(cat),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Complaints Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Complaints",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (complaints.isNotEmpty()) {
                        TextButton(
                            onClick = onViewAllClick,
                            modifier = Modifier.testTag("view_all_complaints_button")
                        ) {
                            Text("View All")
                        }
                    }
                }
            }

            when (complaintsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = EmeraldPrimary)
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        EmptyStatePanel(
                            title = "Could not load complaints",
                            subtitle = complaintsState.message,
                            icon = Icons.Rounded.CleaningServices
                        )
                    }
                }

                is UiState.Success -> {
                    if (recentComplaints.isEmpty()) {
                        item {
                            EmptyStatePanel(
                                title = "No complaints yet",
                                subtitle = "",
                                icon = Icons.Rounded.CleaningServices,
                                actionLabel = "Report Complaint",
                                actionTestTag = "empty_report_complaint_button",
                                onAction = { onReportComplaintClick(null) }
                            )
                        }
                    } else {
                        items(recentComplaints, key = { it.id }) { complaint ->
                            ComplaintCard(
                                complaint = complaint,
                                showReporterName = false,
                                onClick = { onComplaintClick(complaint.id) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}
