package com.example.glassengine.transformer

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Configuration options for XML layout AST transformation.
 */
data class XmlLayoutTransformerOptions(
    val wrapRootInGlassContainer: Boolean = true,
    val convertSolidBackgrounds: Boolean = true,
    val alphaHexPrefix: String = "26", // 15% opacity (0x26 / 0xFF)
    val injectRoundedGlassDrawables: Boolean = true,
    val glassCardDrawableRef: String = "@drawable/bg_glass_card",
    val glassSurfaceDrawableRef: String = "@drawable/bg_glass_surface",
    val targetContainerTags: Set<String> = setOf(
        "LinearLayout",
        "FrameLayout",
        "RelativeLayout",
        "androidx.constraintlayout.widget.ConstraintLayout",
        "androidx.cardview.widget.CardView",
        "com.google.android.material.card.MaterialCardView",
        "androidx.appcompat.widget.LinearLayoutCompat"
    )
)

/**
 * Record of an individual AST modification made during XML transformation.
 */
data class AstChangeLog(
    val nodeName: String,
    val nodeId: String?,
    val changeType: String,
    val detail: String
)

/**
 * Result bundle returned by the [XmlLayoutTransformer].
 */
data class TransformationResult(
    val originalXml: String,
    val transformedXml: String,
    val rootWrapped: Boolean,
    val backgroundsModifiedCount: Int,
    val drawablesInjectedCount: Int,
    val changeLogs: List<AstChangeLog>,
    val companionDrawables: Map<String, String>
)

/**
 * Production-grade AST XML Layout Transformer.
 * Parses raw Android XML layouts into DOM trees, performs glassmorphic AST mutations,
 * and serializes clean, valid XML preserving namespaces, IDs, and constraint attributes.
 */
