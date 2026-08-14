package com.example

import com.example.glassengine.agsl.LiquidGlassShader
import com.example.glassengine.engine.GlassStyle
import com.example.glassengine.injector.ComponentType
import com.example.glassengine.injector.SourceCodeInjector
import com.example.glassengine.injector.SourceInjectorOptions
import com.example.glassengine.injector.SourceLanguage
import com.example.glassengine.modifier.ThemeManifestModifier
import com.example.glassengine.pipeline.FileCategory
import com.example.glassengine.pipeline.ProjectSourceFile
import com.example.glassengine.pipeline.ProjectTransformationPipeline
import com.example.glassengine.pipeline.SampleCodeRepository
import com.example.glassengine.transformer.XmlLayoutTransformer
import com.example.glassengine.transformer.XmlLayoutTransformerOptions
import com.example.glassengine.pipeline.FileType
import com.example.glassengine.pipeline.ProjectFileClassifier
import com.example.glassengine.pipeline.ZipArchiveProcessor
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassTransformationEngineTest {

    @Test
    fun testLiquidGlassShaderSyntax() {
        val shader = LiquidGlassShader.SHADER_SOURCE
        assertTrue("Shader must declare composable uniform", shader.contains("uniform shader composable;"))
        assertTrue("Shader must declare resolution uniform", shader.contains("uniform float2 resolution;"))
        assertTrue("Shader must declare refraction uniform", shader.contains("uniform float refraction;"))
        assertTrue("Shader must declare specular uniform", shader.contains("uniform float specular;"))
        assertTrue("Shader must define main entry point", shader.contains("half4 main(float2 fragCoord)"))
    }

    @Test
    fun testXmlLayoutTransformer() {
        val transformer = XmlLayoutTransformer(
            XmlLayoutTransformerOptions(
                wrapRootInGlassContainer = true,
                convertSolidBackgrounds = true,
                injectRoundedGlassDrawables = true
            )
        )
        val result = transformer.transform(SampleCodeRepository.SAMPLE_CONSTRAINT_LAYOUT)

        assertNotNull("Transformation result must not be null", result)
        assertTrue("Transformed XML must contain FrameLayout root wrapper", result.transformedXml.contains("<FrameLayout"))
        assertTrue("Transformed XML must reference bg_glass_surface", result.transformedXml.contains("@drawable/bg_glass_surface"))
        assertTrue("Transformed XML must reference bg_glass_card", result.transformedXml.contains("@drawable/bg_glass_card"))
        assertTrue("Companion drawables must be generated", result.companionDrawables.isNotEmpty())
    }

    @Test
    fun testKotlinActivitySourceInjection() {
        val injector = SourceCodeInjector(
            SourceInjectorOptions(
                includeSdkGuard = true,
                injectImports = true
            )
        )
        val result = injector.inject(SampleCodeRepository.SAMPLE_KOTLIN_ACTIVITY, "MainActivity.kt")

        assertTrue("Should detect Kotlin source", result.detectedLanguage == SourceLanguage.KOTLIN)
        assertTrue("Should detect Activity component", result.detectedComponent == ComponentType.ACTIVITY)
        assertTrue("Should successfully inject", result.wasInjected)
        assertTrue("Injected code must contain GlassThemeEngine call", result.injectedSource.contains("GlassThemeEngine.applyGlassEffect"))
        assertTrue("Injected code must contain SDK guard", result.injectedSource.contains("Build.VERSION_CODES.S"))
    }

    @Test
    fun testJavaActivitySourceInjection() {
        val injector = SourceCodeInjector(
            SourceInjectorOptions(
                includeSdkGuard = true,
                injectImports = true
            )
        )
        val result = injector.inject(SampleCodeRepository.SAMPLE_JAVA_ACTIVITY, "DashboardActivity.java")

        assertTrue("Should detect Java source", result.detectedLanguage == SourceLanguage.JAVA)
        assertTrue("Should detect Activity component", result.detectedComponent == ComponentType.ACTIVITY)
        assertTrue("Should successfully inject", result.wasInjected)
        assertTrue("Injected Java code must contain GlassThemeEngine call", result.injectedSource.contains("GlassThemeEngine") && result.injectedSource.contains("applyGlassEffect"))
    }

    @Test
    fun testKotlinFragmentSourceInjection() {
        val injector = SourceCodeInjector()
        val result = injector.inject(SampleCodeRepository.SAMPLE_KOTLIN_FRAGMENT, "ProfileFragment.kt")

        assertTrue("Should detect Fragment component", result.detectedComponent == ComponentType.FRAGMENT)
        assertTrue("Should successfully inject in Fragment onViewCreated", result.wasInjected)
        assertTrue("Should contain GlassThemeEngine call", result.injectedSource.contains("GlassThemeEngine.applyGlassEffect"))
    }

    @Test
    fun testThemeManifestModifier() {
        val patchedThemes = ThemeManifestModifier.patchThemesXml(SampleCodeRepository.SAMPLE_THEMES_XML)
        assertTrue("themes.xml must be modified", patchedThemes.isModified)
        assertTrue("themes.xml must contain windowIsTranslucent", patchedThemes.patchedXml.contains("android:windowIsTranslucent"))
        assertTrue("themes.xml must contain transparent windowBackground", patchedThemes.patchedXml.contains("@android:color/transparent"))

        val patchedManifest = ThemeManifestModifier.patchAndroidManifestXml(SampleCodeRepository.SAMPLE_MANIFEST_XML)
        assertTrue("AndroidManifest.xml must be modified", patchedManifest.isModified)
        assertTrue("AndroidManifest.xml must contain hardwareAccelerated", patchedManifest.patchedXml.contains("android:hardwareAccelerated=\"true\""))
    }

    @Test
    fun testProjectTransformationPipeline() {
        val pipeline = ProjectTransformationPipeline(
            glassStyle = GlassStyle.LIQUID
        )
        val report = pipeline.execute(SampleCodeRepository.getDefaultProjectFiles())

        assertEquals("Should process all sample project files", SampleCodeRepository.getDefaultProjectFiles().size, report.totalFilesProcessed)
        assertTrue("Should have non-zero modifications", report.totalModificationsCount > 0)
        assertTrue("Should generate companion drawables", report.generatedDrawables.isNotEmpty())
    }

    @Test
    fun testProjectFileClassifier() {
        assertEquals(FileType.KOTLIN_SOURCE, ProjectFileClassifier.classify("app/src/main/java/com/app/MainActivity.kt"))
        assertEquals(FileType.JAVA_SOURCE, ProjectFileClassifier.classify("app/src/main/java/com/app/OldActivity.java"))
        assertEquals(FileType.LAYOUT_XML, ProjectFileClassifier.classify("app/src/main/res/layout/activity_main.xml", "<LinearLayout></LinearLayout>"))
        assertEquals(FileType.MANIFEST_XML, ProjectFileClassifier.classify("app/src/main/AndroidManifest.xml", "<manifest></manifest>"))
        assertEquals(FileType.THEME_XML, ProjectFileClassifier.classify("app/src/main/res/values/themes.xml", "<resources><style name=\"Theme\"></style></resources>"))
        assertEquals(FileType.IGNORED, ProjectFileClassifier.classify("app/build/intermediates/classes.jar"))
        assertEquals(FileType.IGNORED, ProjectFileClassifier.classify(".git/HEAD"))
    }

    @Test
    fun testZipArchiveProcessor() {
        val sampleZipBytes = ZipArchiveProcessor.createSampleProjectZip()
        assertTrue("Sample zip bytes must be non-empty", sampleZipBytes.isNotEmpty())

        val processor = ZipArchiveProcessor()
        val result = processor.processArchive(ByteArrayInputStream(sampleZipBytes), "test-project.zip")

        assertNotNull("Zip processing result must not be null", result)
        assertTrue("Output zip bytes must not be empty", result.outputZipBytes.isNotEmpty())
        assertTrue("Output zip must have modified AST count > 0", result.report.totalModificationsCount > 0)
        assertTrue("Transformed companion drawables must be generated", result.report.generatedDrawables.isNotEmpty())
        assertTrue("Classifier should have detected Kotlin files", (result.classifiedCounts[FileType.KOTLIN_SOURCE] ?: 0) > 0)
        assertTrue("Classifier should have detected Layout XML files", (result.classifiedCounts[FileType.LAYOUT_XML] ?: 0) > 0)
    }
}
