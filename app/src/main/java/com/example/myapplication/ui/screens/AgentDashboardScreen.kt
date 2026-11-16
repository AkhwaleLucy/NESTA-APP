package com.example.myapplication.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.*
import com.example.myapplication.ui.components.formatPrice
import com.example.myapplication.ui.theme.*

@Composable
fun AgentDashboardScreen(
    currentAgent: User,
    agentProperties: List<Property>,
    viewingRequests: List<ViewingRequest>,
    onAddListingClick: () -> Unit,
    onStatusChange: (String, ListingStatus) -> Unit,
    onPropertyClick: (Property) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Listings, 1 = Viewing Requests

    val totalViews = agentProperties.sumOf { it.viewsCount }
    val totalSaves = agentProperties.sumOf { it.savesCount }
    val totalInquiries = agentProperties.sumOf { it.inquiriesCount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Agent Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Agent Portal", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                Text("${currentAgent.name} · ${currentAgent.agencyName.ifBlank { "Independent Agent" }}", fontSize = 12.sp, color = MutedText)
            }

            Button(
                onClick = onAddListingClick,
                enabled = currentAgent.isAgentApproved || currentAgent.role == UserRole.ADMIN,
                colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Listing", fontSize = 12.sp, color = WarmOffWhite, fontWeight = FontWeight.Bold)
            }
        }

        if (currentAgent.role == UserRole.AGENT && !currentAgent.isAgentApproved) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PendingOrangeBg),
                border = BorderStroke(1.dp, PendingOrange.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = PendingOrange,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Approval Pending", color = WarmOffWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Administrator Approval Required", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PendingOrange)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your agent profile is under review by a platform administrator. Email verification and admin approval are both required before you can publish property listings.",
                        fontSize = 12.sp,
                        color = DarkCharcoal,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Performance Stat Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AgentStatCard("Active Listings", agentProperties.size.toString(), Modifier.weight(1f))
            AgentStatCard("Total Views", totalViews.toString(), Modifier.weight(1f))
            AgentStatCard("Saves", totalSaves.toString(), Modifier.weight(1f))
            AgentStatCard("Inquiries", totalInquiries.toString(), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tab Selector (Listings / Viewings)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = WarmOffWhite,
            contentColor = DeepNavy
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("My Listings (${agentProperties.size})", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Viewings (${viewingRequests.size})", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> {
                // MY LISTINGS MANAGER
                if (agentProperties.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 40.dp), contentAlignment = Alignment.TopCenter) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No properties listed yet", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onAddListingClick, colors = ButtonDefaults.buttonColors(containerColor = DeepNavy)) {
                                Text("Create Your First Listing", color = WarmOffWhite)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(agentProperties) { prop ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CardBorder))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(prop.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkCharcoal, modifier = Modifier.weight(1f))

                                        // Status Pill
                                        Surface(
                                            color = when (prop.status) {
                                                ListingStatus.AVAILABLE -> VerifiedGreenBg
                                                ListingStatus.PENDING -> PendingOrangeBg
                                                else -> RejectedRedBg
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = prop.status.label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (prop.status) {
                                                    ListingStatus.AVAILABLE -> VerifiedGreen
                                                    ListingStatus.PENDING -> PendingOrange
                                                    else -> RejectedRed
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("KSh ${formatPrice(prop.price)} · ${prop.location.neighborhood} · ${prop.viewsCount} views · ${prop.savesCount} saves", fontSize = 12.sp, color = MutedText)

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = CardBorder)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Status Change Trigger Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onPropertyClick(prop) },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(34.dp)
                                        ) {
                                            Text("View", fontSize = 11.sp, color = DarkCharcoal)
                                        }

                                        if (prop.status == ListingStatus.AVAILABLE) {
                                            Button(
                                                onClick = { onStatusChange(prop.id, ListingStatus.RENTED) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MutedTerracotta),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).height(34.dp)
                                            ) {
                                                Text("Mark Rented/Sold", fontSize = 10.sp, color = WarmOffWhite)
                                            }
                                        } else {
                                            Button(
                                                onClick = { onStatusChange(prop.id, ListingStatus.AVAILABLE) },
                                                colors = ButtonDefaults.buttonColors(containerColor = VerifiedGreen),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).height(34.dp)
                                            ) {
                                                Text("Mark Available", fontSize = 10.sp, color = WarmOffWhite)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // VIEWING REQUESTS PIPELINE
                if (viewingRequests.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 40.dp), contentAlignment = Alignment.TopCenter) {
                        Text("No viewing requests yet.", fontWeight = FontWeight.Bold, color = MutedText)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(viewingRequests) { req ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CardBorder))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(req.requesterName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkCharcoal)
                                        Text(req.requestedDate, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MutedTerracotta)
                                    }
                                    Text(req.requesterPhone, fontSize = 12.sp, color = MutedText)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(req.propertyTitle, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DeepNavy)
                                    if (req.message.isNotBlank()) {
                                        Text("Note: ${req.message}", fontSize = 11.sp, color = MutedText)
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
fun AgentStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = DeepNavy)
            Spacer(modifier = Modifier.height(2.dp))
            Text(title, fontSize = 10.sp, color = MutedText, fontWeight = FontWeight.Medium)
        }
    }
}
