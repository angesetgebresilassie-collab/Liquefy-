package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.glassengine.agsl.LiquidGlassShader
import com.example.glassengine.engine.GlassConfig
import com.example.glassengine.engine.GlassStyle
import com.example.glassengine.injector.InjectionResult
import com.example.glassengine.injector.SourceCodeInjector
import com.example.glassengine.injector.SourceInjectorOptions
import com.example.glassengine.modifier.PatchResult
import com.example.glassengine.modifier.ThemeManifestModifier
import com.example.glassengine.pipeline.FileType
import com.example.glassengine.pipeline.GlassMorphPipeline
import com.example.glassengine.pipeline.ProjectSourceFile
import com.example.glassengine.pipeline.ProjectTransformationPipeline
import com.example.glassengine.pipeline.ProjectTransformationReport
import com.example.glassengine.pipeline.SampleCodeRepository
import com.example.glassengine.pipeline.ZipArchiveProcessor
import com.example.glassengine.pipeline.ZipPipelineResult
import com.example.glassengine.transformer.TransformationResult
import com.example.glassengine.transformer.XmlLayoutTransformer
import com.example.glassengine.transformer.XmlLayoutTransformerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

enum class StudioTab(val title: String, val subtitle: String) {
    LIVE_SHADER("Live Shader", "AGSL RuntimeShader & RenderEffect"),
    XML_TRANSFORMER("XML Transformer", "AST Layout Transmutation"),
    SOURCE_INJECTOR("Code Injector", "Kotlin/Java Source Hooks"),
    THEME_MANIFEST("Themes & Manifest", "Window & GPU Acceleration"),
    PROJECT_PIPELINE("Project Pipeline", "Zip Archive & AST Batch Pass")
}

data class StudioUiState(
    val currentTab: StudioTab = StudioTab.LIVE_SHADER,
    
    // Live Shader State
    val selectedGlassStyle: GlassStyle = GlassStyle.LIQUID,
    val liveGlassConfig: GlassConfig = GlassConfig.LIQUID_DEFAULT,
    val isAnimationActive: Boolean = true,
    val shaderSourceCode: String = LiquidGlassShader.SHADER_SOURCE,
    
    // XML Transformer State
    val selectedXmlSampleIndex: Int = 0,
    val rawXmlInput: String = SampleCodeRepository.SAMPLE_CONSTRAINT_LAYOUT,
    val xmlOptions: XmlLayoutTransformerOptions = XmlLayoutTransformerOptions(),
    val xmlTransformationResult: TransformationResult? = null,
    
    // Source Code Injector State
    val selectedSourceSampleIndex: Int = 0,
    val rawSourceInput: String = SampleCodeRepository.SAMPLE_KOTLIN_ACTIVITY,
    val sourceFileName: String = "MainActivity.kt",
    val sourceOptions: SourceInjectorOptions = SourceInjectorOptions(),
    val sourceInjectionResult: InjectionResult? = null,
    
    // Theme & Manifest State
    val rawThemesInput: String = SampleCodeRepository.SAMPLE_THEMES_XML,
    val rawManifestInput: String = SampleCodeRepository.SAMPLE_MANIFEST_XML,
    val patchedThemesResult: PatchResult? = null,
    val patchedManifestResult: PatchResult? = null,
    
    // Batch Pipeline & Zip State
    val projectFiles: List<ProjectSourceFile> = SampleCodeRepository.getDefaultProjectFiles(),
    val pipelineReport: ProjectTransformationReport? = null,
    val isPipelineRunning: Boolean = false,
    val selectedPipelineFileIndex: Int = 0,
    val zipPipelineResult: ZipPipelineResult? = null,
    val isZipProcessing: Boolean = false,
    val uploadedZipFileName: String? = null,
    
    // Toast / Feedback message
    val userNotice: String? = null
)

class GlassStudioViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    init {
        // Run initial transformation previews on default samples
        transformCurrentXml()
        injectCurrentSource()
        patchThemesAndManifest()
        runProjectPipeline()
    }

    fun setTab(tab: StudioTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    // --- Live Shader Controls ---
    fun selectGlassStyle(style: GlassStyle) {
        val config = when (style) {
            GlassStyle.FROSTED -> GlassConfig.FROSTED_DEFAULT
            GlassStyle.LIQUID -> GlassConfig.LIQUID_DEFAULT
            GlassStyle.TINTED_NEON -> GlassConfig.NEON_DEFAULT
            GlassStyle.CRYSTAL_CLEAR -> GlassConfig(refraction = 0.25f, specular = 0.95f, chromaticAberration = 0.15f, tintAlpha = 0.05f)
            GlassStyle.DEEP_ACRYLIC -> GlassConfig(blurRadiusX = 45f, blurRadiusY = 45f, refraction = 0.3f, specular = 0.6f, tintAlpha = 0.25f)
        }
        _uiState.update { it.copy(selectedGlassStyle = style, liveGlassConfig = config) }
    }

    fun updateLiveConfig(config: GlassConfig) {
        _uiState.update { it.copy(liveGlassConfig = config) }
    }

    fun toggleAnimation() {
        _uiState.update { it.copy(isAnimationActive = !it.isAnimationActive) }
    }

    // --- XML Transformer Controls ---
    fun selectXmlSample(index: Int) {
        val content = when (index) {
            0 -> SampleCodeRepository.SAMPLE_CONSTRAINT_LAYOUT
            1 -> SampleCodeRepository.SAMPLE_LOGIN_LAYOUT
            else -> SampleCodeRepository.SAMPLE_CONSTRAINT_LAYOUT
        }
        _uiState.update { it.copy(selectedXmlSampleIndex = index, rawXmlInput = content) }
        transformCurrentXml()
    }

    fun updateRawXmlInput(xml: String) {
        _uiState.update { it.copy(rawXmlInput = xml) }
        transformCurrentXml()
    }

    fun updateXmlOptions(options: XmlLayoutTransformerOptions) {
        _uiState.update { it.copy(xmlOptions = options) }
        transformCurrentXml()
    }

    fun transformCurrentXml() {
        val state = _uiState.value
        val result = XmlLayoutTransformer(state.xmlOptions).transform(state.rawXmlInput)
        _uiState.update { it.copy(xmlTransformationResult = result) }
    }

    // --- Source Code Injector Controls ---
    fun selectSourceSample(index: Int) {
        val (fileName, content) = when (index) {
            0 -> "MainActivity.kt" to SampleCodeRepository.SAMPLE_KOTLIN_ACTIVITY
            1 -> "DashboardActivity.java" to SampleCodeRepository.SAMPLE_JAVA_ACTIVITY
            2 -> "ProfileFragment.kt" to SampleCodeRepository.SAMPLE_KOTLIN_FRAGMENT
            else -> "MainActivity.kt" to SampleCodeRepository.SAMPLE_KOTLIN_ACTIVITY
        }
        _uiState.update {
            it.copy(
                selectedSourceSampleIndex = index,
                rawSourceInput = content,
                sourceFileName = fileName
            )
        }
        injectCurrentSource()
    }

    fun updateRawSourceInput(source: String) {
        _uiState.update { it.copy(rawSourceInput = source) }
        injectCurrentSource()
    }

    fun updateSourceOptions(options: SourceInjectorOptions) {
        _uiState.update { it.copy(sourceOptions = options) }
        injectCurrentSource()
    }

    fun injectCurrentSource() {
        val state = _uiState.value
        val result = SourceCodeInjector(state.sourceOptions).inject(state.rawSourceInput, state.sourceFileName)
        _uiState.update { it.copy(sourceInjectionResult = result) }
    }

    // --- Themes & Manifest Controls ---
    fun updateRawThemesInput(xml: String) {
        _uiState.update { it.copy(rawThemesInput = xml) }
        patchThemesAndManifest()
    }

    fun updateRawManifestInput(xml: String) {
        _uiState.update { it.copy(rawManifestInput = xml) }
        patchThemesAndManifest()
    }

    fun patchThemesAndManifest() {
        val state = _uiState.value
        val themeRes = ThemeManifestModifier.patchThemesXml(state.rawThemesInput)
        val manifestRes = ThemeManifestModifier.patchAndroidManifestXml(state.rawManifestInput)
        _uiState.update {
            it.copy(
                patchedThemesResult = themeRes,
                patchedManifestResult = manifestRes
            )
        }
    }

    // --- Zip Archive & Batch Pipeline Controls ---
    fun selectPipelineFile(index: Int) {
        _uiState.update { it.copy(selectedPipelineFileIndex = index) }
    }

    fun runProjectPipeline() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPipelineRunning = true) }
            val state = _uiState.value
            val pipeline = ProjectTransformationPipeline(
                xmlOptions = state.xmlOptions,
                sourceOptions = state.sourceOptions,
                glassStyle = state.selectedGlassStyle
            )
            val report = pipeline.execute(state.projectFiles)
            _uiState.update {
                it.copy(
                    pipelineReport = report,
                    isPipelineRunning = false,
                    userNotice = "Pipeline completed: ${report.totalModificationsCount} AST modifications injected."
                )
            }
        }
    }

    /**
     * Ingests, classifies, and transforms an uploaded Android Project .zip archive from URI.
     */
    fun processUploadedZipUri(context: Context, zipUri: Uri, fileName: String = "project.zip") {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isZipProcessing = true,
                    uploadedZipFileName = fileName
                )
            }

            try {
                val state = _uiState.value
                val pipeline = GlassMorphPipeline(
                    xmlOptions = state.xmlOptions,
                    sourceOptions = state.sourceOptions,
                    glassStyle = state.selectedGlassStyle
                )
                val result = pipeline.processZipUri(context, zipUri, fileName)

                _uiState.update {
                    it.copy(
                        zipPipelineResult = result,
                        pipelineReport = result.report,
                        isZipProcessing = false,
                        userNotice = "✅ Successfully transformed $fileName (${result.report.totalModificationsCount} AST changes)"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isZipProcessing = false,
                        userNotice = "❌ Failed to process zip: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    /**
     * Synthesizes and transforms a complete sample Android project in a .zip archive.
     */
    fun processSampleZip() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isZipProcessing = true,
                    uploadedZipFileName = "sample-android-app.zip"
                )
            }

            try {
                val state = _uiState.value
                val sampleBytes = ZipArchiveProcessor.createSampleProjectZip()
                val pipeline = GlassMorphPipeline(
                    xmlOptions = state.xmlOptions,
                    sourceOptions = state.sourceOptions,
                    glassStyle = state.selectedGlassStyle
                )
                val result = pipeline.processZipStream(ByteArrayInputStream(sampleBytes), "sample-android-app.zip")

                _uiState.update {
                    it.copy(
                        zipPipelineResult = result,
                        pipelineReport = result.report,
                        isZipProcessing = false,
                        userNotice = "✅ Sample project archive transformed (${result.report.totalModificationsCount} AST mods)"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isZipProcessing = false,
                        userNotice = "❌ Failed to process sample zip: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Exports the transformed project zip to a user-chosen destination URI.
     */
    fun exportTransformedZip(context: Context, destinationUri: Uri, onResult: (Boolean) -> Unit) {
        val result = _uiState.value.zipPipelineResult
        if (result == null) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            val pipeline = GlassMorphPipeline()
            val success = pipeline.exportZipToUri(context, destinationUri, result.outputZipBytes)
            if (success) {
                showNotice("💾 Exported transformed project .zip archive successfully!")
            } else {
                showNotice("❌ Failed to export project archive.")
            }
            onResult(success)
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(userNotice = null) }
    }

    fun showNotice(msg: String) {
        _uiState.update { it.copy(userNotice = msg) }
    }
}
