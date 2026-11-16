package com.example.myapplication

import com.example.myapplication.model.*
import com.example.myapplication.repository.AuthProvider
import com.example.myapplication.repository.AuthRepository
import com.example.myapplication.repository.NestaRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class TestAuthProvider : AuthProvider {
    private val users = mutableMapOf<String, User>()
    private var sessionUser: User? = null

    fun setTestUser(user: User) {
        users[user.email] = user
        sessionUser = user
    }

    override suspend fun login(emailInput: String, passwordInput: String): Result<User> {
        val user = users[emailInput]
        return if (user != null) {
            sessionUser = user
            Result.success(user)
        } else {
            Result.failure(Exception("Invalid credentials"))
        }
    }

    override suspend fun signUp(
        name: String,
        emailInput: String,
        phoneInput: String,
        passwordInput: String,
        requestedRole: UserRole,
        agencyNameInput: String
    ): Result<User> {
        val sanitizedRole = if (requestedRole == UserRole.ADMIN) UserRole.SEEKER else requestedRole
        val user = User(
            id = UUID.randomUUID().toString(),
            name = name,
            email = emailInput,
            phone = phoneInput,
            role = sanitizedRole,
            isEmailVerified = false,
            approvalStatus = AgentApprovalStatus.PENDING,
            agencyName = agencyNameInput,
            memberSince = "2025-01-01T00:00:00Z"
        )
        users[emailInput] = user
        sessionUser = user
        return Result.success(user)
    }

    override suspend fun logout() {
        sessionUser = null
    }

    override suspend fun restoreSession(): User? {
        return sessionUser
    }

    override suspend fun sendPasswordResetEmail(emailInput: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun resendVerificationEmail(): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun checkEmailVerificationStatus(): Result<Boolean> {
        return Result.success(true)
    }
}

class NestaRepositoryTest {

    private val testProvider = TestAuthProvider()

    private val propertyOwnedByAgentA = Property(
        id = "test_prop_agent_a",
        ownerId = "agent_A",
        ownerName = "Agent Alpha",
        ownerPhone = "+254 700 111 222",
        ownerEmail = "alpha@nesta.co.ke",
        ownerAgency = "Alpha Estates",
        isOwnerVerified = true,
        title = "2 Bedroom Executive Apartment in Kilimani",
        description = "Spacious apartment with balcony and gym access.",
        propertyType = PropertyType.APARTMENT,
        listingType = ListingType.FOR_RENT,
        price = 75000.0,
        currency = "KSh",
        billingPeriod = BillingPeriod.MONTHLY,
        bedrooms = 2,
        bathrooms = 2,
        sizeSqm = 110,
        parkingSpaces = 2,
        location = Location(
            county = "Nairobi",
            city = "Nairobi",
            neighborhood = "Kilimani",
            approximateAddress = "Argwings Kodhek Road",
            lat = -1.2921,
            lng = 36.7865
        ),
        amenities = listOf(Amenity.PARKING, Amenity.GYM),
        images = listOf("https://images.unsplash.com/photo-1522708323590-d24dbb6b0267"),
        coverImage = "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267",
        status = ListingStatus.AVAILABLE,
        verificationStatus = VerificationStatus.VERIFIED,
        isFeatured = false,
        savesCount = 10,
        createdAt = "2025-03-10T10:00:00Z"
    )

    @Before
    fun setUp() {
        runBlocking {
            AuthRepository.setProvider(testProvider)
            AuthRepository.logout()
            NestaRepository.resetForTesting(listOf(propertyOwnedByAgentA))
        }
    }

    @Test
    fun `when no provider is configured login and signup fail`() {
        runBlocking {
            AuthRepository.setProvider(null)
            val loginResult = AuthRepository.login("user@test.com", "password")
            assertTrue(loginResult.isFailure)
            assertEquals("Authentication provider is not connected.", loginResult.exceptionOrNull()?.message)

            val signUpResult = AuthRepository.signUp("User", "user2@test.com", "", "pass", UserRole.SEEKER, "")
            assertTrue(signUpResult.isFailure)
            assertEquals("Authentication provider is not connected.", signUpResult.exceptionOrNull()?.message)
        }
    }

    @Test
    fun `natural language search parser extracts location and bedrooms correctly`() {
        val filter = NestaRepository.parseNaturalSearch("2 bedroom apartment in Kilimani under 80k")

        assertEquals(2, filter.bedrooms)
        assertEquals(PropertyType.APARTMENT, filter.propertyType)
        assertEquals("Kilimani", filter.neighborhood)
        assertEquals(80000.0, filter.maxPrice!!, 0.01)
    }

    @Test
    fun `adding a favorite stores property ID and increments saves count`() {
        val propId = propertyOwnedByAgentA.id

        NestaRepository.toggleFavorite(propId)

        assertTrue(NestaRepository.favoritePropertyIds.value.contains(propId))
        val updatedProp = NestaRepository.properties.value.first { it.id == propId }
        assertEquals(11, updatedProp.savesCount)
    }

    @Test
    fun `seekers cannot upload listings`() {
        runBlocking {
            val seekerUser = User(
                id = "seeker_1",
                name = "Test Seeker",
                email = "seeker@test.com",
                phone = "",
                role = UserRole.SEEKER,
                isEmailVerified = true,
                approvalStatus = AgentApprovalStatus.PENDING,
                memberSince = "2025-01-01T00:00:00Z"
            )
            testProvider.setTestUser(seekerUser)
            AuthRepository.restoreSession()

            val draftProp = propertyOwnedByAgentA.copy(id = "draft_1")

            assertThrows(SecurityException::class.java) {
                NestaRepository.addProperty(draftProp)
            }
        }
    }

    @Test
    fun `unapproved agent cannot upload listings`() {
        runBlocking {
            val pendingAgent = User(
                id = "agent_pending",
                name = "Pending Agent",
                email = "agent_pending@test.com",
                phone = "",
                role = UserRole.AGENT,
                isEmailVerified = true,
                approvalStatus = AgentApprovalStatus.PENDING,
                memberSince = "2025-01-01T00:00:00Z"
            )
            testProvider.setTestUser(pendingAgent)
            AuthRepository.restoreSession()

            val draftProp = propertyOwnedByAgentA.copy(id = "draft_2")

            assertThrows(SecurityException::class.java) {
                NestaRepository.addProperty(draftProp)
            }
        }
    }

    @Test
    fun `unverified email agent cannot upload listings`() {
        runBlocking {
            val unverifiedAgent = User(
                id = "agent_unverified",
                name = "Unverified Agent",
                email = "agent_unverified@test.com",
                phone = "",
                role = UserRole.AGENT,
                isEmailVerified = false,
                approvalStatus = AgentApprovalStatus.APPROVED,
                memberSince = "2025-01-01T00:00:00Z"
            )
            testProvider.setTestUser(unverifiedAgent)
            AuthRepository.restoreSession()

            val draftProp = propertyOwnedByAgentA.copy(id = "draft_3")

            assertThrows(SecurityException::class.java) {
                NestaRepository.addProperty(draftProp)
            }
        }
    }

    @Test
    fun `approved and email-verified agent can submit property listing`() {
        runBlocking {
            val approvedAgent = User(
                id = "agent_approved",
                name = "Approved Agent",
                email = "agent_approved@test.com",
                phone = "+254711222333",
                role = UserRole.AGENT,
                isEmailVerified = true,
                approvalStatus = AgentApprovalStatus.APPROVED,
                agencyName = "Prime Properties",
                memberSince = "2025-01-01T00:00:00Z"
            )
            testProvider.setTestUser(approvedAgent)
            AuthRepository.restoreSession()

            val draftProp = propertyOwnedByAgentA.copy(
                id = "draft_4",
                title = "New Approved Listing",
                status = ListingStatus.AVAILABLE, // Attempt pre-approval
                verificationStatus = VerificationStatus.VERIFIED, // Attempt pre-verification
                isFeatured = true // Attempt pre-featured
            )

            NestaRepository.addProperty(draftProp)

            val added = NestaRepository.properties.value.firstOrNull { it.title == "New Approved Listing" }
            assertNotNull(added)
            // Verify forced pending moderation & non-featured security rules
            assertEquals(ListingStatus.PENDING, added?.status)
            assertEquals(VerificationStatus.PENDING, added?.verificationStatus)
            assertFalse(added?.isFeatured ?: true)
        }
    }

    @Test
    fun `approved agent B cannot modify agent A property listing`() {
        runBlocking {
            val agentBUser = User(
                id = "agent_B",
                name = "Agent Beta",
                email = "agent_b@test.com",
                phone = "",
                role = UserRole.AGENT,
                isEmailVerified = true,
                approvalStatus = AgentApprovalStatus.APPROVED,
                memberSince = "2025-01-01T00:00:00Z"
            )
            testProvider.setTestUser(agentBUser)
            AuthRepository.restoreSession()

            val propId = propertyOwnedByAgentA.id

            assertThrows(SecurityException::class.java) {
                NestaRepository.updatePropertyStatus(propId, ListingStatus.RENTED)
            }

            val propertyAfterAttempt = NestaRepository.properties.value.first { it.id == propId }
            assertEquals(ListingStatus.AVAILABLE, propertyAfterAttempt.status)
        }
    }

    @Test
    fun `public registration cannot grant ADMIN access or APPROVED status`() {
        runBlocking {
            val result = AuthRepository.signUp(
                name = "Unauthorized Admin Candidate",
                emailInput = "hacker_admin@nesta.co.ke",
                phoneInput = "+254700000002",
                passwordInput = "pass123",
                requestedRole = UserRole.ADMIN,
                agencyNameInput = ""
            )

            assertTrue(result.isSuccess)
            val createdUser = result.getOrNull()!!

            assertNotEquals(UserRole.ADMIN, createdUser.role)
            assertEquals(UserRole.SEEKER, createdUser.role)
            assertEquals(AgentApprovalStatus.PENDING, createdUser.approvalStatus)
            assertFalse(createdUser.isAgentApproved)
        }
    }
}
