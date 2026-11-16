package com.example.myapplication.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.myapplication.model.*
import com.example.myapplication.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PropertyCard(
    property: Property,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCompareToggle: (() -> Unit)? = null,
    isCompared: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CardBorder)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Image Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(DeepNavy.copy(alpha = 0.05f))
            ) {
                // Property Image with Online Loader & Fallback
                PropertyImagePlaceholder(
                    property = property,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Left Badges Row
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (property.verificationStatus == VerificationStatus.VERIFIED) {
                        Surface(
                            color = VerifiedGreenBg,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, VerifiedGreen.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Verified",
                                    tint = VerifiedGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Verified",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VerifiedGreen
                                )
                            }
                        }
                    }

                    if (property.isFeatured) {
                        Surface(
                            color = MutedTerracotta,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Featured",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Featured",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Top Right Action Buttons
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onCompareToggle != null) {
                        IconButton(
                            onClick = onCompareToggle,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.9f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Compare",
                                tint = if (isCompared) MutedTerracotta else DarkCharcoal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color.Red else DarkCharcoal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Bottom Left Listing Type Pill
                Surface(
                    color = DeepNavy.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(topEnd = 8.dp),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        text = property.listingType.displayName.uppercase(),
                        color = WarmOffWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Card Body
            Column(modifier = Modifier.padding(16.dp)) {
                // Price Row
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${property.currency} ${formatPrice(property.price)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy
                        )
                        if (property.listingType == ListingType.FOR_RENT) {
                            Text(
                                text = " ${property.billingPeriod.label}",
                                fontSize = 13.sp,
                                color = MutedText,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }

                    Surface(
                        color = WarmIvory,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = property.propertyType.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkCharcoal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title
                Text(
                    text = property.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkCharcoal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Location
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MutedTerracotta,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${property.location.neighborhood}, ${property.location.city}",
                        fontSize = 13.sp,
                        color = MutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Specs Bar (Beds, Baths, Size, Parking)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (property.bedrooms > 0) {
                            SpecItem(icon = Icons.Default.Home, label = "${property.bedrooms} Beds")
                        }
                        if (property.bathrooms > 0) {
                            SpecItem(icon = Icons.Default.Info, label = "${property.bathrooms} Baths")
                        }
                        if (property.sizeSqm > 0) {
                            SpecItem(icon = Icons.Default.Place, label = "${property.sizeSqm} m²")
                        }
                    }

                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = "View",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WarmOffWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpecItem(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MutedText,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = DarkCharcoal
        )
    }
}

@Composable
fun PropertyImagePlaceholder(property: Property, modifier: Modifier = Modifier) {
    val neighborhoodColor = when (property.location.neighborhood) {
        "Kilimani" -> Color(0xFF1B2A38)
        "Westlands" -> Color(0xFF145A32)
        "Kileleshwa" -> Color(0xFF4A235A)
        "Karen" -> Color(0xFF7E5109)
        "Lavington" -> Color(0xFF1B4F72)
        else -> DeepNavy
    }

    val imageUrl = property.coverImage.ifBlank { property.images.firstOrNull() ?: "" }

    if (imageUrl.isNotBlank()) {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = property.title,
            contentScale = ContentScale.Crop,
            modifier = modifier,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(neighborhoodColor),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = WarmOffWhite, modifier = Modifier.size(28.dp))
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(neighborhoodColor),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = WarmIvory.copy(alpha = 0.9f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = property.location.neighborhood,
                            color = WarmOffWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )
    } else {
        Box(
            modifier = modifier.background(neighborhoodColor),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = WarmIvory.copy(alpha = 0.9f),
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = property.location.neighborhood,
                    color = WarmOffWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun formatPrice(amount: Double): String {
    return if (amount >= 1_000_000) {
        val millions = amount / 1_000_000
        if (millions % 1.0 == 0.0) "${millions.toInt()} Million" else String.format(Locale.US, "%.1f Million", millions)
    } else {
        val intAmount = amount.toLong()
        NumberFormat.getNumberInstance(Locale.US).format(intAmount)
    }
}
