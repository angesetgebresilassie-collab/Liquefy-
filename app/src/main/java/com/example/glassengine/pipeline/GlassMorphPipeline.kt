package com.example.glassengine.pipeline

import android.content.Context
import android.net.Uri
import com.example.glassengine.engine.GlassStyle
import com.example.glassengine.injector.SourceInjectorOptions
import com.example.glassengine.transformer.XmlLayoutTransformerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

/**
 * Primary pipeline orchestrator for GlassMorph Engine.
 * Handles Zip archive ingestion from URIs, AST file classification,
 * AST source transformation, and re-exporting transformed packages.
 */
class GlassMorphPipeline(
    private val xmlOptions: XmlLayoutTransformerOptions = XmlLayoutTransformerOptions(),
    private val sourceOptions: SourceInjectorOptions = SourceInjectorOptions(),
    private val glassStyle: GlassStyle = GlassStyle.LIQUID
) {
    private val zipProcessor = ZipArchiveProcessor(xmlOptions, sourceOptions, glassStyle)

    /**
     * Ingests and transforms a zip file directly from an Android content Uri.
     */
    suspend fun processZipUri(
        context: Context,
        zipUri: Uri,
        fileName: String = "project.zip"
    ): ZipPipelineResult = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(zipUri)
            ?: throw IllegalArgumentException("Cannot open input stream for Uri: $zipUri")
        inputStream.use { stream ->
            zipProcessor.processArchive(stream, fileName)
        }
    }

    /**
     * Ingests and transforms a zip archive from a raw InputStream.
     */
    suspend fun processZipStream(
        inputStream: InputStream,
        fileName: String = "project.zip"
    ): ZipPipelineResult = withContext(Dispatchers.IO) {
        zipProcessor.processArchive(inputStream, fileName)
    }

    /**
     * Writes transformed zip bytes directly to a target destination Uri.
     */
    suspend fun exportZipToUri(
        context: Context,
        destinationUri: Uri,
        zipBytes: ByteArray
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val outputStream: OutputStream = context.contentResolver.openOutputStream(destinationUri)
                ?: return@withContext false
            outputStream.use { it.write(zipBytes) }
            true
        } catch (_: Exception) {
            false
        }
    }
}
