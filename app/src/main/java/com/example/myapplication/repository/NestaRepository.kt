package com.example.myapplication.repository

import com.example.myapplication.model.*
import com.example.myapplication.ui.components.formatPrice
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
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

    private var activeNotificationsListener: ListenerRegistration? = null
    private var activeConversationsListener: ListenerRegistration? = null
    private var activeMessagesListener: ListenerRegistration? = null

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
        activeNotificationsListener?.remove()
        activeNotificationsListener = null
        activeConversationsListener?.remove()
        activeConversationsListener = null
        activeMessagesListener?.remove()
        activeMessagesListener = null
        _favoritePropertyIds.value = emptySet()
        _comparePropertyIds.value = emptySet()
        _recentlyViewedIds.value = emptyList()
        _viewingRequests.value = emptyList()
        _conversations.value = emptyList()
        _messages.value = emptyMap()
        _notifications.value = emptyList()
        _propertyAlerts.value = emptyList()
    }

    fun resetForTesting(testProperties: List<Property> = emptyList()) {
        clearUserData()
        _properties.value = testProperties
    }

    // --- RECIPIENT NOTIFICATIONS ---
    fun observeNotifications(userId: String) {
        activeNotificationsListener?.remove()
        try {
            val firestore = FirebaseFirestore.getInstance()
            activeNotificationsListener = firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.mapNotNull { doc ->
                        NotificationItem(
                            id = doc.id,
                            userId = userId,
                            title = doc.getString("title") ?: "",
                            message = doc.getString("message") ?: "",
                            type = doc.getString("type") ?: "SYSTEM",
                            isRead = doc.getBoolean("isRead") ?: false,
                            timestamp = doc.getString("timestamp") ?: ""
                        )
                    }
                    _notifications.value = list
                }
        } catch (_: Exception) { }
    }

    suspend fun sendNotification(recipientId: String, title: String, message: String, type: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val notifId = "notif_${System.currentTimeMillis()}"
            val currentTimeStr = SimpleDateFormat("HH:mm", Locale.US).format(Date())
            val isoTime = getCurrentIsoTimestamp()
            val notifMap = mapOf(
                "id" to notifId,
                "userId" to recipientId,
                "title" to title,
                "message" to message,
                "type" to type,
                "isRead" to false,
                "timestamp" to currentTimeStr,
                "createdAt" to isoTime
            )
            FirebaseFirestore.getInstance().collection("users")
                .document(recipientId)
                .collection("notifications")
                .document(notifId)
                .set(notifMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception(e.message ?: "Failed to send notification.", e))
        }
    }

    fun markNotificationAsRead(userId: String, notificationId: String) {
        try {
            FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .collection("notifications")
                .document(notificationId)
                .update("isRead", true)
        } catch (_: Exception) { }
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
        if (prop.images.isNotEmpty()) score += 20
        if (prop.sizeSqm > 0) score += 15
        if (prop.amenities.isNotEmpty()) score += 15
        if (prop.location.approximateAddress.isNotBlank()) score += 15
        return minOf(100, score)
    }

    suspend fun savePropertyDraft(draftProp: Property): Result<Property> = withContext(Dispatchers.IO) {
        val user = currentUser.value ?: return@withContext Result.failure(SecurityException("Authentication required to save property draft."))

        if (user.role != UserRole.AGENT && user.role != UserRole.ADMIN) {
            return@withContext Result.failure(SecurityException("Only authorized Agent or Admin accounts can save property drafts."))
        }

        if (!user.isEmailVerified) {
            return@withContext Result.failure(SecurityException("Your email address must be verified before you can save property drafts."))
        }

        if (user.role == UserRole.AGENT && !user.isAgentApproved) {
            return@withContext Result.failure(SecurityException("Your agent account is currently pending administrator approval."))
        }

        val currentTime = getCurrentIsoTimestamp()
        val draftId = draftProp.id.ifBlank { "prop_${System.currentTimeMillis()}" }

        val sanitizedProperty = draftProp.copy(
            id = draftId,
            ownerId = user.id,
            ownerName = user.name,
            ownerPhone = user.phone,
            ownerEmail = user.email,
            ownerAgency = user.agencyName,
            isOwnerVerified = user.isVerified,
            status = ListingStatus.DRAFT,
            verificationStatus = VerificationStatus.PENDING,
            isFeatured = false,
            createdAt = currentTime
        )

        try {
            val firestore = FirebaseFirestore.getInstance()
            val propMap = mapOf(
                "id" to sanitizedProperty.id,
                "ownerId" to user.id,
                "ownerName" to user.name,
                "ownerPhone" to user.phone,
                "ownerEmail" to user.email,
                "ownerAgency" to user.agencyName,
                "isOwnerVerified" to user.isVerified,
                "title" to sanitizedProperty.title,
                "description" to sanitizedProperty.description,
                "propertyType" to sanitizedProperty.propertyType.name,
                "listingType" to sanitizedProperty.listingType.name,
                "price" to sanitizedProperty.price,
                "billingPeriod" to sanitizedProperty.billingPeriod.name,
                "bedrooms" to sanitizedProperty.bedrooms,
                "bathrooms" to sanitizedProperty.bathrooms,
                "sizeSqm" to sanitizedProperty.sizeSqm,
                "parkingSpaces" to sanitizedProperty.parkingSpaces,
                "status" to ListingStatus.DRAFT.name,
                "verificationStatus" to VerificationStatus.PENDING.name,
                "images" to sanitizedProperty.images,
                "imagePublicIds" to sanitizedProperty.imagePublicIds,
                "coverImage" to sanitizedProperty.coverImage,
                "createdAt" to currentTime
            )
            firestore.collection("properties").document(draftId).set(propMap).await()

            val list = _properties.value.toMutableList()
            list.removeAll { it.id == draftId }
            list.add(0, sanitizedProperty)
            _properties.value = list

            Result.success(sanitizedProperty)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception(e.message ?: "Failed to save draft property in database.", e))
        }
    }

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
            id = draftProp.id.ifBlank { "prop_${System.currentTimeMillis()}" },
            ownerId = user.id,
            ownerName = user.name,
            ownerPhone = user.phone,
            ownerEmail = user.email,
            ownerAgency = user.agencyName,
            isOwnerVerified = user.isVerified,
            status = ListingStatus.PENDING,
            verificationStatus = VerificationStatus.PENDING,
            isFeatured = false,
            createdAt = currentTime
        )

        try {
            val firestore = FirebaseFirestore.getInstance()
            val propMap = mapOf(
                "id" to sanitizedProperty.id,
                "ownerId" to user.id,
                "ownerName" to user.name,
                "ownerPhone" to user.phone,
                "ownerEmail" to user.email,
                "ownerAgency" to user.agencyName,
                "isOwnerVerified" to user.isVerified,
                "title" to sanitizedProperty.title,
                "description" to sanitizedProperty.description,
                "propertyType" to sanitizedProperty.propertyType.name,
                "listingType" to sanitizedProperty.listingType.name,
                "price" to sanitizedProperty.price,
                "billingPeriod" to sanitizedProperty.billingPeriod.name,
                "bedrooms" to sanitizedProperty.bedrooms,
                "bathrooms" to sanitizedProperty.bathrooms,
                "sizeSqm" to sanitizedProperty.sizeSqm,
                "parkingSpaces" to sanitizedProperty.parkingSpaces,
                "status" to ListingStatus.PENDING.name,
                "verificationStatus" to VerificationStatus.PENDING.name,
                "images" to sanitizedProperty.images,
                "imagePublicIds" to sanitizedProperty.imagePublicIds,
                "coverImage" to sanitizedProperty.coverImage,
                "createdAt" to currentTime
            )
            firestore.collection("properties").document(sanitizedProperty.id).set(propMap)
        } catch (_: Exception) { }

        val list = _properties.value.toMutableList()
        list.removeAll { it.id == sanitizedProperty.id }
        list.add(0, sanitizedProperty)
        _properties.value = list
    }

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

    // --- ADMIN AGENT DIRECTORY & MODERATION ACTIONS ---
    suspend fun fetchAgentsByStatus(status: AgentApprovalStatus): Result<List<User>> = withContext(Dispatchers.IO) {
        val user = currentUser.value ?: return@withContext Result.failure(SecurityException("Admin authentication required."))
        if (user.role != UserRole.ADMIN) return@withContext Result.failure(SecurityException("Admin authorization required."))

        try {
            val firestore = FirebaseFirestore.getInstance()
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("role", UserRole.AGENT.name)
                .whereEqualTo("approvalStatus", status.name)
                .get().await()

            val list = querySnapshot.documents.mapNotNull { doc ->
                val uid = doc.id
                val name = doc.getString("name") ?: ""
                val email = doc.getString("email") ?: ""
                val phone = doc.getString("phone") ?: ""
                val agencyName = doc.getString("agencyName") ?: ""
                val isEmailVerified = doc.getBoolean("isEmailVerified") ?: false
                val memberSince = doc.getString("memberSince") ?: ""
                val reviewedBy = doc.getString("reviewedBy") ?: ""
                val reviewedAt = doc.getString("reviewedAt") ?: ""
                val rejectionReason = doc.getString("rejectionReason") ?: ""

                User(
                    id = uid,
                    name = name,
                    email = email,
                    phone = phone,
                    role = UserRole.AGENT,
                    isEmailVerified = isEmailVerified,
                    approvalStatus = status,
                    reviewedBy = reviewedBy,
                    reviewedAt = reviewedAt,
                    rejectionReason = rejectionReason,
                    agencyName = agencyName,
                    memberSince = memberSince
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception(e.message ?: "Failed to fetch agents by status.", e))
        }
    }

    suspend fun updateAgentApprovalStatus(agentUserId: String, status: AgentApprovalStatus, reason: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
        val user = currentUser.value ?: return@withContext Result.failure(SecurityException("Admin authentication required."))
        if (user.role != UserRole.ADMIN) return@withContext Result.failure(SecurityException("Admin authorization required."))

        try {
            val isApproved = status == AgentApprovalStatus.APPROVED
            val updateData = mapOf(
                "approvalStatus" to status.name,
                "isAgentApproved" to isApproved,
                "reviewedBy" to user.id,
                "reviewedAt" to getCurrentIsoTimestamp(),
                "rejectionReason" to reason
            )
            FirebaseFirestore.getInstance()
                .collection("users").document(agentUserId)
                .update(updateData).await()

            val notifTitle = when (status) {
                AgentApprovalStatus.APPROVED -> "Agent Application Approved"
                AgentApprovalStatus.REJECTED -> "Agent Application Rejected"
                AgentApprovalStatus.SUSPENDED -> "Agent Account Suspended"
                AgentApprovalStatus.PENDING -> "Agent Application Under Review"
            }

            val notifMsg = when (status) {
                AgentApprovalStatus.APPROVED -> "Congratulations! Your agent account has been approved by an administrator."
                AgentApprovalStatus.REJECTED -> "Your agent application was rejected. Reason: ${reason.ifBlank { "Does not meet requirements." }}"
                AgentApprovalStatus.SUSPENDED -> "Your agent account has been suspended. Reason: ${reason.ifBlank { "Policy violation." }}"
                AgentApprovalStatus.PENDING -> "Your application status has been set to Pending Review."
            }

            sendNotification(agentUserId, notifTitle, notifMsg, status.name)

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception(e.message ?: "Failed to update agent status.", e))
        }
    }

    suspend fun approveAgent(agentUserId: String): Result<Unit> {
        return updateAgentApprovalStatus(agentUserId, AgentApprovalStatus.APPROVED)
    }

    suspend fun rejectAgent(agentUserId: String, reason: String): Result<Unit> {
        return updateAgentApprovalStatus(agentUserId, AgentApprovalStatus.REJECTED, reason)
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
    fun observeConversations(currentUserId: String) {
        activeConversationsListener?.remove()
        try {
            val firestore = FirebaseFirestore.getInstance()
            activeConversationsListener = firestore.collection("conversations")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.mapNotNull { docSnapshot ->
                        Conversation(
                            id = docSnapshot.id,
                            propertyId = docSnapshot.getString("propertyId") ?: "",
                            propertyTitle = docSnapshot.getString("propertyTitle") ?: "",
                            propertyPrice = docSnapshot.getString("propertyPrice") ?: "",
                            propertyImage = docSnapshot.getString("propertyImage") ?: "",
                            seekerId = docSnapshot.getString("seekerId") ?: "",
                            seekerName = docSnapshot.getString("seekerName") ?: "",
                            agentId = docSnapshot.getString("agentId") ?: "",
                            agentName = docSnapshot.getString("agentName") ?: "",
                            lastMessage = docSnapshot.getString("lastMessage") ?: "",
                            lastTimestamp = docSnapshot.getString("lastTimestamp") ?: "",
                            participants = (docSnapshot.get("participants") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList()
                        )
                    }
                    _conversations.value = list
                }
        } catch (_: Exception) { }
    }

    suspend fun getOrCreateConversation(property: Property): Result<Conversation> = withContext(Dispatchers.IO) {
        val seeker = currentUser.value ?: return@withContext Result.failure(SecurityException("Authentication required to message agents."))

        if (seeker.id == property.ownerId) {
            return@withContext Result.failure(Exception("You cannot start a conversation on your own property listing."))
        }

        val conversationId = "conv_${seeker.id}_${property.ownerId}_${property.id}"
        try {
            val firestore = FirebaseFirestore.getInstance()
            val docRef = firestore.collection("conversations").document(conversationId)
            val docSnapshot = docRef.get().await()

            val conv = if (docSnapshot.exists()) {
                Conversation(
                    id = docSnapshot.id,
                    propertyId = docSnapshot.getString("propertyId") ?: property.id,
                    propertyTitle = docSnapshot.getString("propertyTitle") ?: property.title,
                    propertyPrice = docSnapshot.getString("propertyPrice") ?: "KSh ${formatPrice(property.price)}",
                    propertyImage = docSnapshot.getString("propertyImage") ?: property.coverImage,
                    seekerId = docSnapshot.getString("seekerId") ?: seeker.id,
                    seekerName = docSnapshot.getString("seekerName") ?: seeker.name,
                    agentId = docSnapshot.getString("agentId") ?: property.ownerId,
                    agentName = docSnapshot.getString("agentName") ?: property.ownerName,
                    lastMessage = docSnapshot.getString("lastMessage") ?: "",
                    lastTimestamp = docSnapshot.getString("lastTimestamp") ?: "",
                    participants = (docSnapshot.get("participants") as? List<*>)?.mapNotNull { it.toString() } ?: listOf(seeker.id, property.ownerId)
                )
            } else {
                val timestamp = getCurrentIsoTimestamp()
                val newConv = Conversation(
                    id = conversationId,
                    propertyId = property.id,
                    propertyTitle = property.title,
                    propertyPrice = "KSh ${formatPrice(property.price)}",
                    propertyImage = property.coverImage,
                    seekerId = seeker.id,
                    seekerName = seeker.name,
                    agentId = property.ownerId,
                    agentName = property.ownerName,
                    lastMessage = "Inquiry regarding ${property.title}",
                    lastTimestamp = SimpleDateFormat("HH:mm", Locale.US).format(Date()),
                    participants = listOf(seeker.id, property.ownerId)
                )

                val convMap = mapOf(
                    "id" to conversationId,
                    "propertyId" to property.id,
                    "propertyTitle" to property.title,
                    "propertyPrice" to "KSh ${formatPrice(property.price)}",
                    "propertyImage" to property.coverImage,
                    "seekerId" to seeker.id,
                    "seekerName" to seeker.name,
                    "agentId" to property.ownerId,
                    "agentName" to property.ownerName,
                    "lastMessage" to newConv.lastMessage,
                    "lastTimestamp" to newConv.lastTimestamp,
                    "participants" to listOf(seeker.id, property.ownerId),
                    "createdAt" to timestamp
                )
                docRef.set(convMap).await()
                newConv
            }

            val currentList = _conversations.value.toMutableList()
            if (currentList.none { it.id == conversationId }) {
                currentList.add(0, conv)
                _conversations.value = currentList
            }

            // Also attach conversation list listener for live delivery
            observeConversations(seeker.id)

            Result.success(conv)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception(e.message ?: "Failed to open conversation.", e))
        }
    }

    fun observeMessages(conversationId: String) {
        activeMessagesListener?.remove()
        try {
            val firestore = FirebaseFirestore.getInstance()
            val query = firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)

            activeMessagesListener = query.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val msgs = snapshot.documents.mapNotNull { doc ->
                    val id = doc.id
                    val senderId = doc.getString("senderId") ?: ""
                    val senderName = doc.getString("senderName") ?: ""
                    val text = doc.getString("text") ?: ""
                    val timestamp = doc.getString("timestamp") ?: ""

                    Message(
                        id = id,
                        conversationId = conversationId,
                        senderId = senderId,
                        senderName = senderName,
                        text = text,
                        timestamp = timestamp
                    )
                }

                val currentMap = _messages.value.toMutableMap()
                currentMap[conversationId] = msgs
                _messages.value = currentMap
            }
        } catch (_: Exception) { }
    }

    suspend fun sendMessageAsync(conversationId: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            return@withContext Result.failure(Exception("Message text cannot be empty."))
        }

        val user = currentUser.value ?: return@withContext Result.failure(SecurityException("Authentication required to send messages."))
        val currentTimeStr = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        val isoTime = getCurrentIsoTimestamp()

        try {
            val firestore = FirebaseFirestore.getInstance()
            val msgId = "msg_${System.currentTimeMillis()}"

            val msgMap = mapOf(
                "id" to msgId,
                "conversationId" to conversationId,
                "senderId" to user.id,
                "senderName" to user.name,
                "text" to cleanText,
                "timestamp" to currentTimeStr,
                "createdAt" to isoTime
            )

            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(msgId)
                .set(msgMap).await()

            firestore.collection("conversations")
                .document(conversationId)
                .update(mapOf(
                    "lastMessage" to cleanText,
                    "lastTimestamp" to currentTimeStr
                )).await()

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception(e.message ?: "Failed to send message.", e))
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
