package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.FiberNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.Complaint
import com.example.model.ComplaintCategory
import com.example.model.ComplaintStatus
import com.example.model.UiState
import com.example.ui.components.ComplaintCard
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.KpiStatCard
import com.example.ui.components.SmartCityGradientHeader
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestPrimary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.StatusProgressBg
import com.example.ui.theme.StatusProgressText
import com.example.ui.theme.StatusReportedBg
import com.example.ui.theme.StatusReportedText
import com.example.ui.theme.StatusResolvedBg
import com.example.ui.theme.StatusResolvedText

@Composable
fun AdminDashboardScreen(
    adminName: String,
    complaintsState: UiState<List<Complaint>>,
    isSeeding: Boolean,
    onManageAllClick: () -> Unit,
    onComplaintClick: (String) -> Unit,
    onSeedSampleReports: () -> Unit
) {
    val complaints = (complaintsState as? UiState.Success)?.data.orEmpty()
    val totalCount = complaints.size
    val newCount = complaints.count { it.statusEnum == ComplaintStatus.REPORTED }
    val inProgressCount = complaints.count { it.statusEnum == ComplaintStatus.IN_PROGRESS }
    val resolvedCount = complaints.count { it.statusEnum == ComplaintStatus.RESOLVED }

    val queue = complaints.sortedBy {
        when (it.statusEnum) {
            ComplaintStatus.REPORTED -> 0
            ComplaintStatus.IN_PROGRESS -> 1
            ComplaintStatus.RESOLVED -> 2
        }
    }.take(4)

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
            item {
                SmartCityGradientHeader(
                    badgeText = "ADMIN PORTAL",
                    title = "Admin Dashboard",
                    subtitle = "Welcome, $adminName"
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiStatCard(
                            title = "Total Complaints",
                            value = totalCount.toString(),
                            icon = Icons.Rounded.Assignment,
                            accentColor = ForestPrimary,
                            containerColor = MintContainer,
                            modifier = Modifier.weight(1f)
                        )
                        KpiStatCard(
                            title = "New Complaints",
                            value = newCount.toString(),
                            icon = Icons.Rounded.FiberNew,
                            accentColor = StatusReportedText,
                            containerColor = StatusReportedBg,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiStatCard(
                            title = "In Progress",
                            value = inProgressCount.toString(),
                            icon = Icons.Rounded.Autorenew,
                            accentColor = StatusProgressText,
                            containerColor = StatusProgressBg,
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
            }

            if (complaints.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Category Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            ComplaintCategory.entries.forEach { cat ->
                                val count = complaints.count { it.categoryEnum == cat }
                                val ratio = if (totalCount > 0) count.toFloat() / totalCount else 0f
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = cat.label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$count",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ForestPrimary
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(50)),
                                        color = EmeraldPrimary,
                                        trackColor = MintContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Priority Dispatch Queue",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (complaints.isNotEmpty()) {
                        TextButton(
                            onClick = onManageAllClick,
                            modifier = Modifier.testTag("admin_view_all_button")
                        ) {
                            Text("Manage All")
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
                            title = "Could not load city complaints",
                            subtitle = complaintsState.message,
                            icon = Icons.Rounded.CleaningServices
                        )
                    }
                }

                is UiState.Success -> {
                    if (queue.isEmpty()) {
                        item {
                            EmptyStatePanel(
                                title = "No active complaints",
                                subtitle = "City queue is currently clear.",
                                icon = Icons.Rounded.CleaningServices,
                                actionLabel = if (isSeeding) "Loading..." else "Load Sample Reports",
                                actionTestTag = "seed_sample_reports_button",
                                onAction = if (!isSeeding) onSeedSampleReports else null
                            )
                        }
                    } else {
                        items(queue, key = { it.id }) { complaint ->
                            ComplaintCard(
                                complaint = complaint,
                                showReporterName = true,
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
