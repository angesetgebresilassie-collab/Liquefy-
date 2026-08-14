package com.example.glassengine.pipeline

/**
 * Automated AST & File Routing types for Android projects.
 */
enum class FileType {
    KOTLIN_SOURCE,
    JAVA_SOURCE,
    LAYOUT_XML,
    MANIFEST_XML,
    THEME_XML,
    IGNORED
}

/**
 * Intelligent file classifier for Android project source trees.
 * Inspects relative paths, file extensions, and raw AST/XML content to classify
 * files for transformation routing.
 */
object ProjectFileClassifier {

    fun classify(relativePath: String, content: String = ""): FileType {
        val normalizedPath = relativePath.replace('\\', '/').trim().lowercase()
        val fileName = normalizedPath.substringAfterLast('/')

        // Ignore build directories, binaries, and VCS files
        if (normalizedPath.startsWith(".git/") ||
            normalizedPath.contains("/build/") ||
            normalizedPath.contains("/.gradle/") ||
            normalizedPath.contains("/.idea/") ||
            normalizedPath.endsWith(".class") ||
            normalizedPath.endsWith(".jar") ||
            normalizedPath.endsWith(".aar") ||
            normalizedPath.endsWith(".dex") ||
            normalizedPath.endsWith(".apk") ||
            normalizedPath.endsWith(".aab") ||
            normalizedPath.endsWith(".so")
        ) {
            return FileType.IGNORED
        }

        // Kotlin source files
        if (fileName.endsWith(".kt")) {
            return FileType.KOTLIN_SOURCE
        }

        // Java source files
        if (fileName.endsWith(".java")) {
            return FileType.JAVA_SOURCE
        }

        // XML resource analysis
        if (fileName.endsWith(".xml")) {
            // AndroidManifest.xml
            if (fileName == "androidmanifest.xml" || normalizedPath.endsWith("androidmanifest.xml") || content.contains("<manifest")) {
                return FileType.MANIFEST_XML
            }

            // Theme or styles xml
            if (normalizedPath.contains("res/values") && (fileName.contains("theme") || fileName.contains("style") || fileName.contains("colors"))) {
                return FileType.THEME_XML
            }
            if (content.contains("<resources>") && (content.contains("<style") || content.contains("parent=\"Theme.") || content.contains("name=\"Theme."))) {
                return FileType.THEME_XML
            }

            // Layout XMLs
            if (normalizedPath.contains("res/layout") ||
                content.contains("<androidx.constraintlayout.widget.ConstraintLayout") ||
                content.contains("<LinearLayout") ||
                content.contains("<RelativeLayout") ||
                content.contains("<FrameLayout") ||
                content.contains("<CoordinatorLayout") ||
                content.contains("<ScrollView") ||
                content.contains("<NestedScrollView") ||
                content.contains("<androidx.recyclerview.widget.RecyclerView")
            ) {
                return FileType.LAYOUT_XML
            }

            // Drawables, menus, and other non-layout XMLs are preserved as-is
            return FileType.IGNORED
        }

        return FileType.IGNORED
    }

    /**
     * Maps FileType to the corresponding FileCategory used by ProjectTransformationPipeline.
     */
    fun toFileCategory(fileType: FileType): FileCategory? {
        return when (fileType) {
            FileType.KOTLIN_SOURCE -> FileCategory.KOTLIN_SOURCE
            FileType.JAVA_SOURCE -> FileCategory.JAVA_SOURCE
            FileType.LAYOUT_XML -> FileCategory.XML_LAYOUT
            FileType.MANIFEST_XML -> FileCategory.MANIFEST
            FileType.THEME_XML -> FileCategory.THEME_STYLES
            FileType.IGNORED -> null
        }
    }
}
