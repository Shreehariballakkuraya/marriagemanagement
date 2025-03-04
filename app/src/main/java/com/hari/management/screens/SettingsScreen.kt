package com.hari.management.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hari.management.navigation.Screen
import com.hari.management.util.EmailConfig
import com.hari.management.util.EmailExportWorker
import com.hari.management.util.GuestDataManager
import com.hari.management.util.ImageHelper
import com.hari.management.util.PdfHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.mail.MessagingException
import com.hari.management.viewmodel.GuestViewModel
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: GuestViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var hasPdfTemplate by remember { mutableStateOf(PdfHelper.hasPdfTemplate(context)) }
    var hasImageTemplate by remember { mutableStateOf(ImageHelper.hasImageTemplate(context)) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var selectedFileType by remember { mutableStateOf("pdf") } // "pdf" or "image"
    var showSavedMessage by remember { mutableStateOf(false) }
    var isAdminConfigured by remember { mutableStateOf(false) }

    // Define showSnackbar as a local function
    fun showSnackbar(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    // Load saved message template when screen is first displayed
    var messageTemplate by remember {
        mutableStateOf(
            context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getString("message_template",
                    """Dear {name},

You are cordially invited to our wedding celebration.

We would be honored by your presence.

Please confirm your attendance.

Best regards.""") ?: ""
        )
    }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val success = PdfHelper.savePdfTemplate(context, it)
            if (success) {
                hasPdfTemplate = true
                hasImageTemplate = false
            }
        }
        showUploadDialog = false
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val success = ImageHelper.saveImageTemplate(context, it)
            if (success) {
                hasImageTemplate = true
                hasPdfTemplate = false
            }
        }
        showUploadDialog = false
    }

    LaunchedEffect(Unit) {
        val (email, _, _) = EmailConfig.getCredentials(context)
        isAdminConfigured = email != null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // Make the settings screen scrollable using LazyColumn
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                AdminSettingsCard(navController = navController)
            }

            item {
                EmailSettingsButton(navController = navController)
            }

            item {
                SendExportEmailButton(
                    context = context,
                    scope = scope,
                    snackbarHostState = snackbarHostState,
                    isAdminConfigured = isAdminConfigured
                )
            }

            item {
                Text(
                    text = "Invitation Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            item {
                InvitationTemplateCard(
                    context = context,
                    hasPdfTemplate = hasPdfTemplate,
                    hasImageTemplate = hasImageTemplate,
                    showUploadDialog = showUploadDialog,
                    setShowUploadDialog = { showUploadDialog = it },
                    selectedFileType = selectedFileType,
                    setSelectedFileType = { selectedFileType = it },
                    pdfPicker = pdfPicker,
                    imagePicker = imagePicker
                )
            }

            item {
                MessageTemplateCard(
                    context = context,
                    messageTemplate = messageTemplate,
                    setMessageTemplate = { messageTemplate = it },
                    showSavedMessage = showSavedMessage,
                    setShowSavedMessage = { showSavedMessage = it }
                )
            }

            item {
                ExportExcelButton(
                    context = context,
                    scope = scope,
                    snackbarHostState = snackbarHostState,
                    viewModel = viewModel
                )
            }
        }
    }

    if (showUploadDialog) {
        UploadDialog(
            hasImageTemplate = hasImageTemplate,
            hasPdfTemplate = hasPdfTemplate,
            setShowUploadDialog = { showUploadDialog = it },
            setSelectedFileType = { selectedFileType = it },
            pdfPicker = pdfPicker,
            imagePicker = imagePicker
        )
    }
}

