package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.glassengine.pipeline.FileCategory
import com.example.glassengine.pipeline.FileType
import com.example.glassengine.pipeline.ProjectSourceFile
import com.example.ui.StudioUiState
import com.example.ui.components.CodeBlockView
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPillBadge
import com.example.ui.theme.CodeBackground
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassBorderWhite
import com.example.ui.theme.GlassCyan
import com.example.ui.theme.GlassEmerald
import com.example.ui.theme.GlassObsidian
import com.example.ui.theme.GlassObsidianSurface
import com.example.ui.theme.GlassPink
import com.example.ui.theme.GlassPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Helper to query display name from Android content Uri.
 */
private fun queryFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = it.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path?.substringAfterLast('/')
    }
    return result ?: "android-project.zip"
}

/**
 * End-to-End Zip Archive Processing & AST File Classifier Screen.
 * Demonstrates automated uncompiled Android project ingestion, classification,
 * AST transformation, companion resource injection, and output archive re-packaging.
 */
@Composable
fun ProjectPipelineScreen(
    uiState: StudioUiState,
    onRunPipeline: () -> Unit,
    onSelectFile: (Int) -> Unit,
    onUploadZipUri: (Uri, String) -> Unit,
    onProcessSampleZip: () -> Unit,
    onExportZip: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val report = uiState.pipelineReport
    val zipResult = uiState.zipPipelineResult

    // ActivityResultLauncher for selecting .zip archives
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = queryFileName(context, uri)
            onUploadZipUri(uri, fileName)
        }
    }

    // ActivityResultLauncher for exporting transformed .zip archives
    val zipExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { destinationUri: Uri? ->
        if (destinationUri != null) {
            onExportZip(destinationUri)
        }
    }

    // Combine transformed files and generated drawables for file tree view
    val allProjectFiles = mutableListOf<ProjectSourceFile>()
    if (report != null) {
        allProjectFiles.addAll(report.transformedFiles)
        allProjectFiles.addAll(report.generatedDrawables)
    } else {
        allProjectFiles.addAll(uiState.projectFiles)
    }

    val selectedFile = allProjectFiles.getOrNull(uiState.selectedPipelineFileIndex) ?: allProjectFiles.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Project Pipeline",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    GlassPillBadge(text = "Zip Archive & AST Classifier", color = GlassCyan)
                }
                Text(
                    text = "Upload an uncompiled Android project (.zip) for automated classification & glass transformation.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }

        // Dedicated Zip Upload & Pipeline Ingestion Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderBrush = Brush.horizontalGradient(
                listOf(GlassCyan.copy(alpha = 0.5f), GlassPurple.copy(alpha = 0.5f))
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GlassCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FolderZip,
                        contentDescription = "Zip Archive",
                        tint = GlassCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Android Project Archive Ingestion",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = if (uiState.uploadedZipFileName != null) {
                            "Loaded: ${uiState.uploadedZipFileName}"
                        } else {
                            "Select an uncompiled project .zip or test with built-in sample"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (uiState.uploadedZipFileName != null) GlassCyan else TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Upload Button & Secondary Sample Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { zipPickerLauncher.launch("application/zip") },
                    enabled = !uiState.isZipProcessing && !uiState.isPipelineRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassCyan,
                        contentColor = GlassObsidian
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(46.dp)
                        .testTag("upload_zip_button")
                ) {
                    if (uiState.isZipProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = GlassObsidian
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Transforming...", fontWeight = FontWeight.Bold)
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Upload Project (.zip)", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedButton(
                    onClick = onProcessSampleZip,
                    enabled = !uiState.isZipProcessing && !uiState.isPipelineRunning,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = GlassCyan
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("sample_zip_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Sample Zip", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            // Export Transformed Zip Action when ready
            if (zipResult != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val outName = "glassmorph-" + (uiState.uploadedZipFileName ?: "project.zip")
                        zipExportLauncher.launch(outName)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassEmerald,
                        contentColor = GlassObsidian
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("export_zip_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Download Transformed Archive (.zip) - ${(zipResult.outputZipBytes.size / 1024.0).let { "%.1f KB".format(it) }}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Automated Classifier Summary Banner
        if (zipResult != null) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = GlassObsidianSurface
            ) {
                Text(
                    text = "Automated AST File Classification",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val counts = zipResult.classifiedCounts
                    items(FileType.values().toList()) { fileType ->
                        val count = counts[fileType] ?: 0
                        Surface(
                            color = Color(0x18FFFFFF),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when (fileType) {
                                    FileType.LAYOUT_XML -> GlassPurple.copy(alpha = 0.6f)
                                    FileType.KOTLIN_SOURCE -> GlassCyan.copy(alpha = 0.6f)
                                    FileType.JAVA_SOURCE -> GlassPink.copy(alpha = 0.6f)
                                    FileType.THEME_XML, FileType.MANIFEST_XML -> GlassEmerald.copy(alpha = 0.6f)
                                    FileType.IGNORED -> Color(0x30FFFFFF)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = fileType.name.replace("_", " "),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = when (fileType) {
                                            FileType.LAYOUT_XML -> GlassPurple
                                            FileType.KOTLIN_SOURCE -> GlassCyan
                                            FileType.JAVA_SOURCE -> GlassPink
                                            FileType.THEME_XML, FileType.MANIFEST_XML -> GlassEmerald
                                            FileType.IGNORED -> TextMuted
                                        },
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pipeline Metrics Cards
        if (report != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp,
                    backgroundColor = GlassObsidianSurface
                ) {
                    Text(text = "Files Processed", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                    Text(
                        text = "${report.totalFilesProcessed} Source Files",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = GlassCyan)
                    )
                }

                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp,
                    backgroundColor = GlassObsidianSurface
                ) {
                    Text(text = "AST Modifications", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                    Text(
                        text = "${report.totalModificationsCount} Injected",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = GlassPurple)
                    )
                }

                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp,
                    backgroundColor = GlassObsidianSurface
                ) {
                    Text(text = "Companion Assets", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                    Text(
                        text = "${report.generatedDrawables.size} Drawables",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = GlassEmerald)
                    )
                }
            }
        }

        // Transformed Files Explorer
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = GlassCyan, modifier = Modifier.size(18.dp))
                Text(
                    text = "Transformed Project Bundle",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // File items list
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                allProjectFiles.forEachIndexed { index, file ->
                    val isSelected = index == uiState.selectedPipelineFileIndex
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectFile(index) }
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) GlassCyan else Color(0x15FFFFFF),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .testTag("pipeline_file_$index"),
                        color = if (isSelected) GlassCyan.copy(alpha = 0.15f) else Color(0x0CFFFFFF),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = when (file.fileCategory) {
                                        FileCategory.XML_LAYOUT -> Icons.Default.Layers
                                        FileCategory.KOTLIN_SOURCE, FileCategory.JAVA_SOURCE -> Icons.Default.Code
                                        FileCategory.THEME_STYLES, FileCategory.MANIFEST -> Icons.Default.Settings
                                        FileCategory.DRAWABLE_RESOURCE -> Icons.Default.AccountTree
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) GlassCyan else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = file.relativePath,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            GlassPillBadge(
                                text = file.fileCategory.name.replace("_", " "),
                                color = when (file.fileCategory) {
                                    FileCategory.XML_LAYOUT -> GlassPurple
                                    FileCategory.KOTLIN_SOURCE -> GlassCyan
                                    FileCategory.JAVA_SOURCE -> GlassPink
                                    FileCategory.THEME_STYLES -> GlassEmerald
                                    FileCategory.MANIFEST -> GlassEmerald
                                    FileCategory.DRAWABLE_RESOURCE -> GlassCyan
                                }
                            )
                        }
                    }
                }
            }
        }

        // File Content Viewer
        if (selectedFile != null) {
            CodeBlockView(
                code = selectedFile.content,
                language = when {
                    selectedFile.relativePath.endsWith(".kt") -> "kotlin"
                    selectedFile.relativePath.endsWith(".java") -> "java"
                    else -> "xml"
                },
                title = selectedFile.relativePath,
                maxLines = 60,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pipeline_selected_code_view")
            )
        }

        // Pipeline Execution Logs Card
        if (report != null && report.logs.isNotEmpty()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CodeBackground
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = GlassCyan, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Build Pipeline Execution Log",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = GlassCyan
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    report.logs.forEach { logLine ->
                        Text(
                            text = logLine,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = when {
                                    logLine.startsWith("🚀") || logLine.startsWith("🎉") -> GlassCyan
                                    logLine.startsWith("✨") -> GlassEmerald
                                    logLine.startsWith("⚙️") -> TextPrimary
                                    else -> TextSecondary
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}
