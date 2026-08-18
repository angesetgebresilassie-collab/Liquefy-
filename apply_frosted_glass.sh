#!/bin/bash
set -e

# Target directory setup
SRC_DIR="app/src/main/java/com/example/glassengine"
mkdir -p "$SRC_DIR/agsl" "$SRC_DIR/engine" "$SRC_DIR/ui"

# Create LiquidGlassShader.kt
cat << 'EOF' > "$SRC_DIR/agsl/LiquidGlassShader.kt"
package com.example.glassengine.agsl

import android.graphics.RuntimeShader

object LiquidGlassShader {
    const val SHADER_SRC = """
        uniform shader composable;
        uniform float2 resolution;
        uniform float blurRadius;
        uniform float4 tintColor;
        uniform float noiseAmount;

        float rand(float2 co) {
            return fract(sin(dot(co, float2(12.9898, 78.233))) * 43758.5453);
        }

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            half4 color = half4(0.0);
            float totalWeight = 0.0;
            float radius = max(1.0, blurRadius);

            for (float x = -radius; x <= radius; x += 2.0) {
                for (float y = -radius; y <= radius; y += 2.0) {
                    float2 offset = float2(x, y);
                    color += composable.eval(fragCoord + offset);
                    totalWeight += 1.0;
                }
            }
            color /= totalWeight;

            float noise = (rand(uv) - 0.5) * noiseAmount;
            color.rgb += noise;
            color.rgb = mix(color.rgb, tintColor.rgb, tintColor.a);

            return color;
        }
    """

    fun createShader(): RuntimeShader = RuntimeShader(SHADER_SRC)
}
EOF

# Create GlassStyle.kt
cat << 'EOF' > "$SRC_DIR/engine/GlassStyle.kt"
package com.example.glassengine.engine

import androidx.compose.ui.graphics.Color

data class GlassStyle(
    val blurRadius: Float = 16f,
    val tintColor: Color = Color(0x40FFFFFF),
    val noiseAmount: Float = 0.04f
)
EOF

# Create FrostedGlassBox.kt
cat << 'EOF' > "$SRC_DIR/ui/FrostedGlassBox.kt"
package com.example.glassengine.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.glassengine.agsl.LiquidGlassShader
import com.example.glassengine.engine.GlassStyle

@Composable
fun FrostedGlassBox(
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassStyle(),
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { LiquidGlassShader.createShader() }
        Box(
            modifier = modifier
                .clip(shape)
                .graphicsLayer {
                    shader.setFloatUniform("blurRadius", style.blurRadius)
                    shader.setFloatUniform("tintColor", style.tintColor.red, style.tintColor.green, style.tintColor.blue, style.tintColor.alpha)
                    shader.setFloatUniform("noiseAmount", style.noiseAmount)
                    renderEffect = RenderEffect
                        .createRuntimeShaderEffect(shader, "composable")
                        .asComposeRenderEffect()
                }
                .border(1.dp, Color.White.copy(alpha = 0.3f), shape)
        ) { content() }
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .background(style.tintColor)
                .border(1.dp, Color.White.copy(alpha = 0.3f), shape)
        ) { content() }
    }
}
EOF

echo "Source files and shaders generated."

