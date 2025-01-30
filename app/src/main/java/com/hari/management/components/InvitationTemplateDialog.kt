package com.hari.management.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InvitationTemplateDialog(
    onDismiss: () -> Unit,
    onTemplateSelected: (String) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf(0) }
    val templates = listOf(
        "Dear {name}, You are cordially invited to our event. Looking forward to your presence.",
        "Hello {name}, We would be honored to have you at our event.",
        "Greetings {name}, Please join us for our special occasion."
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Invitation Template") },
        text = {
            Column {
                templates.forEachIndexed { index, template ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = selectedTemplate == index,
                            onClick = { selectedTemplate = index }
                        )
                        Text(
                            text = template,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTemplateSelected(templates[selectedTemplate])
                    onDismiss()
                }
            ) {
                Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 