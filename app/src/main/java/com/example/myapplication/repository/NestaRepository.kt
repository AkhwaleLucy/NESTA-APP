package com.example.myapplication.repository

import com.example.myapplication.model.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NestaRepository {

    // Single source of truth for authenticated user session from AuthRepository
    val currentUser: StateFlow<User?> = AuthRepository.currentUser

    // Master Properties State (Stores persistent agent submissions)
    private val _properties = MutableStateFlow<List<Property>>(emptyList())
    val properties: StateFlow<List<Property>> = _properties.asStateFlow()

    // Support Contacts State (Loaded from managed configuration)
    private val _supportEmail = MutableStateFlow("hello@nesta.co.ke")
    val supportEmail: StateFlow<String> = _supportEmail.asStateFlow()

    private val _supportPhone = MutableStateFlow("+254 712 345 678")
    val supportPhone: StateFlow<String> = _supportPhone.asStateFlow()

    private val _whatsappNumber = MutableStateFlow("+254712345678")
    val whatsappNumber: StateFlow<String> = _whatsappNumber.asStateFlow()

    // Favorites
    private val _favoritePropertyIds = MutableStateFlow<Set<String>>(emptySet())
    val favoritePropertyIds: StateFlow<Set<String>> = _favoritePropertyIds.asStateFlow()

    // Comparison Tray (max 4)
    private val _comparePropertyIds = MutableStateFlow<Set<String>>(emptySet())
    val comparePropertyIds: StateFlow<Set<String>> = _comparePropertyIds.asStateFlow()

    // Recently Viewed
    private val _recentlyViewedIds = MutableStateFlow<List<String>>(emptyList())
    val recentlyViewedIds: StateFlow<List<String>> = _recentlyViewedIds.asStateFlow()

    // Viewing Requests
    private val _viewingRequests = MutableStateFlow<List<ViewingRequest>>(emptyList())
    val viewingRequests: StateFlow<List<ViewingRequest>> = _viewingRequests.asStateFlow()

    // Conversations
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    // Messages Map (conversationId -> List<Message>)
    private val _messages = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    val messages: StateFlow<Map<String, List<Message>>> = _messages.asStateFlow()

    // Reports Queue (Admin moderation)
    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    // Pending Agents Queue (Admin moderation)
    private val _pendingAgents = MutableStateFlow<List<User>>(emptyList())
    val pendingAgents: StateFlow<List<User>> = _pendingAgents.asStateFlow()

    // Notifications
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    // Property Alerts
    private val _propertyAlerts = MutableStateFlow<List<PropertyAlert>>(emptyList())
    val propertyAlerts: StateFlow<List<PropertyAlert>> = _propertyAlerts.asStateFlow()

    private fun getCurrentIsoTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
    }

    // Dynamic Neighborhood Catalog derived from published listings
    fun getFeaturedNeighborhoods(): List<String> {
        return _properties.value
            .filter { it.status == ListingStatus.AVAILABLE && it.verificationStatus == VerificationStatus.VERIFIED }
            .map { it.location.neighborhood.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    // Clear user-specific state on logout or account switch
    fun clearUserData() {
        _favoritePropertyIds.value = emptySet()
        _comparePropertyIds.value = emptySet()
        _recentlyViewedIds.value = emptyList()
        _viewingRequests.value = emptyList()
        _conversations.value = emptyList()
        _messages.value = emptyMap()
        _propertyAlerts.value = emptyList()
    }

    fun resetForTesting(testProperties: List<Property> = emptyList()) {
        clearUserData()
        _properties.value = testProperties
    }

    // --- FAVORITES & COMPARISON ---
    fun toggleFavorite(propertyId: String) {
        val current = _favoritePropertyIds.value.toMutableSet()
        val prop = _properties.value.find { it.id == propertyId }
        if (current.contains(propertyId)) {
            current.remove(propertyId)
            prop?.let { _ ->
                _properties.value = _properties.value.map {
                    if (it.id == propertyId) it.copy(savesCount = maxOf(0, it.savesCount - 1)) else it
                }
            }
        } else {
            current.add(propertyId)
            prop?.let { _ ->
                _properties.value = _properties.value.map {
                    if (it.id == propertyId) it.copy(savesCount = it.savesCount + 1) else it
                }
            }
        }
        _favoritePropertyIds.value = current
    }

    fun toggleCompare(propertyId: String) {
        val current = _comparePropertyIds.value.toMutableSet()
        if (current.contains(propertyId)) {
            current.remove(propertyId)
        } else if (current.size < 4) {
            current.add(propertyId)
        }
        _comparePropertyIds.value = current
    }

    fun addRecentlyViewed(propertyId: String) {
        val current = _recentlyViewedIds.value.toMutableList()
        current.remove(propertyId)
        current.add(0, propertyId)
        _recentlyViewedIds.value = current.take(10)

        // Increment total views
        _properties.value = _properties.value.map {
            if (it.id == propertyId) {
                it.copy(viewsCount = it.viewsCount + 1)
            } else it
        }
    }

    // --- SMART SEARCH & NATURAL LANGUAGE PARSER ---
    fun parseNaturalSearch(query: String): SearchFilter {
        val lower = query.lowercase().trim()
        if (lower.isEmpty()) return SearchFilter()

        var propType: PropertyType? = null
        if (lower.contains("apartment") || lower.contains("flat")) propType = PropertyType.APARTMENT
        else if (lower.contains("townhouse")) propType = PropertyType.TOWN_HOUSE
        else if (lower.contains("house") || lower.contains("maisonette")) propType = PropertyType.HOUSE
        else if (lower.contains("studio")) propType = PropertyType.STUDIO
        else if (lower.contains("villa")) propType = PropertyType.VILLA
        else if (lower.contains("land") || lower.contains("plot")) propType = PropertyType.LAND
        else if (lower.contains("office")) propType = PropertyType.OFFICE

        var beds: Int? = null
        val bedRegex = "(\\d+)\\s*(bed|bedroom|br)".toRegex()
        val bedMatch = bedRegex.find(lower)
        if (bedMatch != null) {
            beds = bedMatch.groupValues[1].toIntOrNull()
        }

        var maxPrice: Double? = null
        val priceRegexUnder = "under\\s*k?s?h?\\s*(\\d+)\\s*(k|thousand)?".toRegex()
        val priceMatchUnder = priceRegexUnder.find(lower)
        if (priceMatchUnder != null) {
            val num = priceMatchUnder.groupValues[1].toDoubleOrNull() ?: 0.0
            val multiplier = if (priceMatchUnder.groupValues[2] == "k" || priceMatchUnder.groupValues[2] == "thousand" || num < 1000) 1000.0 else 1.0
            maxPrice = num * multiplier
        }

        var locationName: String? = null
        val knownHoods = getFeaturedNeighborhoods()
        for (n in knownHoods) {
            if (lower.contains(n.lowercase())) {
                locationName = n
                break
            }
        }

        var listingType: ListingType? = null
        if (lower.contains("rent")) listingType = ListingType.FOR_RENT
        else if (lower.contains("sale") || lower.contains("buy")) listingType = ListingType.FOR_SALE

        return SearchFilter(
            query = query,
            listingType = listingType,
            propertyType = propType,
            neighborhood = locationName,
            maxPrice = maxPrice,
            bedrooms = beds
        )
    }

    // Public Query: Only APPROVED & AVAILABLE agent submissions shown to seekers
    fun getFilteredProperties(filter: SearchFilter): List<Property> {
        val userRole = currentUser.value?.role

        var result = _properties.value.filter {
            if (userRole == UserRole.ADMIN) {
                true
            } else {
                it.status == ListingStatus.AVAILABLE && it.verificationStatus == VerificationStatus.VERIFIED
            }
        }

        // Check text query only if specific filters (like bedrooms/neighborhood) were NOT parsed
        val hasParsedFilters = filter.propertyType != null || filter.neighborhood != null || filter.bedrooms != null || filter.maxPrice != null
        if (filter.query.isNotBlank() && !hasParsedFilters) {
            val q = filter.query.lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) ||
                        it.location.neighborhood.lowercase().contains(q) ||
                        it.description.lowercase().contains(q) ||
                        it.propertyType.displayName.lowercase().contains(q)
            }
        }

        if (filter.listingType != null) {
            result = result.filter { it.listingType == filter.listingType }
        }

        if (filter.propertyType != null) {
            result = result.filter { it.propertyType == filter.propertyType }
        }

        if (!filter.neighborhood.isNull_or_blank()) {
            result = result.filter { it.location.neighborhood.equals(filter.neighborhood, ignoreCase = true) }
        }

        if (filter.minPrice != null) {
            result = result.filter { it.price >= filter.minPrice }
        }

        if (filter.maxPrice != null) {
            result = result.filter { it.price <= filter.maxPrice }
        }

        if (filter.bedrooms != null) {
            result = result.filter { it.bedrooms >= filter.bedrooms }
        }

        if (filter.bathrooms != null) {
            result = result.filter { it.bathrooms >= filter.bathrooms }
        }

        if (filter.minSizeSqm != null) {
            result = result.filter { it.sizeSqm >= filter.minSizeSqm }
        }

        if (filter.selectedAmenities.isNotEmpty()) {
            result = result.filter { p -> p.amenities.containsAll(filter.selectedAmenities) }
        }

        if (filter.isVerifiedOnly) {
            result = result.filter { it.verificationStatus == VerificationStatus.VERIFIED }
        }

        return when (filter.sortBy) {
            SortBy.RECOMMENDED -> result.sortedWith(compareByDescending<Property> { it.isFeatured }.thenByDescending { it.viewsCount })
            SortBy.NEWEST -> result.sortedByDescending { it.createdAt }
            SortBy.PRICE_LOW_HIGH -> result.sortedBy { it.price }
            SortBy.PRICE_HIGH_LOW -> result.sortedByDescending { it.price }
            SortBy.MOST_VIEWED -> result.sortedByDescending { it.viewsCount }
            SortBy.MOST_SAVED -> result.sortedByDescending { it.savesCount }
        }
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()

    // --- LISTING CREATION & QUALITY SCORE ---
    fun calculateQualityScore(prop: Property): Int {
        var score = 0
        if (prop.title.length > 10) score += 15
        if (prop.description.length > 50) score += 20
        if (prop.images.size >= 1) score += 20
        if (prop.sizeSqm > 0) score += 15
        if (prop.amenities.isNotEmpty()) score += 15
        if (prop.location.approximateAddress.isNotBlank()) score += 15
        return minOf(100, score)
    }

    // Submit Real Agent Property:
    // Derives ownership & identity directly from AuthRepository.currentUser
    // Forces status = ListingStatus.PENDING, verificationStatus = VerificationStatus.PENDING, isFeatured = false
    // Resets analytics counters
    fun addProperty(draftProp: Property) {
        val user = currentUser.value ?: throw SecurityException("Authentication required to submit property listings.")

        if (user.role != UserRole.AGENT && user.role != UserRole.ADMIN) {
            throw SecurityException("Only authorized Agent or Admin accounts can submit property listings.")
        }

        if (!user.isEmailVerified) {
            throw SecurityException("Your email address must be verified before you can submit property listings.")
        }

        if (user.role == UserRole.AGENT && !user.isAgentApproved) {
            throw SecurityException("Your agent account is currently pending administrator approval. Property submissions are restricted until approved.")
        }

        val currentTime = getCurrentIsoTimestamp()

        val sanitizedProperty = draftProp.copy(
            id = "prop_${System.currentTimeMillis()}",
            ownerId = user.id,
            ownerName = user.name,
            ownerPhone = user.phone,
            ownerEmail = user.email,
            ownerAgency = user.agencyName,
            isOwnerVerified = user.isVerified,
            status = ListingStatus.PENDING,
            verificationStatus = VerificationStatus.PENDING,
            isFeatured = false, // Client cannot grant featured status
            viewsCount = 0, // Reset analytics
            uniqueViews = 0,
            savesCount = 0,
            sharesCount = 0,
            inquiriesCount = 0,
            createdAt = currentTime
        )

        val list = _properties.value.toMutableList()
        list.add(0, sanitizedProperty)
        _properties.value = list

        // Route notification to authorized moderators
        val notifs = _notifications.value.toMutableList()
        notifs.add(0, NotificationItem(
            id = "n_${System.currentTimeMillis()}",
            userId = "admin_moderators", // Backend moderator routing
            title = "New Agent Listing Submitted",
            message = "Agent ${user.name} submitted '${sanitizedProperty.title}' for review.",
            type = "MODERATION",
            timestamp = currentTime
        ))
        _notifications.value = notifs
    }

    // Agent Ownership Enforced: Agents can ONLY update status for their OWN properties
    fun updatePropertyStatus(propertyId: String, status: ListingStatus) {
        val user = currentUser.value ?: throw SecurityException("Authentication required.")
        _properties.value = _properties.value.map { prop ->
            if (prop.id == propertyId) {
                if (user.role != UserRole.ADMIN && prop.ownerId != user.id) {
                    throw SecurityException("You do not have permission to modify this property listing.")
                }
                prop.copy(status = status)
            } else prop
        }
    }

    // --- ADMIN MODERATION ACTIONS ---
    suspend fun fetchPendingAgents() = withContext(Dispatchers.IO) {
        val user = currentUser.value ?: return@withContext
        if (user.role != UserRole.ADMIN) return@withContext

        try {
            val firestore = FirebaseFirestore.getInstance()
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("role", UserRole.AGENT.name)
                .whereEqualTo("isAgentApproved", false)
                .get().await()

            val list = querySnapshot.documents.mapNotNull { doc ->
                val uid = doc.id
                val name = doc.getString("name") ?: ""
                val email = doc.getString("email") ?: ""
                val phone = doc.getString("phone") ?: ""
                val agencyName = doc.getString("agencyName") ?: ""
                val isEmailVerified = doc.getBoolean("isEmailVerified") ?: false
                val isAgentApproved = doc.getBoolean("isAgentApproved") ?: false
                val memberSince = doc.getString("memberSince") ?: ""

                User(
                    id = uid,
                    name = name,
                    email = email,
                    phone = phone,
                    role = UserRole.AGENT,
                    isEmailVerified = isEmailVerified,
                    approvalStatus = if (isAgentApproved) AgentApprovalStatus.APPROVED else AgentApprovalStatus.PENDING,
                    agencyName = agencyName,
                    memberSince = memberSince
                )
            }
            _pendingAgents.value = list
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    suspend fun approveAgent(agentUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = currentUser.value ?: return@withContext Result.failure(SecurityException("Admin authentication required."))
        if (user.role != UserRole.ADMIN) return@withContext Result.failure(SecurityException("Admin authorization required."))

        try {
            FirebaseFirestore.getInstance()
                .collection("users").document(agentUserId)
                .update("isAgentApproved", true).await()

            _pendingAgents.value = _pendingAgents.value.filter { it.id != agentUserId }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception(e.message ?: "Failed to approve agent application.", e))
        }
    }

    suspend fun rejectAgent(agentUserId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = currentUser.value ?: return@withContext Result.failure(SecurityException("Admin authentication required."))
        if (user.role != UserRole.ADMIN) return@withContext Result.failure(SecurityException("Admin authorization required."))

        try {
            FirebaseFirestore.getInstance()
                .collection("users").document(agentUserId)
                .update(mapOf("isAgentApproved" to false, "rejectionReason" to reason)).await()

            _pendingAgents.value = _pendingAgents.value.filter { it.id != agentUserId }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception(e.message ?: "Failed to reject agent application.", e))
        }
    }

    fun approveProperty(propertyId: String) {
        val user = currentUser.value ?: return
        if (user.role != UserRole.ADMIN) throw SecurityException("Admin authorization required.")

        _properties.value = _properties.value.map {
            if (it.id == propertyId) it.copy(verificationStatus = VerificationStatus.VERIFIED, status = ListingStatus.AVAILABLE) else it
        }
    }

    fun rejectProperty(propertyId: String, reason: String) {
        val user = currentUser.value ?: return
        if (user.role != UserRole.ADMIN) throw SecurityException("Admin authorization required.")

        _properties.value = _properties.value.map {
            if (it.id == propertyId) it.copy(verificationStatus = VerificationStatus.REJECTED, rejectionReason = reason) else it
        }
    }

    fun toggleVerifyProperty(propertyId: String) {
        val user = currentUser.value ?: return
        if (user.role != UserRole.ADMIN) throw SecurityException("Admin authorization required.")

        _properties.value = _properties.value.map {
            if (it.id == propertyId) {
                val newStatus = if (it.verificationStatus == VerificationStatus.VERIFIED) VerificationStatus.PENDING else VerificationStatus.VERIFIED
                it.copy(verificationStatus = newStatus)
            } else it
        }
    }

    fun suspendProperty(propertyId: String) {
        val user = currentUser.value ?: return
        if (user.role != UserRole.ADMIN) throw SecurityException("Admin authorization required.")

        _properties.value = _properties.value.map {
            if (it.id == propertyId) it.copy(status = ListingStatus.SUSPENDED) else it
        }
    }

    // --- MESSAGING & VIEWING REQUESTS ---
    fun sendViewingRequest(request: ViewingRequest) {
        val user = currentUser.value ?: throw SecurityException("Authentication required.")
        val currentTime = getCurrentIsoTimestamp()

        val authenticatedRequest = request.copy(
            id = "v_${System.currentTimeMillis()}",
            requesterId = user.id,
            requesterName = user.name,
            requesterPhone = user.phone,
            createdAt = currentTime
        )

        val list = _viewingRequests.value.toMutableList()
        list.add(0, authenticatedRequest)
        _viewingRequests.value = list

        _properties.value = _properties.value.map {
            if (it.id == request.propertyId) it.copy(inquiriesCount = it.inquiriesCount + 1) else it
        }
    }

    fun sendMessage(conversationId: String, text: String) {
        val user = currentUser.value ?: throw SecurityException("Authentication required.")
        val currentTime = SimpleDateFormat("HH:mm", Locale.US).format(Date())

        val map = _messages.value.toMutableMap()
        val currentMsgs = map[conversationId]?.toMutableList() ?: mutableListOf()
        val newMsg = Message(
            id = "msg_${System.currentTimeMillis()}",
            conversationId = conversationId,
            senderId = user.id,
            senderName = user.name,
            text = text,
            timestamp = currentTime
        )
        currentMsgs.add(newMsg)
        map[conversationId] = currentMsgs
        _messages.value = map

        _conversations.value = _conversations.value.map {
            if (it.id == conversationId) it.copy(lastMessage = text, lastTimestamp = currentTime) else it
        }
    }

    fun submitReport(report: Report) {
        val user = currentUser.value ?: throw SecurityException("Authentication required.")
        val currentTime = getCurrentIsoTimestamp()

        val authenticatedReport = report.copy(
            id = "rep_${System.currentTimeMillis()}",
            reporterId = user.id,
            createdAt = currentTime
        )

        val list = _reports.value.toMutableList()
        list.add(0, authenticatedReport)
        _reports.value = list
    }

    fun addPropertyAlert(alert: PropertyAlert) {
        val user = currentUser.value ?: throw SecurityException("Authentication required.")
        val authenticatedAlert = alert.copy(
            id = "pa_${System.currentTimeMillis()}",
            userId = user.id
        )
        val list = _propertyAlerts.value.toMutableList()
        list.add(0, authenticatedAlert)
        _propertyAlerts.value = list
    }
}
