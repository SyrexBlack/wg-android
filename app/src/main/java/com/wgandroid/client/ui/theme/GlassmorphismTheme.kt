package com.wgandroid.client.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Glassmorphism цвета
object GlassColors {
    val glassWhite = Color(0x40FFFFFF)
    val glassWhiteStrong = Color(0x60FFFFFF)  
    val glassBlack = Color(0x30000000)
    val glassBorder = Color(0x80FFFFFF)
    val glassAccent = Color(0x70FF6B6B)
    val glassAccentBlue = Color(0x704FC3F7)
    val glassAccentGreen = Color(0x7081C784)
    val glassAccentPurple = Color(0x70BA68C8)
    
    // Основные цвета для UI
    val primaryGradientStart = Color(0xFF6366F1)
    val primaryGradientEnd = Color(0xFF8B5CF6)
    val accentBlue = Color(0xFF3B82F6)
    val accentGreen = Color(0xFF10B981)
    val accentPurple = Color(0xFF8B5CF6)
    val accentOrange = Color(0xFFEF6C00)
    
    // Градиенты для glassmorphism
    val glassGradient = Brush.linearGradient(
        colors = listOf(
            Color(0x40FFFFFF),
            Color(0x20FFFFFF),
            Color(0x10FFFFFF)
        )
    )
    
    val glassGradientBlue = Brush.linearGradient(
        colors = listOf(
            Color(0x502196F3),
            Color(0x301976D2),
            Color(0x101565C0)
        )
    )
    
    val glassGradientPurple = Brush.linearGradient(
        colors = listOf(
            Color(0x509C27B0),
            Color(0x30673AB7),
            Color(0x10512DA8)
        )
    )
    
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF667eea),
            Color(0xFF764ba2),
            Color(0xFF667eea)
        )
    )
}

// Glassmorphism модификаторы
object GlassModifiers {
    
    @Composable
    fun glassCard(
        cornerRadius: Int = 16,
        borderWidth: Int = 1,
        alpha: Float = 0.25f
    ): Modifier {
        return Modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alpha),
                        Color.White.copy(alpha = alpha * 0.5f)
                    )
                )
            )
            .border(
                width = borderWidth.dp,
                color = Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(cornerRadius.dp)
            )
    }
    
    @Composable 
    fun glassCardColored(
        gradient: Brush = GlassColors.glassGradientBlue,
        cornerRadius: Int = 16,
        borderWidth: Int = 1
    ): Modifier {
        return Modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(brush = gradient)
            .border(
                width = borderWidth.dp,
                color = Color.White.copy(alpha = 0.4f),
                shape = RoundedCornerShape(cornerRadius.dp)
            )
    }
    
    @Composable
    fun glassButton(
        cornerRadius: Int = 12,
        alpha: Float = 0.3f
    ): Modifier {
        return Modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alpha),
                        Color.White.copy(alpha = alpha * 0.7f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.5f),
                shape = RoundedCornerShape(cornerRadius.dp)
            )
    }
    
    @Composable
    fun glassTopBar(): Modifier {
        return Modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                )
            )
            .border(
                width = (0.5).dp,
                color = Color.White.copy(alpha = 0.3f)
            )
    }
    
    @Composable
    fun glassFAB(): Modifier {
        return Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.2f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
    }
} 