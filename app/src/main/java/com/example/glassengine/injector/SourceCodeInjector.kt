package com.example.glassengine.injector

import com.example.glassengine.engine.GlassStyle
import java.io.File

/**
 * Programming language detected for source files.
 */
enum class SourceLanguage {
    KOTLIN,
    JAVA,
    UNKNOWN
}

/**
 * Android component type detected in the source file.
 */
enum class ComponentType {
    ACTIVITY,
    FRAGMENT,
    COMPOSE_ACTIVITY,
    UNKNOWN
}

/**
 * Configuration options for source code injection.
 */
data class SourceInjectorOptions(
    val style: GlassStyle = GlassStyle.LIQUID,
    val includeSdkGuard: Boolean = true,
    val targetMinSdk: Int = 31, // Build.VERSION_CODES.S (31) or TIRAMISU (33)
    val injectImports: Boolean = true
)

/**
 * Result details returned after scanning and modifying source files.
 */
data class InjectionResult(
    val originalSource: String,
    val injectedSource: String,
    val wasInjected: Boolean,
    val detectedLanguage: SourceLanguage,
    val detectedComponent: ComponentType,
    val injectionPointsFound: List<String>,
    val logMessages: List<String>
)

/**
 * Production-grade AST / Token Scanner and Code Injector.
 * Scans Java and Kotlin Activity and Fragment files and automatically injects
 * GlassThemeEngine initialization hooks with compile-safe SDK version bounds.
 */
