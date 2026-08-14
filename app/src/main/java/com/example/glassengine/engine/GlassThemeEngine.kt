package com.example.glassengine.engine

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.glassengine.agsl.LiquidGlassShader

/**
 * Main runtime engine for applying Frosted Glass and Liquid Glass shader effects
 * to Android Views and Jetpack Compose surfaces.
 */
object GlassThemeEngine {

    private const val TAG = "GlassThemeEngine"

    /**
     * Primary entry point to apply Glassmorphism effect to any Android View hierarchy.
     *
     * @param view The target root view (e.g. `window.decorView` or container View).
     * @param style The requested glass style (Frosted, Liquid, Neon, etc.).
     * @param config Custom configuration parameters for blur, refraction, and highlights.
     */
    @JvmStatic
    @JvmOverloads
    fun applyGlassEffect(
        view: View,
        style: GlassStyle = GlassStyle.LIQUID,
        config: GlassConfig = GlassConfig.DEFAULT
    ) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                applyApi33Glass(view, style, config)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                applyApi31Blur(view, config)
            }
            else -> {
                applyLegacyFallback(view, config)
            }
        }
    }

    /**
     * API 33+ (Tiramisu / Android 13+):
     * Compiles the AGSL Liquid Glass shader into a RuntimeShader and chains it with
     * RenderEffect.createBlurEffect.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun applyApi33Glass(view: View, style: GlassStyle, config: GlassConfig) {
        try {
            if (style == GlassStyle.FROSTED) {
                // Frosted glass purely uses high-fidelity gaussian blur
                val blurEffect = RenderEffect.createBlurEffect(
                    config.blurRadiusX.coerceAtLeast(1f),
                    config.blurRadiusY.coerceAtLeast(1f),
                    Shader.TileMode.CLAMP
                )
                view.setRenderEffect(blurEffect)
                return
            }

            // Liquid Glass: Instantiate AGSL RuntimeShader
            val runtimeShader = RuntimeShader(LiquidGlassShader.SHADER_SOURCE)
            
            // Set shader uniforms
            val width = if (view.width > 0) view.width.toFloat() else 1080f
            val height = if (view.height > 0) view.height.toFloat() else 1920f
            
            runtimeShader.setFloatUniform("resolution", width, height)
            runtimeShader.setFloatUniform("refraction", config.refraction)
            runtimeShader.setFloatUniform("specular", config.specular)
            runtimeShader.setFloatUniform("chromaticAberration", config.chromaticAberration)
            runtimeShader.setFloatUniform("time", (System.currentTimeMillis() % 100000L) / 1000f)
            runtimeShader.setFloatUniform(
                "tintColor",
                config.tintColorR,
                config.tintColorG,
                config.tintColorB,
                config.tintAlpha
            )

            // Create AGSL runtime shader effect
            val shaderEffect = RenderEffect.createRuntimeShaderEffect(runtimeShader, "composable")

            // Chain with underlying blur effect for true physical liquid glass depth
            val blurEffect = RenderEffect.createBlurEffect(
                config.blurRadiusX.coerceAtLeast(1f),
                config.blurRadiusY.coerceAtLeast(1f),
                Shader.TileMode.CLAMP
            )

            val chainedEffect = RenderEffect.createChainEffect(shaderEffect, blurEffect)
            view.setRenderEffect(chainedEffect)

            // Setup onLayout listener to dynamically update resolution uniform when view resizes
            view.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                val newW = (right - left).toFloat()
                val newH = (bottom - top).toFloat()
                if (newW > 0 && newH > 0 && (newW != (oldRight - oldLeft).toFloat() || newH != (oldBottom - oldTop).toFloat())) {
                    runtimeShader.setFloatUniform("resolution", newW, newH)
                }
            }

        } catch (e: Throwable) {
            Log.e(TAG, "Failed to compile AGSL shader; falling back to BlurEffect", e)
            applyApi31Blur(view, config)
        }
    }

    /**
     * API 31+ (Android 12 S):
     * Uses native RenderEffect blur.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyApi31Blur(view: View, config: GlassConfig) {
        try {
            val blurEffect = RenderEffect.createBlurEffect(
                config.blurRadiusX.coerceAtLeast(1f),
                config.blurRadiusY.coerceAtLeast(1f),
                Shader.TileMode.CLAMP
            )
            view.setRenderEffect(blurEffect)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to apply RenderEffect blur", e)
            applyLegacyFallback(view, config)
        }
    }

    /**
     * API < 31 Legacy fallback:
     * Injects a high-quality semi-transparent gradient background with white specular stroke.
     */
    private fun applyLegacyFallback(view: View, config: GlassConfig) {
        val density = view.resources.displayMetrics.density
        val cornerRadiusPx = config.cornerRadiusDp * density

        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusPx
            // Translucent glass fill
            setColor(android.graphics.Color.argb((config.tintAlpha * 255).toInt(), 255, 255, 255))
            // 1.5dp white specular stroke
            setStroke(
                (1.5f * density).toInt(),
                android.graphics.Color.argb((config.specular * 100).toInt().coerceIn(30, 200), 255, 255, 255)
            )
        }
        view.background = drawable
    }
}

