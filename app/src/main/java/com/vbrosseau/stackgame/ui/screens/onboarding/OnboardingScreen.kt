package com.vbrosseau.stackgame.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onLoginClick: () -> Unit,
    onGuestClick: () -> Unit,
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
        val pagerState = rememberPagerState(pageCount = { 3 })
        val scope = rememberCoroutineScope()
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                OnboardingPage(page = page, onLoginClick = onLoginClick, onGuestClick = onGuestClick)
            }
            
            // Indicators and Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 12.dp else 8.dp)
                                .background(
                                    color = if (pagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
                
                // Navigation Button (Next or None if last page)
                if (pagerState.currentPage < 2) {
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ),
                        shape = CircleShape
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.onboarding_next_btn))
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPage(
    page: Int,
    onLoginClick: () -> Unit,
    onGuestClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (page) {
            0 -> {
                Text(
                    text = "🏗️",
                    fontSize = 80.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.onboarding_p1_title),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.onboarding_p1_desc),
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
            1 -> {
                Text(
                    text = "⚖️",
                    fontSize = 80.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.onboarding_p2_title),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.onboarding_p2_desc),
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
            2 -> {
                Text(
                    text = "🚀",
                    fontSize = 80.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.onboarding_p3_title),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(48.dp))
                
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700), // Gold
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.onboarding_login_btn),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = onGuestClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.vbrosseau.stackgame.R.string.onboarding_guest_btn),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
