package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.core.BrandingConfig
import com.example.myapplication.model.*
import com.example.myapplication.repository.NestaRepository
import com.example.myapplication.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ListPropertyWizardScreen(
    currentAgent: User,
    onBackClick: () -> Unit,
    onSubmitProperty: (Property) -> Unit
) {
    if (currentAgent.role == UserRole.AGENT && !currentAgent.isAgentApproved) {
        Scaffold(
            topBar = {
                Surface(color = WarmOffWhite, shadowElevation = 1.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Text("New Property Listing", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PendingOrangeBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PendingOrange)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Agent Approval Pending", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PendingOrange)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Your agent account is currently pending administrator approval. Both verified email and admin approval are required before creating or publishing property listings.",
                            fontSize = 13.sp,
                            color = DarkCharcoal,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onBackClick,
                            colors = ButtonDefaults.buttonColors(containerColor = DeepNavy)
                        ) {
                            Text("Back to Agent Portal", color = WarmOffWhite)
                        }
                    }
                }
            }
        }
        return
    }

    var currentStep by remember { mutableStateOf(1) }

    // Wizard Form Fields
    var listingType by remember { mutableStateOf(ListingType.FOR_RENT) }
    var propertyType by remember { mutableStateOf(PropertyType.APARTMENT) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var billingPeriod by remember { mutableStateOf(BillingPeriod.MONTHLY) }
    var bedrooms by remember { mutableStateOf("2") }
    var bathrooms by remember { mutableStateOf("2") }
    var sizeSqmText by remember { mutableStateOf("") }
    var parkingText by remember { mutableStateOf("1") }
    var selectedNeighborhood by remember { mutableStateOf("Kilimani") }
    var approximateAddress by remember { mutableStateOf("") }
    var selectedAmenities by remember { mutableStateOf(setOf<Amenity>()) }
    val propertyImages = remember { mutableStateListOf("https://images.unsplash.com/photo-1522708323590-d24dbb6b0267") }
    val propertyImagePublicIds = remember { mutableStateListOf<String>() }

    val actualTime = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()) }

    // Create Temporary Property for Quality Score
    val tempProp = Property(
        id = "temp",
        ownerId = currentAgent.id,
        ownerName = currentAgent.name,
        ownerPhone = currentAgent.phone,
        ownerEmail = currentAgent.email,
        ownerAgency = currentAgent.agencyName,
        isOwnerVerified = currentAgent.isVerified,
        title = title,
        description = description,
        propertyType = propertyType,
        listingType = listingType,
        price = priceText.toDoubleOrNull() ?: 0.0,
        billingPeriod = billingPeriod,
        bedrooms = bedrooms.toIntOrNull() ?: 0,
        bathrooms = bathrooms.toIntOrNull() ?: 0,
        sizeSqm = sizeSqmText.toIntOrNull() ?: 0,
        parkingSpaces = parkingText.toIntOrNull() ?: 0,
        location = Location(
            county = "Nairobi",
            city = "Nairobi",
            neighborhood = selectedNeighborhood,
            approximateAddress = approximateAddress,
            lat = -1.2921,
            lng = 36.7865
        ),
        amenities = selectedAmenities.toList(),
        images = propertyImages.toList(),
        imagePublicIds = propertyImagePublicIds.toList(),
        coverImage = propertyImages.firstOrNull() ?: "",
        status = ListingStatus.PENDING,
        verificationStatus = VerificationStatus.PENDING,
        createdAt = actualTime
    )

    val qualityScore = NestaRepository.calculateQualityScore(tempProp)

    Scaffold(
        topBar = {
            Surface(color = WarmOffWhite, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("List Your Property", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                        Text("Step $currentStep of 5 · Completion Score: $qualityScore%", fontSize = 11.sp, color = MutedTerracotta)
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { currentStep / 5f },
                color = MutedTerracotta,
                trackColor = CardBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            when (currentStep) {
                1 -> {
                    // STEP 1: Listing Type & Category
                    Text("Step 1: Listing Type & Property Category", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Transaction Type", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ListingType.entries.forEach { type ->
                            Surface(
                                onClick = { listingType = type },
                                shape = RoundedCornerShape(10.dp),
                                color = if (listingType == type) DeepNavy else CardBackground,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(modifier = Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                                    Text(type.displayName, color = if (listingType == type) WarmOffWhite else DarkCharcoal, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Property Category", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val propertyTypeChunks = PropertyType.entries.toList().chunked(2)
                        propertyTypeChunks.forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                pair.forEach { cat ->
                                    Surface(
                                        onClick = { propertyType = cat },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (propertyType == cat) WarmIvory else CardBackground,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (propertyType == cat) MutedTerracotta else CardBorder),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                            Text(cat.displayName, color = DarkCharcoal, fontSize = 13.sp, fontWeight = if (propertyType == cat) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // STEP 2: Basic Info
                    Text("Step 2: Property Title & Specs", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Property Title *") },
                        placeholder = { Text("e.g. Executive 3 Bedroom Apartment with Balcony") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Detailed Description *") },
                        placeholder = { Text("Describe living area, kitchen, security, proximity to amenities...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = bedrooms,
                            onValueChange = { bedrooms = it },
                            label = { Text("Bedrooms") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = bathrooms,
                            onValueChange = { bathrooms = it },
                            label = { Text("Bathrooms") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = sizeSqmText,
                            onValueChange = { sizeSqmText = it },
                            label = { Text("Size (m²)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = parkingText,
                            onValueChange = { parkingText = it },
                            label = { Text("Parking Slots") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                3 -> {
                    // STEP 3: Pricing & Location
                    Text("Step 3: Pricing & Location", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("Price (KSh) *") },
                            placeholder = { Text("e.g. 85000") },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (listingType == ListingType.FOR_RENT) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = {},
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                ) {
                                    Text("/ Month", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Neighborhood in Nairobi *", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(BrandingConfig.featuredNeighborhoods) { hood ->
                            Surface(
                                onClick = { selectedNeighborhood = hood },
                                shape = RoundedCornerShape(20.dp),
                                color = if (selectedNeighborhood == hood) DeepNavy else WarmIvory,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                            ) {
                                Text(
                                    text = hood,
                                    color = if (selectedNeighborhood == hood) WarmOffWhite else DarkCharcoal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = approximateAddress,
                        onValueChange = { approximateAddress = it },
                        label = { Text("Approximate Road / Street Address") },
                        placeholder = { Text("e.g., Argwings Kodhek Road, Kilimani") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                4 -> {
                    // STEP 4: Amenities
                    Text("Step 4: Amenities & Features", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Amenity.entries.forEach { amenity ->
                            val isChecked = selectedAmenities.contains(amenity)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newSet = selectedAmenities.toMutableSet()
                                        if (isChecked) newSet.remove(amenity) else newSet.add(amenity)
                                        selectedAmenities = newSet
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        val newSet = selectedAmenities.toMutableSet()
                                        if (checked) newSet.add(amenity) else newSet.remove(amenity)
                                        selectedAmenities = newSet
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(amenity.displayName, fontSize = 13.sp, color = DarkCharcoal)
                            }
                        }
                    }
                }

                5 -> {
                    // STEP 5: Photos & Preview Submit
                    Text("Step 5: Photos & Final Submission", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Property Photos (${propertyImages.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { propertyImages.add("https://images.unsplash.com/photo-1560448204-e02f11c3d0e2") },
                            colors = ButtonDefaults.buttonColors(containerColor = MutedTerracotta)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Photo", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quality meter card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = WarmIvory)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Listing Quality Score", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("$qualityScore%", fontWeight = FontWeight.Bold, color = VerifiedGreen, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { qualityScore / 100f },
                                color = VerifiedGreen,
                                trackColor = CardBorder,
                                modifier = Modifier.fillMaxWidth().height(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Bottom Navigation Buttons (Back & Next / Submit)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Back", color = DarkCharcoal)
                    }
                }

                Button(
                    onClick = {
                        if (currentStep < 5) {
                            currentStep++
                        } else {
                            val newProp = tempProp.copy(
                                id = "prop_${System.currentTimeMillis()}",
                                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
                            )
                            onSubmitProperty(newProp)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (currentStep == 5) "Submit for Admin Review" else "Next Step",
                        color = WarmOffWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
