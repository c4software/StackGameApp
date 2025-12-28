package com.vbrosseau.stackgame.ui.screens.purchase

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vbrosseau.stackgame.models.UserLevel

@Composable
fun PurchaseScreen(
    currentLevel: UserLevel,
    isGuest: Boolean,
    onBack: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    // Determine ownership based on current level
    val hasPremium = currentLevel >= UserLevel.PREMIUM
    val hasUltra = currentLevel == UserLevel.ULTRA
    
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
            // Header
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
                    text = androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.purchase_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            // Content - Horizontal Pager
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.purchase_header),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Horizontal Pager for offers
                val pagerState = rememberPagerState(pageCount = { 2 })
                
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    pageSpacing = 16.dp
                ) { page ->
                    // Calculate page offset for transition effects
                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                // Scale effect
                                val scale = 1f - (kotlin.math.abs(pageOffset) * 0.1f)
                                scaleX = scale
                                scaleY = scale
                                
                                // Alpha effect
                                alpha = 1f - (kotlin.math.abs(pageOffset) * 0.3f)
                            }
                            .background(
                                brush = when (page) {
                                    0 -> Brush.verticalGradient(
                                        listOf(
                                            Color(0xFFFFD700).copy(alpha = 0.3f),
                                            Color(0xFFFFA000).copy(alpha = 0.2f)
                                        )
                                    )
                                    else -> Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF00BCD4).copy(alpha = 0.3f),
                                            Color(0xFF0097A7).copy(alpha = 0.2f)
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (page) {
                            0 -> PurchaseCard(
                                title = androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.purchase_premium_title),
                                price = "1€",
                                icon = "⭐",
                                color = Color(0xFFFFD700),
                                features = listOf(
                                    androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.purchase_feature_ads),
                                    androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.purchase_feature_ghost),
                                    androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.purchase_feature_lives_bonus),
                                    androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.purchase_feature_support)
                                ),
                                isOwned = hasPremium,
                                isCurrent = currentLevel == UserLevel.PREMIUM,
                                onPurchase = {
                                    if (isGuest) {
                                        onLoginClick()
                                    } else {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(com.vbrosseau.stackgame.BuildConfig.PURCHASE_URL)
                                        )
                                        activity?.startActivity(intent)
                                    }
                                }
                            )
                            1 -> PurchaseCard(
                                title = androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.purchase_ultra_title),
                                price = "5€",
                                icon = "💎",
                                color = Color(0xFF00BCD4),
                                features = listOf(
                                    androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.purchase_feature_all_premium),
                                    androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.purchase_feature_rewind),
                                    androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.purchase_feature_lives_unlimited),
                                    androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.purchase_feature_badge),
                                    androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.purchase_feature_early_access)
                                ),
                                isOwned = hasUltra,
                                isCurrent = currentLevel == UserLevel.ULTRA,
                                onPurchase = {
                                    if (isGuest) {
                                        onLoginClick()
                                    } else {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(com.vbrosseau.stackgame.BuildConfig.PURCHASE_URL)
                                        )
                                        activity?.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Page indicator
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(2) { index ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(8.dp)
                                .background(
                                    color = if (pagerState.currentPage == index) 
                                        Color.White 
                                    else 
                                        Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun PurchaseCard(
    title: String,
    price: String,
    icon: String,
    color: Color,
    features: List<String>,
    isOwned: Boolean,
    isCurrent: Boolean,
    onPurchase: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Text(
                text = icon,
                fontSize = 64.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Title
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            
            // Price
            Text(
                text = price,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Features
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = feature,
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Purchase Button
            Button(
                onClick = onPurchase,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isOwned,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOwned) Color.Gray else color,
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = when {
                        isCurrent -> androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.purchase_current_level)
                        isOwned -> androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.purchase_owned)
                        else -> androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.purchase_buy_prefix, price)
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
