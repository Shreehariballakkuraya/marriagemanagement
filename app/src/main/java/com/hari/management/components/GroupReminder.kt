package com.hari.management.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hari.management.data.GuestCategory
import com.hari.management.data.GuestEntity
import com.hari.management.viewmodel.GuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupReminder(
    viewModel: GuestViewModel,
    guests: List<GuestEntity>,
    categories: List<GuestCategory>
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<GuestCategory?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    OutlinedCard(
        onClick = { showDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "Set Group Reminder",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select Category") },
            text = {
                Column {
                    categories.forEach { category ->
                        TextButton(
                            onClick = {
                                selectedCategory = category
                                showDialog = false
                                showDatePicker = true
                            }
                        ) {
                            Text(category.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date ->
                        selectedCategory?.let { category ->
                            guests.filter { it.categoryId == category.id }
                                .forEach { guest ->
                                    viewModel.updateGuestReminder(guest.id, date)
                                }
                        }
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState
            )
        }
    }
} 