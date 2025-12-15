package com.hotelsaidarshan.guestdocscanner

sealed interface Screen {
    data object Home : Screen
    data object Camera : Screen
    data object Processing : Screen
    data object Verification : Screen
}