@Composable
fun AdminSettingsCard(navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Admin Settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedButton(
                onClick = { navController.navigate(Screen.AdminSettings.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configure Admin Email")
            }

            Text(
                "One-time setup for admin email and app password",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmailSettingsButton(navController: NavController) {
    OutlinedButton(
        onClick = { navController.navigate(Screen.EmailSettings.route) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Email Settings")
    }
}

@Composable
fun SendExportEmailButton(
    context: Context,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    isAdminConfigured: Boolean
) {
    OutlinedButton(
        onClick = {
            scope.launch {
                try {
                    EmailExportWorker.exportAndSendEmail(context)
                    snackbarHostState.showSnackbar("Export email sent successfully")
                } catch (e: Exception) {
                    val errorMessage = when (e) {
                        is IllegalStateException -> e.message
                        is MessagingException -> e.message
                        else -> "System error occurred. Please try again."
                    }
                    snackbarHostState.showSnackbar(errorMessage ?: "Unknown error")
                    e.printStackTrace() // Log the error
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = isAdminConfigured
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (isAdminConfigured) "Send Export Email Now" else "Configure Admin Email First")
    }
}

@Composable
fun InvitationTemplateCard(
    context: Context,
    hasPdfTemplate: Boolean,
    hasImageTemplate: Boolean,
    showUploadDialog: Boolean,
    setShowUploadDialog: (Boolean) -> Unit,
    selectedFileType: String,
    setSelectedFileType: (String) -> Unit,
    pdfPicker: ManagedActivityResultLauncher<String, Uri?>,
    imagePicker: ManagedActivityResultLauncher<String, Uri?>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Invitation Template",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (hasPdfTemplate) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PDF Template is Set",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = {
                                setSelectedFileType("pdf")
                                setShowUploadDialog(true)
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = "Change template",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Change Template")
                        }
                    }
                } else if (hasImageTemplate) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Image Template is Set",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = {
                                setSelectedFileType("image")
                                setShowUploadDialog(true)
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = "Change template",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Change Template")
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No Template Set",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = { setShowUploadDialog(true) },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add template",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Add Template")
                        }
                    }
                }
            }

            Text(
                text = "This template will be used when sending invitations through WhatsApp",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun MessageTemplateCard(
    context: Context,
    messageTemplate: String,
    setMessageTemplate: (String) -> Unit,
    showSavedMessage: Boolean,
    setShowSavedMessage: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Message Template",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = messageTemplate,
                onValueChange = { setMessageTemplate(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                label = { Text("Message Template") },
                placeholder = { Text("Enter your message template here. Use {name} for guest's name.") },
                textStyle = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = "This message will be sent along with the invitation template",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )

            Button(
                onClick = {
                    // Save the message template
                    context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                        .edit()
                        .putString("message_template", messageTemplate)
                        .apply()
                    setShowSavedMessage(true)
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Save Message Template")
            }

            if (showSavedMessage) {
                LaunchedEffect(Unit) {
                    delay(2000)
                    setShowSavedMessage(false)
                }
                Text(
                    text = "Message template saved!",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ExportExcelButton(
    context: Context,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    viewModel: GuestViewModel
) {
    Button(
        onClick = {
            scope.launch {
                try {
                    val currentGuests = viewModel.guests.value
                    val uri = GuestDataManager.exportGuestsToExcel(context, currentGuests)
                    if (uri != null) {
                        snackbarHostState.showSnackbar("Excel file exported successfully")
                    } else {
                        snackbarHostState.showSnackbar("Failed to export Excel file")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(e.message ?: "Error exporting file")
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.FileDownload,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Export Excel Now")
    }
}

@Composable
fun UploadDialog(
    hasImageTemplate: Boolean,
    hasPdfTemplate: Boolean,
    setShowUploadDialog: (Boolean) -> Unit,
    setSelectedFileType: (String) -> Unit,
    pdfPicker: ManagedActivityResultLauncher<String, Uri?>,
    imagePicker: ManagedActivityResultLauncher<String, Uri?>
) {
    AlertDialog(
        onDismissRequest = { setShowUploadDialog(false) },
        title = { Text("Upload Invitation Template") },
        text = {
            Column {
                Text("Choose a file to use as your invitation template.")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            setSelectedFileType("pdf")
                            pdfPicker.launch("application/pdf")
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !hasImageTemplate
                    ) {
                        Text("PDF")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            setSelectedFileType("image")
                            imagePicker.launch("image/*")
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !hasPdfTemplate
                    ) {
                        Text("Image")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { setShowUploadDialog(false) }) {
                Text("Cancel")
            }
        }
    )
}