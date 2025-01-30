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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import com.hari.management.util.GoogleContactsHelper
import java.text.SimpleDateFormat
import java.util.*
import com.hari.management.util.GuestDataManager
import com.hari.management.components.BulkActionMenu
import com.hari.management.components.GuestDashboard
import com.hari.management.components.AdvancedSearch
import com.hari.management.components.GroupReminder
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.runtime.collectAsState

enum class ViewType {
    LIST, GRID
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestListScreen(
    navController: NavController,
    viewModel: GuestViewModel
) {
    val context = LocalContext.current
    var showImportDialog by remember { mutableStateOf(false) }
    var viewType by remember { mutableStateOf(ViewType.LIST) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    var selectedGuests by remember { mutableStateOf(setOf<GuestEntity>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guest List") },
                actions = {
                    if (selectedGuests.isNotEmpty()) {
                        BulkActionMenu(
                            viewModel = viewModel,
                            selectedGuests = selectedGuests.toList(),
                            onExportJson = { selectedGuests ->
                                val uri = GuestDataManager.exportGuestsToJson(context, selectedGuests)
                                uri?.let {
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_STREAM, it)
                                        type = "application/json"
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Guest List"))
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Exported ${selectedGuests.size} guests as JSON")
                                    }
                                }
                            },
                            onExportExcel = { selectedGuests ->
                                val uri = GuestDataManager.exportGuestsToExcel(context, selectedGuests)
                                uri?.let {
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_STREAM, it)
                                        type = "application/vnd.ms-excel"
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Guest List"))
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Exported ${selectedGuests.size} guests as Excel")
                                    }
                                }
                            }
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.Statistics.route) }) {
                        Icon(Icons.Default.Analytics, "Statistics")
                    }
                    IconButton(onClick = { navController.navigate(Screen.ManageCategories.route) }) {
                        Icon(Icons.Default.Settings, "Manage Categories")
                    }
                    IconButton(
                        onClick = { navController.navigate(Screen.BulkOperations.route) }
                    ) {
                        Icon(Icons.Default.Group, "Bulk Operations")
                    }
                    IconButton(
                        onClick = { 
                            viewType = if (viewType == ViewType.LIST) ViewType.GRID else ViewType.LIST 
                        }
                    ) {
                        Icon(
                            if (viewType == ViewType.LIST) 
                                Icons.Default.GridView 
                            else Icons.AutoMirrored.Filled.ViewList,
                            "Toggle View"
                        )
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            AdvancedSearch(
                viewModel = viewModel,
                categories = categories,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            GuestListContent(
                viewModel = viewModel,
                onGuestClick = { guestId ->
                    navController.navigate(Screen.GuestDetail.createRoute(guestId))
                },
                onGuestLongClick = { guest ->
                    selectedGuests = if (selectedGuests.contains(guest)) {
                        selectedGuests - guest
                    } else {
                        selectedGuests + guest
                    }
                },
                selectedGuests = selectedGuests,
                viewType = viewType,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Contacts") },
            text = {
                ImportContactsScreen { contacts ->
                    scope.launch {
                        contacts.forEach { contact ->
                            viewModel.insertGuest(
                                GuestEntity(
                                    name = contact.name,
                                    phoneNumber = contact.phoneNumber,
                                    invitationStatus = InvitationStatus.NOT_INVITED,
                                    categoryId = null
                                )
                            )
                        }
                        snackbarHostState.showSnackbar(
                            message = "${contacts.size} contacts imported successfully"
                        )
                    }
                    showImportDialog = false
                }
            },
            confirmButton = {},
            modifier = Modifier.fillMaxHeight(0.8f)
        )
    }

    fun handleGoogleSignIn(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                try {
                    val contacts = GoogleContactsHelper.fetchContacts(context)
                    contacts.forEach { contact ->
                        viewModel.insertGuest(
                            GuestEntity(
                                name = contact.name,
                                phoneNumber = contact.phoneNumber,
                                invitationStatus = InvitationStatus.NOT_INVITED,
                                categoryId = null
                            )
                        )
                    }
                    snackbarHostState.showSnackbar(
                        message = "${contacts.size} contacts imported successfully"
                    )
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(
                        message = e.message ?: "Error fetching contacts",
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GuestListContent(
    viewModel: GuestViewModel,
    onGuestClick: (Int) -> Unit,
    onGuestLongClick: (GuestEntity) -> Unit,
    selectedGuests: Set<GuestEntity>,
    viewType: ViewType,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val guests by viewModel.guests.collectAsState(initial = emptyList())
    val interactedGuests by viewModel.interactedGuests.collectAsState()
    var showStatusUpdateDialog by remember { mutableStateOf(false) }
    var currentGuestToUpdate by remember { mutableStateOf<GuestEntity?>(null) }

    LaunchedEffect(interactedGuests) {
        if (interactedGuests.isNotEmpty()) {
            showStatusUpdateDialog = true
            currentGuestToUpdate = interactedGuests.first()
        }
    }

    Column(modifier = modifier.padding(8.dp)) {
        when (viewType) {
            ViewType.LIST -> {
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
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .background(
                                    if (selectedGuests.contains(guest)) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = { onGuestLongClick(guest) }
                                    )
                                }
                        )
                    }
                }
            }
            ViewType.GRID -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 128.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
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
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .background(
                                    if (selectedGuests.contains(guest)) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = { onGuestLongClick(guest) }
                                    )
                                }
                        )
                    }
                }
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
    viewModel: GuestViewModel,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showStatusMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onClick() }
                )
            },
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = guest.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Text(
                            text = guest.phoneNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Reminder button
                    IconButton(
                        onClick = { 
                            if (guest.reminderDate == null) {
                                onReminderClick()
                            } else {
                                viewModel.updateGuestReminder(guest.id, null)
                            }
                        }
                    ) {
                        Icon(
                            if (guest.reminderDate == null) 
                                Icons.Default.Notifications 
                            else Icons.Default.NotificationsOff,
                            contentDescription = if (guest.reminderDate == null) 
                                "Set Reminder" else "Clear Reminder"
                        )
                    }

                    // Delete button
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, "Delete Guest")
                    }
                }
            }

            // Add reminder date display
            guest.reminderDate?.let { timestamp ->
                val date = remember(timestamp) {
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(Date(timestamp))
                }
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Reminder date",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reminder: $date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
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

@Composable
fun GuestGridItem(
    guest: GuestEntity,
    onGuestClick: () -> Unit,
    onGuestLongClick: () -> Unit,
    isSelected: Boolean,
    viewModel: GuestViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onGuestClick() },
                    onLongPress = { onGuestLongClick() }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        when (guest.invitationStatus) {
                            InvitationStatus.INVITED -> Color.Green
                            InvitationStatus.PENDING -> Color.Yellow
                            InvitationStatus.NOT_INVITED -> Color.Gray
                        },
                        shape = CircleShape
                    )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = guest.name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = guest.phoneNumber,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = { 
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${guest.phoneNumber}")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = "Call",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = { 
                        WhatsAppHelper.sendInvitation(context, guest.phoneNumber, guest.name)
                        viewModel.updateGuestInteraction(guest.id, true)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_whatsapp),
                        contentDescription = "WhatsApp",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
} 