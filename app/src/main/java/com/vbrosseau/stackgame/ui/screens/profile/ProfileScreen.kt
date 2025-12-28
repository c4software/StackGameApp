package com.vbrosseau.stackgame.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.profile_back_desc),
                        tint = Color.White
                    )
                }
                Text(
                    text = androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.profile_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

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
                

                Text(
                    text = user.displayName,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                

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
                            text = when (user.level) {
                                UserLevel.NORMAL -> androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.level_normal)
                                UserLevel.PREMIUM -> androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.level_premium)
                                UserLevel.ULTRA -> androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.level_ultra)
                            },
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
                            text = androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.profile_purchase_btn),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.profile_features_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            val features = when (user.level) {
                                UserLevel.PREMIUM -> listOf(
                                    com.vbrosseau.stackgame.R.string.purchase_feature_ads,
                                    com.vbrosseau.stackgame.R.string.purchase_feature_ghost,
                                    com.vbrosseau.stackgame.R.string.purchase_feature_lives_bonus
                                )
                                UserLevel.ULTRA -> listOf(
                                    com.vbrosseau.stackgame.R.string.purchase_feature_all_premium,
                                    com.vbrosseau.stackgame.R.string.purchase_feature_rewind,
                                    com.vbrosseau.stackgame.R.string.purchase_feature_lives_unlimited
                                )
                                else -> emptyList()
                            }
                            
                            features.forEach { featureRes ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (user.level == UserLevel.ULTRA) Color(0xFF00BCD4) else Color(0xFFFFD700),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = androidx.compose.ui.res.stringResource(featureRes),
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Logout button (visible if not guest)
                if (!user.isGuest()) {
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.profile_logout_btn),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
