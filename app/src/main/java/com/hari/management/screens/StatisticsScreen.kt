package com.hari.management.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hari.management.components.GuestDashboard
import com.hari.management.viewmodel.GuestViewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.foundation.lazy.LazyColumn
import com.hari.management.data.GuestCategory
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.lazy.items
import com.hari.management.components.GroupReminder
import kotlin.math.min
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch
import com.hari.management.util.GuestDataManager
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    navController: NavController,
    viewModel: GuestViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val guests by viewModel.guests.collectAsState(initial = emptyList())

    // Calculate statistics
    val statistics = remember(guests) {
        val total = guests.size
        val invited = guests.count { it.invitationStatus == com.hari.management.data.InvitationStatus.INVITED }
        val pending = guests.count { it.invitationStatus == com.hari.management.data.InvitationStatus.PENDING }
        val notInvited = guests.count { it.invitationStatus == com.hari.management.data.InvitationStatus.NOT_INVITED }
        
        mapOf(
            "Total Guests" to total,
            "Invited" to invited,
            "Pending" to pending,
            "Not Invited" to notInvited
        )
    }

    // Calculate percentages
    val percentages = remember(statistics) {
        val total = statistics["Total Guests"] ?: 0
        if (total > 0) {
            mapOf(
                com.hari.management.data.InvitationStatus.INVITED to ((statistics["Invited"] ?: 0).toFloat() / total * 100),
                com.hari.management.data.InvitationStatus.PENDING to ((statistics["Pending"] ?: 0).toFloat() / total * 100),
                com.hari.management.data.InvitationStatus.NOT_INVITED to ((statistics["Not Invited"] ?: 0).toFloat() / total * 100)
            )
        } else {
            mapOf(
                com.hari.management.data.InvitationStatus.INVITED to 0f,
                com.hari.management.data.InvitationStatus.PENDING to 0f,
                com.hari.management.data.InvitationStatus.NOT_INVITED to 0f
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { exportGuests(context, guests, false, scope, snackbarHostState) }
                    ) {
                        Icon(Icons.Default.FileDownload, "Export Excel")
                    }
                    IconButton(
                        onClick = { exportGuests(context, guests, true, scope, snackbarHostState) }
                    ) {
                        Icon(Icons.Default.Code, "Export JSON")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Statistics Cards
            statistics.forEach { (label, value) ->
                StatisticCard(
                    label = label,
                    value = value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }

            // Status Distribution
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Status Distribution",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    percentages.forEach { (status, percentage) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            when (status) {
                                                com.hari.management.data.InvitationStatus.INVITED -> Color.Green
                                                com.hari.management.data.InvitationStatus.PENDING -> Color.Yellow
                                                com.hari.management.data.InvitationStatus.NOT_INVITED -> Color.Gray
                                            },
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(status.name)
                            }
                            Text("${String.format("%.1f", percentage)}%")
                        }
                    }
                }
            }
        }
    }
}

private fun exportGuests(
    context: Context,
    guests: List<com.hari.management.data.GuestEntity>,
    asJson: Boolean,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    val uri = if (asJson) {
        GuestDataManager.exportGuestsToJson(context, guests)
    } else {
        GuestDataManager.exportGuestsToExcel(context, guests)
    }
    
    uri?.let {
        try {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, it)
                type = if (asJson) "application/json" else "application/vnd.ms-excel"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Guest List"))
            scope.launch {
                snackbarHostState.showSnackbar("Guest list exported successfully")
            }
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Failed to export: ${e.message}")
            }
        }
    }
}

@Composable
private fun StatisticCard(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
} 