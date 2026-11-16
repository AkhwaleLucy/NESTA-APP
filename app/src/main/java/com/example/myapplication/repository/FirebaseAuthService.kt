package com.example.myapplication.repository

import android.content.Context
import android.util.Log
import com.example.myapplication.model.AgentApprovalStatus
import com.example.myapplication.model.User
import com.example.myapplication.model.UserRole
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class FirebaseAuthService(private val context: Context) : AuthProvider {

    companion object {
        private const val TAG = "NESTA_AUTH"
    }

    private var profileListenerRegistration: ListenerRegistration? = null
    private val emailPattern = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmailFormat(email: String): Boolean {
        return emailPattern.matcher(email.trim()).matches()
    }

    private fun isFirebaseConfigured(): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            false
        }
    }

    private fun getCurrentIsoTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
    }

    fun stopUserProfileListener() {
        profileListenerRegistration?.remove()
        profileListenerRegistration = null
    }

    fun startUserProfileListener(
        uid: String,
        fallbackEmail: String,
        onUserUpdated: (User) -> Unit,
        onError: (String) -> Unit
    ) {
        stopUserProfileListener()
        if (!isFirebaseConfigured()) return

        try {
            val firestore = FirebaseFirestore.getInstance()
            val docRef = firestore.collection("users").document(uid)
            val auth = FirebaseAuth.getInstance()

            profileListenerRegistration = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Profile snapshot listener error: ${error.message}")
                    onError("Failed to observe user profile: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val firebaseUser = auth.currentUser
                    val isEmailVerified = firebaseUser?.isEmailVerified ?: false

                    val roleStr = snapshot.getString("role") ?: UserRole.SEEKER.name
                    val role = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.SEEKER }

                    val isApprovedBool = snapshot.getBoolean("isAgentApproved") == true
                    val approvalStatusStr = snapshot.getString("approvalStatus")
                        ?: if (isApprovedBool) AgentApprovalStatus.APPROVED.name else AgentApprovalStatus.PENDING.name
                    val approvalStatus = try {
                        AgentApprovalStatus.valueOf(approvalStatusStr)
                    } catch (_: Exception) {
                        if (isApprovedBool) AgentApprovalStatus.APPROVED else AgentApprovalStatus.PENDING
                    }

                    val reviewedBy = snapshot.getString("reviewedBy") ?: ""
                    val reviewedAt = snapshot.getString("reviewedAt") ?: ""
                    val rejectionReason = snapshot.getString("rejectionReason") ?: ""

                    val name = snapshot.getString("name") ?: fallbackEmail.substringBefore("@")
                    val email = snapshot.getString("email") ?: fallbackEmail
                    val phone = snapshot.getString("phone") ?: ""
                    val agencyName = snapshot.getString("agencyName") ?: ""
                    val memberSince = snapshot.getString("memberSince") ?: getCurrentIsoTimestamp()

                    val updatedUser = User(
                        id = uid,
                        name = name,
                        email = email,
                        phone = phone,
                        role = role,
                        isEmailVerified = isEmailVerified,
                        approvalStatus = approvalStatus,
                        reviewedBy = reviewedBy,
                        reviewedAt = reviewedAt,
                        rejectionReason = rejectionReason,
                        agencyName = agencyName,
                        memberSince = memberSince
                    )

                    onUserUpdated(updatedUser)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            onError("Failed to initialize user profile listener: ${e.message}")
        }
    }

    override suspend fun login(emailInput: String, passwordInput: String): Result<User> {
        val cleanEmail = emailInput.trim()

        if (cleanEmail.isBlank() || passwordInput.isBlank()) {
            return Result.failure(Exception("Email and password are required."))
        }

        if (!isValidEmailFormat(cleanEmail)) {
            return Result.failure(Exception("Please enter a valid email address format."))
        }

        if (!isFirebaseConfigured()) {
            return Result.failure(Exception("Firebase authentication provider is not connected. Please add google-services.json to connect your Firebase project."))
        }

        val auth = FirebaseAuth.getInstance()

        // Stage 1: Firebase Authentication
        Log.d(TAG, "Stage 1: Attempting signInWithEmailAndPassword for $cleanEmail")
        val authResult = try {
            auth.signInWithEmailAndPassword(cleanEmail, passwordInput).await()
        } catch (authErr: Exception) {
            if (authErr is CancellationException) throw authErr

            val exceptionClass = authErr.javaClass.simpleName
            val errorCode = (authErr as? FirebaseAuthException)?.errorCode ?: "UNKNOWN_AUTH_ERROR"
            Log.e(TAG, "Authentication failed [$exceptionClass / $errorCode]: ${authErr.message}")

            val userFriendlyMessage = when (errorCode) {
                "ERROR_INVALID_EMAIL" -> "The email address format is invalid."
                "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "INVALID_LOGIN_CREDENTIALS" ->
                    "Incorrect email address or password. Please check your credentials and try again."
                "ERROR_USER_NOT_FOUND" ->
                    "No account exists with this email address. Please check the email or click Sign Up to register."
                "ERROR_USER_DISABLED" ->
                    "This account has been disabled by a platform administrator."
                "ERROR_TOO_MANY_REQUESTS" ->
                    "Too many failed login attempts. Please wait a moment before trying again or use 'Forgot Password?' to reset it."
                "ERROR_OPERATION_NOT_ALLOWED" ->
                    "Email/Password sign-in is not enabled in Firebase Console. Please enable Email/Password under Authentication -> Sign-in method."
                else -> authErr.message ?: "Authentication failed. Please check your credentials."
            }
            return Result.failure(Exception(userFriendlyMessage, authErr))
        }

        val firebaseUser = authResult.user ?: return Result.failure(Exception("Authentication succeeded, but user identity is unavailable."))

        try {
            firebaseUser.reload().await()
        } catch (reloadErr: Exception) {
            if (reloadErr is CancellationException) throw reloadErr
            Log.w(TAG, "FirebaseUser reload warning: ${reloadErr.message}")
        }

        Log.d(TAG, "Stage 2: Loading user profile for ${firebaseUser.uid}")
        val user = try {
            loadUserFromFirestore(firebaseUser.uid, firebaseUser.email ?: cleanEmail, firebaseUser.isEmailVerified)
        } catch (profileErr: Exception) {
            if (profileErr is CancellationException) throw profileErr
            Log.e(TAG, "Profile loading warning post-authentication: ${profileErr.message}")
            User(
                id = firebaseUser.uid,
                name = firebaseUser.email?.substringBefore("@") ?: cleanEmail.substringBefore("@"),
                email = firebaseUser.email ?: cleanEmail,
                phone = "",
                role = UserRole.SEEKER,
                isEmailVerified = firebaseUser.isEmailVerified,
                approvalStatus = AgentApprovalStatus.PENDING,
                memberSince = getCurrentIsoTimestamp()
            )
        }

        return Result.success(user)
    }

    override suspend fun signUp(
        name: String,
        emailInput: String,
        phoneInput: String,
        passwordInput: String,
        requestedRole: UserRole,
        agencyNameInput: String
    ): Result<User> {
        val cleanEmail = emailInput.trim()
        val cleanName = name.trim()

        if (cleanName.isBlank() || cleanEmail.isBlank() || passwordInput.isBlank()) {
            return Result.failure(Exception("Full name, email address, and password are required for registration."))
        }

        if (!isValidEmailFormat(cleanEmail)) {
            return Result.failure(Exception("Please enter a valid email address format."))
        }

        if (passwordInput.length < 6) {
            return Result.failure(Exception("Password must be at least 6 characters long."))
        }

        if (!isFirebaseConfigured()) {
            return Result.failure(Exception("Firebase authentication provider is not connected. Please add google-services.json to connect your Firebase project."))
        }

        val sanitizedRole = if (requestedRole == UserRole.ADMIN) UserRole.SEEKER else requestedRole
        val auth = FirebaseAuth.getInstance()

        Log.d(TAG, "Stage 1: Creating Firebase Auth user for $cleanEmail")
        val uid: String
        try {
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, passwordInput).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Registration failed: Account creation failed."))
            uid = firebaseUser.uid
        } catch (e: FirebaseAuthUserCollisionException) {
            Log.w(TAG, "Account collision for $cleanEmail. Attempting recovery sign-in.")
            return try {
                val signInResult = auth.signInWithEmailAndPassword(cleanEmail, passwordInput).await()
                val user = signInResult.user ?: throw Exception("Email already registered.")
                val loadedUser = loadUserFromFirestore(user.uid, cleanEmail, user.isEmailVerified)
                Result.success(loadedUser)
            } catch (recoveryErr: Exception) {
                if (recoveryErr is CancellationException) throw recoveryErr
                Result.failure(Exception("An account with this email address already exists. Please switch to Log In or reset your password."))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val exceptionClass = e.javaClass.simpleName
            val errorCode = (e as? FirebaseAuthException)?.errorCode ?: "UNKNOWN_SIGNUP_ERROR"
            Log.e(TAG, "Signup failed [$exceptionClass / $errorCode]: ${e.message}")

            val userFriendlyMessage = when (errorCode) {
                "ERROR_INVALID_EMAIL" -> "The email address format is invalid."
                "ERROR_WEAK_PASSWORD" -> "The password is too weak. Please use at least 6 characters."
                "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email address already exists. Please switch to Log In."
                "ERROR_OPERATION_NOT_ALLOWED" -> "Email/Password registration is not enabled in Firebase Console. Please enable Email/Password under Authentication -> Sign-in method."
                else -> e.message ?: "Registration failed. An error occurred."
            }
            return Result.failure(Exception(userFriendlyMessage, e))
        }

        Log.d(TAG, "Stage 2: Dispatching verification email to $cleanEmail")
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            try {
                withTimeoutOrNull(6000) {
                    firebaseUser.sendEmailVerification().await()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Stage 2 verification email dispatch warning: ${e.message}")
            }
        }

        Log.d(TAG, "Stage 3: Persisting Firestore user document for $uid")
        val timestamp = getCurrentIsoTimestamp()
        val userMap = mapOf(
            "uid" to uid,
            "name" to cleanName,
            "email" to cleanEmail,
            "phone" to phoneInput.trim(),
            "role" to sanitizedRole.name,
            "isEmailVerified" to false,
            "isAgentApproved" to false,
            "approvalStatus" to AgentApprovalStatus.PENDING.name,
            "reviewedBy" to "",
            "reviewedAt" to "",
            "rejectionReason" to "",
            "agencyName" to if (sanitizedRole == UserRole.AGENT) agencyNameInput.trim() else "",
            "memberSince" to timestamp
        )

        try {
            withTimeoutOrNull(8000) {
                FirebaseFirestore.getInstance().collection("users").document(uid).set(userMap).await()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Stage 3 profile persistence warning: ${e.message}")
        }

        val user = User(
            id = uid,
            name = cleanName,
            email = cleanEmail,
            phone = phoneInput.trim(),
            role = sanitizedRole,
            isEmailVerified = false,
            approvalStatus = AgentApprovalStatus.PENDING,
            reviewedBy = "",
            reviewedAt = "",
            rejectionReason = "",
            agencyName = if (sanitizedRole == UserRole.AGENT) agencyNameInput.trim() else "",
            memberSince = timestamp
        )

        return Result.success(user)
    }

    override suspend fun logout() {
        stopUserProfileListener()
        if (isFirebaseConfigured()) {
            try {
                FirebaseAuth.getInstance().signOut()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    override suspend fun restoreSession(): User? {
        if (!isFirebaseConfigured()) return null

        return try {
            val auth = FirebaseAuth.getInstance()
            val firebaseUser = auth.currentUser ?: return null
            try {
                firebaseUser.reload().await()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }

            loadUserFromFirestore(firebaseUser.uid, firebaseUser.email ?: "", firebaseUser.isEmailVerified)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }

    override suspend fun sendPasswordResetEmail(emailInput: String): Result<Unit> {
        val cleanEmail = emailInput.trim()
        if (cleanEmail.isBlank() || !isValidEmailFormat(cleanEmail)) {
            return Result.failure(Exception("Please provide a valid email address for password reset."))
        }

        if (!isFirebaseConfigured()) {
            return Result.failure(Exception("Firebase authentication provider is not connected."))
        }

        return try {
            FirebaseAuth.getInstance().sendPasswordResetEmail(cleanEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val exceptionClass = e.javaClass.simpleName
            val errorCode = (e as? FirebaseAuthException)?.errorCode ?: "UNKNOWN_RESET_ERROR"
            Log.e(TAG, "Password reset error [$exceptionClass / $errorCode]: ${e.message}")

            val userFriendlyMessage = when (errorCode) {
                "ERROR_INVALID_EMAIL" -> "The email address format is invalid."
                "ERROR_USER_NOT_FOUND" -> "No account exists with this email address. Please check the email address."
                "ERROR_TOO_MANY_REQUESTS" -> "Too many password reset requests. Please wait a moment before trying again."
                else -> e.message ?: "Failed to send password reset email."
            }
            Result.failure(Exception(userFriendlyMessage, e))
        }
    }

    override suspend fun resendVerificationEmail(): Result<Unit> {
        if (!isFirebaseConfigured()) {
            return Result.failure(Exception("Firebase authentication provider is not connected."))
        }

        return try {
            val auth = FirebaseAuth.getInstance()
            val firebaseUser = auth.currentUser ?: return Result.failure(Exception("No active user session to resend verification email."))
            firebaseUser.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val exceptionClass = e.javaClass.simpleName
            val errorCode = (e as? FirebaseAuthException)?.errorCode ?: "UNKNOWN_RESEND_ERROR"
            Log.e(TAG, "Resend verification email error [$exceptionClass / $errorCode]: ${e.message}")

            val msg = if (errorCode == "ERROR_TOO_MANY_REQUESTS" || e.message?.contains("TOO_MANY_REQUESTS", ignoreCase = true) == true) {
                "Too many verification requests. Please wait a moment before trying again."
            } else {
                e.message ?: "Failed to resend verification email."
            }
            Result.failure(Exception(msg, e))
        }
    }

    override suspend fun checkEmailVerificationStatus(): Result<Boolean> {
        if (!isFirebaseConfigured()) {
            return Result.failure(Exception("Firebase authentication provider is not connected."))
        }

        return try {
            val auth = FirebaseAuth.getInstance()
            val firebaseUser = auth.currentUser ?: return Result.failure(Exception("No active user session to check verification."))
            firebaseUser.reload().await()
            val isVerified = firebaseUser.isEmailVerified
            if (isVerified) {
                try {
                    firebaseUser.getIdToken(true).await()
                    FirebaseFirestore.getInstance().collection("users").document(firebaseUser.uid)
                        .update("isEmailVerified", true).await()
                } catch (_: Exception) { }
            }
            Result.success(isVerified)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception(e.message ?: "Network error while checking email verification status.", e))
        }
    }

    private suspend fun loadUserFromFirestore(uid: String, fallbackEmail: String, isEmailVerified: Boolean): User {
        val timestamp = getCurrentIsoTimestamp()
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val docRef = firestore.collection("users").document(uid)
            val docSnapshot = docRef.get().await()

            if (docSnapshot.exists()) {
                val roleStr = docSnapshot.getString("role") ?: UserRole.SEEKER.name
                val role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.SEEKER }

                val isApprovedBool = docSnapshot.getBoolean("isAgentApproved") == true
                val approvalStatusStr = docSnapshot.getString("approvalStatus")
                    ?: if (isApprovedBool) AgentApprovalStatus.APPROVED.name else AgentApprovalStatus.PENDING.name
                val approvalStatus = try {
                    AgentApprovalStatus.valueOf(approvalStatusStr)
                } catch (e: Exception) {
                    if (isApprovedBool) AgentApprovalStatus.APPROVED else AgentApprovalStatus.PENDING
                }

                val reviewedBy = docSnapshot.getString("reviewedBy") ?: ""
                val reviewedAt = docSnapshot.getString("reviewedAt") ?: ""
                val rejectionReason = docSnapshot.getString("rejectionReason") ?: ""

                val name = docSnapshot.getString("name") ?: fallbackEmail.substringBefore("@")
                val email = docSnapshot.getString("email") ?: fallbackEmail
                val phone = docSnapshot.getString("phone") ?: ""
                val agencyName = docSnapshot.getString("agencyName") ?: ""
                val memberSince = docSnapshot.getString("memberSince") ?: timestamp

                User(
                    id = uid,
                    name = name,
                    email = email,
                    phone = phone,
                    role = role,
                    isEmailVerified = isEmailVerified,
                    approvalStatus = approvalStatus,
                    reviewedBy = reviewedBy,
                    reviewedAt = reviewedAt,
                    rejectionReason = rejectionReason,
                    agencyName = agencyName,
                    memberSince = memberSince
                )
            } else {
                val name = fallbackEmail.substringBefore("@")
                val userMap = mapOf(
                    "uid" to uid,
                    "name" to name,
                    "email" to fallbackEmail,
                    "phone" to "",
                    "role" to UserRole.SEEKER.name,
                    "isEmailVerified" to isEmailVerified,
                    "isAgentApproved" to false,
                    "approvalStatus" to AgentApprovalStatus.PENDING.name,
                    "reviewedBy" to "",
                    "reviewedAt" to "",
                    "rejectionReason" to "",
                    "agencyName" to "",
                    "memberSince" to timestamp
                )
                try {
                    docRef.set(userMap).await()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }

                User(
                    id = uid,
                    name = name,
                    email = fallbackEmail,
                    phone = "",
                    role = UserRole.SEEKER,
                    isEmailVerified = isEmailVerified,
                    approvalStatus = AgentApprovalStatus.PENDING,
                    reviewedBy = "",
                    reviewedAt = "",
                    rejectionReason = "",
                    agencyName = "",
                    memberSince = timestamp
                )
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            User(
                id = uid,
                name = fallbackEmail.substringBefore("@"),
                email = fallbackEmail,
                phone = "",
                role = UserRole.SEEKER,
                isEmailVerified = isEmailVerified,
                approvalStatus = AgentApprovalStatus.PENDING,
                reviewedBy = "",
                reviewedAt = "",
                rejectionReason = "",
                agencyName = "",
                memberSince = timestamp
            )
        }
    }
}
