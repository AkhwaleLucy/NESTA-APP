package com.example.myapplication.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.core.BrandingConfig
import com.example.myapplication.model.*
import com.example.myapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentFilter: SearchFilter,
    onApplyFilter: (SearchFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var listingType by remember { mutableStateOf(currentFilter.listingType) }
    var propertyType by remember { mutableStateOf(currentFilter.propertyType) }
    var selectedNeighborhood by remember { mutableStateOf(currentFilter.neighborhood) }
    var minPriceText by remember { mutableStateOf(currentFilter.minPrice?.toInt()?.toString() ?: "") }
    var maxPriceText by remember { mutableStateOf(currentFilter.maxPrice?.toInt()?.toString() ?: "") }
    var bedrooms by remember { mutableStateOf(currentFilter.bedrooms) }
    var bathrooms by remember { mutableStateOf(currentFilter.bathrooms) }
    var selectedAmenities by remember { mutableStateOf(currentFilter.selectedAmenities.toMutableSet()) }
    var isVerifiedOnly by remember { mutableStateOf(currentFilter.isVerifiedOnly) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = WarmOffWhite,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = DeepNavy,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Filter Properties",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 12.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                // Transaction Type (Rent / Sale)
                Text("Transaction Type", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChipPill(
                        text = "All Transactions",
                        selected = listingType == null,
                        onClick = { listingType = null }
                    )
                    ListingType.values().forEach { type ->
                        FilterChipPill(
                            text = type.displayName,
                            selected = listingType == type,
                            onClick = { listingType = type }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Property Type
                Text("Property Type", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChipPill(
                            text = "All Types",
                            selected = propertyType == null,
                            onClick = { propertyType = null }
                        )
                    }
                    items(PropertyType.values()) { type ->
                        FilterChipPill(
                            text = type.displayName,
                            selected = propertyType == type,
                            onClick = { propertyType = type }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Neighborhood
                Text("Popular Nairobi Neighborhoods", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChipPill(
                            text = "All Locations",
                            selected = selectedNeighborhood == null,
                            onClick = { selectedNeighborhood = null }
                        )
                    }
                    items(BrandingConfig.featuredNeighborhoods) { hood ->
                        FilterChipPill(
                            text = hood,
                            selected = selectedNeighborhood == hood,
                            onClick = { selectedNeighborhood = hood }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Price Range
                Text("Price Range (KSh)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = minPriceText,
                        onValueChange = { minPriceText = it },
                        label = { Text("Min Price") },
                        placeholder = { Text("e.g. 30000") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = maxPriceText,
                        onValueChange = { maxPriceText = it },
                        label = { Text("Max Price") },
                        placeholder = { Text("e.g. 100000") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bedrooms Selector
                Text("Bedrooms", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null, 1, 2, 3, 4, 5).forEach { count ->
                        FilterChipPill(
                            text = if (count == null) "Any" else "$count+",
                            selected = bedrooms == count,
                            onClick = { bedrooms = count }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Verified Listings Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Verified Listings Only", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                        Text("Show properties verified by Nesta moderators", fontSize = 11.sp, color = MutedText)
                    }
                    Switch(
                        checked = isVerifiedOnly,
                        onCheckedChange = { isVerifiedOnly = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = VerifiedGreen, checkedTrackColor = VerifiedGreenBg)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amenities Selection
                Text("Amenities & Features", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Amenity.values().take(8).forEach { amenity ->
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

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Bottom Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        listingType = null
                        propertyType = null
                        selectedNeighborhood = null
                        minPriceText = ""
                        maxPriceText = ""
                        bedrooms = null
                        bathrooms = null
                        selectedAmenities = mutableSetOf()
                        isVerifiedOnly = false
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Reset All", color = DarkCharcoal)
                }

                Button(
                    onClick = {
                        val minP = minPriceText.toDoubleOrNull()
                        val maxP = maxPriceText.toDoubleOrNull()
                        onApplyFilter(
                            currentFilter.copy(
                                listingType = listingType,
                                propertyType = propertyType,
                                neighborhood = selectedNeighborhood,
                                minPrice = minP,
                                maxPrice = maxP,
                                bedrooms = bedrooms,
                                bathrooms = bathrooms,
                                selectedAmenities = selectedAmenities,
                                isVerifiedOnly = isVerifiedOnly
                            )
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply Filters", color = WarmOffWhite, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FilterChipPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) DeepNavy else WarmIvory,
        border = if (selected) null else BorderStroke(1.dp, CardBorder)
    ) {
        Text(
            text = text,
            color = if (selected) WarmOffWhite else DarkCharcoal,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
