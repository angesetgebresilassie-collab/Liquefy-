package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.glassengine.engine.glassmorphicSurface
import com.example.glassengine.transformer.XmlLayoutTransformerOptions
import com.example.ui.StudioUiState
import com.example.ui.components.CodeBlockView
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPillBadge
import com.example.ui.components.GlassSwitchRow
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassBorderWhite
import com.example.ui.theme.GlassCyan
import com.example.ui.theme.GlassEmerald
import com.example.ui.theme.GlassObsidianCard
import com.example.ui.theme.GlassObsidianSurface
import com.example.ui.theme.GlassPink
import com.example.ui.theme.GlassPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class XmlViewMode {
    TRANSFORMED,
    ORIGINAL,
    AST_LOGS,
    DRAWABLES
}

/**
 * Component 2: XML Layout Transformer Studio.
 * Live layout transmution, AST modifications, solid-to-translucent background conversion,
 * rounded glass drawable injection, and companion drawable inspection.
 */
@Composable
fun XmlTransformerScreen(
    uiState: StudioUiState,
    onSampleSelect: (Int) -> Unit,
    onXmlInputChange: (String) -> Unit,
    onOptionsChange: (XmlLayoutTransformerOptions) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var viewMode by remember { mutableStateOf(XmlViewMode.TRANSFORMED) }
    val result = uiState.xmlTransformationResult

    val samples = listOf(
        "ConstraintLayout Telemetry",
        "LinearLayout Login Form"
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
                        text = "XML Layout Transformer",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    GlassPillBadge(text = "DOM AST Engine", color = GlassPurple)
                }
                Text(
                    text = "Transmutes solid backgrounds to translucent alpha, injects glass drawables & wraps roots.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }

        // Sample Switcher
        Text(
            text = "Select Layout Sample",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(samples) { index, sampleName ->
                val isSelected = index == uiState.selectedXmlSampleIndex
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSampleSelect(index) }
                        .border(
                            1.dp,
                            if (isSelected) GlassCyan else GlassBorderWhite,
                            RoundedCornerShape(12.dp)
                        )
                        .testTag("xml_sample_$index"),
                    color = if (isSelected) GlassCyan.copy(alpha = 0.15f) else GlassObsidianSurface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = sampleName,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) GlassCyan else TextPrimary
                        )
                    )
                }
            }
        }

        // AST Options Configuration Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = GlassCyan, modifier = Modifier.size(18.dp))
                Text(
                    text = "AST Transformation Rules",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            GlassSwitchRow(
                title = "1. Wrap Top-Level Root in Glass Container",
                description = "Encloses root view in a FrameLayout with @drawable/bg_glass_surface while preserving bounds.",
                checked = uiState.xmlOptions.wrapRootInGlassContainer,
                onCheckedChange = {
                    onOptionsChange(uiState.xmlOptions.copy(wrapRootInGlassContainer = it))
                },
                modifier = Modifier.testTag("switch_wrap_root")
            )

            Divider(color = Color(0x15FFFFFF), thickness = 1.dp)

            GlassSwitchRow(
                title = "2. Transmute Solid Backgrounds (#FFFFFF -> #26FFFFFF)",
                description = "Converts opaque color hexes and theme colors into translucent alpha values for glass blur pass-through.",
                checked = uiState.xmlOptions.convertSolidBackgrounds,
                onCheckedChange = {
                    onOptionsChange(uiState.xmlOptions.copy(convertSolidBackgrounds = it))
                },
                modifier = Modifier.testTag("switch_solid_bg")
            )

            Divider(color = Color(0x15FFFFFF), thickness = 1.dp)

            GlassSwitchRow(
                title = "3. Inject Rounded Glass Drawables into Containers",
                description = "Injects '@drawable/bg_glass_card' and flattens CardViews to transparent for specular rendering.",
                checked = uiState.xmlOptions.injectRoundedGlassDrawables,
                onCheckedChange = {
                    onOptionsChange(uiState.xmlOptions.copy(injectRoundedGlassDrawables = it))
                },
                modifier = Modifier.testTag("switch_inject_drawables")
            )
        }

        // Metrics Summary
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
                    Text(text = "Backgrounds", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                    Text(
                        text = "${result.backgroundsModifiedCount} Changed",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = GlassCyan)
                    )
                }

                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp,
                    backgroundColor = GlassObsidianSurface
                ) {
                    Text(text = "Drawables", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                    Text(
                        text = "${result.drawablesInjectedCount} Injected",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = GlassPurple)
                    )
                }

                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp,
                    backgroundColor = GlassObsidianSurface
                ) {
                    Text(text = "Root Container", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                    Text(
                        text = if (result.rootWrapped) "Wrapped" else "Preserved",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = GlassEmerald)
                    )
                }
            }
        }

        // View Mode Navigation Tabs
        TabRow(
            selectedTabIndex = viewMode.ordinal,
            containerColor = GlassObsidianSurface,
            contentColor = GlassCyan,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[viewMode.ordinal]),
                    color = GlassCyan
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, GlassBorderWhite, RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = viewMode == XmlViewMode.TRANSFORMED,
                onClick = { viewMode = XmlViewMode.TRANSFORMED },
                text = { Text("Transformed XML", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = viewMode == XmlViewMode.ORIGINAL,
                onClick = { viewMode = XmlViewMode.ORIGINAL },
                text = { Text("Original XML", fontSize = 12.sp) }
            )
            Tab(
                selected = viewMode == XmlViewMode.AST_LOGS,
                onClick = { viewMode = XmlViewMode.AST_LOGS },
                text = { Text("AST Log (${result?.changeLogs?.size ?: 0})", fontSize = 12.sp) }
            )
            Tab(
                selected = viewMode == XmlViewMode.DRAWABLES,
                onClick = { viewMode = XmlViewMode.DRAWABLES },
                text = { Text("Drawables (${result?.companionDrawables?.size ?: 0})", fontSize = 12.sp) }
            )
        }

        // Tab Content Display
        when (viewMode) {
            XmlViewMode.TRANSFORMED -> {
                CodeBlockView(
                    code = result?.transformedXml ?: uiState.rawXmlInput,
                    language = "xml",
                    title = "Transformed layout.xml (Glass Engine AST Output)",
                    modifier = Modifier.testTag("transformed_xml_view")
                )
            }
            XmlViewMode.ORIGINAL -> {
                CodeBlockView(
                    code = uiState.rawXmlInput,
                    language = "xml",
                    title = "Raw Uncompiled layout.xml",
                    modifier = Modifier.testTag("original_xml_view")
                )
            }
            XmlViewMode.AST_LOGS -> {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "AST Modification History",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (result?.changeLogs.isNullOrEmpty()) {
                        Text(
                            text = "No AST mutations registered.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            result?.changeLogs?.forEach { log ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0x12FFFFFF),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "<${log.nodeName}> ${log.nodeId ?: ""}",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GlassCyan
                                                )
                                            )
                                            GlassPillBadge(text = log.changeType, color = GlassPurple)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = log.detail,
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            XmlViewMode.DRAWABLES -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    result?.companionDrawables?.forEach { (name, content) ->
                        CodeBlockView(
                            code = content,
                            language = "xml",
                            title = name,
                            maxLines = 25
                        )
                    }
                }
            }
        }
    }
}
