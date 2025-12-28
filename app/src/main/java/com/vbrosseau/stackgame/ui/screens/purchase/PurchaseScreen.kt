package com.vbrosseau.stackgame.ui.screens.purchase

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vbrosseau.stackgame.data.BillingManager
import com.vbrosseau.stackgame.models.UserLevel
import org.koin.androidx.compose.koinViewModel

@Composable
fun PurchaseScreen(
    currentLevel: UserLevel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PurchaseViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val scrollState = rememberScrollState()
    
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
                        contentDescription = "Retour",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Acheter Premium",
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
                    text = "Améliorez votre expérience",
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
                                title = "Premium",
                                price = "1€",
                                icon = "⭐",
                                color = Color(0xFFFFD700),
                                features = listOf(
                                    "Sans publicité",
                                    "Fonction fantôme",
                                    "Vies bonus",
                                    "Support du développeur"
                                ),
                                isOwned = uiState.purchaseState.hasPremium,
                                isCurrent = currentLevel == UserLevel.PREMIUM,
                                onPurchase = {
                                    activity?.let { act ->
                                        // TODO: Get BillingManager from Koin
                                        // billingManager.launchPurchaseFlow(act, BillingManager.PRODUCT_PREMIUM)
                                    }
                                }
                            )
                            1 -> PurchaseCard(
                                title = "Ultra",
                                price = "5€",
                                icon = "💎",
                                color = Color(0xFF00BCD4),
                                features = listOf(
                                    "Tous les avantages Premium",
                                    "Fonction retour en arrière",
                                    "Vies illimitées",
                                    "Badge exclusif",
                                    "Accès anticipé aux nouvelles fonctionnalités"
                                ),
                                isOwned = uiState.purchaseState.hasUltra,
                                isCurrent = currentLevel == UserLevel.ULTRA,
                                onPurchase = {
                                    activity?.let { act ->
                                        // TODO: Get BillingManager from Koin
                                        // billingManager.launchPurchaseFlow(act, BillingManager.PRODUCT_ULTRA)
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
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            color.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
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
                        isCurrent -> "Niveau actuel"
                        isOwned -> "Déjà acheté"
                        else -> "Acheter $price"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
