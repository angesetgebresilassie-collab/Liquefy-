package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.glassengine.engine.GlassConfig
import com.example.glassengine.engine.GlassStyle
import com.example.glassengine.engine.glassmorphicSurface
import com.example.ui.StudioUiState
import com.example.ui.components.CodeBlockView
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPillBadge
import com.example.ui.components.GlassSliderControl
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassBorderWhite
import com.example.ui.theme.GlassCyan
import com.example.ui.theme.GlassObsidian
import com.example.ui.theme.GlassObsidianCard
import com.example.ui.theme.GlassObsidianSurface
import com.example.ui.theme.GlassPink
import com.example.ui.theme.GlassPurple
import com.example.ui.theme.GlassViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

/**
 * Component 1 Interactive Laboratory:
 * Real-time AGSL Liquid Glass & RenderEffect Frosted Glass playground
 * with animated canvas backdrop, interactive parameter tuning, and AGSL shader script inspection.
 */
@Composable
fun LiveGlassPlaygroundScreen(
    uiState: StudioUiState,
    onStyleSelected: (GlassStyle) -> Unit,
    onConfigChange: (GlassConfig) -> Unit,
    onToggleAnimation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showShaderSource by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "LiquidOrbit")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitPhase"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Liquid Glass Shader Lab",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    GlassPillBadge(
                        text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "AGSL API 33+" else "RenderEffect API 31+",
                        color = GlassCyan
                    )
                }
                Text(
                    text = "Hardware-accelerated UV lens refraction, chromatic dispersion & specular highlights.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }

            IconButton(
                onClick = { showShaderSource = !showShaderSource },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GlassObsidianSurface)
                    .border(1.dp, GlassBorderWhite, CircleShape)
                    .testTag("toggle_shader_source_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "View Shader Code",
                    tint = if (showShaderSource) GlassCyan else TextSecondary
                )
            }
        }

        // AGSL Shader Source Disclosure
        AnimatedVisibility(visible = showShaderSource) {
            CodeBlockView(
                code = uiState.shaderSourceCode,
                language = "glsl",
                title = "LiquidGlassShader.agsl (API 33+)",
                maxLines = 45,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Live Animated Glass Viewport
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF070B14))
            ) {
                // Animated canvas background with floating neon energy orbs & telemetry grid
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val phase = if (uiState.isAnimationActive) animProgress else 0f

                    // Subtle background grid lines
                    val step = 32.dp.toPx()
                    var x = 0f
                    while (x < w) {
                        drawLine(
                            color = Color(0x1500F0FF),
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1f
                        )
                        x += step
                    }
                    var y = 0f
                    while (y < h) {
                        drawLine(
                            color = Color(0x1500F0FF),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                        y += step
                    }

                    // Floating animated energy orbs
                    val orb1X = w * 0.35f + cos(phase) * 80.dp.toPx()
                    val orb1Y = h * 0.45f + sin(phase) * 60.dp.toPx()
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xE600F0FF), Color(0x4D00F0FF), Color.Transparent),
                            center = Offset(orb1X, orb1Y),
                            radius = 110.dp.toPx()
                        ),
                        radius = 110.dp.toPx(),
                        center = Offset(orb1X, orb1Y)
                    )

                    val orb2X = w * 0.65f + sin(phase * 1.3f) * 90.dp.toPx()
                    val orb2Y = h * 0.55f + cos(phase * 1.3f) * 70.dp.toPx()
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xE6A855F7), Color(0x4D7C3AED), Color.Transparent),
                            center = Offset(orb2X, orb2Y),
                            radius = 130.dp.toPx()
                        ),
                        radius = 130.dp.toPx(),
                        center = Offset(orb2X, orb2Y)
                    )

                    val orb3X = w * 0.5f + cos(phase * 0.7f) * 110.dp.toPx()
                    val orb3Y = h * 0.3f + sin(phase * 0.9f) * 50.dp.toPx()
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xCCF43F5E), Color(0x33F43F5E), Color.Transparent),
                            center = Offset(orb3X, orb3Y),
                            radius = 95.dp.toPx()
                        ),
                        radius = 95.dp.toPx(),
                        center = Offset(orb3X, orb3Y)
                    )
                }

                // Centered Glassmorphic Card (Applies Liquid/Frosted Glass RenderEffect & Specular Sheen)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(0.92f)
                        .glassmorphicSurface(
                            cornerRadius = uiState.liveGlassConfig.cornerRadiusDp.dp,
                            blurRadius = uiState.liveGlassConfig.blurRadiusX.dp,
                            backgroundColor = Color(
                                red = uiState.liveGlassConfig.tintColorR,
                                green = uiState.liveGlassConfig.tintColorG,
                                blue = uiState.liveGlassConfig.tintColorB,
                                alpha = uiState.liveGlassConfig.tintAlpha
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.selectedGlassStyle.displayName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            GlassPillBadge(
                                text = "Active Engine",
                                icon = Icons.Default.AutoAwesome,
                                color = GlassCyan
                            )
                        }

                        Text(
                            text = uiState.selectedGlassStyle.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary.copy(alpha = 0.9f),
                                lineHeight = 16.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassPillBadge(
                                text = "Refraction: ${"%.2f".format(uiState.liveGlassConfig.refraction)}",
                                color = GlassCyan
                            )
                            GlassPillBadge(
                                text = "Specular: ${"%.2f".format(uiState.liveGlassConfig.specular)}",
                                color = GlassPurple
                            )
                            GlassPillBadge(
                                text = "Blur: ${uiState.liveGlassConfig.blurRadiusX.toInt()}dp",
                                color = GlassPink
                            )
                        }
                    }
                }
            }
        }

        // Glass Presets Selector
        Text(
            text = "Glass Theme Presets",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(GlassStyle.values()) { style ->
                val isSelected = style == uiState.selectedGlassStyle
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onStyleSelected(style) }
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) GlassCyan else GlassBorderWhite,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .testTag("preset_${style.name}"),
                    color = if (isSelected) GlassCyan.copy(alpha = 0.15f) else GlassObsidianSurface,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = style.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) GlassCyan else TextPrimary
                            )
                        )
                    }
                }
            }
        }

        // Real-Time Shader Parameters Tuning
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = GlassCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Shader Parameters",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Button(
                    onClick = onToggleAnimation,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isAnimationActive) GlassCyan.copy(alpha = 0.2f) else GlassObsidianSurface,
                        contentColor = GlassCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("toggle_anim_btn")
                ) {
                    Text(
                        text = if (uiState.isAnimationActive) "Pause Motion" else "Play Motion",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlassSliderControl(
                title = "Refraction Index (UV Lens Distortion)",
                value = uiState.liveGlassConfig.refraction,
                onValueChange = { onConfigChange(uiState.liveGlassConfig.copy(refraction = it)) },
                valueRange = 0.0f..1.0f,
                accentColor = GlassCyan,
                modifier = Modifier.testTag("slider_refraction")
            )

            GlassSliderControl(
                title = "Specular Edge Highlight (Rim Shine)",
                value = uiState.liveGlassConfig.specular,
                onValueChange = { onConfigChange(uiState.liveGlassConfig.copy(specular = it)) },
                valueRange = 0.0f..1.5f,
                accentColor = GlassPurple,
                modifier = Modifier.testTag("slider_specular")
            )

            GlassSliderControl(
                title = "Chromatic Aberration (RGB Dispersion)",
                value = uiState.liveGlassConfig.chromaticAberration,
                onValueChange = { onConfigChange(uiState.liveGlassConfig.copy(chromaticAberration = it)) },
                valueRange = 0.0f..1.0f,
                accentColor = GlassPink,
                modifier = Modifier.testTag("slider_chromatic")
            )

            GlassSliderControl(
                title = "RenderEffect Blur Radius",
                value = uiState.liveGlassConfig.blurRadiusX,
                onValueChange = {
                    onConfigChange(
                        uiState.liveGlassConfig.copy(
                            blurRadiusX = it,
                            blurRadiusY = it
                        )
                    )
                },
                valueRange = 1.0f..60.0f,
                valueFormatter = { "${it.toInt()} dp" },
                accentColor = GlassCyan,
                modifier = Modifier.testTag("slider_blur")
            )

            GlassSliderControl(
                title = "Glass Tint Alpha",
                value = uiState.liveGlassConfig.tintAlpha,
                onValueChange = { onConfigChange(uiState.liveGlassConfig.copy(tintAlpha = it)) },
                valueRange = 0.02f..0.50f,
                accentColor = GlassPurple,
                modifier = Modifier.testTag("slider_alpha")
            )
        }
    }
}
