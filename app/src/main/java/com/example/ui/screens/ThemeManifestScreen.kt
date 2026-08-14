package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StudioUiState
import com.example.ui.components.CodeBlockView
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPillBadge
import com.example.ui.theme.GlassBorderWhite
import com.example.ui.theme.GlassCyan
import com.example.ui.theme.GlassEmerald
import com.example.ui.theme.GlassObsidianSurface
import com.example.ui.theme.GlassPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class ThemeManifestTab {
    THEMES_XML,
    MANIFEST_XML
}

/**
 * Component 4: Theme & Manifest Modifier Studio.
 * Patches styles.xml/themes.xml to enable windowIsTranslucent & transparent window backgrounds,
 * and ensures AndroidManifest.xml has hardware acceleration enabled.
 */
@Composable
fun ThemeManifestScreen(
    uiState: StudioUiState,
    onThemesChange: (String) -> Unit,
    onManifestChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var activeTab by remember { mutableStateOf(ThemeManifestTab.THEMES_XML) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
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
                        text = "Theme & Manifest Modifiers",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    GlassPillBadge(text = "GPU Acceleration", color = GlassEmerald)
                }
                Text(
                    text = "Enables translucent window compositing and GPU hardware acceleration.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }

        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassCard(
                modifier = Modifier.weight(1f),
                cornerRadius = 14.dp,
                backgroundColor = GlassObsidianSurface
            ) {
                Text(text = "Window Translucency", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                Text(
                    text = "windowIsTranslucent=true",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = GlassCyan)
                )
            }

            GlassCard(
                modifier = Modifier.weight(1f),
                cornerRadius = 14.dp,
                backgroundColor = GlassObsidianSurface
            ) {
                Text(text = "Hardware Acceleration", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                Text(
                    text = "hardwareAccelerated=true",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = GlassPurple)
                )
            }
        }

        // Tab Selector
        TabRow(
            selectedTabIndex = activeTab.ordinal,
            containerColor = GlassObsidianSurface,
            contentColor = GlassCyan,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]),
                    color = GlassCyan
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, GlassBorderWhite, RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = activeTab == ThemeManifestTab.THEMES_XML,
                onClick = { activeTab = ThemeManifestTab.THEMES_XML },
                text = { Text("themes.xml (Window Glass)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == ThemeManifestTab.MANIFEST_XML,
                onClick = { activeTab = ThemeManifestTab.MANIFEST_XML },
                text = { Text("AndroidManifest.xml (GPU Acceleration)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        when (activeTab) {
            ThemeManifestTab.THEMES_XML -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CodeBlockView(
                        code = uiState.patchedThemesResult?.patchedXml ?: uiState.rawThemesInput,
                        language = "xml",
                        title = "res/values/themes.xml (Patched by GlassMorph)",
                        maxLines = 30,
                        modifier = Modifier.testTag("patched_themes_view")
                    )

                    CodeBlockView(
                        code = uiState.rawThemesInput,
                        language = "xml",
                        title = "Original themes.xml",
                        maxLines = 20,
                        modifier = Modifier.testTag("original_themes_view")
                    )
                }
            }

            ThemeManifestTab.MANIFEST_XML -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CodeBlockView(
                        code = uiState.patchedManifestResult?.patchedXml ?: uiState.rawManifestInput,
                        language = "xml",
                        title = "AndroidManifest.xml (Patched with hardwareAccelerated=\"true\")",
                        maxLines = 35,
                        modifier = Modifier.testTag("patched_manifest_view")
                    )

                    CodeBlockView(
                        code = uiState.rawManifestInput,
                        language = "xml",
                        title = "Original AndroidManifest.xml",
                        maxLines = 25,
                        modifier = Modifier.testTag("original_manifest_view")
                    )
                }
            }
        }
    }
}
