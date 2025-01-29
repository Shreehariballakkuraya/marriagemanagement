package com.hari.management.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.hari.management.viewmodel.GuestViewModel
import com.hari.management.navigation.Screen
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hari.management.data.GuestEntity
import com.hari.management.data.InvitationStatus
import com.hari.management.util.PhoneNumberValidator
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.hari.management.R
import com.hari.management.util.WhatsAppHelper
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestListScreen(
    navController: NavController,
    viewModel: GuestViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guest List") },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.ManageCategories.route) }) {
                        Icon(Icons.Default.Settings, "Manage Categories")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddGuest.route) }
            ) {
                Icon(Icons.Default.Add, "Add Guest")
            }
        }
    ) { padding ->
        GuestListContent(
            viewModel = viewModel,
            onGuestClick = { guestId ->
                navController.navigate(Screen.GuestDetail.createRoute(guestId))
            },
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun GuestListContent(
    viewModel: GuestViewModel,
    onGuestClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val guests by viewModel.guests.observeAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val interactedGuests by viewModel.interactedGuests.observeAsState(initial = emptyList())
    var showStatusUpdateDialog by remember { mutableStateOf(false) }
    var currentGuestToUpdate by remember { mutableStateOf<GuestEntity?>(null) }

    LaunchedEffect(interactedGuests) {
        if (interactedGuests.isNotEmpty()) {
            showStatusUpdateDialog = true
            currentGuestToUpdate = interactedGuests.first()
        }
    }

    Column(modifier = modifier.padding(8.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                viewModel.searchGuests(it)
            },
            label = { Text("Search Guests") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(Color.White, shape = RoundedCornerShape(8.dp)),
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            singleLine = true
        )

        // Status filters
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    onClick = { viewModel.setStatusFilter(null) },
                    label = { Text("All Status") },
                    selected = selectedStatus == null,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            items(InvitationStatus.values()) { status ->
                FilterChip(
                    onClick = { viewModel.setStatusFilter(status) },
                    label = { Text(status.name) },
                    selected = selectedStatus == status,
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    when (status) {
                                        InvitationStatus.INVITED -> Color.Green
                                        InvitationStatus.NOT_INVITED -> Color.Gray
                                        InvitationStatus.PENDING -> Color.Yellow
                                    },
                                    shape = CircleShape
                                )
                        )
                    },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // Category filters
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    onClick = { viewModel.setCategoryFilter(null) },
                    label = { Text("All Categories") },
                    selected = selectedCategory == null,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            items(categories) { category ->
                FilterChip(
                    onClick = { viewModel.setCategoryFilter(category) },
                    label = { Text(category.name) },
                    selected = selectedCategory == category,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(guests, key = { it.id }) { guest ->
                GuestItem(
                    guest = guest,
                    onVerificationChanged = { isVerified ->
                        viewModel.updateGuestVerification(guest.id, isVerified)
                    },
                    onReminderClick = {
                        viewModel.showDatePickerFor(guest.id)
                    },
                    onDeleteClick = {
                        viewModel.deleteGuest(guest)
                    },
                    onStatusClick = { status ->
                        viewModel.updateGuestStatus(guest.id, status)
                    },
                    onClick = {
                        onGuestClick(guest.id)
                    },
                    onCallClick = { phoneNumber ->
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$phoneNumber")
                        }
                        context.startActivity(intent)
                    },
                    onWhatsAppClick = { phoneNumber, name ->
                        WhatsAppHelper.sendInvitation(context, phoneNumber, name)
                        viewModel.updateGuestInteraction(guest.id, true)
                    },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clickable { onGuestClick(guest.id) }
                        .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                        .padding(16.dp)
                )
            }
        }
    }

    if (showStatusUpdateDialog && currentGuestToUpdate != null) {
        AlertDialog(
            onDismissRequest = {
                showStatusUpdateDialog = false
                currentGuestToUpdate = null
            },
            title = { Text("Update Invitation Status") },
            text = { Text("You have interacted with ${currentGuestToUpdate?.name}. Would you like to update their invitation status?") },
            confirmButton = {
                TextButton(onClick = {
                    currentGuestToUpdate?.let { guest ->
                        viewModel.updateGuestStatus(guest.id, InvitationStatus.INVITED)
                        viewModel.updateGuestInteraction(guest.id, false)
                    }
                    showStatusUpdateDialog = false
                    currentGuestToUpdate = null
                }) {
                    Text("Invited")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    currentGuestToUpdate?.let { guest ->
                        viewModel.updateGuestStatus(guest.id, InvitationStatus.PENDING)
                        viewModel.updateGuestInteraction(guest.id, false)
                    }
                    showStatusUpdateDialog = false
                    currentGuestToUpdate = null
                }) {
                    Text("Pending")
                }
            }
        )
    }
}

@Composable
fun GuestItem(
    guest: GuestEntity,
    onVerificationChanged: (Boolean) -> Unit,
    onReminderClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onStatusClick: (InvitationStatus) -> Unit,
    onClick: () -> Unit,
    onCallClick: (String) -> Unit,
    onWhatsAppClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStatusMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator with dropdown menu
                Box {
                    IconButton(onClick = { showStatusMenu = true }) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    when (guest.invitationStatus) {
                                        InvitationStatus.INVITED -> Color.Green
                                        InvitationStatus.NOT_INVITED -> Color.Gray
                                        InvitationStatus.PENDING -> Color.Yellow
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        InvitationStatus.values().forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.name) },
                                onClick = {
                                    onStatusClick(status)
                                    showStatusMenu = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = guest.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )

                // Verification toggle
                Checkbox(
                    checked = guest.isInvitationVerified,
                    onCheckedChange = onVerificationChanged,
                    modifier = Modifier.padding(end = 8.dp)
                )

                // Reminder button
                IconButton(onClick = onReminderClick) {
                    Icon(Icons.Default.Notifications, "Set Reminder")
                }

                // Delete button
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, "Delete Guest")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call button
                IconButton(onClick = { onCallClick(guest.phoneNumber) }) {
                    Icon(Icons.Default.Call, "Call")
                }

                // WhatsApp button
                IconButton(onClick = { onWhatsAppClick(guest.phoneNumber, guest.name) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_whatsapp),
                        contentDescription = "WhatsApp",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
} 