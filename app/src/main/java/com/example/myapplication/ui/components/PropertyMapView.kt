package com.example.myapplication.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.Property
import com.example.myapplication.ui.theme.*
import java.util.Locale

@Composable
fun PropertyMapView(
    properties: List<Property>,
    onPropertySelect: (Property) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPropertyId by remember { mutableStateOf<String?>(properties.firstOrNull()?.id) }
    val selectedProperty = properties.find { it.id == selectedPropertyId }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE5ECE9)) // Soft Nairobi terrain color
    ) {
        // Map Canvas Drawing (Simulated Nairobi road network and neighborhood sectors)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val roadColor = Color(0xFFFFFFFF)
            drawPath(
                path = Path().apply {
                    moveTo(0f, height * 0.4f)
                    cubicTo(width * 0.3f, height * 0.38f, width * 0.7f, height * 0.45f, width, height * 0.42f)
                },
                color = roadColor,
                style = Stroke(width = 16f)
            )

            drawPath(
                path = Path().apply {
                    moveTo(width * 0.2f, 0f)
                    cubicTo(width * 0.25f, height * 0.4f, width * 0.35f, height * 0.7f, width * 0.5f, height)
                },
                color = roadColor,
                style = Stroke(width = 12f)
            )

            drawPath(
                path = Path().apply {
                    moveTo(0f, height * 0.7f)
                    lineTo(width, height * 0.65f)
                },
                color = roadColor,
                style = Stroke(width = 10f)
            )
        }

        // Floating Header Badge
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = DeepNavy,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MutedTerracotta,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Nairobi Interactive Map (${properties.size} Listings)",
                    color = WarmOffWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Map Property Pins Box
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val w = maxWidth
            val h = maxHeight

            properties.forEachIndexed { index, prop ->
                val (offsetX, offsetY) = when (prop.location.neighborhood) {
                    "Kilimani" -> Pair(w * 0.35f, h * 0.48f)
                    "Kileleshwa" -> Pair(w * 0.28f, h * 0.38f)
                    "Westlands" -> Pair(w * 0.42f, h * 0.22f)
                    "Karen" -> Pair(w * 0.18f, h * 0.72f)
                    "Lavington" -> Pair(w * 0.22f, h * 0.42f)
                    "Ruaka" -> Pair(w * 0.52f, h * 0.12f)
                    else -> Pair(w * (0.3f + (index * 0.12f) % 0.5f), h * (0.25f + (index * 0.15f) % 0.5f))
                }

                val isSelected = prop.id == selectedPropertyId

                Box(
                    modifier = Modifier
                        .offset(x = offsetX, y = offsetY)
                        .clickable { selectedPropertyId = prop.id }
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MutedTerracotta else DeepNavy,
                        shadowElevation = if (isSelected) 8.dp else 4.dp,
                        border = BorderStroke(2.dp, WarmOffWhite)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = WarmOffWhite,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "KSh ${formatShortPrice(prop.price)}",
                                color = WarmOffWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Bottom Selected Property Callout Card
        if (selectedProperty != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { onPropertySelect(selectedProperty) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(DeepNavy, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = WarmIvory,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedProperty.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkCharcoal,
                            maxLines = 1
                        )
                        Text(
                            text = "${selectedProperty.location.neighborhood} · ${selectedProperty.propertyType.displayName}",
                            fontSize = 12.sp,
                            color = MutedText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${selectedProperty.currency} ${formatPrice(selectedProperty.price)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy
                        )
                    }

                    IconButton(
                        onClick = { onPropertySelect(selectedProperty) },
                        modifier = Modifier
                            .background(DeepNavy, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "View Details",
                            tint = WarmOffWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

fun formatShortPrice(amount: Double): String {
    return if (amount >= 1_000_000) {
        val millions = amount / 1_000_000
        if (millions % 1.0 == 0.0) "${millions.toInt()}M" else String.format(Locale.US, "%.1fM", millions)
    } else {
        val thousands = (amount / 1000).toInt()
        "${thousands}k"
    }
}