/**
 * Extension function on [View] for idiomatic Kotlin glassmorphism invocation.
 */
fun View.applyGlassmorphism(
    style: GlassStyle = GlassStyle.LIQUID,
    config: GlassConfig = GlassConfig.DEFAULT
) {
    GlassThemeEngine.applyGlassEffect(this, style, config)
}

/**
 * Jetpack Compose modifier extension to apply glassmorphic visual rendering
 * with specular border highlights, gradient reflections, and optional AGSL / graphicsLayer effects.
 */
fun Modifier.glassmorphicSurface(
    cornerRadius: Dp = 20.dp,
    strokeWidth: Dp = 1.5.dp,
    backgroundColor: Color = Color(0x18FFFFFF),
    borderBrush: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0x99FFFFFF),
            Color(0x22FFFFFF),
            Color(0x05FFFFFF),
            Color(0x5500F0FF)
        )
    ),
    blurRadius: Dp = 20.dp
): Modifier = this
    .graphicsLayer {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val blurPx = blurRadius.toPx().coerceAtLeast(1f)
                val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && size.width > 0 && size.height > 0) {
                    try {
                        val shader = RuntimeShader(LiquidGlassShader.SHADER_SOURCE).apply {
                            setFloatUniform("resolution", size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))
                            setFloatUniform("refraction", 0.4f)
                            setFloatUniform("specular", 0.7f)
                            setFloatUniform("chromaticAberration", 0.3f)
                            setFloatUniform("time", (System.currentTimeMillis() % 100000L) / 1000f)
                            setFloatUniform("tintColor", 1f, 1f, 1f, 0.08f)
                        }
                        val shaderEffect = RenderEffect.createRuntimeShaderEffect(shader, "composable")
                        val blur = RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
                        RenderEffect.createChainEffect(shaderEffect, blur)
                    } catch (_: Throwable) {
                        RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
                    }
                } else {
                    RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
                }
                renderEffect = effect.asComposeRenderEffect()
            } catch (_: Throwable) {
                // Gracefully fallback to canvas drawBehind rendering
            }
        }
    }
    .drawBehind {
        val cr = cornerRadius.toPx()
        val sw = strokeWidth.toPx()
        
        // Draw glass background fill with subtle top-to-bottom light falloff
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    backgroundColor.copy(alpha = backgroundColor.alpha * 1.3f),
                    backgroundColor.copy(alpha = backgroundColor.alpha * 0.7f)
                )
            ),
            cornerRadius = CornerRadius(cr, cr)
        )
        
        // Draw specular physical glass border
        drawRoundRect(
            brush = borderBrush,
            cornerRadius = CornerRadius(cr, cr),
            style = Stroke(width = sw)
        )
        
        // Top-left physical reflection sheen line
        val sheenPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = sw,
                    top = sw,
                    right = size.width - sw,
                    bottom = size.height * 0.45f,
                    radiusX = cr,
                    radiusY = cr
                )
            )
        }
        drawPath(
            path = sheenPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0x40FFFFFF),
                    Color(0x05FFFFFF),
                    Color(0x00FFFFFF)
                ),
                start = Offset.Zero,
                end = Offset(0f, size.height * 0.45f)
            )
        )
    }
