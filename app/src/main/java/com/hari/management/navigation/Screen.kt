package com.hari.management.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object GuestList : Screen("guest_list")
    object AddGuest : Screen("add_guest")
    object GuestDetail : Screen("guest_detail/{guestId}") {
        fun createRoute(guestId: Int) = "guest_detail/$guestId"
    }
    object ManageCategories : Screen("manage_categories")
    object Settings : Screen("settings")
    object Statistics : Screen("statistics")
    object BulkOperations : Screen("bulk_operations")
    object EmailSettings : Screen("email_settings")
    object AdminSettings : Screen("admin_settings")

    protected fun String.replacePlaceholders(vararg args: String): String {
        var result = this
        args.forEach { arg ->
            result = result.replace("{$arg}", arg)
        }
        return result
    }

    fun createRoute(vararg args: String) = buildString {
        append(route)
        args.forEach { arg ->
            append("/$arg")
        }
    }
} 