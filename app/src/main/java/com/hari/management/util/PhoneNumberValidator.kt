package com.hari.management.util

object PhoneNumberValidator {
    private val PHONE_REGEX = Regex("^[+]?[0-9]{10,13}\$")

    fun isValid(phone: String): Boolean {
        return phone.replace(Regex("[-\\s()]"), "").matches(PHONE_REGEX)
    }

    fun format(phone: String): String {
        val digits = phone.replace(Regex("[^0-9+]"), "")
        return when {
            digits.length == 10 -> "${digits.substring(0,3)}-${digits.substring(3,6)}-${digits.substring(6)}"
            else -> digits
        }
    }
} 