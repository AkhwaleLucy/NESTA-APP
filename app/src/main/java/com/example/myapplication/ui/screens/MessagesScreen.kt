package com.example.myapplication.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.Conversation
import com.example.myapplication.model.Message
import com.example.myapplication.repository.NestaRepository
import com.example.myapplication.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MessagesScreen(
    conversations: List<Conversation>,
    messagesMap: Map<String, List<Message>>,
    currentUserId: String,
    initialConversationId: String? = null,
    onSendMessageAsync: suspend (String, String) -> Result<Unit> = { _, _ -> Result.success(Unit) },
    onBackToList: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedConversationId by remember { mutableStateOf(initialConversationId ?: conversations.firstOrNull()?.id) }
    val activeConversation = conversations.find { it.id == selectedConversationId }

    var isSending by remember { mutableStateOf(false) }
    var sendErrorMessage by remember { mutableStateOf<String?>(null) }

    if (activeConversation != null) {
        LaunchedEffect(activeConversation.id) {
            NestaRepository.observeMessages(activeConversation.id)
        }

        // --- CHAT THREAD VIEW ---
        val conversationMessages = messagesMap[activeConversation.id] ?: emptyList()
        var messageInput by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmOffWhite)
        ) {
            // Chat Header
            Surface(
                color = CardBackground,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            selectedConversationId = null
                            onBackToList()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(4.dp))

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(DeepNavy, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (activeConversation.seekerId == currentUserId) activeConversation.agentName.take(1) else activeConversation.seekerName.take(1),
                                color = WarmOffWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (activeConversation.seekerId == currentUserId) activeConversation.agentName else activeConversation.seekerName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkCharcoal
                            )
                            Text("Direct In-App Conversation", fontSize = 11.sp, color = MutedText)
                        }
                    }

                    HorizontalDivider(color = CardBorder)

                    // Attached Property Context Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WarmIvory)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = DeepNavy, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${activeConversation.propertyTitle} (${activeConversation.propertyPrice})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (sendErrorMessage != null) {
                Surface(
                    color = RejectedRedBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = sendErrorMessage!!,
                        color = RejectedRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Message Bubble List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (conversationMessages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No messages yet. Send a message to start the conversation!", fontSize = 12.sp, color = MutedText)
                        }
                    }
                } else {
                    items(conversationMessages) { msg ->
                        val isMine = msg.senderId == currentUserId
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 14.dp,
                                    topEnd = 14.dp,
                                    bottomStart = if (isMine) 14.dp else 2.dp,
                                    bottomEnd = if (isMine) 2.dp else 14.dp
                                ),
                                color = if (isMine) DeepNavy else CardBackground,
                                border = if (isMine) null else BorderStroke(1.dp, CardBorder),
                                shadowElevation = 1.dp
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = msg.text,
                                        fontSize = 14.sp,
                                        color = if (isMine) WarmOffWhite else DarkCharcoal
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = msg.timestamp,
                                        fontSize = 10.sp,
                                        color = if (isMine) WarmIvory.copy(alpha = 0.7f) else MutedText,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Message Input Dock
            Surface(
                color = CardBackground,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("Write a message...", fontSize = 13.sp) },
                        enabled = !isSending,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val textToSend = messageInput.trim()
                            if (textToSend.isNotBlank()) {
                                isSending = true
                                sendErrorMessage = null
                                coroutineScope.launch {
                                    val res = onSendMessageAsync(activeConversation.id, textToSend)
                                    isSending = false
                                    res.fold(
                                        onSuccess = {
                                            messageInput = "" // Clear input only on success
                                        },
                                        onFailure = { err ->
                                            sendErrorMessage = err.message ?: "Failed to send message. Please retry."
                                            // Keeps typed text in messageInput for retry!
                                        }
                                    )
                                }
                            }
                        },
                        enabled = !isSending && messageInput.isNotBlank(),
                        modifier = Modifier
                            .background(DeepNavy, CircleShape)
                            .size(44.dp)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(color = WarmOffWhite, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = WarmOffWhite, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    } else {
        // --- CONVERSATIONS LIST ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmOffWhite)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text("In-App Messages", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
            Text("Direct property inquiries with Nairobi agents & owners", fontSize = 12.sp, color = MutedText)

            Spacer(modifier = Modifier.height(16.dp))

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 80.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = MutedText, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No active conversations", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Start a conversation from any property details page", fontSize = 12.sp, color = MutedText)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(conversations) { conv ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedConversationId = conv.id },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CardBorder))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(DeepNavy, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (conv.seekerId == currentUserId) conv.agentName.take(1) else conv.seekerName.take(1),
                                        color = WarmOffWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (conv.seekerId == currentUserId) conv.agentName else conv.seekerName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkCharcoal
                                        )
                                        Text(conv.lastTimestamp, fontSize = 11.sp, color = MutedText)
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${conv.propertyTitle} (${conv.propertyPrice})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MutedTerracotta
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = conv.lastMessage,
                                        fontSize = 12.sp,
                                        color = MutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
