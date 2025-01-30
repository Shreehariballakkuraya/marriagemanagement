package com.hari.management.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hari.management.components.BulkActionMenu
import com.hari.management.data.GuestEntity
import com.hari.management.data.InvitationStatus
import com.hari.management.data.GuestCategory
import com.hari.management.viewmodel.GuestViewModel
import androidx.compose.runtime.livedata.observeAsState
import kotlinx.coroutines.launch
import android.content.Intent
import com.hari.management.util.GuestDataManager
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkOperationsScreen(
    navController: NavController,
    viewModel: GuestViewModel
) {
    val guests by viewModel.guests.collectAsState(initial = emptyList())
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    var selectedGuests by remember { mutableStateOf(setOf<GuestEntity>()) }
    var selectedStatus by remember { mutableStateOf<InvitationStatus?>(null) }
    var selectedCategory by remember { mutableStateOf<GuestCategory?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Filter guests based on selected filters
    val filteredGuests = remember(guests, selectedStatus, selectedCategory) {
        guests.filter { guest ->
            (selectedStatus == null || guest.invitationStatus == selectedStatus) &&
            (selectedCategory == null || guest.categoryId == selectedCategory?.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulk Operations") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
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
                    IconButton(
                        onClick = { 
                            selectedGuests = if (selectedGuests.size == filteredGuests.size) {
                                emptySet()
                            } else {
                                filteredGuests.toSet()
                            }
                        }
                    ) {
                        Icon(
                            if (selectedGuests.size == filteredGuests.size) 
                                Icons.Default.CheckBoxOutlineBlank 
                            else Icons.Default.CheckBox,
                            "Select All"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Filters",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Status Filter
                Text(
                    "Status",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedStatus == null,
                            onClick = { selectedStatus = null },
                            label = { Text("All") }
                        )
                    }
                    items(InvitationStatus.values()) { status ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = { Text(status.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            when (status) {
                                                InvitationStatus.INVITED -> Color.Green
                                                InvitationStatus.PENDING -> Color.Yellow
                                                InvitationStatus.NOT_INVITED -> Color.Gray
                                            },
                                            shape = CircleShape
                                        )
                                )
                            }
                        )
                    }
                }

                // Category Filter
                Text(
                    "Category",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All") }
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.name) }
                        )
                    }
                }
            }

            // Selected count
            if (selectedGuests.isNotEmpty()) {
                Text(
                    "${selectedGuests.size} guests selected",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Guest List
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredGuests) { guest ->
                    val isSelected = guest in selectedGuests
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        onClick = {
                            selectedGuests = if (isSelected) {
                                selectedGuests - guest
                            } else {
                                selectedGuests + guest
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedGuests = if (checked) {
                                        selectedGuests + guest
                                    } else {
                                        selectedGuests - guest
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    guest.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    guest.phoneNumber,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        when (guest.invitationStatus) {
                                            InvitationStatus.INVITED -> Color.Green
                                            InvitationStatus.PENDING -> Color.Yellow
                                            InvitationStatus.NOT_INVITED -> Color.Gray
                                        },
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
} 