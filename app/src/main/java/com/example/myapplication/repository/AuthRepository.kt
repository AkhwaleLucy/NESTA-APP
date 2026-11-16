package com.example.myapplication.repository

import android.content.Context
import com.example.myapplication.model.User
import com.example.myapplication.model.UserRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

sealed class AuthenticationState {
    object Unauthenticated : AuthenticationState()
    object Loading : AuthenticationState()
    data class EmailVerificationPending(val user: User) : AuthenticationState()
    data class Authenticated(val user: User) : AuthenticationState()
    data class Error(val message: String) : AuthenticationState()
}

interface AuthProvider {
    suspend fun login(emailInput: String, passwordInput: String): Result<User>
    suspend fun signUp(
        name: String,
        emailInput: String,
        phoneInput: String,
        passwordInput: String,
        requestedRole: UserRole,
        agencyNameInput: String
    ): Result<User>
    suspend fun logout()
    suspend fun restoreSession(): User?
    suspend fun sendPasswordResetEmail(emailInput: String): Result<Unit>
    suspend fun resendVerificationEmail(): Result<Unit>
    suspend fun checkEmailVerificationStatus(): Result<Boolean>
}

object AuthRepository : AuthProvider {

    private val _authState = MutableStateFlow<AuthenticationState>(AuthenticationState.Loading)
    val authState: StateFlow<AuthenticationState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private var activeProvider: AuthProvider? = null
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isInitialized = AtomicBoolean(false)

    private fun attachProfileListener(user: User) {
        val provider = activeProvider
        if (provider is FirebaseAuthService) {
            provider.startUserProfileListener(
                uid = user.id,
                fallbackEmail = user.email,
                onUserUpdated = { updatedUser ->
                    _currentUser.value = updatedUser
                    if (updatedUser.isEmailVerified) {
                        _authState.value = AuthenticationState.Authenticated(updatedUser)
                    } else {
                        _authState.value = AuthenticationState.EmailVerificationPending(updatedUser)
                    }
                },
                onError = { err ->
                    _authState.value = AuthenticationState.Error(err)
                }
            )
        }
    }

    fun setProvider(provider: AuthProvider?) {
        (activeProvider as? FirebaseAuthService)?.stopUserProfileListener()
        activeProvider = provider
        _currentUser.value = null
        _authState.value = AuthenticationState.Unauthenticated
    }

    fun initialize(context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            val firebaseService = FirebaseAuthService(context.applicationContext)
            activeProvider = firebaseService
            repositoryScope.launch {
                restoreSession()
            }
        }
    }

    override suspend fun restoreSession(): User? = withContext(Dispatchers.IO) {
        _authState.value = AuthenticationState.Loading
        try {
            val provider = activeProvider
            if (provider != null) {
                val restoredUser = provider.restoreSession()
                if (restoredUser != null) {
                    _currentUser.value = restoredUser
                    attachProfileListener(restoredUser)
                    if (restoredUser.isEmailVerified) {
                        _authState.value = AuthenticationState.Authenticated(restoredUser)
                    } else {
                        _authState.value = AuthenticationState.EmailVerificationPending(restoredUser)
                    }
                    restoredUser
                } else {
                    _currentUser.value = null
                    _authState.value = AuthenticationState.Unauthenticated
                    null
                }
            } else {
                _currentUser.value = null
                _authState.value = AuthenticationState.Unauthenticated
                null
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _currentUser.value = null
            _authState.value = AuthenticationState.Error(e.message ?: "Session restoration failed.")
            null
        }
    }

    override suspend fun login(emailInput: String, passwordInput: String): Result<User> = withContext(Dispatchers.IO) {
        val cleanEmail = emailInput.trim().lowercase()

        if (cleanEmail.isBlank() || passwordInput.isBlank()) {
            val err = "Email and password are required."
            return@withContext Result.failure(Exception(err))
        }

        val provider = activeProvider
        if (provider == null) {
            val err = "Authentication provider is not connected."
            return@withContext Result.failure(Exception(err))
        }

        try {
            val result = provider.login(cleanEmail, passwordInput)
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    attachProfileListener(user)
                    if (!user.isEmailVerified) {
                        _authState.value = AuthenticationState.EmailVerificationPending(user)
                        Result.failure(Exception("Your email address is not verified yet. Please check your inbox or click 'Resend Email'."))
                    } else {
                        _authState.value = AuthenticationState.Authenticated(user)
                        Result.success(user)
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val err = e.message ?: "An unexpected error occurred during login."
            Result.failure(Exception(err, e))
        }
    }

    override suspend fun signUp(
        name: String,
        emailInput: String,
        phoneInput: String,
        passwordInput: String,
        requestedRole: UserRole,
        agencyNameInput: String
    ): Result<User> = withContext(Dispatchers.IO) {
        val cleanEmail = emailInput.trim().lowercase()
        val cleanName = name.trim()

        if (cleanEmail.isBlank() || passwordInput.isBlank() || cleanName.isBlank()) {
            val err = "Name, email, and password are required for registration."
            return@withContext Result.failure(Exception(err))
        }

        val provider = activeProvider
        if (provider == null) {
            val err = "Authentication provider is not connected."
            return@withContext Result.failure(Exception(err))
        }

        val sanitizedRole = if (requestedRole == UserRole.ADMIN) UserRole.SEEKER else requestedRole

        try {
            val result = provider.signUp(
                name = cleanName,
                emailInput = cleanEmail,
                phoneInput = phoneInput.trim(),
                passwordInput = passwordInput,
                requestedRole = sanitizedRole,
                agencyNameInput = if (sanitizedRole == UserRole.AGENT) agencyNameInput.trim() else ""
            )
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    attachProfileListener(user)
                    _authState.value = AuthenticationState.EmailVerificationPending(user)
                    Result.success(user)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val err = e.message ?: "An unexpected error occurred during sign up."
            Result.failure(Exception(err, e))
        }
    }

    override suspend fun logout(): Unit = withContext(Dispatchers.IO) {
        try {
            (activeProvider as? FirebaseAuthService)?.stopUserProfileListener()
            activeProvider?.logout()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        } finally {
            NestaRepository.clearUserData()
            _currentUser.value = null
            _authState.value = AuthenticationState.Unauthenticated
        }
    }

    override suspend fun sendPasswordResetEmail(emailInput: String): Result<Unit> = withContext(Dispatchers.IO) {
        val provider = activeProvider ?: return@withContext Result.failure(Exception("Authentication provider is not connected."))
        try {
            provider.sendPasswordResetEmail(emailInput)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception(e.message ?: "Password reset failed.", e))
        }
    }

    override suspend fun resendVerificationEmail(): Result<Unit> = withContext(Dispatchers.IO) {
        val provider = activeProvider ?: return@withContext Result.failure(Exception("Authentication provider is not connected."))
        try {
            provider.resendVerificationEmail()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception(e.message ?: "Failed to resend verification email.", e))
        }
    }

    override suspend fun checkEmailVerificationStatus(): Result<Boolean> = withContext(Dispatchers.IO) {
        val provider = activeProvider ?: return@withContext Result.failure(Exception("Authentication provider is not connected."))
        try {
            val result = provider.checkEmailVerificationStatus()
            result.fold(
                onSuccess = { isVerified ->
                    if (isVerified) {
                        logout()
                        Result.success(true)
                    } else {
                        Result.success(false)
                    }
                },
                onFailure = { err ->
                    Result.failure(err)
                }
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception(e.message ?: "Failed to check email verification status.", e))
        }
    }
}
