package com.hari.management.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromInvitationStatus(status: InvitationStatus): String {
        return status.name
    }

    @TypeConverter
    fun toInvitationStatus(value: String): InvitationStatus {
        return try {
            InvitationStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            InvitationStatus.NOT_INVITED
        }
    }
} 