package com.hari.management

import android.os.Bundle
import android.widget.DatePicker
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.hari.management.data.GuestEntity
import com.hari.management.ui.theme.MarriageInviteTheme
import com.hari.management.viewmodel.GuestViewModel
import com.hari.management.viewmodel.GuestViewModelFactory
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
import com.hari.management.navigation.Screen
import com.hari.management.screens.GuestListScreen
import com.hari.management.screens.AddGuestScreen
import com.hari.management.screens.GuestDetailScreen
import com.hari.management.screens.ManageCategoriesScreen
import androidx.navigation.NavController
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CenterAlignedTopAppBar
import com.hari.management.navigation.NavigationGraph
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TextButton
import com.hari.management.util.Scheduler

class MainActivity : ComponentActivity() {
    private val viewModel: GuestViewModel by viewModels {
        GuestViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MarriageInviteTheme {
                val navController = rememberNavController()
                val showDatePicker by viewModel.showDatePicker.collectAsState()
                
                Scaffold(
                    topBar = { TopAppBar(navController = navController) }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        NavigationGraph(navController = navController, viewModel = viewModel)
                        
                        // Show date picker dialog if needed
                        showDatePicker?.let { guestId ->
                            DatePickerDialog(
                                onDismiss = { viewModel.hideDatePicker() },
                                onDateSelected = { date ->
                                    viewModel.updateGuestReminder(guestId, date)
                                    viewModel.hideDatePicker()
                                }
                            )
                        }
                    }
                }
            }
        }
        Scheduler.scheduleDailyExport(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val datePickerState = rememberDatePickerState()
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = datePickerState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(navController: NavController) {
    CenterAlignedTopAppBar(
        title = { Text("Guest Management") },
        actions = {
            IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        }
    )
} 