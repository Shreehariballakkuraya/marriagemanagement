package com.hari.management.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hari.management.util.EmailConfig
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import android.content.Context
import com.hari.management.util.Scheduler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailSettingsScreen(navController: NavController) {
    var recipient by remember { mutableStateOf("") }
    val context = LocalContext.current
    var showSuccessMessage by remember { mutableStateOf(false) }
    var adminEmail by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val (savedEmail, _, savedRecipient) = EmailConfig.getCredentials(context)
        adminEmail = savedEmail ?: ""
        recipient = savedRecipient ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Email Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                "Email Settings",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                "Sender Email: $adminEmail",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it },
                label = { Text("Recipient Email") },
                placeholder = { Text("Leave empty to send to admin email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    EmailConfig.saveRecipient(context, recipient.takeIf { it.isNotBlank() })
                    showSuccessMessage = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Recipient")
            }

            if (showSuccessMessage) {
                Text(
                    "Recipient saved successfully!",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
} 