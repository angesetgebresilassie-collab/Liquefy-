package com.example.glassengine.pipeline

import com.example.glassengine.engine.GlassConfig
import com.example.glassengine.engine.GlassStyle
import com.example.glassengine.injector.InjectionResult
import com.example.glassengine.injector.SourceCodeInjector
import com.example.glassengine.injector.SourceInjectorOptions
import com.example.glassengine.modifier.PatchResult
import com.example.glassengine.modifier.ThemeManifestModifier
import com.example.glassengine.transformer.TransformationResult
import com.example.glassengine.transformer.XmlLayoutTransformer
import com.example.glassengine.transformer.XmlLayoutTransformerOptions

/**
 * Encapsulates an individual file within an uncompiled Android source project.
 */
data class ProjectSourceFile(
    val relativePath: String,
    val content: String,
    val fileCategory: FileCategory
)

enum class FileCategory {
    XML_LAYOUT,
    KOTLIN_SOURCE,
    JAVA_SOURCE,
    THEME_STYLES,
    MANIFEST,
    DRAWABLE_RESOURCE
}

/**
 * Result report for an entire transformed Android project.
 */
data class ProjectTransformationReport(
    val transformedFiles: List<ProjectSourceFile>,
    val generatedDrawables: List<ProjectSourceFile>,
    val totalFilesProcessed: Int,
    val totalModificationsCount: Int,
    val layoutResults: Map<String, TransformationResult>,
    val sourceResults: Map<String, InjectionResult>,
    val themeResult: PatchResult?,
    val manifestResult: PatchResult?,
    val logs: List<String>
)

/**
 * Production-grade Project Pipeline orchestrator.
 * Accepts raw Android source files (XML layouts, Kotlin/Java activities/fragments, themes, manifest)
 * and runs the complete multi-stage Glassmorphism injection pass.
 */
class ProjectTransformationPipeline(
    private val xmlOptions: XmlLayoutTransformerOptions = XmlLayoutTransformerOptions(),
    private val sourceOptions: SourceInjectorOptions = SourceInjectorOptions(),
    private val glassStyle: GlassStyle = GlassStyle.LIQUID
) {
    private val xmlTransformer = XmlLayoutTransformer(xmlOptions)
    private val sourceInjector = SourceCodeInjector(sourceOptions)

    fun execute(inputFiles: List<ProjectSourceFile>): ProjectTransformationReport {
        val transformedFiles = mutableListOf<ProjectSourceFile>()
        val generatedDrawables = mutableListOf<ProjectSourceFile>()
        val layoutResults = mutableMapOf<String, TransformationResult>()
        val sourceResults = mutableMapOf<String, InjectionResult>()
        var themeResult: PatchResult? = null
        var manifestResult: PatchResult? = null
        val logs = mutableListOf<String>()

        var totalMods = 0

        logs.add("🚀 Starting GlassMorph AST pre-compilation transformation pipeline...")

        for (file in inputFiles) {
            when (file.fileCategory) {
                FileCategory.XML_LAYOUT -> {
                    logs.add("⚙️ Transforming XML Layout: ${file.relativePath}")
                    val res = xmlTransformer.transform(file.content)
                    layoutResults[file.relativePath] = res
                    transformedFiles.add(
                        file.copy(content = res.transformedXml)
                    )
                    totalMods += res.backgroundsModifiedCount + res.drawablesInjectedCount + (if (res.rootWrapped) 1 else 0)
                    logs.add("  - Transformed ${res.backgroundsModifiedCount} backgrounds, injected ${res.drawablesInjectedCount} drawables (rootWrapped=${res.rootWrapped})")
                }

                FileCategory.KOTLIN_SOURCE, FileCategory.JAVA_SOURCE -> {
                    logs.add("⚙️ Injecting Glass Hooks into: ${file.relativePath}")
                    val res = sourceInjector.inject(file.content, file.relativePath.substringAfterLast('/'))
                    sourceResults[file.relativePath] = res
                    transformedFiles.add(
                        file.copy(content = res.injectedSource)
                    )
                    if (res.wasInjected) totalMods++
                    logs.add("  - Language: ${res.detectedLanguage}, Component: ${res.detectedComponent}, Injected: ${res.wasInjected}")
                }

                FileCategory.THEME_STYLES -> {
                    logs.add("⚙️ Patching Theme/Styles: ${file.relativePath}")
                    val res = ThemeManifestModifier.patchThemesXml(file.content)
                    themeResult = res
                    transformedFiles.add(
                        file.copy(content = res.patchedXml)
                    )
                    if (res.isModified) totalMods++
                    logs.add("  - Themes patched with windowIsTranslucent & transparent windowBackground")
                }

                FileCategory.MANIFEST -> {
                    logs.add("⚙️ Patching AndroidManifest: ${file.relativePath}")
                    val res = ThemeManifestModifier.patchAndroidManifestXml(file.content)
                    manifestResult = res
                    transformedFiles.add(
                        file.copy(content = res.patchedXml)
                    )
                    if (res.isModified) totalMods++
                    logs.add("  - AndroidManifest patched with android:hardwareAccelerated=\"true\"")
                }

                FileCategory.DRAWABLE_RESOURCE -> {
                    transformedFiles.add(file)
                }
            }
        }

        // Generate companion drawables
        val companionDrawables = xmlTransformer.generateCompanionDrawables()
        for ((name, content) in companionDrawables) {
            generatedDrawables.add(
                ProjectSourceFile(
                    relativePath = "app/src/main/res/drawable/$name",
                    content = content,
                    fileCategory = FileCategory.DRAWABLE_RESOURCE
                )
            )
        }

        logs.add("✨ Generated ${generatedDrawables.size} companion glass drawables: ${companionDrawables.keys.joinToString(", ")}")
        logs.add("🎉 Project Transformation Pipeline completed successfully with $totalMods AST modifications!")

        return ProjectTransformationReport(
            transformedFiles = transformedFiles,
            generatedDrawables = generatedDrawables,
            totalFilesProcessed = inputFiles.size,
            totalModificationsCount = totalMods,
            layoutResults = layoutResults,
            sourceResults = sourceResults,
            themeResult = themeResult,
            manifestResult = manifestResult,
            logs = logs
        )
    }
}
