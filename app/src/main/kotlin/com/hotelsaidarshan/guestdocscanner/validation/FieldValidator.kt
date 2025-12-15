package com.hotelsaidarshan.guestdocscanner.validation

object FieldValidator {
    fun isValidMobileNumber(input: String): Boolean {
        return input.matches(Regex("\\d{10}"))
    }

    fun isValidVehicleNumber(input: String): Boolean {
        val normalized = input.uppercase().replace(" ", "")
        return normalized.matches(Regex("^[A-Z]{2}\\d{2}[A-Z]{2}\\d{4}$"))
    }

    fun isValidRoomNumber(input: String): Boolean {
        if (input.isBlank()) return false
        return input.matches(Regex("^[A-Za-z0-9-]+$"))
    }
}
