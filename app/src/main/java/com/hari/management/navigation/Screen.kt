package com.hari.management.navigation

sealed class Screen(val route: String) {
    object GuestList : Screen("guest_list")
    object AddGuest : Screen("add_guest")
    object GuestDetail : Screen("guest_detail/{guestId}") {
        fun createRoute(guestId: Int) = "guest_detail/$guestId"
    }
    object ManageCategories : Screen("manage_categories")
    object Settings : Screen("settings")

    protected fun String.replacePlaceholders(vararg args: String): String {
        var result = this
        args.forEach { arg ->
            result = result.replace("{$arg}", arg)
        }
        return result
    }
} 