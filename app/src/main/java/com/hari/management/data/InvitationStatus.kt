package com.hari.management.data

enum class InvitationStatus {
    INVITED,
    PENDING,
    NOT_INVITED;

    companion object {
        fun fromInt(value: Int): InvitationStatus = values()[value]
    }
} 