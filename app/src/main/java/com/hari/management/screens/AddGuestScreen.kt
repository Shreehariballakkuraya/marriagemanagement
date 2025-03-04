package com.hari.management.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hari.management.util.PhoneNumberValidator
import com.hari.management.viewmodel.GuestViewModel
import com.hari.management.data.GuestCategory
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.TextButton
import com.hari.management.navigation.Screen
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Context
import androidx.core.app.ActivityCompat.startActivityForResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.hari.management.data.GuestEntity
import com.hari.management.util.GoogleContactsHelper
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hari.management.data.InvitationStatus
import android.widget.Toast

data class Contact(val name: String, val number: String)

private const val REQUEST_CODE_CONTACTS = 100
private const val RC_SIGN_IN = 1000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGuestScreen(
    navController: NavController,
    viewModel: GuestViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<GuestCategory?>(null) }
    var showImportProgress by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Contact picker launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let {
            // Query for contact name and phone number
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    name = cursor.getString(0)
                }
            }

            val contactId = uri.lastPathSegment
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    phoneNumber = PhoneNumberValidator.format(cursor.getString(0))
                }
            }
        }
    }

    // Google Sign-in launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                showImportProgress = true
                try {
                    val contacts = GoogleContactsHelper.fetchContacts(context)
                    if (contacts.isEmpty()) {
                        importError = "No contacts found or permission denied"
                        return@launch
                    }
                    
                    contacts.forEach { contact ->
                        viewModel.addGuest(
                            name = contact.name,
                            phoneNumber = contact.phoneNumber,
                            categoryId = selectedCategory?.id
                        )
                    }
                    importError = null
                    Toast.makeText(context, 
                        "Successfully imported ${contacts.size} contacts", 
                        Toast.LENGTH_LONG
                    ).show()
                    navController.navigateUp()
                } catch (e: Exception) {
                    importError = "Failed to import contacts: ${e.message}"
                } finally {
                    showImportProgress = false
                }
            }
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Guest") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Import from Google Contacts Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                onClick = {
                    GoogleContactsHelper.initGoogleSignIn(context)
                    signInLauncher.launch(GoogleContactsHelper.getSignInIntent())
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ContactPhone,
                        contentDescription = "Import Contacts",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Import from Google Contacts")
                }
            }

            if (showImportProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            importError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Text(
                "Or add guest manually",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Manual guest addition form
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Guest Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { 
                    phoneNumber = it
                    phoneError = if (!PhoneNumberValidator.isValid(it)) {
                        "Please enter a valid phone number"
                    } else null
                },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                isError = phoneError != null,
                supportingText = phoneError?.let { { Text(it) } },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    ) {
                        Icon(Icons.Default.Contacts, "Pick Contact")
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Category selection
            OutlinedCard(
                onClick = { showCategoryDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedCategory?.name ?: "Select Category",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Icon(Icons.Default.ArrowDropDown, "Select")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (name.isNotBlank() && phoneNumber.isNotBlank() && phoneError == null) {
                        viewModel.addGuest(
                            name = name,
                            phoneNumber = PhoneNumberValidator.format(phoneNumber),
                            categoryId = selectedCategory?.id
                        )
                        navController.navigateUp()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && phoneNumber.isNotBlank() && phoneError == null
            ) {
                Icon(Icons.Default.Add, "Add")
                Spacer(Modifier.width(8.dp))
                Text("Add Guest")
            }
        }
    }

    // Category selection dialog
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Select Category") },
            text = {
                LazyColumn {
                    items(categories) { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCategory = category
                                    showCategoryDialog = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(category.color), CircleShape)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(category.name)
                        }
                    }
                    item {
                        TextButton(
                            onClick = {
                                navController.navigate(Screen.ManageCategories.route)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add New Category")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

fun loadGoogleContacts(context: Context): List<Contact> {
    val contacts = mutableListOf<Contact>()
    val cursor = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        null,
        "${ContactsContract.RawContacts.ACCOUNT_TYPE} = ?",
        arrayOf("com.google"),
        null
    )

    cursor?.use {
        val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

        while (it.moveToNext()) {
            val name = it.getString(nameIndex)
            val number = it.getString(numberIndex)
            contacts.add(Contact(name, number))
        }
    }

    return contacts
}

private fun fetchContacts(account: GoogleSignInAccount) {
    // Use the account to fetch contacts
} 