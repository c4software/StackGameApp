package com.vbrosseau.stackgame.ui.screens.game

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vbrosseau.stackgame.models.User
import com.vbrosseau.stackgame.ui.components.GameOverAdOverlay
import com.vbrosseau.stackgame.ui.components.MilestoneCelebration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import kotlin.random.Random

// --- STAR MODEL (Pure UI) ---
data class Star(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float
)

@Composable
fun StackGame(
    user: User,
    onLoginClick: () -> Unit,
    onPurchaseClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = koinViewModel()
) {
    val view = LocalView.current
    
    // --- STATE OBSERVATION ---
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val currentBlock by viewModel.currentBlockState.collectAsStateWithLifecycle()

    // --- EFFECT HANDLING ---
    LaunchedEffect(Unit) {
        viewModel.gameEffects.collectLatest { effect ->
            when (effect) {
                GameEffect.VibrateLight -> view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                GameEffect.VibrateMedium -> view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                GameEffect.VibrateHeavy -> view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                GameEffect.VibrateFail -> view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE)
                GameEffect.VibrateSuccess -> view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
            }
        }
    }

    // --- GAME LOOP ---
    var lastTime by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { time ->
                if (lastTime == 0L) lastTime = time
                // We don't strictly use delta time in logic yet, but good practice
                // val delta = time - lastTime
                lastTime = time
                
                viewModel.updateGameLoop()
            }
        }
    }
    
    // --- UI HELPERS ---
    val stars = remember { List(100) { Star(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 3 + 1, Random.nextFloat()) } }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { viewModel.handleTap(user) },
                        onLongPress = { viewModel.onRewind(user) }
                    )
                }
        ) {
            // Init Game Dimensions
            viewModel.initGame(size.width, size.height)
            if (size.width == 0f) return@Canvas

            // Background
            val bgBrush = when {
                gameState.score < 10 -> Brush.verticalGradient(listOf(Color(0xFF3E2723), Color(0xFF4E342E)))
                gameState.score < 25 -> Brush.verticalGradient(listOf(Color(0xFF1976D2), Color(0xFFBBDEFB)))
                else -> Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF1A237E)))
            }
            drawRect(brush = bgBrush, size = size)

            // Stars
            if (gameState.score > 15) {
                val starAlphaMultiplier = ((gameState.score - 15) / 10f).coerceIn(0f, 1f)
                stars.forEach { star ->
                    val starY = (star.y * size.height + gameState.cameraY * 0.2f) % size.height
                    drawCircle(
                        color = Color.White.copy(alpha = star.alpha * starAlphaMultiplier),
                        radius = star.size,
                        center = Offset(star.x * size.width, starY)
                    )
                }
            }
            
            // Shake
            val shakeTime = gameState.shakeTime
            val offsetX = if (shakeTime > 0) (Random.nextFloat() - 0.5f) * shakeTime * 2 else 0f
            val offsetY = if (shakeTime > 0) (Random.nextFloat() - 0.5f) * shakeTime * 2 else 0f

            translate(left = offsetX, top = offsetY) {
                translate(left = 0f, top = gameState.cameraY) {
                    // Draw Stack
                    gameState.stack.forEach { block ->
                        // Rotation logic
                        if (block.rotation != 0f) {
                            val centerX = block.rect.center.x
                            val centerY = block.rect.center.y
                            drawContext.canvas.save()
                            drawContext.canvas.translate(centerX, centerY)
                            drawContext.canvas.rotate(block.rotation)
                            drawContext.canvas.translate(-centerX, -centerY)
                        }
                        
                        // Draw Block
                        drawRect(
                            color = block.color,
                            topLeft = Offset(block.rect.left, block.rect.top),
                            size = Size(block.rect.width, block.rect.height)
                        )
                        
                        // Highlight
                        drawRect(
                            color = Color.White.copy(alpha = 0.2f),
                            topLeft = Offset(block.rect.left, block.rect.top),
                            size = Size(block.rect.width, 10f)
                        )
                        
                        // Windows
                        val windowCount = (block.rect.width / 35f).toInt()
                        if (windowCount > 0) {
                            val windowHeight = GameViewModel.BLOCK_HEIGHT * 0.5f
                            val windowY = block.rect.top + (GameViewModel.BLOCK_HEIGHT - windowHeight) / 2
                            val totalGapsWidth = block.rect.width * 0.3f
                            val gapWidth = totalGapsWidth / (windowCount + 1)
                            val windowWidth = (block.rect.width - totalGapsWidth) / windowCount

                            val windowColor = if (gameState.score > 25) Color.Yellow.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.15f)

                            for (i in 0 until windowCount) {
                                val windowX = block.rect.left + ((i + 1) * gapWidth) + (i * windowWidth)
                                drawRect(
                                    color = windowColor,
                                    topLeft = Offset(windowX, windowY),
                                    size = Size(windowWidth, windowHeight)
                                )
                            }
                        }
                        
                        // Instability Indicator
                        if (!block.isStable && !block.isFalling) {
                             drawRect(
                                color = Color.Red.copy(alpha = 0.15f),
                                topLeft = Offset(block.rect.left, block.rect.top),
                                size = Size(block.rect.width, block.rect.height)
                            )
                        }

                        if (block.rotation != 0f) {
                            drawContext.canvas.restore()
                        }
                    }
                    
                    // Placement Shadow (Ghost)
                    if (!gameState.isGameOver && !currentBlock.isFalling && gameState.stack.isNotEmpty() && user.hasGhostFeature()) {
                        val shadowAlpha = (0.3f - (gameState.score / 100f)).coerceIn(0f, 0.3f)
                        if (shadowAlpha > 0f) {
                            drawRect(
                                color = Color.Black.copy(alpha = shadowAlpha),
                                topLeft = Offset(currentBlock.x, gameState.stack.last().rect.top - GameViewModel.BLOCK_HEIGHT),
                                size = Size(currentBlock.width, GameViewModel.BLOCK_HEIGHT)
                            )
                        }
                    }
                    
                    // Particles
                    gameState.particles.forEach { p ->
                        drawCircle(
                            color = p.color.copy(alpha = p.life),
                            radius = p.size * p.life,
                            center = Offset(p.x, p.y)
                        )
                    }
                }
                
                // Draw Current Block (No Camera Translation? Original didn't seem to apply camera to current block Y?)
                // Actually the original code had:
                // translate(left = 0f, top = cameraY) { stack... } ... drawRect(currentBlock...)
                // Current block was drawn OUTSIDE camera translation but checking collision against translated stack?
                // Logic in VM: if (newY + BLOCK_HEIGHT >= topBlock.rect.top + cameraY)
                // This implies currentBlockY is Screen Relative, and topBlock.rect.top is World Relative.
                // CameraY brings World Relative to Screen Relative.
                // So drawing currentBlock WITHOUT camera translation is correct.
                
                if (!gameState.isGameOver && gameState.stack.isNotEmpty()) {
                    drawRect(
                        color = Color.Red.copy(alpha = 0.9f),
                        topLeft = Offset(currentBlock.x, currentBlock.y),
                        size = Size(currentBlock.width, GameViewModel.BLOCK_HEIGHT)
                    )
                }
            }
            
            // Game Over Text
            if (gameState.isGameOver) {
                drawContext.canvas.nativeCanvas.apply {
                    val subPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 50f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    val context = LocalView.current.context
                    val gameOverText = context.getString(com.vbrosseau.stackgame.R.string.game_over, user.displayName)
                    drawText(gameOverText, size.width / 2, size.height / 2 - 40f, subPaint)
                    drawText(context.getString(com.vbrosseau.stackgame.R.string.tap_to_restart), size.width / 2, size.height / 2 + 20f, subPaint)
                    
                     if (user.hasRewindFeature()) {
                        drawText(
                            context.getString(com.vbrosseau.stackgame.R.string.long_press_rewind), 
                            size.width / 2, 
                            size.height / 2 + 100f, 
                            subPaint
                        )
                    }
                }
            }
        }
        
        // Header Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
                .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "👤",
                fontSize = 28.sp,
                color = Color.White,
                modifier = Modifier
                    .clickable { onLoginClick() }
                    .padding(4.dp)
            )

            Text(
                text = gameState.score.toString(),
                fontSize = 32.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "❤ ".repeat(gameState.lives.coerceAtLeast(0)).trim(),
                fontSize = 28.sp,
                color = Color(0xFFFF5252)
            )
        }
    }
    
    // Ads and Milestone Overlays
    
    if (gameState.showAdOverlay && user.showsAds()) {
        // Local state for timer, or move to VM? 
        // VM has the flag, but timer is UI-centric for dismissal.
        // Let's keep timer in UI for now as it's purely for the "X" button enablement.
        var adTimerRemaining by remember { mutableLongStateOf(GameViewModel.AD_BLOCKING_DURATION) }
        
        LaunchedEffect(gameState.showAdOverlay) {
             adTimerRemaining = GameViewModel.AD_BLOCKING_DURATION
             while (adTimerRemaining > 0) {
                delay(100)
                adTimerRemaining -= 100
             }
        }
        
        GameOverAdOverlay(
            isGuest = user.isGuest(),
            canClose = adTimerRemaining <= 0,
            timeRemaining = (adTimerRemaining / 1000f).toInt() + 1,
            onDismiss = {
                // We need to tell VM we dismissed
                // VM resetGame() will hide it, but we need separate action?
                // VM handleTap calls resetGame which clears flag if set?
                // Implementation in VM: resetGame() sets showAdOverlay = false
                viewModel.hideAdOverlay() // We need this exposed or use resetGame
                viewModel.resetGame() // Explicitly reset
            },
            onPurchaseClick = onPurchaseClick
        )
    }
    
    if (gameState.showMilestoneCelebration) {
        MilestoneCelebration(
            firstName = user.displayName,
            score = gameState.celebrationScore,
            onDismiss = { viewModel.dismissMilestone() }
        )
    }
}
