package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.components.SmartCityGradientHeader
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestPrimary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSurface

@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    fallbackName: String,
    fallbackEmail: String,
    isAdminUser: Boolean,
    activeRoleMode: UserRole,
    isSaving: Boolean,
    onUpdateProfile: (name: String, phone: String, ward: String, onDone: () -> Unit) -> Unit,
    onSwitchRoleMode: (UserRole) -> Unit,
    onLogout: () -> Unit
) {
    val displayName = userProfile?.name?.takeIf { it.isNotBlank() } ?: fallbackName.ifBlank { "CleanCity User" }
    val displayEmail = userProfile?.email?.takeIf { it.isNotBlank() } ?: fallbackEmail
    val displayPhone = userProfile?.phone?.takeIf { it.isNotBlank() } ?: "Not set"
    val displayWard = userProfile?.ward?.takeIf { it.isNotBlank() } ?: "General City Zone"

    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        var editName by remember(displayName) { mutableStateOf(displayName) }
        var editPhone by remember(userProfile?.phone) { mutableStateOf(userProfile?.phone.orEmpty()) }
        var editWard by remember(userProfile?.ward) { mutableStateOf(userProfile?.ward.orEmpty()) }
        var editError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showEditDialog = false },
            title = { Text("Edit Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = {
                            editName = it
                            editError = null
                        },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_name_input")
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = {
                            editPhone = it
                            editError = null
                        },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_phone_input")
                    )
                    OutlinedTextField(
                        value = editWard,
                        onValueChange = {
                            editWard = it
                            editError = null
                        },
                        label = { Text("Ward / Neighborhood") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_ward_input")
                    )
                    if (!editError.isNullOrBlank()) {
                        Text(
                            text = editError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tName = editName.trim()
                        val tPhone = editPhone.trim()
                        when {
                            tName.isEmpty() -> editError = "Name cannot be empty."
                            tPhone.length !in 7..20 -> editError = "Phone must be 7–20 characters."
                            else -> onUpdateProfile(tName, tPhone, editWard.trim()) {
                                showEditDialog = false
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.testTag("save_profile_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (isSaving) "Saving..." else "Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = false },
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SmartCityGradientHeader(
                badgeText = if (activeRoleMode == UserRole.ADMIN) "VERIFIED ADMIN" else "CITIZEN ACCOUNT",
                title = displayName,
                subtitle = displayEmail,
                trailingContent = {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.firstOrNull()?.uppercase() ?: "C",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Account Details",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (userProfile != null) {
                            OutlinedButton(
                                onClick = { showEditDialog = true },
                                modifier = Modifier.testTag("edit_profile_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit")
                            }
                        }
                    }

                    ProfileDetailItem(
                        icon = Icons.Rounded.Person,
                        label = "Full Name",
                        value = displayName
                    )
                    ProfileDetailItem(
                        icon = Icons.Rounded.Email,
                        label = "Email",
                        value = displayEmail
                    )
                    ProfileDetailItem(
                        icon = Icons.Rounded.Phone,
                        label = "Phone Number",
                        value = displayPhone
                    )
                    ProfileDetailItem(
                        icon = Icons.Rounded.LocationCity,
                        label = "Ward / Neighborhood",
                        value = displayWard
                    )
                    ProfileDetailItem(
                        icon = Icons.Rounded.AdminPanelSettings,
                        label = "Role",
                        value = if (isAdminUser) "Administrator (Citizen & Admin Access)" else "Citizen"
                    )
                }
            }

            if (isAdminUser) {
                val targetRole = if (activeRoleMode == UserRole.ADMIN) UserRole.CITIZEN else UserRole.ADMIN
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MintSurface),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Workspace Switcher",
                            style = MaterialTheme.typography.titleSmall,
                            color = ForestPrimary
                        )
                        Text(
                            text = if (activeRoleMode == UserRole.ADMIN) {
                                "Currently viewing the Admin Command Center. Switch to Citizen View to submit test reports."
                            } else {
                                "Currently in Citizen View. Switch to Admin Command Center to manage city-wide complaints."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { onSwitchRoleMode(targetRole) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("switch_role_mode_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (targetRole == UserRole.ADMIN) "Switch to Admin Command Center"
                                else "Switch to Citizen View"
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("logout_button"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign Out",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfileDetailItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MintContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = ForestPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
