package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.User
import com.example.myapplication.model.UserRole
import com.example.myapplication.repository.AuthenticationState
import com.example.myapplication.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthFormState {
    object Idle : AuthFormState()
    object Loading : AuthFormState()
    data class EmailVerificationPending(val user: User) : AuthFormState()
    data class Success(val user: User) : AuthFormState()
    data class Error(val message: String) : AuthFormState()
}

class AuthViewModel : ViewModel() {

    val authState: StateFlow<AuthenticationState> = AuthRepository.authState
    val currentUser: StateFlow<User?> = AuthRepository.currentUser

    private val _formState = MutableStateFlow<AuthFormState>(AuthFormState.Idle)
    val formState: StateFlow<AuthFormState> = _formState.asStateFlow()

    fun resetFormState() {
        _formState.value = AuthFormState.Idle
    }

    fun login(emailInput: String, passwordInput: String) {
        if (_formState.value is AuthFormState.Loading) return
        _formState.value = AuthFormState.Loading

        viewModelScope.launch {
            try {
                val result = AuthRepository.login(emailInput, passwordInput)
                result.fold(
                    onSuccess = { user ->
                        _formState.value = AuthFormState.Success(user)
                    },
                    onFailure = { error ->
                        _formState.value = AuthFormState.Error(error.message ?: "Login failed. Please check your credentials.")
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _formState.value = AuthFormState.Error(e.message ?: "An unexpected error occurred.")
            }
        }
    }

    fun signUp(
        name: String,
        emailInput: String,
        phoneInput: String,
        passwordInput: String,
        requestedRole: UserRole,
        agencyNameInput: String
    ) {
        if (_formState.value is AuthFormState.Loading) return
        _formState.value = AuthFormState.Loading

        viewModelScope.launch {
            try {
                val result = AuthRepository.signUp(
                    name = name,
                    emailInput = emailInput,
                    phoneInput = phoneInput,
                    passwordInput = passwordInput,
                    requestedRole = requestedRole,
                    agencyNameInput = agencyNameInput
                )
                result.fold(
                    onSuccess = { user ->
                        _formState.value = AuthFormState.EmailVerificationPending(user)
                    },
                    onFailure = { error ->
                        _formState.value = AuthFormState.Error(error.message ?: "Registration failed.")
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _formState.value = AuthFormState.Error(e.message ?: "An unexpected error occurred.")
            }
        }
    }

    fun sendPasswordResetEmail(emailInput: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                val res = AuthRepository.sendPasswordResetEmail(emailInput)
                onResult(res)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                onResult(Result.failure(e))
            }
        }
    }

    fun resendVerificationEmail(onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                val res = AuthRepository.resendVerificationEmail()
                onResult(res)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                onResult(Result.failure(e))
            }
        }
    }

    fun checkEmailVerificationStatus(onResult: (isVerified: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            try {
                val res = AuthRepository.checkEmailVerificationStatus()
                res.fold(
                    onSuccess = { isVerified ->
                        if (isVerified) {
                            _formState.value = AuthFormState.Idle
                        }
                        onResult(isVerified, null)
                    },
                    onFailure = { err ->
                        onResult(false, err.message ?: "Verification check failed.")
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                onResult(false, e.message ?: "Verification check failed.")
            }
        }
    }

    fun cancelVerificationAndSignOut() {
        viewModelScope.launch {
            try {
                AuthRepository.logout()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                _formState.value = AuthFormState.Idle
            }
        }
    }
}
