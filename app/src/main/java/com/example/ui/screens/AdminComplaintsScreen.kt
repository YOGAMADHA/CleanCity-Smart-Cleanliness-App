package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AssignmentLate
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.Complaint
import com.example.model.ComplaintStatus
import com.example.model.UiState
import com.example.ui.components.ComplaintCard
import com.example.ui.components.EmptyStatePanel
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestPrimary
import com.example.ui.theme.MintContainer

@Composable
fun AdminComplaintsScreen(
    complaintsState: UiState<List<Complaint>>,
    onComplaintClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<ComplaintStatus?>(null) }

    val allComplaints = (complaintsState as? UiState.Success)?.data.orEmpty()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Manage Complaints",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Complaint ID, citizen, or street...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_search_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { selectedStatus = null },
                        label = { Text("All (${allComplaints.size})") },
                        modifier = Modifier.testTag("admin_filter_all"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MintContainer,
                            selectedLabelColor = ForestPrimary
                        )
                    )
                    ComplaintStatus.entries.forEach { status ->
                        val count = allComplaints.count { it.statusEnum == status }
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = {
                                selectedStatus = if (selectedStatus == status) null else status
                            },
                            label = { Text("${status.label} ($count)") },
                            modifier = Modifier.testTag("admin_filter_${status.name.lowercase()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MintContainer,
                                selectedLabelColor = ForestPrimary
                            )
                        )
                    }
                }
            }

            when (complaintsState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = EmeraldPrimary)
                    }
                }

                is UiState.Error -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        EmptyStatePanel(
                            title = "Unable to load city complaints",
                            subtitle = complaintsState.message,
                            icon = Icons.Rounded.AssignmentLate
                        )
                    }
                }

                is UiState.Success -> {
                    val query = searchQuery.trim()
                    val filtered = remember(complaintsState.data, query, selectedStatus) {
                        complaintsState.data.filter { item ->
                            val matchesStatus = selectedStatus == null || item.statusEnum == selectedStatus
                            val matchesQuery = query.isEmpty() ||
                                item.complaintCode.contains(query, ignoreCase = true) ||
                                item.title.contains(query, ignoreCase = true) ||
                                item.citizenName.contains(query, ignoreCase = true) ||
                                item.locationText.contains(query, ignoreCase = true) ||
                                item.category.contains(query, ignoreCase = true)
                            matchesStatus && matchesQuery
                        }
                    }

                    if (filtered.isEmpty()) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            EmptyStatePanel(
                                title = "No matching complaints",
                                subtitle = "Adjust your Complaint ID search or status filter.",
                                icon = Icons.Rounded.AssignmentLate
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filtered, key = { it.id }) { complaint ->
                                ComplaintCard(
                                    complaint = complaint,
                                    showReporterName = true,
                                    onClick = { onComplaintClick(complaint.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
