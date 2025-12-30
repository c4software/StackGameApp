package com.vbrosseau.stackgame.ui.screens.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

object IsometricUtils {
    
    // Isometric projection angle (30 degrees)
    private const val ISO_ANGLE = 0.5f // tan(26.57°) ≈ 0.5 for 2:1 ratio
    
    /**
     * Draw an isometric block (parallelepiped) with 3 visible faces
     * @param left Left edge X position
     * @param top Top edge Y position (in 2D screen space)
     * @param width Width of the block
     * @param height Height of the block (visual height in screen space)
     * @param depth Depth for isometric effect
     * @param color Base color of the block
     */
    fun DrawScope.drawIsometricBlock(
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        depth: Float,
        color: Color
    ) {
        val isoDepth = depth * ISO_ANGLE
        
        // Colors for different faces
        val topColor = color.copy(alpha = color.alpha) // Brightest
        val leftColor = Color(
            red = (color.red * 0.7f).coerceIn(0f, 1f),
            green = (color.green * 0.7f).coerceIn(0f, 1f),
            blue = (color.blue * 0.7f).coerceIn(0f, 1f),
            alpha = color.alpha
        )
        val rightColor = Color(
            red = (color.red * 0.5f).coerceIn(0f, 1f),
            green = (color.green * 0.5f).coerceIn(0f, 1f),
            blue = (color.blue * 0.5f).coerceIn(0f, 1f),
            alpha = color.alpha
        )
        
        // Top face (parallelogram)
        val topPath = Path().apply {
            moveTo(left, top)
            lineTo(left + width, top)
            lineTo(left + width + isoDepth, top - isoDepth)
            lineTo(left + isoDepth, top - isoDepth)
            close()
        }
        drawPath(topPath, topColor)
        
        // Front face (rectangle)
        val frontPath = Path().apply {
            moveTo(left, top)
            lineTo(left + width, top)
            lineTo(left + width, top + height)
            lineTo(left, top + height)
            close()
        }
        drawPath(frontPath, leftColor)
        
        // Right face (parallelogram)
        val rightPath = Path().apply {
            moveTo(left + width, top)
            lineTo(left + width + isoDepth, top - isoDepth)
            lineTo(left + width + isoDepth, top + height - isoDepth)
            lineTo(left + width, top + height)
            close()
        }
        drawPath(rightPath, rightColor)
    }
    
    /**
     * Draw windows on the front face of an isometric block
     */
    fun DrawScope.drawIsometricWindows(
        left: Float,
        top: Float,
        blockWidth: Float,
        blockHeight: Float,
        windowColor: Color,
        score: Int
    ) {
        val windowCount = (blockWidth / 35f).toInt()
        if (windowCount <= 0) return
        
        val windowHeightRatio = 0.5f
        val windowHeight = blockHeight * windowHeightRatio
        val windowY = top + (blockHeight - windowHeight) / 2
        val totalGapsWidth = blockWidth * 0.3f
        val gapWidth = totalGapsWidth / (windowCount + 1)
        val windowWidth = (blockWidth - totalGapsWidth) / windowCount
        
        val finalWindowColor = if (score > 25) {
            Color.Yellow.copy(alpha = 0.6f)
        } else {
            windowColor
        }
        
        for (i in 0 until windowCount) {
            val windowX = left + ((i + 1) * gapWidth) + (i * windowWidth)
            drawRect(
                color = finalWindowColor,
                topLeft = Offset(windowX, windowY),
                size = androidx.compose.ui.geometry.Size(windowWidth, windowHeight)
            )
        }
    }
    
    /**
     * Draw highlight on top of block for 3D effect
     */
    fun DrawScope.drawBlockHighlight(
        left: Float,
        top: Float,
        width: Float,
        depth: Float
    ) {
        val isoDepth = depth * ISO_ANGLE
        
        // Draw subtle highlight line on top edge
        val highlightPath = Path().apply {
            moveTo(left, top)
            lineTo(left + width, top)
            lineTo(left + width + isoDepth, top - isoDepth)
        }
        drawPath(
            highlightPath,
            Color.White.copy(alpha = 0.3f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
    }
}
