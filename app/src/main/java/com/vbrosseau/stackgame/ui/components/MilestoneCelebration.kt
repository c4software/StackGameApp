package com.vbrosseau.stackgame.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun MilestoneCelebration(
    firstName: String,
    score: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(true) }
    
    // Auto dismiss after delay
    LaunchedEffect(Unit) {
        delay(2000)
        visible = false
        delay(500) // Wait for fade out animation
        onDismiss()
    }
    
    // Zoom and fade animation
    val scale by animateFloatAsState(
        targetValue = if (visible) 1.2f else 0.8f,
        animationSpec = tween(durationMillis = 400, easing = EaseOutBack),
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "alpha"
    )
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "BRAVO ${firstName.uppercase()}! 🎉",
            fontSize = (32 * scale).sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD700).copy(alpha = alpha)
        )
    }
}
