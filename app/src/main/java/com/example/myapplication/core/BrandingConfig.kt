package com.example.myapplication.core

import com.example.myapplication.BuildConfig
import com.example.myapplication.repository.NestaRepository

object BrandingConfig {
    // Dynamic Support Contacts (Managed via repository / remote configuration cache with offline fallbacks)
    val supportEmail: String
        get() = NestaRepository.supportEmail.value

    val supportPhone: String
        get() = NestaRepository.supportPhone.value

    val whatsappNumber: String
        get() = NestaRepository.whatsappNumber.value

    // App Version managed via BuildConfig
    val appVersion: String
        get() = BuildConfig.VERSION_NAME

    // Featured Neighborhoods loaded from existing data source
    val featuredNeighborhoods: List<String>
        get() = NestaRepository.getFeaturedNeighborhoods()
}
