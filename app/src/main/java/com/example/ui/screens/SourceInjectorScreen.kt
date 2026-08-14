package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.glassengine.engine.GlassStyle
import com.example.glassengine.injector.SourceInjectorOptions
import com.example.ui.StudioUiState
import com.example.ui.components.CodeBlockView
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPillBadge
import com.example.ui.components.GlassSwitchRow
import com.example.ui.theme.GlassBorderWhite
import com.example.ui.theme.GlassCyan
import com.example.ui.theme.GlassEmerald
import com.example.ui.theme.GlassObsidianSurface
import com.example.ui.theme.GlassPink
import com.example.ui.theme.GlassPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class SourceViewTab {
    INJECTED_SOURCE,
    ORIGINAL_SOURCE,
    INSPECTION_LOGS
}

/**
 * Component 3: Kotlin/Java Source Code Injector Studio.
 * Scans Activities and Fragments, detects setContentView / onCreateView / setContent anchors,
 * and injects GlassThemeEngine initialization hooks with compile-safe SDK version guards.
 */
@Composable
fun SourceInjectorScreen(
    uiState: StudioUiState,
    onSampleSelect: (Int) -> Unit,
    onOptionsChange: (SourceInjectorOptions) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var activeTab by remember { mutableStateOf(SourceViewTab.INJECTED_SOURCE) }
    val result = uiState.sourceInjectionResult

    val samples = listOf(
        "Kotlin Activity" to "MainActivity.kt",
        "Java Activity" to "DashboardActivity.java",
        "Kotlin Fragment" to "ProfileFragment.kt"
    )

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
                        text = "Source Code Injector",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    GlassPillBadge(text = "Kotlin & Java AST", color = GlassCyan)
                }
                Text(
                    text = "Automatically locates setContentView/onCreateView & injects GlassThemeEngine hooks.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }

        // Sample Switcher
        Text(
            text = "Select Component Sample",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(samples) { index, (label, fileName) ->
                val isSelected = index == uiState.selectedSourceSampleIndex
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSampleSelect(index) }
                        .border(
                            1.dp,
                            if (isSelected) GlassCyan else GlassBorderWhite,
                            RoundedCornerShape(12.dp)
                        )
                        .testTag("source_sample_$index"),
                    color = if (isSelected) GlassCyan.copy(alpha = 0.15f) else GlassObsidianSurface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) GlassCyan else TextPrimary
                            )
                        )
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }

        // Configuration Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = GlassCyan, modifier = Modifier.size(18.dp))
                Text(
                    text = "Injection Settings",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            GlassSwitchRow(
                title = "Android SDK Version Guard",
                description = "Wraps injection in 'if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)' for backward compatibility.",
                checked = uiState.sourceOptions.includeSdkGuard,
                onCheckedChange = {
                    onOptionsChange(uiState.sourceOptions.copy(includeSdkGuard = it))
                },
                modifier = Modifier.testTag("switch_sdk_guard")
            )

            Divider(color = Color(0x15FFFFFF), thickness = 1.dp)

            GlassSwitchRow(
                title = "Auto-Inject Import Statements",
                description = "Automatically adds required GlassThemeEngine & GlassStyle imports after package declaration.",
                checked = uiState.sourceOptions.injectImports,
                onCheckedChange = {
                    onOptionsChange(uiState.sourceOptions.copy(injectImports = it))
                },
                modifier = Modifier.testTag("switch_inject_imports")
            )
        }

        // Status & Metadata Badges
        if (result != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp,
                    backgroundColor = GlassObsidianSurface
                ) {
                    Text(text = "Language", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                    Text(
                        text = result.detectedLanguage.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = GlassCyan)
                    )
                }

                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp,
                    backgroundColor = GlassObsidianSurface
                ) {
                    Text(text = "Component", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                    Text(
                        text = result.detectedComponent.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = GlassPurple)
                    )
                }

                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp,
                    backgroundColor = GlassObsidianSurface
                ) {
                    Text(text = "Status", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                    Text(
                        text = if (result.wasInjected) "Injected" else "Unchanged",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (result.wasInjected) GlassEmerald else GlassPink
                        )
                    )
                }
            }
        }

        // Navigation Tabs
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
                selected = activeTab == SourceViewTab.INJECTED_SOURCE,
                onClick = { activeTab = SourceViewTab.INJECTED_SOURCE },
                text = { Text("Injected Code", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == SourceViewTab.ORIGINAL_SOURCE,
                onClick = { activeTab = SourceViewTab.ORIGINAL_SOURCE },
                text = { Text("Original Code", fontSize = 12.sp) }
            )
            Tab(
                selected = activeTab == SourceViewTab.INSPECTION_LOGS,
                onClick = { activeTab = SourceViewTab.INSPECTION_LOGS },
                text = { Text("Injection Log", fontSize = 12.sp) }
            )
        }

        when (activeTab) {
            SourceViewTab.INJECTED_SOURCE -> {
                CodeBlockView(
                    code = result?.injectedSource ?: uiState.rawSourceInput,
                    language = if (uiState.sourceFileName.endsWith(".java")) "java" else "kotlin",
                    title = "${uiState.sourceFileName} (Injected by GlassMorph)",
                    modifier = Modifier.testTag("injected_source_view")
                )
            }
            SourceViewTab.ORIGINAL_SOURCE -> {
                CodeBlockView(
                    code = uiState.rawSourceInput,
                    language = if (uiState.sourceFileName.endsWith(".java")) "java" else "kotlin",
                    title = uiState.sourceFileName,
                    modifier = Modifier.testTag("original_source_view")
                )
            }
            SourceViewTab.INSPECTION_LOGS -> {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "AST Hook Detection Details",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (result?.logMessages.isNullOrEmpty()) {
                        Text(
                            text = "No log messages.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            result?.logMessages?.forEach { log ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = GlassCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = log,
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
