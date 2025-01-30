package com.hari.management.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hari.management.data.GuestEntity
import com.hari.management.data.InvitationStatus
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

@Composable
fun GuestDashboard(guests: List<GuestEntity>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Statistics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Guest Statistics",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                StatisticRow(
                    label = "Total Guests",
                    value = guests.size,
                    color = MaterialTheme.colorScheme.primary
                )
                StatisticRow(
                    label = "Invited",
                    value = guests.count { it.invitationStatus == InvitationStatus.INVITED },
                    color = Color.Green
                )
                StatisticRow(
                    label = "Pending",
                    value = guests.count { it.invitationStatus == InvitationStatus.PENDING },
                    color = Color.Yellow
                )
                StatisticRow(
                    label = "Not Invited",
                    value = guests.count { it.invitationStatus == InvitationStatus.NOT_INVITED },
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun StatisticRow(
    label: String,
    value: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.PeopleAlt,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label)
        }
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
    }
} 