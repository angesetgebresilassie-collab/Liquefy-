package com.example.glassengine.agsl

/**
 * Production-grade AGSL (Android Graphics Shading Language) Liquid Glass shader script for Android 13+ (API 33+).
 *
 * Capabilities:
 * - High-precision lens refraction and curved surface distortion based on normalized UV coordinates.
 * - Directional physical lighting model with specular rim/border highlights.
 * - Sub-pixel chromatic aberration (RGB dispersion) at refraction boundaries.
 * - Subtle ambient shimmer time modulation.
 * - Translucent tint overlay blending with physical glass luminance.
 */
object LiquidGlassShader {

    val SHADER_SOURCE: String = """
        uniform shader composable;
        uniform float2 resolution;
        uniform float refraction;
        uniform float specular;
        uniform float chromaticAberration;
        uniform float time;
        uniform float4 tintColor;

        // Fast pseudo-noise for micro-surface roughness
        float hash(float2 p) {
            float3 p3 = fract(float3(p.xyx) * 0.1031);
            p3 += dot(p3, p3.yzx + 33.33);
            return fract((p3.x + p3.y) * p3.z);
        }

        half4 main(float2 fragCoord) {
            // Normalize UV coordinates [0.0, 1.0]
            float2 uv = fragCoord / resolution;
            
            // Centered coordinates [-0.5, 0.5] with aspect correction
            float aspect = resolution.x / resolution.y;
            float2 centered = uv - 0.5;
            float2 p = float2(centered.x * aspect, centered.y);
            
            // Distance from center for radial lens profile
            float dist = length(p);
            
            // Calculate physical lens surface curvature normal (convex glass bubble)
            float dome = sqrt(max(0.0, 0.5 * 0.5 - dist * dist));
            float2 lensNormal = normalize(float2(p.x, p.y)) * (dist * 2.0);
            
            // Dynamic subtle flow wave modulation
            float wave = sin(uv.x * 12.0 + time * 1.5) * cos(uv.y * 12.0 + time * 1.2) * 0.003;
            
            // Refraction vector with edge falloff
            float refractionStrength = refraction * 0.08;
            float2 offset = (lensNormal + wave) * refractionStrength;
            
            // Calculate chromatic dispersion by sampling R, G, B channels with coordinate offsets
            float dispersion = chromaticAberration * 0.015;
            float2 uvR = uv + offset + lensNormal * dispersion;
            float2 uvG = uv + offset;
            float2 uvB = uv + offset - lensNormal * dispersion;
            
            // Sample base composable texture with clamp boundaries
            float4 colorR = composable.eval(clamp(uvR, 0.0, 1.0) * resolution);
            float4 colorG = composable.eval(clamp(uvG, 0.0, 1.0) * resolution);
            float4 colorB = composable.eval(clamp(uvB, 0.0, 1.0) * resolution);
            
            float4 refractedColor = float4(colorR.r, colorG.g, colorB.b, colorG.a);
            
            // Directional specular light source (top-left key light)
            float3 lightDir = normalize(float3(-0.55, -0.65, 0.52));
            float3 surfaceNormal = normalize(float3(lensNormal.x, lensNormal.y, max(0.2, dome)));
            float NdotL = max(0.0, dot(surfaceNormal, lightDir));
            
            // Specular hotspot shine
            float specPower = pow(NdotL, 16.0) * specular * 1.4;
            
            // Specular border / rim highlight simulating rounded physical glass edges (Fresnel)
            float edgeDistX = min(uv.x, 1.0 - uv.x);
            float edgeDistY = min(uv.y, 1.0 - uv.y);
            float edgeDist = min(edgeDistX, edgeDistY);
            
            // Sharp beveled highlight at the perimeter
            float rimHighlight = smoothstep(0.035, 0.002, edgeDist) * specular * 0.85;
            
            // Top-left physical reflection bevel
            float cornerGleam = smoothstep(0.12, 0.0, length(uv - float2(0.08, 0.08))) * specular * 0.6;
            
            // Blend glass tint color
            float4 tinted = mix(refractedColor, tintColor, tintColor.a);
            
            // Add specular highlights (white gleams)
            float totalHighlight = specPower + rimHighlight + cornerGleam;
            float3 finalRgb = tinted.rgb + float3(totalHighlight);
            
            return half4(clamp(finalRgb, 0.0, 1.0), tinted.a);
        }
    """.trimIndent()
}
