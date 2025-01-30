package com.hari.management.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.hari.management.data.GuestEntity
import com.hari.management.data.InvitationStatus
import com.hari.management.viewmodel.GuestViewModel
import com.hari.management.util.WhatsAppHelper
import kotlinx.coroutines.launch

@Composable
fun BulkActionMenu(
    viewModel: GuestViewModel,
    selectedGuests: List<GuestEntity>,
    onExportJson: (List<GuestEntity>) -> Unit,
    onExportExcel: (List<GuestEntity>) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Default.MoreVert, "More actions")
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Update Status") },
                onClick = {
                    showMenu = false
                    showStatusDialog = true
                },
                leadingIcon = { Icon(Icons.Default.Edit, "Update Status") }
            )
            DropdownMenuItem(
                text = { Text("Export") },
                onClick = {
                    showMenu = false
                    showExportMenu = true
                },
                leadingIcon = { Icon(Icons.Default.Share, "Export") }
            )
            DropdownMenuItem(
                text = { Text("Delete Selected") },
                onClick = {
                    scope.launch {
                        selectedGuests.forEach { guest ->
                            viewModel.deleteGuest(guest)
                        }
                        snackbarHostState.showSnackbar("Deleted ${selectedGuests.size} guests")
                    }
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.Delete, "Delete") }
            )
        }

        DropdownMenu(
            expanded = showExportMenu,
            onDismissRequest = { showExportMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Export as JSON") },
                onClick = {
                    onExportJson(selectedGuests)
                    showExportMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Export as Excel") },
                onClick = {
                    onExportExcel(selectedGuests)
                    showExportMenu = false
                }
            )
        }
    }

    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Update Status") },
            text = {
                Column {
                    InvitationStatus.values().forEach { status ->
                        TextButton(
                            onClick = {
                                scope.launch {
                                    selectedGuests.forEach { guest ->
                                        viewModel.updateGuestStatus(guest.id, status)
                                    }
                                    snackbarHostState.showSnackbar(
                                        "Updated ${selectedGuests.size} guests to ${status.name}"
                                    )
                                }
                                showStatusDialog = false
                            }
                        ) {
                            Text(status.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    SnackbarHost(hostState = snackbarHostState)
} 