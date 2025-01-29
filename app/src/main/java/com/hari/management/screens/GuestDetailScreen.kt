package com.hari.management.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hari.management.data.GuestEntity
import com.hari.management.viewmodel.GuestViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestDetailScreen(
    navController: NavController,
    viewModel: GuestViewModel,
    guestId: Int
) {
    val guest = remember { mutableStateOf<GuestEntity?>(null) }
    
    LaunchedEffect(guestId) {
        guest.value = viewModel.getGuestById(guestId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guest Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            guest.value?.let { viewModel.deleteGuestById(guestId) }
                            navController.navigateUp()
                        }
                    ) {
                        Icon(Icons.Default.Delete, "Delete Guest")
                    }
                }
            )
        }
    ) { padding ->
        guest.value?.let { currentGuest ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = currentGuest.name,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = currentGuest.phoneNumber,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = currentGuest.isInvitationVerified,
                                onCheckedChange = { isVerified ->
                                    viewModel.updateGuestVerification(guestId, isVerified)
                                }
                            )
                            Text("Invitation Verified")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.showDatePickerFor(guestId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DateRange, "Set Reminder")
                            Spacer(Modifier.width(8.dp))
                            Text("Set Reminder")
                        }
                    }
                }
            }
        }
    }
} 