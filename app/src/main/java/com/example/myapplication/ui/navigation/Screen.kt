package com.example.myapplication.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Record : Screen("record")
    object Details : Screen("details/{recordingId}") {
        fun createRoute(recordingId: String) = "details/$recordingId"
    }
}
