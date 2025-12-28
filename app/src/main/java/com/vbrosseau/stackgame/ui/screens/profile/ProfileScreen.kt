package com.vbrosseau.stackgame.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vbrosseau.stackgame.models.User
import com.vbrosseau.stackgame.models.UserLevel

@Composable
fun ProfileScreen(
    user: User,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onPurchaseClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A237E), Color(0xFF000000))
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Retour",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Profil",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Profile icon with glassmorphism
                Card(
                    modifier = Modifier.size(120.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (user.level) {
                            UserLevel.NORMAL -> Color(0xFF9E9E9E).copy(alpha = 0.3f)
                            UserLevel.PREMIUM -> Color(0xFFFFD700).copy(alpha = 0.3f)
                            UserLevel.ULTRA -> Color(0xFF00BCD4).copy(alpha = 0.3f)
                        }
                    ),
                    shape = CircleShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (user.level) {
                                UserLevel.NORMAL -> "🆓"
                                UserLevel.PREMIUM -> "⭐"
                                UserLevel.ULTRA -> "💎"
                            },
                            fontSize = 56.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // User name
                Text(
                    text = user.displayName,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Level badge with glassmorphism
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.2f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                )
                            )
                    ) {
                        Text(
                            text = user.level.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (user.level) {
                                UserLevel.NORMAL -> Color(0xFFE0E0E0)
                                UserLevel.PREMIUM -> Color(0xFFFFD700)
                                UserLevel.ULTRA -> Color(0xFF00BCD4)
                            },
                            modifier = Modifier.padding(horizontal = 40.dp, vertical = 20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Purchase button (only for NORMAL users)
                if (user.level == UserLevel.NORMAL) {
                    Button(
                        onClick = onPurchaseClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD700).copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "⭐ Passer Premium",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
