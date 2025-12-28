package com.vbrosseau.stackgame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun GameOverAdOverlay(
    canClose: Boolean = true,
    timeRemaining: Int = 0,
    onDismiss: () -> Unit,
    onPurchaseClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Randomly choose between PREMIUM and ULTRA ad
    val showPremiumAd = remember { Random.nextBoolean() }
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Semi-transparent background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
        )
        
        // Ad content
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(450.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                if (showPremiumAd) {
                                    listOf(Color(0xFFFFD700), Color(0xFFFFA000))
                                } else {
                                    listOf(Color(0xFF00BCD4), Color(0xFF0097A7))
                                }
                            )
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (showPremiumAd) {
                        PremiumAdContent()
                    } else {
                        UltraAdContent()
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Ad message
                    Text(
                        text = "Publicité - Passez à PREMIUM pour la retirer",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Purchase button
                    Button(
                        onClick = onPurchaseClick,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD700).copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⭐",
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Passer Premium - 1€",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Timer or close button
                    if (!canClose && timeRemaining > 0) {
                        Text(
                            text = "Fermeture dans $timeRemaining secondes...",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF5722)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Peut-être plus tard",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Close button (only enabled after timer)
                if (canClose) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumAdContent() {
    Text(
        text = "⭐",
        fontSize = 64.sp,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    
    Text(
        text = "Passez à PREMIUM!",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Text(
        text = "Profitez de:",
        fontSize = 18.sp,
        color = Color.White,
        fontWeight = FontWeight.Medium
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AdFeatureItem("✓ Ombre de placement")
        AdFeatureItem("✓ Sans publicités")
        AdFeatureItem("✓ 4 vies maximum")
        AdFeatureItem("✓ Expérience premium")
    }
}

@Composable
private fun UltraAdContent() {
    Text(
        text = "💎",
        fontSize = 64.sp,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    
    Text(
        text = "Passez à ULTRA!",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Text(
        text = "Le meilleur niveau:",
        fontSize = 18.sp,
        color = Color.White,
        fontWeight = FontWeight.Medium
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AdFeatureItem("✓ Tout PREMIUM")
        AdFeatureItem("✓ Fonction Rewind")
        AdFeatureItem("✓ 5 vies maximum")
        AdFeatureItem("✓ Expérience ultime")
    }
}

@Composable
private fun AdFeatureItem(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        color = Color.White,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
