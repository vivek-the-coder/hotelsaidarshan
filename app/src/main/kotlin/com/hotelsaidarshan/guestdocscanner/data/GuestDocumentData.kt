package com.hotelsaidarshan.guestdocscanner.data

data class GuestDocumentData(
    val name: String = "",
    val dob: String = "",
    val gender: String = "",
    val address: String = "",
    val comingFrom: String = "",
    val goingTo: String = "",
    val mobileNumber: String = "",
    val vehicleNumber: String = "",
    val roomNumber: String = "",
) {
    companion object {
        fun empty(): GuestDocumentData = GuestDocumentData()
    }
}
