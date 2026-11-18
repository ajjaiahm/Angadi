package com.angadi.app.domain.model

/** Local, on-device profile. There is no account system, so this is not an authenticated identity. */
data class UserProfile(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val imagePath: String = "",
) {
    val firstName: String get() = name.trim().substringBefore(' ')
    val greeting: String get() = if (firstName.isBlank()) "Hello there!" else "Hi $firstName!"
}
