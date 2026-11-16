package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.core.BrandingConfig
import com.example.myapplication.model.*
import com.example.myapplication.ui.components.PropertyCard
import com.example.myapplication.ui.theme.*

@Composable
fun ProfileScreen(
    currentUser: User,
    recentlyViewedProperties: List<Property>,
    propertyAlerts: List<PropertyAlert>,
    favoriteIds: Set<String>,
    compareIds: Set<String>,
    onAddAlertSubmit: (PropertyAlert) -> Unit,
    onPropertyClick: (Property) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onLogoutClick: () -> Unit
) {
    var showCreateAlertModal by remember { mutableStateOf(false) }
    var alertLocation by remember { mutableStateOf("Kilimani") }
    var alertMaxPriceText by remember { mutableStateOf("80000") }
    var alertBedroomsText by remember { mutableStateOf("2") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // User Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(DeepNavy, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = WarmOffWhite, modifier = Modifier.size(32.dp))
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(currentUser.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                            if (currentUser.isVerified) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.Check, contentDescription = "Verified", tint = VerifiedGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(currentUser.email, fontSize = 12.sp, color = MutedText)
                        Text(currentUser.phone, fontSize = 12.sp, color = MutedText)
                        if (currentUser.agencyName.isNotBlank()) {
                            Text(currentUser.agencyName, fontSize = 12.sp, color = MutedTerracotta, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = CardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Authenticated Account Role Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Account Type", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                    Surface(
                        color = when (currentUser.role) {
                            UserRole.SEEKER -> WarmIvory
                            UserRole.AGENT -> MutedTerracotta.copy(alpha = 0.15f)
                            UserRole.ADMIN -> DeepNavy
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = when (currentUser.role) {
                                UserRole.SEEKER -> "Property Seeker Account"
                                UserRole.AGENT -> "Property Owner / Agent Account"
                                UserRole.ADMIN -> "Administrator Account"
                            },
                            color = when (currentUser.role) {
                                UserRole.ADMIN -> WarmOffWhite
                                UserRole.AGENT -> MutedTerracotta
                                else -> DarkCharcoal
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Logout Button
                OutlinedButton(
                    onClick = onLogoutClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Log Out", color = RejectedRed, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- PROPERTY ALERTS SECTION ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Property Search Alerts", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                Text("Get notified when matching listings open in Nairobi", fontSize = 11.sp, color = MutedText)
            }

            IconButton(onClick = { showCreateAlertModal = true }) {
                Icon(Icons.Default.Add, contentDescription = "New Alert", tint = DeepNavy)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (propertyAlerts.isEmpty()) {
            Text("No active property alerts.", fontSize = 12.sp, color = MutedText)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                propertyAlerts.forEach { alert ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MutedTerracotta, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${alert.bedrooms} Bed ${alert.propertyType.displayName} in ${alert.location}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkCharcoal)
                                Text("Max Price: KSh ${alert.maxPrice.toInt()} · ${alert.frequency}", fontSize = 11.sp, color = MutedText)
                            }
                            Switch(checked = alert.isActive, onCheckedChange = {})
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // --- RECENTLY VIEWED PROPERTIES ---
        if (recentlyViewedProperties.isNotEmpty()) {
            Text("Recently Viewed Properties", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(recentlyViewedProperties) { prop ->
                    Box(modifier = Modifier.width(280.dp)) {
                        PropertyCard(
                            property = prop,
                            isFavorite = favoriteIds.contains(prop.id),
                            isCompared = compareIds.contains(prop.id),
                            onFavoriteToggle = { onFavoriteToggle(prop.id) },
                            onClick = { onPropertyClick(prop) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // --- ABOUT NESTA BRAND ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = WarmIvory)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.brand_name), fontWeight = FontWeight.Black, fontSize = 16.sp, color = DeepNavy)
                Text(stringResource(R.string.brand_tagline), fontSize = 12.sp, color = DarkCharcoal)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Support: ${BrandingConfig.supportEmail} · Version ${BrandingConfig.appVersion}", fontSize = 11.sp, color = MutedText)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    // CREATE ALERT MODAL
    if (showCreateAlertModal) {
        AlertDialog(
            onDismissRequest = { showCreateAlertModal = false },
            title = { Text("Create Property Alert") },
            text = {
                Column {
                    OutlinedTextField(
                        value = alertLocation,
                        onValueChange = { alertLocation = it },
                        label = { Text("Location (e.g. Kilimani)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = alertMaxPriceText,
                        onValueChange = { alertMaxPriceText = it },
                        label = { Text("Max Rent / Price (KSh)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = alertBedroomsText,
                        onValueChange = { alertBedroomsText = it },
                        label = { Text("Bedrooms") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddAlertSubmit(
                            PropertyAlert(
                                id = "pa_${System.currentTimeMillis()}",
                                userId = currentUser.id,
                                location = alertLocation,
                                propertyType = PropertyType.APARTMENT,
                                minPrice = 0.0,
                                maxPrice = alertMaxPriceText.toDoubleOrNull() ?: 80000.0,
                                bedrooms = alertBedroomsText.toIntOrNull() ?: 2
                            )
                        )
                        showCreateAlertModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavy)
                ) {
                    Text("Save Alert", color = WarmOffWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAlertModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
