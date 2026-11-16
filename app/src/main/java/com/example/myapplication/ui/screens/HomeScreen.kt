package com.example.myapplication.ui.screens

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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
fun HomeScreen(
    properties: List<Property>,
    favoriteIds: Set<String>,
    compareIds: Set<String>,
    onPropertyClick: (Property) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onCompareToggle: (String) -> Unit,
    onSearchSubmit: (String, ListingType?) -> Unit,
    onListPropertyClick: () -> Unit
) {
    var searchInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf<ListingType?>(ListingType.FOR_RENT) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .verticalScroll(rememberScrollState())
    ) {
        // --- HERO SECTION ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepNavy)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.brand_tagline),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarmOffWhite,
                    lineHeight = 32.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.hero_subtitle),
                    fontSize = 13.sp,
                    color = WarmIvory.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Search Box Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Buy / Rent Tab Switcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(WarmIvory, RoundedCornerShape(10.dp))
                                .padding(4.dp)
                        ) {
                            listOf(
                                Pair("Rent", ListingType.FOR_RENT),
                                Pair("Buy", ListingType.FOR_SALE)
                            ).forEach { (label, type) ->
                                val isSelected = selectedTab == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) DeepNavy else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { selectedTab = type }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) WarmOffWhite else DarkCharcoal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Search Input
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            placeholder = { Text("e.g., 2 bedroom apartment in Kilimani under 80k", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MutedTerracotta) },
                            trailingIcon = {
                                if (searchInput.isNotEmpty()) {
                                    IconButton(onClick = { searchInput = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MutedText)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Search Button
                        Button(
                            onClick = { onSearchSubmit(searchInput, selectedTab) },
                            colors = ButtonDefaults.buttonColors(containerColor = MutedTerracotta),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Search Properties in Nairobi",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmOffWhite
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- POPULAR NAIROBI LOCATIONS ---
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Popular Locations in Nairobi",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
                Text(
                    text = "View All",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedTerracotta,
                    modifier = Modifier.clickable { onSearchSubmit("", null) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(BrandingConfig.featuredNeighborhoods) { hood ->
                    val count = properties.count { it.location.neighborhood == hood }
                    Surface(
                        onClick = { onSearchSubmit(hood, null) },
                        shape = RoundedCornerShape(14.dp),
                        color = CardBackground,
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = hood, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "$count available", fontSize = 11.sp, color = MutedText)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // --- FEATURED PROPERTIES ---
        val featuredList = properties.filter { it.isFeatured }
        if (featuredList.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MutedTerracotta, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Featured Listings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                    }
                    Text("Explore", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MutedTerracotta, modifier = Modifier.clickable { onSearchSubmit("", null) })
                }

                Spacer(modifier = Modifier.height(12.dp))

                featuredList.forEach { prop ->
                    PropertyCard(
                        property = prop,
                        isFavorite = favoriteIds.contains(prop.id),
                        isCompared = compareIds.contains(prop.id),
                        onFavoriteToggle = { onFavoriteToggle(prop.id) },
                        onCompareToggle = { onCompareToggle(prop.id) },
                        onClick = { onPropertyClick(prop) },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- RECENTLY ADDED LISTINGS ---
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text("Recently Added Listings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
            Spacer(modifier = Modifier.height(12.dp))

            properties.take(4).forEach { prop ->
                PropertyCard(
                    property = prop,
                    isFavorite = favoriteIds.contains(prop.id),
                    isCompared = compareIds.contains(prop.id),
                    onFavoriteToggle = { onFavoriteToggle(prop.id) },
                    onCompareToggle = { onCompareToggle(prop.id) },
                    onClick = { onPropertyClick(prop) },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- WHY USE NESTA TRUST SECTION ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = WarmIvory),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Why Find Property on Nesta?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                Spacer(modifier = Modifier.height(14.dp))

                listOf(
                    Pair("✓ Verified Listings", "Every verified listing is reviewed by our Nairobi moderation team."),
                    Pair("📍 Location Transparency", "Approximate neighborhood location and nearby landmarks disclosed."),
                    Pair("💬 Direct Contact", "Instant in-app chat, WhatsApp, and phone connection with owners."),
                    Pair("⚖️ Property Comparison", "Compare up to 4 properties side-by-side easily.")
                ).forEach { (title, desc) ->
                    Row(modifier = Modifier.padding(bottom = 12.dp)) {
                        Text(title.substring(0, 1), color = VerifiedGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(title.substring(2), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkCharcoal)
                            Text(desc, fontSize = 11.sp, color = MutedText)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- CALL TO ACTION FOR PROPERTY OWNERS / AGENTS ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepNavy)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Have a Property to Rent or Sell?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WarmOffWhite)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Reach thousands of active property seekers across Nairobi today.",
                    fontSize = 12.sp,
                    color = WarmIvory.copy(alpha = 0.85f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onListPropertyClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MutedTerracotta),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("List Your Property Free", fontWeight = FontWeight.Bold, color = WarmOffWhite)
                }
            }
        }
    }
}
