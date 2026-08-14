package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.glassengine.engine.GlassStyle
import com.example.glassengine.engine.GlassThemeEngine
import com.example.ui.GlassStudioViewModel
import com.example.ui.StudioTab
import com.example.ui.StudioUiState
import com.example.ui.components.GlassPillBadge
import com.example.ui.screens.LiveGlassPlaygroundScreen
import com.example.ui.screens.ProjectPipelineScreen
import com.example.ui.screens.SourceInjectorScreen
import com.example.ui.screens.ThemeManifestScreen
import com.example.ui.screens.XmlTransformerScreen
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassBorderWhite
import com.example.ui.theme.GlassCyan
import com.example.ui.theme.GlassObsidian
import com.example.ui.theme.GlassObsidianSurface
import com.example.ui.theme.GlassPurple
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                GlassMorphStudioApp()
            }
        }
    }
}

@Composable
fun GlassMorphStudioApp(
    viewModel: GlassStudioViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userNotice) {
        uiState.userNotice?.let { notice ->
            snackbarHostState.showSnackbar(notice)
            viewModel.clearNotice()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassObsidian),
        containerColor = GlassObsidian,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GlassStudioTopBar(currentTab = uiState.currentTab)
        },
        bottomBar = {
            GlassStudioBottomNav(
                currentTab = uiState.currentTab,
                onTabSelect = { viewModel.setTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(GlassObsidian)
        ) {
            when (uiState.currentTab) {
                StudioTab.LIVE_SHADER -> {
                    LiveGlassPlaygroundScreen(
                        uiState = uiState,
                        onStyleSelected = { viewModel.selectGlassStyle(it) },
                        onConfigChange = { viewModel.updateLiveConfig(it) },
                        onToggleAnimation = { viewModel.toggleAnimation() }
                    )
                }

                StudioTab.XML_TRANSFORMER -> {
                    XmlTransformerScreen(
                        uiState = uiState,
                        onSampleSelect = { viewModel.selectXmlSample(it) },
                        onXmlInputChange = { viewModel.updateRawXmlInput(it) },
                        onOptionsChange = { viewModel.updateXmlOptions(it) }
                    )
                }

                StudioTab.SOURCE_INJECTOR -> {
                    SourceInjectorScreen(
                        uiState = uiState,
                        onSampleSelect = { viewModel.selectSourceSample(it) },
                        onOptionsChange = { viewModel.updateSourceOptions(it) }
                    )
                }

                StudioTab.THEME_MANIFEST -> {
                    ThemeManifestScreen(
                        uiState = uiState,
                        onThemesChange = { viewModel.updateRawThemesInput(it) },
                        onManifestChange = { viewModel.updateRawManifestInput(it) }
                    )
                }

                StudioTab.PROJECT_PIPELINE -> {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    ProjectPipelineScreen(
                        uiState = uiState,
                        onRunPipeline = { viewModel.runProjectPipeline() },
                        onSelectFile = { viewModel.selectPipelineFile(it) },
                        onUploadZipUri = { uri, fileName ->
                            viewModel.processUploadedZipUri(context, uri, fileName)
                        },
                        onProcessSampleZip = {
                            viewModel.processSampleZip()
                        },
                        onExportZip = { destinationUri ->
                            viewModel.exportTransformedZip(context, destinationUri) { }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GlassStudioTopBar(currentTab: StudioTab) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = GlassObsidianSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Glass Prism Glow Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(GlassCyan, GlassPurple, Color(0xFFF43F5E))
                            )
                        )
                        .padding(1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(9.dp))
                            .background(GlassObsidian),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "GlassMorph Logo",
                            tint = GlassCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "GlassMorph Engine",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = currentTab.subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GlassCyan,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            GlassPillBadge(
                text = "v2.0 Native",
                color = GlassCyan
            )
        }
    }
}

@Composable
fun GlassStudioBottomNav(
    currentTab: StudioTab,
    onTabSelect: (StudioTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = GlassObsidianSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderWhite)
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier.height(64.dp)
        ) {
            val navItems = listOf(
                Triple(StudioTab.LIVE_SHADER, "Shader", Icons.Default.AutoAwesome),
                Triple(StudioTab.XML_TRANSFORMER, "XML AST", Icons.Default.Layers),
                Triple(StudioTab.SOURCE_INJECTOR, "Injector", Icons.Default.Code),
                Triple(StudioTab.THEME_MANIFEST, "Themes", Icons.Default.Palette),
                Triple(StudioTab.PROJECT_PIPELINE, "Pipeline", Icons.Default.Terminal)
            )

            navItems.forEach { (tab, label, icon) ->
                val isSelected = currentTab == tab
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabSelect(tab) },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) GlassCyan else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) GlassCyan else TextSecondary,
                                fontSize = 10.sp
                            )
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GlassCyan,
                        unselectedIconColor = TextSecondary,
                        selectedTextColor = GlassCyan,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = GlassCyan.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                )
            }
        }
    }
}
