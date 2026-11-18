package com.angadi.app.data.repository

import com.angadi.app.data.local.AngadiDatabase
import com.angadi.app.data.local.ProfileEntity
import com.angadi.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepository(private val db: AngadiDatabase) {
    val profile: Flow<UserProfile> = db.profileDao().observe().map { it?.let { e -> UserProfile(e.name, e.email, e.phone, e.imagePath) } ?: UserProfile() }

    /** Returns a validation message or null on success. */
    suspend fun save(profile: UserProfile): String? {
        val email = profile.email.trim()
        if (profile.name.trim().length > 60) return "Name is too long."
        if (email.isNotEmpty() && !EMAIL.matches(email)) return "Enter a valid email address."
        val phone = profile.phone.filter { it.isDigit() }
        if (profile.phone.isNotBlank() && phone.length !in 10..12) return "Enter a valid phone number."
        db.profileDao().save(ProfileEntity(1, profile.name.trim(), email, profile.phone.trim(), profile.imagePath))
        return null
    }

    private companion object {
        val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
