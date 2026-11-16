package com.example.myapplication.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.*
import com.example.myapplication.repository.NestaRepository
import com.example.myapplication.ui.components.formatPrice
import com.example.myapplication.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminModerationScreen(
    allProperties: List<Property>,
    reports: List<Report>,
    pendingAgents: List<User> = emptyList(),
    onApproveProperty: (String) -> Unit,
    onRejectProperty: (String, String) -> Unit,
    onToggleVerifyProperty: (String) -> Unit,
    onSuspendProperty: (String) -> Unit,
    onApproveAgent: (String) -> Unit = {},
    onRejectAgent: (String, String) -> Unit = { _, _ -> },
    onPropertyClick: (Property) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Property Queue, 1 = Agent Management, 2 = Reports
    var selectedAgentStatusTab by remember { mutableStateOf(AgentApprovalStatus.PENDING) }

    var agentListState by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoadingAgents by remember { mutableStateOf(false) }

    var showRejectDialogForId by remember { mutableStateOf<String?>(null) }
    var showAgentRejectDialogForId by remember { mutableStateOf<String?>(null) }
    var showAgentSuspendDialogForId by remember { mutableStateOf<String?>(null) }
    var rejectionReasonText by remember { mutableStateOf("") }
    var agentRejectionReasonText by remember { mutableStateOf("") }
    var agentSuspendReasonText by remember { mutableStateOf("") }
    var adminFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var isActionInProgress by remember { mutableStateOf(false) }

    val pendingProperties = allProperties.filter { it.verificationStatus == VerificationStatus.PENDING || it.status == ListingStatus.PENDING }

    fun refreshAgentList() {
        isLoadingAgents = true
        coroutineScope.launch {
            val res = NestaRepository.fetchAgentsByStatus(selectedAgentStatusTab)
            isLoadingAgents = false
            res.fold(
                onSuccess = { agentListState = it },
                onFailure = { err -> adminFeedbackMessage = err.message ?: "Failed to load agents." }
            )
        }
    }

    LaunchedEffect(selectedAgentStatusTab, selectedTab) {
        if (selectedTab == 1) {
            refreshAgentList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Admin Header
        Text("Admin Moderation Console", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
        Text("Verify listings, manage agent directories, and maintain platform trust", fontSize = 12.sp, color = MutedText)

        Spacer(modifier = Modifier.height(16.dp))

        // Platform Stat Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminStatCard("Listings", allProperties.size.toString(), Modifier.weight(1f))
            AdminStatCard("Pending", pendingProperties.size.toString(), Modifier.weight(1f))
            AdminStatCard("Agents", agentListState.size.toString(), Modifier.weight(1f))
            AdminStatCard("Reports", reports.size.toString(), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (adminFeedbackMessage != null) {
            Surface(
                color = VerifiedGreenBg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = VerifiedGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(adminFeedbackMessage!!, color = VerifiedGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Main Console Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = WarmOffWhite,
            contentColor = DeepNavy
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Listings (${pendingProperties.size})", modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Agent Directory", modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text("Reports (${reports.size})", modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> {
                // MODERATION QUEUE FOR PROPERTIES
                if (pendingProperties.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 40.dp), contentAlignment = Alignment.TopCenter) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = VerifiedGreen, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("All clear! No pending property listings.", fontWeight = FontWeight.Bold, color = DarkCharcoal)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(pendingProperties) { prop ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CardBorder))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(prop.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkCharcoal)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Owner: ${prop.ownerName} (${prop.ownerAgency.ifBlank { "Independent" }})", fontSize = 12.sp, color = MutedText)
                                    Text("Location: ${prop.location.neighborhood} · KSh ${formatPrice(prop.price)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepNavy)

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = CardBorder)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onApproveProperty(prop.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = VerifiedGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(36.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Approve", fontSize = 11.sp, color = WarmOffWhite)
                                        }

                                        Button(
                                            onClick = { onToggleVerifyProperty(prop.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(36.dp)
                                        ) {
                                            Text("Verify Badge", fontSize = 11.sp, color = WarmOffWhite)
                                        }

                                        OutlinedButton(
                                            onClick = { showRejectDialogForId = prop.id },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(36.dp)
                                        ) {
                                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reject", fontSize = 11.sp, color = RejectedRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // AGENT DIRECTORY WITH 4 STATUS TABS
                Column {
                    ScrollableTabRow(
                        selectedTabIndex = selectedAgentStatusTab.ordinal,
                        containerColor = WarmOffWhite,
                        contentColor = DeepNavy,
                        edgePadding = 0.dp
                    ) {
                        AgentApprovalStatus.entries.forEach { status ->
                            Tab(
                                selected = selectedAgentStatusTab == status,
                                onClick = { selectedAgentStatusTab = status }
                            ) {
                                Text(
                                    text = status.label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isLoadingAgents) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = DeepNavy, modifier = Modifier.size(24.dp))
                        }
                    } else if (agentListState.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(top = 40.dp), contentAlignment = Alignment.TopCenter) {
                            Text("No agents found under '${selectedAgentStatusTab.label}'.", fontWeight = FontWeight.Bold, color = MutedText, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            items(agentListState) { agent ->
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
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = DeepNavy, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(agent.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkCharcoal)
                                            }

                                            Surface(
                                                color = if (agent.isEmailVerified) VerifiedGreenBg else PendingOrangeBg,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (agent.isEmailVerified) "Email Verified" else "Unverified Email",
                                                    color = if (agent.isEmailVerified) VerifiedGreen else PendingOrange,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Agency: ${agent.agencyName.ifBlank { "Independent Agent" }}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MutedTerracotta)
                                        Text("Email: ${agent.email} · Phone: ${agent.phone}", fontSize = 12.sp, color = MutedText)
                                        Text("Registered: ${agent.memberSince}", fontSize = 11.sp, color = MutedText)

                                        if (agent.reviewedBy.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Reviewed By: ${agent.reviewedBy} on ${agent.reviewedAt}", fontSize = 10.sp, color = MutedText)
                                        }

                                        if (agent.rejectionReason.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                color = RejectedRedBg,
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "Reason: ${agent.rejectionReason}",
                                                    color = RejectedRed,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(6.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = CardBorder)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (selectedAgentStatusTab != AgentApprovalStatus.APPROVED) {
                                                Button(
                                                    onClick = {
                                                        isActionInProgress = true
                                                        coroutineScope.launch {
                                                            val res = NestaRepository.updateAgentApprovalStatus(agent.id, AgentApprovalStatus.APPROVED)
                                                            isActionInProgress = false
                                                            res.fold(
                                                                onSuccess = {
                                                                    adminFeedbackMessage = "Approved agent '${agent.name}'."
                                                                    refreshAgentList()
                                                                },
                                                                onFailure = { err -> adminFeedbackMessage = err.message ?: "Failed to approve agent." }
                                                            )
                                                        }
                                                    },
                                                    enabled = !isActionInProgress,
                                                    colors = ButtonDefaults.buttonColors(containerColor = VerifiedGreen),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f).height(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Approve Agent", fontSize = 11.sp, color = WarmOffWhite)
                                                }
                                            }

                                            if (selectedAgentStatusTab == AgentApprovalStatus.PENDING) {
                                                OutlinedButton(
                                                    onClick = { showAgentRejectDialogForId = agent.id },
                                                    enabled = !isActionInProgress,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f).height(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Reject Agent", fontSize = 11.sp, color = RejectedRed)
                                                }
                                            }

                                            if (selectedAgentStatusTab == AgentApprovalStatus.APPROVED) {
                                                Button(
                                                    onClick = { showAgentSuspendDialogForId = agent.id },
                                                    enabled = !isActionInProgress,
                                                    colors = ButtonDefaults.buttonColors(containerColor = RejectedRed),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f).height(36.dp)
                                                ) {
                                                    Text("Suspend Agent", fontSize = 11.sp, color = WarmOffWhite)
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

            2 -> {
                // FLAGGED REPORTS REVIEW
                if (reports.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 40.dp), contentAlignment = Alignment.TopCenter) {
                        Text("No reported listings.", fontWeight = FontWeight.Bold, color = MutedText)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(reports) { rep ->
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = RejectedRed, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(rep.reason, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RejectedRed)
                                        }
                                        Text(rep.createdAt, fontSize = 11.sp, color = MutedText)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Listing: ${rep.propertyTitle}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                                    Text("Details: ${rep.description}", fontSize = 12.sp, color = DarkCharcoal)

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = { onSuspendProperty(rep.propertyId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = RejectedRed),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Suspend Listing", fontSize = 11.sp, color = WarmOffWhite)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Rejection Reason Dialog for Properties
        showRejectDialogForId?.let { targetId ->
            AlertDialog(
                onDismissRequest = { showRejectDialogForId = null },
                title = { Text("Reject Listing") },
                text = {
                    Column {
                        Text("Provide a mandatory rejection reason for the property owner:", fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = rejectionReasonText,
                            onValueChange = { rejectionReasonText = it },
                            placeholder = { Text("e.g. Incomplete details or unverified photos") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (rejectionReasonText.isNotBlank()) {
                                onRejectProperty(targetId, rejectionReasonText)
                                showRejectDialogForId = null
                                rejectionReasonText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RejectedRed)
                    ) {
                        Text("Confirm Rejection", color = WarmOffWhite)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRejectDialogForId = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Rejection Reason Dialog for Agent Applications
        showAgentRejectDialogForId?.let { targetAgentId ->
            AlertDialog(
                onDismissRequest = { showAgentRejectDialogForId = null },
                title = { Text("Reject Agent Application") },
                text = {
                    Column {
                        Text("Provide a reason for rejecting this agent application:", fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = agentRejectionReasonText,
                            onValueChange = { agentRejectionReasonText = it },
                            placeholder = { Text("e.g. Unverifiable business credentials") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isActionInProgress = true
                            coroutineScope.launch {
                                val res = NestaRepository.updateAgentApprovalStatus(targetAgentId, AgentApprovalStatus.REJECTED, agentRejectionReasonText.ifBlank { "Application rejected by administrator." })
                                isActionInProgress = false
                                showAgentRejectDialogForId = null
                                agentRejectionReasonText = ""
                                res.fold(
                                    onSuccess = {
                                        adminFeedbackMessage = "Rejected agent application."
                                        refreshAgentList()
                                    },
                                    onFailure = { err -> adminFeedbackMessage = err.message ?: "Failed to reject agent." }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RejectedRed)
                    ) {
                        Text("Confirm Rejection", color = WarmOffWhite)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAgentRejectDialogForId = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Suspension Dialog for Agent Accounts
        showAgentSuspendDialogForId?.let { targetAgentId ->
            AlertDialog(
                onDismissRequest = { showAgentSuspendDialogForId = null },
                title = { Text("Suspend Agent Account") },
                text = {
                    Column {
                        Text("Provide a reason for suspending this agent account:", fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = agentSuspendReasonText,
                            onValueChange = { agentSuspendReasonText = it },
                            placeholder = { Text("e.g. Fraudulent listing submission") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isActionInProgress = true
                            coroutineScope.launch {
                                val res = NestaRepository.updateAgentApprovalStatus(targetAgentId, AgentApprovalStatus.SUSPENDED, agentSuspendReasonText.ifBlank { "Account suspended by administrator." })
                                isActionInProgress = false
                                showAgentSuspendDialogForId = null
                                agentSuspendReasonText = ""
                                res.fold(
                                    onSuccess = {
                                        adminFeedbackMessage = "Suspended agent account."
                                        refreshAgentList()
                                    },
                                    onFailure = { err -> adminFeedbackMessage = err.message ?: "Failed to suspend agent." }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RejectedRed)
                    ) {
                        Text("Confirm Suspension", color = WarmOffWhite)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAgentSuspendDialogForId = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun AdminStatCard(title: String, value: String, modifier: Modifier = Modifier) {
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
