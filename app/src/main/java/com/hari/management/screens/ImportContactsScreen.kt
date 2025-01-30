package com.hari.management.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hari.management.util.GoogleContactsHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportContactsScreen(
    onContactsImported: (List<GoogleContactsHelper.Contact>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var contacts by remember { mutableStateOf<List<GoogleContactsHelper.Contact>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedContacts by remember { mutableStateOf(setOf<GoogleContactsHelper.Contact>()) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            scope.launch {
                isLoading = true
                contacts = GoogleContactsHelper.fetchContacts(context)
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (!isLoading && contacts.isEmpty()) {
            Button(
                onClick = {
                    GoogleContactsHelper.initGoogleSignIn(context)
                    signInLauncher.launch(GoogleContactsHelper.getSignInIntent())
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Sign in with Google")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(contacts) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = contact in selectedContacts,
                            onCheckedChange = { checked ->
                                selectedContacts = if (checked) {
                                    selectedContacts + contact
                                } else {
                                    selectedContacts - contact
                                }
                            }
                        )
                        Column(
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = contact.phoneNumber,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { onContactsImported(selectedContacts.toList()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                enabled = selectedContacts.isNotEmpty()
            ) {
                Text("Import Selected Contacts (${selectedContacts.size})")
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
} 