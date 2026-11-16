package com.example.myapplication.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.model.UserRole
import com.example.myapplication.ui.theme.*

@Composable
fun NestaTopAppBar(
    currentUserRole: UserRole,
    unreadNotificationCount: Int,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    Surface(
        color = WarmOffWhite,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo & Static Role Badge Tag
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.brand_name),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = DeepNavy,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Role Badge Pill (Static role indicator)
                    Surface(
                        color = when (currentUserRole) {
                            UserRole.SEEKER -> WarmIvory
                            UserRole.AGENT -> MutedTerracotta.copy(alpha = 0.15f)
                            UserRole.ADMIN -> DeepNavy
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable { onProfileClick() }
                    ) {
                        Text(
                            text = when (currentUserRole) {
                                UserRole.SEEKER -> "Seeker"
                                UserRole.AGENT -> "Agent"
                                UserRole.ADMIN -> "Admin"
                            },
                            color = when (currentUserRole) {
                                UserRole.ADMIN -> WarmOffWhite
                                UserRole.AGENT -> MutedTerracotta
                                else -> DarkCharcoal
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.brand_tagline),
                    fontSize = 11.sp,
                    color = MutedText,
                    fontWeight = FontWeight.Normal
                )
            }

            // Right Action Icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = DarkCharcoal,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = DarkCharcoal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (unreadNotificationCount > 0) {
                        Surface(
                            color = MutedTerracotta,
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier
                        .size(38.dp)
                        .background(DeepNavy.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = DeepNavy,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
