package com.hari.management.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hari.management.data.GuestCategory
import com.hari.management.data.InvitationStatus
import com.hari.management.viewmodel.GuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSearch(
    viewModel: GuestViewModel,
    categories: List<GuestCategory>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<InvitationStatus?>(null) }
    var selectedCategory by remember { mutableStateOf<GuestCategory?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Search TextField
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { 
                searchQuery = it
                viewModel.searchGuests(it)
            },
            label = { Text("Search Guests") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        // Status Filters
        Text(
            "Status Filter",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { 
                        selectedStatus = null
                        viewModel.setStatusFilter(null)
                    },
                    label = { Text("All") }
                )
            }
            items(InvitationStatus.values()) { status ->
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = { 
                        selectedStatus = status
                        viewModel.setStatusFilter(status)
                    },
                    label = { Text(status.name) }
                )
            }
        }

        // Category Filters
        Text(
            "Category Filter",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { 
                        selectedCategory = null
                        viewModel.setCategoryFilter(null)
                    },
                    label = { Text("All") }
                )
            }
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { 
                        selectedCategory = category
                        viewModel.setCategoryFilter(category)
                    },
                    label = { Text(category.name) }
                )
            }
        }
    }
} 