class SourceCodeInjector(
    private val options: SourceInjectorOptions = SourceInjectorOptions()
) {

    /**
     * Injects glass effect initialization into the provided Kotlin or Java source string.
     */
    fun inject(sourceCode: String, fileName: String = "MainActivity.kt"): InjectionResult {
        if (sourceCode.isBlank()) {
            return InjectionResult(
                originalSource = sourceCode,
                injectedSource = sourceCode,
                wasInjected = false,
                detectedLanguage = SourceLanguage.UNKNOWN,
                detectedComponent = ComponentType.UNKNOWN,
                injectionPointsFound = emptyList(),
                logMessages = listOf("Source code was empty.")
            )
        }

        val language = detectLanguage(fileName, sourceCode)
        val componentType = detectComponentType(sourceCode)
        val logs = mutableListOf<String>()
        val injectionPoints = mutableListOf<String>()

        // Check if already injected
        if (sourceCode.contains("GlassThemeEngine.applyGlassEffect")) {
            logs.add("GlassThemeEngine is already injected in this file.")
            return InjectionResult(
                originalSource = sourceCode,
                injectedSource = sourceCode,
                wasInjected = false,
                detectedLanguage = language,
                detectedComponent = componentType,
                injectionPointsFound = listOf("Already present"),
                logMessages = logs
            )
        }

        var modifiedSource = sourceCode

        // Phase 1: Inject required Imports
        if (options.injectImports) {
            modifiedSource = injectImports(modifiedSource, language, logs)
        }

        // Phase 2: Inject Theme Engine invocation based on language & component type
        val injectionResult = when (language) {
            SourceLanguage.KOTLIN -> injectKotlin(modifiedSource, componentType, logs, injectionPoints)
            SourceLanguage.JAVA -> injectJava(modifiedSource, componentType, logs, injectionPoints)
            SourceLanguage.UNKNOWN -> injectKotlin(modifiedSource, componentType, logs, injectionPoints)
        }

        return InjectionResult(
            originalSource = sourceCode,
            injectedSource = injectionResult,
            wasInjected = injectionPoints.isNotEmpty(),
            detectedLanguage = language,
            detectedComponent = componentType,
            injectionPointsFound = injectionPoints,
            logMessages = logs
        )
    }

    private fun detectLanguage(fileName: String, content: String): SourceLanguage {
        return when {
            fileName.endsWith(".kt", ignoreCase = true) -> SourceLanguage.KOTLIN
            fileName.endsWith(".java", ignoreCase = true) -> SourceLanguage.JAVA
            content.contains("fun onCreate") || content.contains("val ") || content.contains("var ") -> SourceLanguage.KOTLIN
            content.contains("public class") || content.contains("protected void") -> SourceLanguage.JAVA
            else -> SourceLanguage.KOTLIN
        }
    }

    private fun detectComponentType(content: String): ComponentType {
        return when {
            content.contains("setContent {") || content.contains("setContent{") -> ComponentType.COMPOSE_ACTIVITY
            content.contains(": Fragment(") || content.contains("extends Fragment") || content.contains("onCreateView(") -> ComponentType.FRAGMENT
            content.contains("Activity") || content.contains("setContentView(") -> ComponentType.ACTIVITY
            else -> ComponentType.UNKNOWN
        }
    }

    private fun injectImports(
        source: String,
        language: SourceLanguage,
        logs: MutableList<String>
    ): String {
        val importsToAdd = if (language == SourceLanguage.JAVA) {
            listOf(
                "import com.example.glassengine.engine.GlassThemeEngine;",
                "import com.example.glassengine.engine.GlassStyle;",
                "import android.os.Build;"
            )
        } else {
            listOf(
                "import com.example.glassengine.engine.GlassThemeEngine",
                "import com.example.glassengine.engine.GlassStyle",
                "import android.os.Build"
            )
        }

        val neededImports = importsToAdd.filter { !source.contains(it) }
        if (neededImports.isEmpty()) return source

        // Find insertion point after package declaration
        val packageRegex = Regex("^package\\s+[a-zA-Z0-9_.]+\\s*;?", RegexOption.MULTILINE)
        val packageMatch = packageRegex.find(source)

        return if (packageMatch != null) {
            val insertIndex = packageMatch.range.last + 1
            val importBlock = "\n\n" + neededImports.joinToString("\n")
            logs.add("Injected ${neededImports.size} import statements after package declaration.")
            source.substring(0, insertIndex) + importBlock + source.substring(insertIndex)
        } else {
            // Prepend if no package statement found
            neededImports.joinToString("\n") + "\n\n" + source
        }
    }

    private fun injectKotlin(
        source: String,
        componentType: ComponentType,
        logs: MutableList<String>,
        injectionPoints: MutableList<String>
    ): String {
        val sdkGuard = if (options.includeSdkGuard) {
            if (options.targetMinSdk >= 33) {
                "if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)"
            } else {
                "if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)"
            }
        } else ""

        val styleEnum = "GlassStyle.${options.style.name}"

        // Case A: Kotlin Activity with setContentView(...)
        val setContentViewRegex = Regex("""(setContentView\s*\([^)]+\))""")
        val matchSetContent = setContentViewRegex.find(source)
        if (matchSetContent != null) {
            val matchedStatement = matchSetContent.value
            val indentation = detectIndentation(source, matchSetContent.range.first)
            val codeToInject = buildString {
                append("\n")
                append(indentation).append("// Injected by GlassMorph Theme Engine\n")
                if (sdkGuard.isNotEmpty()) {
                    append(indentation).append("$sdkGuard {\n")
                    append(indentation).append("    GlassThemeEngine.applyGlassEffect(window.decorView, $styleEnum)\n")
                    append(indentation).append("}")
                } else {
                    append(indentation).append("GlassThemeEngine.applyGlassEffect(window.decorView, $styleEnum)")
                }
            }
            injectionPoints.add("Kotlin Activity: After $matchedStatement")
            logs.add("Injected GlassThemeEngine hook after '$matchedStatement'.")
            return source.replaceRange(matchSetContent.range, matchedStatement + codeToInject)
        }

        // Case B: Kotlin Compose Activity with setContent { ... }
        val composeRegex = Regex("""setContent\s*\{""")
        val matchCompose = composeRegex.find(source)
        if (matchCompose != null) {
            val indentation = detectIndentation(source, matchCompose.range.first)
            val codeToInject = buildString {
                append("// Injected by GlassMorph Theme Engine\n")
                if (sdkGuard.isNotEmpty()) {
                    append(indentation).append("$sdkGuard {\n")
                    append(indentation).append("    GlassThemeEngine.applyGlassEffect(window.decorView, $styleEnum)\n")
                    append(indentation).append("}\n")
                } else {
                    append(indentation).append("GlassThemeEngine.applyGlassEffect(window.decorView, $styleEnum)\n")
                }
                append(indentation)
            }
            injectionPoints.add("Compose ComponentActivity: Before setContent { ... }")
            logs.add("Injected GlassThemeEngine hook before 'setContent {'.")
            return source.substring(0, matchCompose.range.first) + codeToInject + source.substring(matchCompose.range.first)
        }

        // Case C: Kotlin Fragment onViewCreated(view, savedInstanceState)
        val onViewCreatedRegex = Regex("""override\s+fun\s+onViewCreated\s*\([^)]*\)\s*\{""")
        val matchOnViewCreated = onViewCreatedRegex.find(source)
        if (matchOnViewCreated != null) {
            val indentation = "        "
            val codeToInject = buildString {
                append("\n")
                append(indentation).append("// Injected by GlassMorph Theme Engine\n")
                if (sdkGuard.isNotEmpty()) {
                    append(indentation).append("$sdkGuard {\n")
                    append(indentation).append("    GlassThemeEngine.applyGlassEffect(view, $styleEnum)\n")
                    append(indentation).append("}")
                } else {
                    append(indentation).append("GlassThemeEngine.applyGlassEffect(view, $styleEnum)")
                }
            }
            injectionPoints.add("Kotlin Fragment: Inside onViewCreated")
            logs.add("Injected GlassThemeEngine hook inside 'onViewCreated'.")
            val insertPos = matchOnViewCreated.range.last + 1
            return source.substring(0, insertPos) + codeToInject + source.substring(insertPos)
        }

        // Case D: Kotlin Fragment onCreateView returning a View
        val returnViewRegex = Regex("""return\s+([a-zA-Z0-9_.]+(?:\.root)?)""")
        val matchReturn = returnViewRegex.find(source)
        if (matchReturn != null && source.contains("onCreateView")) {
            val viewVar = matchReturn.groupValues[1]
            val indentation = detectIndentation(source, matchReturn.range.first)
            val codeToInject = buildString {
                append("// Injected by GlassMorph Theme Engine\n")
                if (sdkGuard.isNotEmpty()) {
                    append(indentation).append("$sdkGuard {\n")
                    append(indentation).append("    GlassThemeEngine.applyGlassEffect($viewVar, $styleEnum)\n")
                    append(indentation).append("}\n")
                } else {
                    append(indentation).append("GlassThemeEngine.applyGlassEffect($viewVar, $styleEnum)\n")
                }
                append(indentation)
            }
            injectionPoints.add("Kotlin Fragment: Before return $viewVar in onCreateView")
            logs.add("Injected GlassThemeEngine hook before return '$viewVar'.")
            return source.substring(0, matchReturn.range.first) + codeToInject + source.substring(matchReturn.range.first)
        }

        logs.add("No standard Activity/Fragment injection anchor found in Kotlin source.")
        return source
    }

    private fun injectJava(
        source: String,
        componentType: ComponentType,
        logs: MutableList<String>,
        injectionPoints: MutableList<String>
    ): String {
        val sdkGuard = if (options.includeSdkGuard) {
            if (options.targetMinSdk >= 33) {
                "if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)"
            } else {
                "if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)"
            }
        } else ""

        val styleEnum = "GlassStyle.${options.style.name}"

        // Case A: Java Activity setContentView(...)
        val setContentViewRegex = Regex("""(setContentView\s*\([^;]+\);)""")
        val matchSetContent = setContentViewRegex.find(source)
        if (matchSetContent != null) {
            val matchedStatement = matchSetContent.value
            val indentation = detectIndentation(source, matchSetContent.range.first)
            val codeToInject = buildString {
                append("\n")
                append(indentation).append("// Injected by GlassMorph Theme Engine\n")
                if (sdkGuard.isNotEmpty()) {
                    append(indentation).append("$sdkGuard {\n")
                    append(indentation).append("    GlassThemeEngine.INSTANCE.applyGlassEffect(getWindow().getDecorView(), $styleEnum);\n")
                    append(indentation).append("}")
                } else {
                    append(indentation).append("GlassThemeEngine.INSTANCE.applyGlassEffect(getWindow().getDecorView(), $styleEnum);")
                }
            }
            injectionPoints.add("Java Activity: After $matchedStatement")
            logs.add("Injected Java GlassThemeEngine hook after '$matchedStatement'.")
            return source.replaceRange(matchSetContent.range, matchedStatement + codeToInject)
        }

        // Case B: Java Fragment onViewCreated(View view, ...)
        val onViewCreatedRegex = Regex("""public\s+void\s+onViewCreated\s*\([^)]*\)\s*\{""")
        val matchOnViewCreated = onViewCreatedRegex.find(source)
        if (matchOnViewCreated != null) {
            val indentation = "        "
            val codeToInject = buildString {
                append("\n")
                append(indentation).append("// Injected by GlassMorph Theme Engine\n")
                if (sdkGuard.isNotEmpty()) {
                    append(indentation).append("$sdkGuard {\n")
                    append(indentation).append("    GlassThemeEngine.INSTANCE.applyGlassEffect(view, $styleEnum);\n")
                    append(indentation).append("}")
                } else {
                    append(indentation).append("GlassThemeEngine.INSTANCE.applyGlassEffect(view, $styleEnum);")
                }
            }
            injectionPoints.add("Java Fragment: Inside onViewCreated")
            logs.add("Injected Java GlassThemeEngine hook inside 'onViewCreated'.")
            val insertPos = matchOnViewCreated.range.last + 1
            return source.substring(0, insertPos) + codeToInject + source.substring(insertPos)
        }

        logs.add("No standard Activity/Fragment injection anchor found in Java source.")
        return source
    }

    private fun detectIndentation(source: String, position: Int): String {
        val lineStart = source.lastIndexOf('\n', position - 1)
        if (lineStart == -1) return "        "
        val prefix = source.substring(lineStart + 1, position)
        val spacesOrTabs = prefix.takeWhile { it.isWhitespace() }
        return if (spacesOrTabs.isNotEmpty()) spacesOrTabs else "        "
    }
}

/**
 * Extension function to inject glass effects into any Kotlin or Java source string.
 */
fun String.injectGlassHooks(
    fileName: String = "MainActivity.kt",
    options: SourceInjectorOptions = SourceInjectorOptions()
): InjectionResult {
    return SourceCodeInjector(options).inject(this, fileName)
}

/**
 * Extension function to inject glass effects into a source file on disk.
 */
fun File.injectGlassHooks(
    outputFile: File = this,
    options: SourceInjectorOptions = SourceInjectorOptions()
): InjectionResult {
    val inputSource = this.readText()
    val result = SourceCodeInjector(options).inject(inputSource, this.name)
    if (result.wasInjected) {
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(result.injectedSource)
    }
    return result
}
