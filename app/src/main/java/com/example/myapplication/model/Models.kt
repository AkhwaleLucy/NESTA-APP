package com.example.myapplication.model

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<out T>(val data: T) : UiState<T>()
    object Empty : UiState<Nothing>()
    data class Error(val message: String) : UiState<Nothing>()
}

enum class UserRole {
    SEEKER,
    AGENT,
    ADMIN
}

enum class AgentApprovalStatus(val label: String) {
    PENDING("Pending Approval"),
    APPROVED("Approved"),
    REJECTED("Application Rejected"),
    SUSPENDED("Account Suspended")
}

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val profileImage: String = "",
    val isEmailVerified: Boolean = false,
    val approvalStatus: AgentApprovalStatus = AgentApprovalStatus.PENDING,
    val reviewedBy: String = "",
    val reviewedAt: String = "",
    val rejectionReason: String = "",
    val agencyName: String = "",
    val activeListingsCount: Int = 0,
    val memberSince: String
) {
    val isAgentApproved: Boolean
        get() = approvalStatus == AgentApprovalStatus.APPROVED

    val isVerified: Boolean
        get() = isEmailVerified && (role != UserRole.AGENT || isAgentApproved)
}

enum class PropertyType(val displayName: String) {
    APARTMENT("Apartment"),
    HOUSE("House"),
    BEDSITTER("Bedsitter"),
    STUDIO("Studio"),
    MAISONETTE("Maisonette"),
    TOWN_HOUSE("Townhouse"),
    VILLA("Villa"),
    LAND("Land"),
    OFFICE("Office"),
    SHOP("Shop"),
    WAREHOUSE("Warehouse")
}

enum class ListingType(val displayName: String) {
    FOR_RENT("For Rent"),
    FOR_SALE("For Sale")
}

enum class BillingPeriod(val label: String) {
    MONTHLY("/ month"),
    YEARLY("/ year"),
    ONE_TIME("")
}

enum class ListingStatus(val label: String) {
    DRAFT("Draft"),
    AVAILABLE("Available"),
    PENDING("Pending Review"),
    RENTED("Rented"),
    SOLD("Sold"),
    SUSPENDED("Suspended")
}

enum class VerificationStatus(val label: String) {
    PENDING("Pending Verification"),
    VERIFIED("Verified Listing"),
    REJECTED("Rejected"),
    NEEDS_CHANGES("Needs Changes")
}

enum class Amenity(val displayName: String, val iconName: String) {
    PARKING("Parking", "LocalParking"),
    POOL("Swimming Pool", "Pool"),
    GYM("Gym", "FitnessCenter"),
    CCTV("CCTV Security", "Videocam"),
    BACKUP_GENERATOR("Backup Generator", "ElectricBolt"),
    BOREHOLE("Borehole Water", "WaterDrop"),
    ELEVATOR("Elevator", "Elevator"),
    INTERNET("High Speed Fiber", "Wifi"),
    FURNISHED("Furnished", "Chair"),
    SERVICED("Serviced", "CleaningServices"),
    PET_FRIENDLY("Pet Friendly", "Pets"),
    SOLAR("Solar Water", "WbSunny"),
    DSQ("DSQ", "HomeWork"),
    AIR_CONDITIONING("Air Conditioning", "AcUnit"),
    BALCONY("Private Balcony", "Balcony"),
    GARDEN("Landscaped Garden", "Park"),
    SECURITY("24/7 Guards", "Security")
}

data class Location(
    val county: String,
    val city: String,
    val neighborhood: String,
    val approximateAddress: String,
    val lat: Double,
    val lng: Double,
    val nearbyLandmarks: List<String> = emptyList()
)

data class Property(
    val id: String,
    val ownerId: String,
    val ownerName: String,
    val ownerPhone: String,
    val ownerEmail: String,
    val ownerAgency: String = "",
    val isOwnerVerified: Boolean = false,
    val title: String,
    val description: String,
    val propertyType: PropertyType,
    val listingType: ListingType,
    val price: Double,
    val currency: String = "KSh",
    val billingPeriod: BillingPeriod = BillingPeriod.MONTHLY,
    val bedrooms: Int,
    val bathrooms: Int,
    val sizeSqm: Int,
    val parkingSpaces: Int,
    val location: Location,
    val amenities: List<Amenity>,
    val images: List<String>,
    val imagePublicIds: List<String> = emptyList(),
    val coverImage: String,
    val status: ListingStatus,
    val verificationStatus: VerificationStatus,
    val rejectionReason: String = "",
    val isFeatured: Boolean = false,
    val viewsCount: Int = 0,
    val uniqueViews: Int = 0,
    val savesCount: Int = 0,
    val sharesCount: Int = 0,
    val inquiriesCount: Int = 0,
    val createdAt: String,
    val estimatedDeposit: Double? = null,
    val estimatedServiceCharge: Double? = null
)

enum class SortBy(val label: String) {
    RECOMMENDED("Recommended"),
    NEWEST("Newest"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    MOST_VIEWED("Most Viewed"),
    MOST_SAVED("Most Saved")
}

data class SearchFilter(
    val query: String = "",
    val listingType: ListingType? = null,
    val propertyType: PropertyType? = null,
    val neighborhood: String? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val bedrooms: Int? = null,
    val bathrooms: Int? = null,
    val minSizeSqm: Int? = null,
    val selectedAmenities: Set<Amenity> = emptySet(),
    val isVerifiedOnly: Boolean = false,
    val sortBy: SortBy = SortBy.RECOMMENDED
)

enum class ViewingStatus(val label: String) {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    RESCHEDULED("Rescheduled"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

data class ViewingRequest(
    val id: String,
    val propertyId: String,
    val propertyTitle: String,
    val requesterId: String,
    val requesterName: String,
    val requesterPhone: String,
    val ownerId: String,
    val requestedDate: String,
    val requestedTime: String,
    val status: ViewingStatus = ViewingStatus.PENDING,
    val message: String = "",
    val createdAt: String
)

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: String,
    val isRead: Boolean = false
)

data class Conversation(
    val id: String,
    val propertyId: String,
    val propertyTitle: String,
    val propertyPrice: String,
    val propertyImage: String = "",
    val seekerId: String = "",
    val seekerName: String = "",
    val agentId: String = "",
    val agentName: String = "",
    val lastMessage: String = "",
    val lastTimestamp: String = "",
    val unreadCount: Int = 0,
    val participants: List<String> = emptyList()
)

data class Report(
    val id: String,
    val propertyId: String,
    val propertyTitle: String,
    val reporterId: String,
    val reason: String,
    val description: String,
    val status: String = "Pending Review",
    val createdAt: String
)

data class NotificationItem(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String,
    val isRead: Boolean = false,
    val timestamp: String
)

data class PropertyAlert(
    val id: String,
    val userId: String,
    val location: String,
    val propertyType: PropertyType,
    val minPrice: Double,
    val maxPrice: Double,
    val bedrooms: Int,
    val frequency: String = "Daily",
    val isActive: Boolean = true
)
