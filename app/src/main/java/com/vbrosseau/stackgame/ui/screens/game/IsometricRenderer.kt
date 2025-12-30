package com.vbrosseau.stackgame.ui.screens.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Isometric 3D renderer for the Stack game.
 * 
 * Transforms 2D game coordinates to isometric 3D screen coordinates.
 * The game logic remains 2D, only the visual rendering is 3D.
 */
object IsometricRenderer {
    
    /**
     * Height of the 3D extrusion (how tall the blocks appear)
     * Increased significantly for visible 3D effect
     */
    const val BLOCK_DEPTH = 50f
    
    /**
     * Isometric angle factor (for 2:1 isometric ratio)
     * tan(26.57°) ≈ 0.5
     */
    private const val ISO_FACTOR = 0.5f
    
    /**
     * Draws an isometric 3D block.
     * 
     * The block is rendered with:
     * - A front face (main color)
     * - A top face (lighter, angled)
     * - A right side face (darker)
     * 
     * @param left Left edge in game coordinates
     * @param top Top edge in game coordinates
     * @param width Block width
     * @param height Block height
     * @param color Base color
     * @param screenCenterX Center of screen (for centering the isometric view)
     * @param hasWindows Whether to draw windows
     * @param windowColor Color for windows
     * @param windowCount Number of windows
     * @param isUnstable Show instability indicator
     */
    fun DrawScope.drawIsometricBlock(
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        color: Color,
        screenCenterX: Float,
        hasWindows: Boolean = false,
        windowColor: Color = Color.Black,
        windowCount: Int = 0,
        isUnstable: Boolean = false
    ) {
        // Isometric projection offset - much larger for visible effect
        val isoOffsetX = BLOCK_DEPTH
        val isoOffsetY = BLOCK_DEPTH * ISO_FACTOR
        
        // Use original positions (no adjustment)
        val adjustedLeft = left
        val adjustedRight = left + width
        
        // Front face corners
        val frontTopLeft = Offset(adjustedLeft, top)
        val frontTopRight = Offset(adjustedRight, top)
        val frontBottomLeft = Offset(adjustedLeft, top + height)
        val frontBottomRight = Offset(adjustedRight, top + height)
        
        // Top face back corners (shifted for 3D effect)
        val backTopLeft = Offset(adjustedLeft + isoOffsetX, top - isoOffsetY)
        val backTopRight = Offset(adjustedRight + isoOffsetX, top - isoOffsetY)
        
        // Right face back corner
        val backBottomRight = Offset(adjustedRight + isoOffsetX, top + height - isoOffsetY)
        
        // Calculate colors for each face
        val topColor = color.copy(
            red = (color.red * 1.2f).coerceAtMost(1f),
            green = (color.green * 1.2f).coerceAtMost(1f),
            blue = (color.blue * 1.2f).coerceAtMost(1f)
        )
        val rightColor = color.copy(
            red = color.red * 0.6f,
            green = color.green * 0.6f,
            blue = color.blue * 0.6f
        )
        
        // Draw RIGHT face (darkest, behind)
        val rightPath = Path().apply {
            moveTo(frontTopRight.x, frontTopRight.y)
            lineTo(backTopRight.x, backTopRight.y)
            lineTo(backBottomRight.x, backBottomRight.y)
            lineTo(frontBottomRight.x, frontBottomRight.y)
            close()
        }
        drawPath(rightPath, rightColor)
        
        // Draw TOP face (lighter, angled)
        val topPath = Path().apply {
            moveTo(frontTopLeft.x, frontTopLeft.y)
            lineTo(backTopLeft.x, backTopLeft.y)
            lineTo(backTopRight.x, backTopRight.y)
            lineTo(frontTopRight.x, frontTopRight.y)
            close()
        }
        drawPath(topPath, topColor)
        
        // Draw FRONT face (main color)
        val frontPath = Path().apply {
            moveTo(frontTopLeft.x, frontTopLeft.y)
            lineTo(frontTopRight.x, frontTopRight.y)
            lineTo(frontBottomRight.x, frontBottomRight.y)
            lineTo(frontBottomLeft.x, frontBottomLeft.y)
            close()
        }
        drawPath(frontPath, color)
        
        // Highlight on front face top edge
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = frontTopLeft,
            end = frontTopRight,
            strokeWidth = 2f
        )
        
        // Edge definition
        drawLine(
            color = Color.Black.copy(alpha = 0.2f),
            start = frontTopRight,
            end = backTopRight,
            strokeWidth = 1f
        )
        
        // Draw windows on front face if requested
        if (hasWindows && windowCount > 0) {
            val windowHeight = height * 0.5f
            val windowY = top + (height - windowHeight) / 2
            val totalGaps = width * 0.3f
            val gapWidth = totalGaps / (windowCount + 1)
            val winWidth = (width - totalGaps) / windowCount
            
            for (i in 0 until windowCount) {
                val winX = adjustedLeft + ((i + 1) * gapWidth) + (i * winWidth)
                drawRect(
                    color = windowColor,
                    topLeft = Offset(winX, windowY),
                    size = androidx.compose.ui.geometry.Size(winWidth, windowHeight)
                )
            }
        }
        
        // Unstable indicator
        if (isUnstable) {
            drawPath(frontPath, Color.Red.copy(alpha = 0.15f))
        }
    }
}
