package com.example.glassengine.pipeline

import android.content.Context
import android.net.Uri
import com.example.glassengine.engine.GlassStyle
import com.example.glassengine.injector.SourceInjectorOptions
import com.example.glassengine.transformer.XmlLayoutTransformerOptions
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Result data holder for Zip archive processing.
 */
data class ZipPipelineResult(
    val report: ProjectTransformationReport,
    val outputZipBytes: ByteArray,
    val classifiedCounts: Map<FileType, Int>,
    val totalEntriesCount: Int,
    val inputArchiveName: String
)

/**
 * High-performance In-Memory Zip Archive Processor and Re-packager.
 * Unpacks Android project `.zip` archives, routes through AST classifiers and transformers,
 * injects companion glass resources, and synthesizes a clean output `.zip` archive.
 */
class ZipArchiveProcessor(
    private val xmlOptions: XmlLayoutTransformerOptions = XmlLayoutTransformerOptions(),
    private val sourceOptions: SourceInjectorOptions = SourceInjectorOptions(),
    private val glassStyle: GlassStyle = GlassStyle.LIQUID
) {
    private val transformationPipeline = ProjectTransformationPipeline(xmlOptions, sourceOptions, glassStyle)

    /**
     * Processes an Android project ZIP stream and generates a transformed project ZIP.
     */
    fun processArchive(
        inputStream: InputStream,
        archiveName: String = "project.zip"
    ): ZipPipelineResult {
        val rawEntries = mutableMapOf<String, ByteArray>()
        val textFiles = mutableListOf<ProjectSourceFile>()
        val classifiedCounts = mutableMapOf<FileType, Int>()

        // Initialize classification count maps
        FileType.values().forEach { classifiedCounts[it] = 0 }

        // Read all entries from the incoming zip
        ZipInputStream(inputStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val entryName = entry.name
                if (!entry.isDirectory) {
                    val bytes = zis.readBytes()
                    rawEntries[entryName] = bytes

                    // Attempt text decoding for classification
                    try {
                        val content = String(bytes, StandardCharsets.UTF_8)
                        val fileType = ProjectFileClassifier.classify(entryName, content)
                        classifiedCounts[fileType] = (classifiedCounts[fileType] ?: 0) + 1

                        val category = ProjectFileClassifier.toFileCategory(fileType)
                        if (category != null) {
                            textFiles.add(
                                ProjectSourceFile(
                                    relativePath = entryName,
                                    content = content,
                                    fileCategory = category
                                )
                            )
                        }
                    } catch (_: Exception) {
                        // Binary file, keep in rawEntries for passthrough
                        classifiedCounts[FileType.IGNORED] = (classifiedCounts[FileType.IGNORED] ?: 0) + 1
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // Execute AST Transformation Pipeline on identified source files
        val report = transformationPipeline.execute(textFiles)

        // Build output map with modified contents
        val finalFiles = HashMap<String, ByteArray>(rawEntries)

        // Overwrite transformed source files
        for (transformed in report.transformedFiles) {
            finalFiles[transformed.relativePath] = transformed.content.toByteArray(StandardCharsets.UTF_8)
        }

        // Add newly generated companion glass drawables
        for (drawable in report.generatedDrawables) {
            finalFiles[drawable.relativePath] = drawable.content.toByteArray(StandardCharsets.UTF_8)
        }

        // Package all files into output Zip archive
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            for ((path, bytes) in finalFiles) {
                val newEntry = ZipEntry(path)
                zos.putNextEntry(newEntry)
                zos.write(bytes)
                zos.closeEntry()
            }
        }

        return ZipPipelineResult(
            report = report,
            outputZipBytes = baos.toByteArray(),
            classifiedCounts = classifiedCounts,
            totalEntriesCount = rawEntries.size,
            inputArchiveName = archiveName
        )
    }

    /**
     * Generates a sample uncompiled Android project in a `.zip` archive byte array.
     */
    companion object {
        fun createSampleProjectZip(): ByteArray {
            val baos = ByteArrayOutputStream()
            ZipOutputStream(baos).use { zos ->
                for (file in SampleCodeRepository.getDefaultProjectFiles()) {
                    val entry = ZipEntry(file.relativePath)
                    zos.putNextEntry(entry)
                    zos.write(file.content.toByteArray(StandardCharsets.UTF_8))
                    zos.closeEntry()
                }

                // Add sample build.gradle.kts and settings.gradle.kts
                val buildGradle = """
                    plugins {
                        alias(libs.plugins.android.application)
                        alias(libs.plugins.kotlin.android)
                    }
                    android {
                        namespace = "com.sample.app"
                        compileSdk = 34
                    }
                """.trimIndent()
                val gradleEntry = ZipEntry("app/build.gradle.kts")
                zos.putNextEntry(gradleEntry)
                zos.write(buildGradle.toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()
            }
            return baos.toByteArray()
        }
    }
}
