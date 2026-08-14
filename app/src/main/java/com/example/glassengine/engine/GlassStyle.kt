package com.example.glassengine.engine

import androidx.compose.ui.graphics.Color

/**
 * Visual styling presets supported by the GlassThemeEngine.
 */
enum class GlassStyle(val displayName: String, val description: String) {
    FROSTED(
        displayName = "Frosted Glass",
        description = "Diffusion blur effect (API 31+) creating a smooth matte glass substrate."
    ),
    LIQUID(
        displayName = "Liquid Glass",
        description = "Chained AGSL RuntimeShader (API 33+) with UV lens refraction, chromatic dispersion, and physical specular rim highlights."
    ),
    CRYSTAL_CLEAR(
        displayName = "Crystal Clear",
        description = "Subtle refraction with ultra-crisp specular borders and minimal blur."
    ),
    TINTED_NEON(
        displayName = "Tinted Neon Glass",
        description = "Liquid refraction infused with a luminous cyan-violet neon tint."
    ),
    DEEP_ACRYLIC(
        displayName = "Deep Acrylic",
        description = "Heavy gaussian diffusion paired with directional glass bevel shine."
    )
}

/**
 * Configuration parameters for fine-tuning glassmorphism rendering.
 */
data class GlassConfig(
    val blurRadiusX: Float = 25f,
    val blurRadiusY: Float = 25f,
    val refraction: Float = 0.45f,
    val specular: Float = 0.75f,
    val chromaticAberration: Float = 0.35f,
    val tintColorR: Float = 1.0f,
    val tintColorG: Float = 1.0f,
    val tintColorB: Float = 1.0f,
    val tintAlpha: Float = 0.12f,
    val cornerRadiusDp: Float = 20f
) {
    companion object {
        val DEFAULT = GlassConfig()
        
        val FROSTED_DEFAULT = GlassConfig(
            blurRadiusX = 35f,
            blurRadiusY = 35f,
            refraction = 0.0f,
            specular = 0.3f,
            chromaticAberration = 0.0f,
            tintAlpha = 0.15f
        )
        
        val LIQUID_DEFAULT = GlassConfig(
            blurRadiusX = 20f,
            blurRadiusY = 20f,
            refraction = 0.55f,
            specular = 0.85f,
            chromaticAberration = 0.4f,
            tintAlpha = 0.10f
        )

        val NEON_DEFAULT = GlassConfig(
            blurRadiusX = 22f,
            blurRadiusY = 22f,
            refraction = 0.65f,
            specular = 0.95f,
            chromaticAberration = 0.6f,
            tintColorR = 0.2f,
            tintColorG = 0.85f,
            tintColorB = 1.0f,
            tintAlpha = 0.18f
        )
    }
}
