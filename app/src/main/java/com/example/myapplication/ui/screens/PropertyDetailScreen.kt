package com.example.myapplication.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.*
import com.example.myapplication.ui.components.PropertyImagePlaceholder
import com.example.myapplication.ui.components.formatPrice
import com.example.myapplication.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    property: Property,
    isFavorite: Boolean,
    isCompared: Boolean,
    onFavoriteToggle: () -> Unit,
    onCompareToggle: () -> Unit,
    onBackClick: () -> Unit,
    onRequestViewingSubmit: (ViewingRequest) -> Unit,
    onReportSubmit: (Report) -> Unit,
    onStartChat: (Property) -> Unit
) {
    val context = LocalContext.current
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var showViewingModal by remember { mutableStateOf(false) }
    var showReportModal by remember { mutableStateOf(false) }

    // Viewing Request Form State
    var preferredDate by remember { mutableStateOf("2025-03-22") }
    var preferredTime by remember { mutableStateOf("10:00 AM") }
    var requesterName by remember { mutableStateOf("Alex Kamau") }
    var requesterPhone by remember { mutableStateOf("+254 722 111 222") }
    var viewingNotes by remember { mutableStateOf("") }

    // Report Form State
    var selectedReportReason by remember { mutableStateOf("Incorrect Information") }
    var reportDescription by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Scaffold(
        bottomBar = {
            // Sticky Contact & Viewing Trigger Bar
            Surface(
                color = CardBackground,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chat Button
                    OutlinedButton(
                        onClick = { onStartChat(property) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = "Message", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chat", fontWeight = FontWeight.Bold)
                    }

                    // Request Viewing Primary CTA Button
                    Button(
                        onClick = { showViewingModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Request Viewing", fontWeight = FontWeight.Bold, color = WarmOffWhite)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(WarmOffWhite)
                .verticalScroll(scrollState)
        ) {
            // --- TOP IMAGE GALLERY SLIDER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(DeepNavy)
            ) {
                PropertyImagePlaceholder(
                    property = property,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Floating Toolbar (Back, Favorite, Compare, Share)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DarkCharcoal)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Check out this property on Nesta: ${property.title} in ${property.location.neighborhood} for KSh ${formatPrice(property.price)}")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.White.copy(alpha = 0.9f), CircleShape)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = DarkCharcoal)
                        }

                        IconButton(
                            onClick = onCompareToggle,
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.White.copy(alpha = 0.9f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Compare",
                                tint = if (isCompared) MutedTerracotta else DarkCharcoal
                            )
                        }

                        IconButton(
                            onClick = onFavoriteToggle,
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.White.copy(alpha = 0.9f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color.Red else DarkCharcoal
                            )
                        }
                    }
                }

                // Bottom Gallery Counter Pill
                Surface(
                    color = DeepNavy.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "1 / ${maxOf(3, property.images.size)} Photos",
                        color = WarmOffWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // --- PROPERTY HEADLINE & PRICING ---
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${property.currency} ${formatPrice(property.price)} ${if (property.listingType == ListingType.FOR_RENT) property.billingPeriod.label else ""}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepNavy
                        )
                        if (property.estimatedDeposit != null) {
                            Text(
                                text = "Est. Deposit: KSh ${formatPrice(property.estimatedDeposit)} · Service Chg: KSh ${formatPrice(property.estimatedServiceCharge ?: 0.0)}",
                                fontSize = 11.sp,
                                color = MutedText
                            )
                        }
                    }

                    if (property.verificationStatus == VerificationStatus.VERIFIED) {
                        Surface(
                            color = VerifiedGreenBg,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VerifiedGreen.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = VerifiedGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Verified Listing", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VerifiedGreen)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = property.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkCharcoal
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = MutedTerracotta, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${property.location.neighborhood}, ${property.location.city} (${property.location.approximateAddress})",
                        fontSize = 13.sp,
                        color = MutedText
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // KEY SPECS GRID (Beds, Baths, Size, Parking)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground, RoundedCornerShape(12.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    DetailSpecCell(label = "Bedrooms", value = "${property.bedrooms} Beds", icon = Icons.Default.Home)
                    DetailSpecCell(label = "Bathrooms", value = "${property.bathrooms} Baths", icon = Icons.Default.Info)
                    DetailSpecCell(label = "Property Size", value = "${property.sizeSqm} m²", icon = Icons.Default.Place)
                    DetailSpecCell(label = "Parking", value = "${property.parkingSpaces} Slots", icon = Icons.Default.Home)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- DESCRIPTION ---
                Text("About Property", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = property.description,
                    fontSize = 14.sp,
                    color = DarkCharcoal,
                    lineHeight = 22.sp,
                    maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isDescriptionExpanded) "Show Less" else "Read More...",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedTerracotta,
                    modifier = Modifier
                        .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                        .padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- AMENITIES GRID ---
                Text("Property Amenities", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    property.amenities.forEach { amenity ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WarmIvory,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MutedTerracotta, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(amenity.displayName, fontSize = 12.sp, color = DarkCharcoal, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- LOCATION & NEARBY LANDMARKS ---
                Text("Location & Nearby Landmarks", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Approximate Location in ${property.location.neighborhood}. Exact private address shared upon viewing confirmation for security.",
                    fontSize = 12.sp,
                    color = MutedText
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    property.location.nearbyLandmarks.forEach { landmark ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = DeepNavy, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(landmark, fontSize = 13.sp, color = DarkCharcoal)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // --- AGENT / OWNER SECTION ---
                Text("Listed By Agent / Owner", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(DeepNavy, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = WarmOffWhite, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(property.ownerName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                                    if (property.isOwnerVerified) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.Check, contentDescription = "Verified Agent", tint = VerifiedGreen, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(property.ownerAgency.ifBlank { "Independent Listing Agent" }, fontSize = 12.sp, color = MutedText)
                                Text("Member since ${property.createdAt.take(7)} · Response Rate: 98%", fontSize = 11.sp, color = VerifiedGreen, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = CardBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Direct Communication Buttons (Call, WhatsApp, Email)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${property.ownerPhone}"))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val url = "https://api.whatsapp.com/send?phone=${property.ownerPhone}&text=Hi%20${property.ownerName},%20I'm%20interested%20in%20your%20property%20listing:%20${property.title}"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = VerifiedGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "WhatsApp", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", fontSize = 12.sp, color = WarmOffWhite)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Report Listing Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = { showReportModal = true }) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MutedText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Report Suspicious or Inaccurate Listing", fontSize = 12.sp, color = MutedText)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // --- REQUEST VIEWING MODAL SHEET ---
    if (showViewingModal) {
        ModalBottomSheet(
            onDismissRequest = { showViewingModal = false },
            containerColor = WarmOffWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text("Schedule Property Viewing", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                Text(property.title, fontSize = 13.sp, color = MutedText)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = requesterName,
                    onValueChange = { requesterName = it },
                    label = { Text("Your Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = requesterPhone,
                    onValueChange = { requesterPhone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = preferredDate,
                        onValueChange = { preferredDate = it },
                        label = { Text("Preferred Date") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = preferredTime,
                        onValueChange = { preferredTime = it },
                        label = { Text("Time") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = viewingNotes,
                    onValueChange = { viewingNotes = it },
                    label = { Text("Optional Message or Notes for Agent") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onRequestViewingSubmit(
                            ViewingRequest(
                                id = "v_${System.currentTimeMillis()}",
                                propertyId = property.id,
                                propertyTitle = property.title,
                                requesterId = "user_seeker_1",
                                requesterName = requesterName,
                                requesterPhone = requesterPhone,
                                ownerId = property.ownerId,
                                requestedDate = preferredDate,
                                requestedTime = preferredTime,
                                message = viewingNotes,
                                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
                            )
                        )
                        showViewingModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Submit Viewing Request", color = WarmOffWhite, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- REPORT LISTING MODAL ---
    if (showReportModal) {
        AlertDialog(
            onDismissRequest = { showReportModal = false },
            title = { Text("Report Listing to Admin") },
            text = {
                Column {
                    Text("Reason for reporting:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    listOf("Scam / Fraudulent", "Incorrect Price/Details", "Property Unavailable", "Duplicate Listing", "Other").forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReportReason = reason }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = selectedReportReason == reason, onClick = { selectedReportReason = reason })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(reason, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportDescription,
                        onValueChange = { reportDescription = it },
                        placeholder = { Text("Add details for moderators...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReportSubmit(
                            Report(
                                id = "rep_${System.currentTimeMillis()}",
                                propertyId = property.id,
                                propertyTitle = property.title,
                                reporterId = "user_seeker_1",
                                reason = selectedReportReason,
                                description = reportDescription,
                                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
                            )
                        )
                        showReportModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedTerracotta)
                ) {
                    Text("Submit Report", color = WarmOffWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DetailSpecCell(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = DeepNavy, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
        Text(text = label, fontSize = 10.sp, color = MutedText)
    }
}
