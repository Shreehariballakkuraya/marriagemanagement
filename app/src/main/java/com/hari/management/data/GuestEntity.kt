package com.hari.management.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "guests",
    foreignKeys = [
        ForeignKey(
            entity = GuestCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("categoryId") // Add index for foreign key
    ]
)
data class GuestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val phoneNumber: String,
    val isInvitationVerified: Boolean = false,
    val reminderDate: Long? = null,
    val categoryId: Int? = null, // Can be null if no category is selected
    val invitationStatus: InvitationStatus = InvitationStatus.NOT_INVITED,
    val hasInteracted: Boolean = false // New field to track interaction
) 