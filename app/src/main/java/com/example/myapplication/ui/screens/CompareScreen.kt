package com.example.myapplication.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.Amenity
import com.example.myapplication.model.Property
import com.example.myapplication.ui.components.formatPrice
import com.example.myapplication.ui.theme.*

@Composable
fun CompareScreen(
    compareProperties: List<Property>,
    onRemoveFromCompare: (String) -> Unit,
    onPropertyClick: (Property) -> Unit,
    onBrowseClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Compare Properties", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                Text("Side-by-side spec comparison (${compareProperties.size} selected)", fontSize = 12.sp, color = MutedText)
            }

            if (compareProperties.isNotEmpty()) {
                TextButton(onClick = { compareProperties.forEach { onRemoveFromCompare(it.id) } }) {
                    Text("Clear All", color = MutedTerracotta, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (compareProperties.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = MutedText, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No properties selected for comparison", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Tap the Compare icon on any property card to compare up to 4 listings", fontSize = 12.sp, color = MutedText, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onBrowseClick, colors = ButtonDefaults.buttonColors(containerColor = DeepNavy)) {
                        Text("Browse Properties", color = WarmOffWhite)
                    }
                }
            }
        } else {
            // Comparison Scroll Table
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScrollState)
                ) {
                    // Left Metric Label Column
                    Column(
                        modifier = Modifier
                            .width(130.dp)
                            .padding(end = 8.dp)
                    ) {
                        Spacer(modifier = Modifier.height(160.dp)) // Header space
                        CompareMetricHeader("Price")
                        CompareMetricHeader("Neighborhood")
                        CompareMetricHeader("Property Type")
                        CompareMetricHeader("Listing Type")
                        CompareMetricHeader("Bedrooms")
                        CompareMetricHeader("Bathrooms")
                        CompareMetricHeader("Size (m²)")
                        CompareMetricHeader("Parking")
                        Amenity.entries.take(6).forEach { amenity ->
                            CompareMetricHeader(amenity.displayName)
                        }
                    }

                    // Property Columns
                    compareProperties.forEach { prop ->
                        Column(
                            modifier = Modifier
                                .width(160.dp)
                                .padding(horizontal = 4.dp)
                        ) {
                            // Top Card Header
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CardBorder))
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    IconButton(
                                        onClick = { onRemoveFromCompare(prop.id) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Remove", tint = DarkCharcoal, modifier = Modifier.size(16.dp))
                                    }

                                    Column(
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .align(Alignment.BottomStart)
                                    ) {
                                        Text(prop.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = { onPropertyClick(prop) },
                                            colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("View", fontSize = 10.sp, color = WarmOffWhite)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            CompareValueCell("${prop.currency} ${formatPrice(prop.price)}", isHighlight = true)
                            CompareValueCell(prop.location.neighborhood)
                            CompareValueCell(prop.propertyType.displayName)
                            CompareValueCell(prop.listingType.displayName)
                            CompareValueCell("${prop.bedrooms} Beds")
                            CompareValueCell("${prop.bathrooms} Baths")
                            CompareValueCell("${prop.sizeSqm} m²")
                            CompareValueCell("${prop.parkingSpaces} Slots")

                            Amenity.entries.take(6).forEach { amenity ->
                                val hasAmenity = prop.amenities.contains(amenity)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .padding(vertical = 2.dp),
                                    color = if (hasAmenity) VerifiedGreenBg else WarmIvory,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (hasAmenity) {
                                            Icon(Icons.Default.Check, contentDescription = "Yes", tint = VerifiedGreen, modifier = Modifier.size(16.dp))
                                        } else {
                                            Text("—", color = MutedText, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompareMetricHeader(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal, maxLines = 1)
    }
}

@Composable
fun CompareValueCell(value: String, isHighlight: Boolean = false) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .padding(vertical = 2.dp),
        color = if (isHighlight) WarmIvory else CardBackground,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
                color = if (isHighlight) DeepNavy else DarkCharcoal,
                textAlign = TextAlign.Center
            )
        }
    }
}
