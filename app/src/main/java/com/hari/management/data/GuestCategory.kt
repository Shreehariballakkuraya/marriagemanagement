package com.hari.management.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "guest_categories")
data class GuestCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val color: Long // Store color as Long for Room compatibility
) 