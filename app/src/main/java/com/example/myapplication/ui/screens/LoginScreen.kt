package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.model.User
import com.example.myapplication.model.UserRole
import com.example.myapplication.repository.AuthenticationState
import com.example.myapplication.ui.theme.*
import com.example.myapplication.ui.viewmodel.AuthFormState
import com.example.myapplication.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoginSuccess: (User) -> Unit = {}
) {
    val formState by authViewModel.formState.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    var isSignUp by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.SEEKER) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var agencyName by remember { mutableStateOf("") }

    var resetInfoMessage by remember { mutableStateOf<String?>(null) }
    var localValidationMessage by remember { mutableStateOf<String?>(null) }

    val pendingUser = (formState as? AuthFormState.EmailVerificationPending)?.user
        ?: (authState as? AuthenticationState.EmailVerificationPending)?.user

    if (pendingUser != null) {
        EmailVerificationScreen(
            user = pendingUser,
            authViewModel = authViewModel,
            onVerifiedAndSignedOut = { msg ->
                password = ""
                resetInfoMessage = msg
                isSignUp = false
                localValidationMessage = null
            },
            onBackToSignIn = {
                password = ""
                isSignUp = false
                localValidationMessage = null
            }
        )
    } else {
        val isLoading = formState is AuthFormState.Loading
        val activeErrorMessage = localValidationMessage ?: when (val fs = formState) {
            is AuthFormState.Error -> fs.message
            else -> (authState as? AuthenticationState.Error)?.message
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmOffWhite)
                .statusBarsPadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Brand Logo & Tagline
            Text(
                text = stringResource(R.string.brand_name),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = DeepNavy,
                letterSpacing = 2.sp
            )
            Text(
                text = stringResource(R.string.brand_tagline),
                fontSize = 13.sp,
                color = MutedText,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Mode Toggle Bar (Log In vs Sign Up)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WarmIvory, RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (!isSignUp) DeepNavy else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable(enabled = !isLoading) {
                                    isSignUp = false
                                    localValidationMessage = null
                                    authViewModel.resetFormState()
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Log In", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (!isSignUp) WarmOffWhite else DarkCharcoal)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSignUp) DeepNavy else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable(enabled = !isLoading) {
                                    isSignUp = true
                                    localValidationMessage = null
                                    authViewModel.resetFormState()
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Sign Up", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSignUp) WarmOffWhite else DarkCharcoal)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isSignUp) {
                        // Account Type Selector
                        Text("Account Type", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                Pair("Property Seeker", UserRole.SEEKER),
                                Pair("Agent / Owner", UserRole.AGENT)
                            ).forEach { (label, role) ->
                                val isSel = selectedRole == role
                                Surface(
                                    onClick = { if (!isLoading) selectedRole = role },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) WarmIvory else CardBackground,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) MutedTerracotta else CardBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                                        Text(label, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, color = DarkCharcoal)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MutedText) },
                            singleLine = true,
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MutedText) },
                            singleLine = true,
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (selectedRole == UserRole.AGENT) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = agencyName,
                                onValueChange = { agencyName = it },
                                label = { Text("Agency / Business Name") },
                                singleLine = true,
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address *") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MutedText) },
                        singleLine = true,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password *") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MutedText) },
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (!isSignUp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                enabled = !isLoading,
                                onClick = {
                                    if (email.isBlank()) {
                                        localValidationMessage = "Please enter your email address above to send a password reset link."
                                    } else {
                                        localValidationMessage = null
                                        authViewModel.sendPasswordResetEmail(email) { res ->
                                            res.fold(
                                                onSuccess = { resetInfoMessage = "Password reset email sent to $email. Please check your inbox." },
                                                onFailure = { err -> localValidationMessage = err.message ?: "Password reset failed." }
                                            )
                                        }
                                    }
                                }
                            ) {
                                Text("Forgot Password?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MutedTerracotta)
                            }
                        }
                    }

                    resetInfoMessage?.let { msg ->
                        Surface(
                            color = VerifiedGreenBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Text(msg, color = VerifiedGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))
                        }
                    }

                    if (activeErrorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = RejectedRedBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = activeErrorMessage,
                                color = RejectedRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            localValidationMessage = null
                            resetInfoMessage = null
                            if (isSignUp) {
                                authViewModel.signUp(
                                    name = name,
                                    emailInput = email,
                                    phoneInput = phone,
                                    passwordInput = password,
                                    requestedRole = selectedRole,
                                    agencyNameInput = agencyName
                                )
                            } else {
                                authViewModel.login(
                                    emailInput = email,
                                    passwordInput = password
                                )
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = WarmOffWhite, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSignUp) "Creating account…" else "Signing in…",
                                    color = WarmOffWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            Text(if (isSignUp) "Create Account" else "Log In to Nesta", color = WarmOffWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
