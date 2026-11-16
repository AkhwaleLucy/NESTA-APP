package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.myapplication.R
import com.example.myapplication.model.User
import com.example.myapplication.ui.theme.*
import com.example.myapplication.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmailVerificationScreen(
    user: User,
    authViewModel: AuthViewModel,
    onVerifiedAndSignedOut: (message: String) -> Unit,
    onBackToSignIn: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var cooldownSeconds by remember { mutableStateOf(0) }
    var statusMessage by remember { mutableStateOf<String?>("Verification email requested. Check your inbox and spam folder.") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun startCooldown() {
        cooldownSeconds = 30
        coroutineScope.launch {
            while (cooldownSeconds > 0) {
                delay(1000L)
                cooldownSeconds--
            }
        }
    }

    fun verifyEmailStatus() {
        if (isChecking) return
        isChecking = true
        errorMessage = null

        authViewModel.checkEmailVerificationStatus { isVerified, err ->
            isChecking = false
            if (isVerified) {
                onVerifiedAndSignedOut("Email verified. Please sign in.")
            } else if (err != null) {
                errorMessage = err
            } else {
                statusMessage = "Email is not verified yet. Please check your inbox and click the verification link."
            }
        }
    }

    // Auto-check when user returns to app foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                verifyEmailStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
        Spacer(modifier = Modifier.height(24.dp))

        // Brand Logo Header
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

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(WarmIvory, RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = null,
                        tint = DeepNavy,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Verify Your Email Address",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkCharcoal,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Verification email requested for:",
                    fontSize = 13.sp,
                    color = MutedText,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = user.email,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedTerracotta,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Please check your email inbox and spam folder. Click the verification link, then tap below to confirm.",
                    fontSize = 12.sp,
                    color = DarkCharcoal,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = WarmIvory,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusMessage!!,
                            color = DarkCharcoal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = RejectedRedBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = RejectedRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { verifyEmailStatus() }) {
                                Text("Retry Verification Check", color = RejectedRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Button 1: I Have Verified My Email
                Button(
                    onClick = { verifyEmailStatus() },
                    enabled = !isChecking && !isResending,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isChecking) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = WarmOffWhite, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Checking status...", color = WarmOffWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        Text("I Have Verified My Email", color = WarmOffWhite, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Button 2: Resend Email
                OutlinedButton(
                    onClick = {
                        isResending = true
                        statusMessage = null
                        errorMessage = null
                        authViewModel.resendVerificationEmail { res ->
                            isResending = false
                            res.fold(
                                onSuccess = {
                                    statusMessage = "Verification email requested. Check your inbox and spam folder."
                                    startCooldown()
                                },
                                onFailure = { err ->
                                    errorMessage = err.message ?: "Failed to resend verification email."
                                }
                            )
                        }
                    },
                    enabled = !isChecking && !isResending && cooldownSeconds == 0,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    if (isResending) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = DeepNavy, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Requesting email...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else if (cooldownSeconds > 0) {
                        Text("Resend Email (${cooldownSeconds}s)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MutedText)
                    } else {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resend Verification Email", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Button 3: Back to Sign In
                TextButton(
                    enabled = !isChecking && !isResending,
                    onClick = {
                        authViewModel.cancelVerificationAndSignOut()
                        onBackToSignIn()
                    }
                ) {
                    Text("Back to Sign In", color = MutedText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
