package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.ListingStatus
import com.example.myapplication.model.Property
import com.example.myapplication.ui.components.PropertyCard
import com.example.myapplication.ui.theme.*

@Composable
fun FavoritesScreen(
    favoriteProperties: List<Property>,
    favoriteIds: Set<String>,
    compareIds: Set<String>,
    onFavoriteToggle: (String) -> Unit,
    onCompareToggle: (String) -> Unit,
    onPropertyClick: (Property) -> Unit,
    onNavigateToCompare: () -> Unit,
    onBrowseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Saved Properties", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                Text("${favoriteProperties.size} listings saved in your wishlist", fontSize = 12.sp, color = MutedText)
            }

            if (compareIds.isNotEmpty()) {
                Button(
                    onClick = onNavigateToCompare,
                    colors = ButtonDefaults.buttonColors(containerColor = MutedTerracotta),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Compare (${compareIds.size})", fontSize = 12.sp, color = WarmOffWhite)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (favoriteProperties.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = MutedText, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Nothing saved yet.", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Save properties you love and come back to them anytime to compare or schedule viewings.",
                        fontSize = 13.sp,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onBrowseClick, colors = ButtonDefaults.buttonColors(containerColor = DeepNavy)) {
                        Text("Explore Properties", color = WarmOffWhite)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(favoriteProperties) { prop ->
                    Column {
                        // Availability Notice if no longer available
                        if (prop.status == ListingStatus.RENTED || prop.status == ListingStatus.SOLD || prop.status == ListingStatus.SUSPENDED) {
                            Surface(
                                color = RejectedRedBg,
                                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⚠️ Notice: This property is no longer available (${prop.status.label})",
                                    color = RejectedRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        PropertyCard(
                            property = prop,
                            isFavorite = favoriteIds.contains(prop.id),
                            isCompared = compareIds.contains(prop.id),
                            onFavoriteToggle = { onFavoriteToggle(prop.id) },
                            onCompareToggle = { onCompareToggle(prop.id) },
                            onClick = { onPropertyClick(prop) }
                        )
                    }
                }
            }
        }
    }
}