class XmlLayoutTransformer(
    private val options: XmlLayoutTransformerOptions = XmlLayoutTransformerOptions()
) {

    /**
     * Transforms raw Android XML layout string into a Glassmorphism-themed layout.
     */
    fun transform(xmlContent: String): TransformationResult {
        if (xmlContent.isBlank()) {
            return TransformationResult(
                originalXml = xmlContent,
                transformedXml = xmlContent,
                rootWrapped = false,
                backgroundsModifiedCount = 0,
                drawablesInjectedCount = 0,
                changeLogs = emptyList(),
                companionDrawables = generateCompanionDrawables()
            )
        }

        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isIgnoringComments = false
        }
        val builder = factory.newDocumentBuilder()
        val doc: Document = builder.parse(InputSource(StringReader(xmlContent)))

        val logs = mutableListOf<AstChangeLog>()
        var backgroundsCount = 0
        var drawablesCount = 0
        var rootWrapped = false

        val rootElement = doc.documentElement

        // Phase 1: Recursively traverse all elements for background transmutation & drawable injection
        val allElements = mutableListOf<Element>()
        collectAllElements(rootElement, allElements)

        for (element in allElements) {
            val elementId = element.getAttribute("android:id").ifBlank { null }

            // 1. Solid Background Conversion
            if (options.convertSolidBackgrounds && element.hasAttribute("android:background")) {
                val currentBg = element.getAttribute("android:background")
                val transformedBg = convertBackgroundValue(currentBg, options.alphaHexPrefix)
                if (transformedBg != currentBg) {
                    element.setAttribute("android:background", transformedBg)
                    backgroundsCount++
                    logs.add(
                        AstChangeLog(
                            nodeName = element.tagName,
                            nodeId = elementId,
                            changeType = "BACKGROUND_TRANSLUCENT",
                            detail = "Changed background from '$currentBg' to '$transformedBg'"
                        )
                    )
                }
            }

            // 2. Rounded Glass Drawable Injection on Containers
            if (options.injectRoundedGlassDrawables && isContainerElement(element.tagName)) {
                val hasBg = element.hasAttribute("android:background")
                val isCard = element.tagName.contains("CardView", ignoreCase = true)

                if (!hasBg) {
                    element.setAttribute("android:background", options.glassCardDrawableRef)
                    drawablesCount++
                    logs.add(
                        AstChangeLog(
                            nodeName = element.tagName,
                            nodeId = elementId,
                            changeType = "DRAWABLE_INJECTED",
                            detail = "Injected '${options.glassCardDrawableRef}' into container"
                        )
                    )
                }

                // If CardView, ensure cardBackgroundColor is transparent so the glass drawable shines through
                if (isCard) {
                    if (element.hasAttribute("app:cardBackgroundColor")) {
                        element.setAttribute("app:cardBackgroundColor", "@android:color/transparent")
                    } else {
                        element.setAttribute("app:cardBackgroundColor", "@android:color/transparent")
                    }
                    if (element.hasAttribute("app:cardElevation")) {
                        element.setAttribute("app:cardElevation", "0dp")
                    }
                    logs.add(
                        AstChangeLog(
                            nodeName = element.tagName,
                            nodeId = elementId,
                            changeType = "CARD_GLASS_FLATTEN",
                            detail = "Set cardBackgroundColor to transparent for specular glass rendering"
                        )
                    )
                }
            }
        }

        // Phase 2: Root container wrapping (if enabled and root is not already a glass host)
        var finalDoc = doc
        if (options.wrapRootInGlassContainer && !rootElement.getAttribute("android:tag").contains("glass_root")) {
            finalDoc = wrapRootInGlassHost(doc, options, logs)
            rootWrapped = true
        }

        // Phase 3: Serialize DOM back to formatted XML string
        val serializedXml = serializeDocument(finalDoc)

        return TransformationResult(
            originalXml = xmlContent,
            transformedXml = serializedXml,
            rootWrapped = rootWrapped,
            backgroundsModifiedCount = backgroundsCount,
            drawablesInjectedCount = drawablesCount,
            changeLogs = logs,
            companionDrawables = generateCompanionDrawables()
        )
    }

    private fun collectAllElements(node: Node, list: MutableList<Element>) {
        if (node.nodeType == Node.ELEMENT_NODE) {
            val element = node as Element
            list.add(element)
            val children = element.childNodes
            for (i in 0 until children.length) {
                collectAllElements(children.item(i), list)
            }
        }
    }

    private fun isContainerElement(tagName: String): Boolean {
        val simpleName = tagName.substringAfterLast('.')
        return options.targetContainerTags.any { target ->
            target == tagName || target.substringAfterLast('.') == simpleName
        }
    }

    /**
     * Transmutes solid color definitions into translucent alpha hex codes.
     */
    private fun convertBackgroundValue(value: String, alphaPrefix: String): String {
        val trimmed = value.trim()

        // 6-digit hex: #RRGGBB -> #AARRGGBB
        if (trimmed.matches(Regex("^#[0-9a-fA-F]{6}$"))) {
            val hexDigits = trimmed.removePrefix("#")
            return "#$alphaPrefix$hexDigits"
        }

        // 8-digit solid hex: #FFRRGGBB -> #AARRGGBB
        if (trimmed.matches(Regex("^#[fF]{2}[0-9a-fA-F]{6}$"))) {
            val hexDigits = trimmed.substring(3)
            return "#$alphaPrefix$hexDigits"
        }

        // Common Theme references
        if (trimmed.equals("?android:attr/colorBackground", ignoreCase = true) ||
            trimmed.equals("?attr/colorSurface", ignoreCase = true) ||
            trimmed.equals("?attr/backgroundColor", ignoreCase = true) ||
            trimmed.equals("@android:color/white", ignoreCase = true) ||
            trimmed.equals("@android:color/background_light", ignoreCase = true)
        ) {
            return "#${alphaPrefix}FFFFFF"
        }

        if (trimmed.equals("@android:color/black", ignoreCase = true) ||
            trimmed.equals("@android:color/background_dark", ignoreCase = true)
        ) {
            return "#${alphaPrefix}0A0E1A"
        }

        return value
    }

    /**
     * Wraps the existing root element in a top-level Glass Container while preserving all namespaces and layout bounds.
     */
    private fun wrapRootInGlassHost(
        doc: Document,
        options: XmlLayoutTransformerOptions,
        logs: MutableList<AstChangeLog>
    ): Document {
        val oldRoot = doc.documentElement

        // Create new Glass Root FrameLayout
        val newRoot = doc.createElement("FrameLayout")
        newRoot.setAttribute("xmlns:android", "http://schemas.android.com/apk/res/android")
        newRoot.setAttribute("xmlns:app", "http://schemas.android.com/apk/res-auto")
        newRoot.setAttribute("xmlns:tools", "http://schemas.android.com/tools")
        newRoot.setAttribute("android:layout_width", "match_parent")
        newRoot.setAttribute("android:layout_height", "match_parent")
        newRoot.setAttribute("android:background", options.glassSurfaceDrawableRef)
        newRoot.setAttribute("android:tag", "glass_root_container")

        // Copy namespaces from old root if any
        if (oldRoot.hasAttribute("xmlns:android")) oldRoot.removeAttribute("xmlns:android")
        if (oldRoot.hasAttribute("xmlns:app")) oldRoot.removeAttribute("xmlns:app")
        if (oldRoot.hasAttribute("xmlns:tools")) oldRoot.removeAttribute("xmlns:tools")

        // Move old root inside new root
        doc.removeChild(oldRoot)
        newRoot.appendChild(oldRoot)
        doc.appendChild(newRoot)

        logs.add(
            AstChangeLog(
                nodeName = "FrameLayout",
                nodeId = null,
                changeType = "ROOT_GLASS_WRAPPER",
                detail = "Wrapped root layout in FrameLayout with '${options.glassSurfaceDrawableRef}'"
            )
        )

        return doc
    }

    /**
     * Formats and serializes a DOM Document back into an XML string.
     */
    private fun serializeDocument(doc: Document): String {
        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
            setOutputProperty(OutputKeys.ENCODING, "utf-8")
        }
        val writer = StringWriter()
        transformer.transform(DOMSource(doc), StreamResult(writer))
        return writer.toString()
    }

    /**
     * Generates companion drawable assets required by the transformed XML layouts.
     */
    fun generateCompanionDrawables(): Map<String, String> {
        return mapOf(
            "bg_glass_card.xml" to """
                <?xml version="1.0" encoding="utf-8"?>
                <!-- Auto-generated by GlassMorph Theme Engine -->
                <shape xmlns:android="http://schemas.android.com/apk/res/android"
                    android:shape="rectangle">
                    <corners android:radius="18dp" />
                    <solid android:color="#1AFFFFFF" />
                    <stroke
                        android:width="1.2dp"
                        android:color="#4DFFFFFF" />
                    <padding
                        android:left="12dp"
                        android:top="12dp"
                        android:right="12dp"
                        android:bottom="12dp" />
                </shape>
            """.trimIndent(),

            "bg_glass_surface.xml" to """
                <?xml version="1.0" encoding="utf-8"?>
                <!-- Auto-generated by GlassMorph Theme Engine -->
                <shape xmlns:android="http://schemas.android.com/apk/res/android"
                    android:shape="rectangle">
                    <gradient
                        android:startColor="#0DFFFFFF"
                        android:centerColor="#14FFFFFF"
                        android:endColor="#08FFFFFF"
                        android:angle="315" />
                    <stroke
                        android:width="1dp"
                        android:color="#33FFFFFF" />
                </shape>
            """.trimIndent(),

            "bg_glass_pill.xml" to """
                <?xml version="1.0" encoding="utf-8"?>
                <!-- Auto-generated by GlassMorph Theme Engine -->
                <shape xmlns:android="http://schemas.android.com/apk/res/android"
                    android:shape="rectangle">
                    <corners android:radius="100dp" />
                    <solid android:color="#26FFFFFF" />
                    <stroke
                        android:width="1dp"
                        android:color="#80FFFFFF" />
                    <padding
                        android:left="16dp"
                        android:top="8dp"
                        android:right="16dp"
                        android:bottom="8dp" />
                </shape>
            """.trimIndent(),

            "bg_liquid_panel.xml" to """
                <?xml version="1.0" encoding="utf-8"?>
                <!-- Auto-generated by GlassMorph Theme Engine -->
                <layer-list xmlns:android="http://schemas.android.com/apk/res/android">
                    <item>
                        <shape android:shape="rectangle">
                            <corners android:radius="24dp" />
                            <gradient
                                android:startColor="#2600F0FF"
                                android:centerColor="#1F7A22FF"
                                android:endColor="#14FFFFFF"
                                android:angle="135" />
                        </shape>
                    </item>
                    <item>
                        <shape android:shape="rectangle">
                            <corners android:radius="24dp" />
                            <stroke
                                android:width="1.5dp"
                                android:color="#66FFFFFF" />
                        </shape>
                    </item>
                </layer-list>
            """.trimIndent()
        )
    }
}

/**
 * Extension function to transform any XML layout string.
 */
fun String.transformToGlassLayout(
    options: XmlLayoutTransformerOptions = XmlLayoutTransformerOptions()
): TransformationResult {
    return XmlLayoutTransformer(options).transform(this)
}

/**
 * Extension function to transform an XML layout file directly.
 */
fun File.transformToGlassLayout(
    outputFile: File,
    options: XmlLayoutTransformerOptions = XmlLayoutTransformerOptions()
): TransformationResult {
    val inputXml = this.readText()
    val result = XmlLayoutTransformer(options).transform(inputXml)
    outputFile.parentFile?.mkdirs()
    outputFile.writeText(result.transformedXml)
    return result
}
