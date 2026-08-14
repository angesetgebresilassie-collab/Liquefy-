package com.example.glassengine.modifier

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Modification result from theme and manifest patchers.
 */
data class PatchResult(
    val originalXml: String,
    val patchedXml: String,
    val isModified: Boolean,
    val changeLogs: List<String>
)

/**
 * Production-grade helper utilities to patch Android `themes.xml`/`styles.xml` and `AndroidManifest.xml`
 * to enable glass transparency, translucent window compositing, and GPU hardware acceleration.
 */
object ThemeManifestModifier {

    /**
     * Patches a `themes.xml` or `styles.xml` string to inject glassmorphism window attributes:
     * - `android:windowIsTranslucent` = `true`
     * - `android:windowBackground` = `@android:color/transparent`
     * - `android:statusBarColor` = `@android:color/transparent`
     * - `android:navigationBarColor` = `@android:color/transparent`
     */
    @JvmStatic
    @JvmOverloads
    fun patchThemesXml(
        themesXmlContent: String,
        transparentBars: Boolean = true
    ): PatchResult {
        if (themesXmlContent.isBlank()) {
            return PatchResult(themesXmlContent, themesXmlContent, false, listOf("Themes content was empty."))
        }

        val logs = mutableListOf<String>()

        try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                isIgnoringComments = false
            }
            val doc: Document = factory.newDocumentBuilder().parse(InputSource(StringReader(themesXmlContent)))
            val styleNodes = doc.getElementsByTagName("style")

            if (styleNodes.length == 0) {
                logs.add("No <style> elements found in themes XML.")
                return PatchResult(themesXmlContent, themesXmlContent, false, logs)
            }

            var modified = false

            for (i in 0 until styleNodes.length) {
                val styleElement = styleNodes.item(i) as Element
                val styleName = styleElement.getAttribute("name")

                // Inject / Update windowIsTranslucent
                setOrUpdateStyleItem(doc, styleElement, "android:windowIsTranslucent", "true", logs, styleName)
                
                // Inject / Update windowBackground
                setOrUpdateStyleItem(doc, styleElement, "android:windowBackground", "@android:color/transparent", logs, styleName)

                if (transparentBars) {
                    setOrUpdateStyleItem(doc, styleElement, "android:statusBarColor", "@android:color/transparent", logs, styleName)
                    setOrUpdateStyleItem(doc, styleElement, "android:navigationBarColor", "@android:color/transparent", logs, styleName)
                    setOrUpdateStyleItem(doc, styleElement, "android:enforceNavigationBarContrast", "false", logs, styleName)
                }

                modified = true
            }

            val serialized = serializeDoc(doc)
            return PatchResult(
                originalXml = themesXmlContent,
                patchedXml = serialized,
                isModified = modified,
                changeLogs = logs
            )

        } catch (e: Exception) {
            // Regex fallback if DOM parse encounters non-standard XML tokens
            logs.add("DOM parsing failed (${e.message}), using regex patcher fallback.")
            return patchThemesWithRegex(themesXmlContent, transparentBars, logs)
        }
    }

    private fun setOrUpdateStyleItem(
        doc: Document,
        styleElement: Element,
        itemName: String,
        itemValue: String,
        logs: MutableList<String>,
        styleName: String
    ) {
        val childNodes = styleElement.childNodes
        var existingItem: Element? = null

        for (j in 0 until childNodes.length) {
            val child = childNodes.item(j)
            if (child.nodeType == Node.ELEMENT_NODE && child.nodeName == "item") {
                val elem = child as Element
                if (elem.getAttribute("name") == itemName) {
                    existingItem = elem
                    break
                }
            }
        }

        if (existingItem != null) {
            if (existingItem.textContent != itemValue) {
                val oldVal = existingItem.textContent
                existingItem.textContent = itemValue
                logs.add("[$styleName] Updated <item name=\"$itemName\"> from '$oldVal' to '$itemValue'")
            }
        } else {
            val newItem = doc.createElement("item")
            newItem.setAttribute("name", itemName)
            newItem.textContent = itemValue
            styleElement.appendChild(newItem)
            logs.add("[$styleName] Injected <item name=\"$itemName\">$itemValue</item>")
        }
    }

    private fun patchThemesWithRegex(
        content: String,
        transparentBars: Boolean,
        logs: MutableList<String>
    ): PatchResult {
        val styleTagRegex = Regex("""(<style[^>]*>)([\s\S]*?)(</style>)""")
        var modified = false

        val patched = styleTagRegex.replace(content) { match ->
            val openTag = match.groupValues[1]
            var body = match.groupValues[2]
            val closeTag = match.groupValues[3]

            val itemsToEnsure = mutableListOf(
                "android:windowIsTranslucent" to "true",
                "android:windowBackground" to "@android:color/transparent"
            )
            if (transparentBars) {
                itemsToEnsure.add("android:statusBarColor" to "@android:color/transparent")
                itemsToEnsure.add("android:navigationBarColor" to "@android:color/transparent")
            }

            for ((name, value) in itemsToEnsure) {
                val itemPattern = Regex("""<item\s+name=["']$name["']>[\s\S]*?</item>""")
                if (itemPattern.containsMatchIn(body)) {
                    body = itemPattern.replace(body, "<item name=\"$name\">$value</item>")
                    logs.add("Regex updated '$name' -> '$value'")
                } else {
                    body += "\n        <item name=\"$name\">$value</item>"
                    logs.add("Regex injected '<item name=\"$name\">$value</item>'")
                }
            }

            modified = true
            "$openTag$body\n    $closeTag"
        }

        return PatchResult(content, patched, modified, logs)
    }

    /**
     * Patches an `AndroidManifest.xml` string to ensure `android:hardwareAccelerated="true"`
     * is configured on the `<application>` element.
     */
    @JvmStatic
    fun patchAndroidManifestXml(manifestContent: String): PatchResult {
        if (manifestContent.isBlank()) {
            return PatchResult(manifestContent, manifestContent, false, listOf("Manifest content was empty."))
        }

        val logs = mutableListOf<String>()

        try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                isIgnoringComments = false
            }
            val doc: Document = factory.newDocumentBuilder().parse(InputSource(StringReader(manifestContent)))
            val appNodes = doc.getElementsByTagName("application")

            if (appNodes.length == 0) {
                logs.add("No <application> element found in manifest.")
                return PatchResult(manifestContent, manifestContent, false, logs)
            }

            val appElement = appNodes.item(0) as Element
            val currentHw = appElement.getAttribute("android:hardwareAccelerated")

            var isModified = false
            if (currentHw != "true") {
                appElement.setAttribute("android:hardwareAccelerated", "true")
                logs.add("Configured 'android:hardwareAccelerated=\"true\"' on <application>.")
                isModified = true
            } else {
                logs.add("<application> already has hardware acceleration enabled.")
            }

            val serialized = serializeDoc(doc)
            return PatchResult(manifestContent, serialized, isModified, logs)

        } catch (e: Exception) {
            logs.add("DOM parsing failed (${e.message}), applying regex manifest patch.")
            return patchManifestWithRegex(manifestContent, logs)
        }
    }

    private fun patchManifestWithRegex(content: String, logs: MutableList<String>): PatchResult {
        val appTagRegex = Regex("""<application\b([^>]*)>""")
        val match = appTagRegex.find(content)

        if (match != null) {
            val attributes = match.groupValues[1]
            return if (!attributes.contains("android:hardwareAccelerated")) {
                val newAppTag = "<application\n        android:hardwareAccelerated=\"true\"$attributes>"
                val patched = content.replaceRange(match.range, newAppTag)
                logs.add("Injected 'android:hardwareAccelerated=\"true\"' into <application> via regex.")
                PatchResult(content, patched, true, logs)
            } else {
                val updatedAttrs = attributes.replace(Regex("""android:hardwareAccelerated\s*=\s*["'][^"']*["']"""), "android:hardwareAccelerated=\"true\"")
                val newAppTag = "<application$updatedAttrs>"
                val patched = content.replaceRange(match.range, newAppTag)
                logs.add("Updated 'android:hardwareAccelerated' to 'true' via regex.")
                PatchResult(content, patched, true, logs)
            }
        }

        logs.add("Could not find <application> tag in manifest.")
        return PatchResult(content, content, false, logs)
    }

    private fun serializeDoc(doc: Document): String {
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
}

/**
 * Extension function on String to patch themes XML for glassmorphism.
 */
fun String.patchThemeForGlassmorphism(transparentBars: Boolean = true): PatchResult {
    return ThemeManifestModifier.patchThemesXml(this, transparentBars)
}

/**
 * Extension function on String to patch AndroidManifest.xml for GPU hardware acceleration.
 */
fun String.patchManifestForHardwareAcceleration(): PatchResult {
    return ThemeManifestModifier.patchAndroidManifestXml(this)
}

/**
 * Extension function on File to patch themes or styles file.
 */
fun File.patchThemeForGlassmorphism(outputFile: File = this, transparentBars: Boolean = true): PatchResult {
    val result = ThemeManifestModifier.patchThemesXml(this.readText(), transparentBars)
    if (result.isModified) {
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(result.patchedXml)
    }
    return result
}

/**
 * Extension function on File to patch AndroidManifest.xml file.
 */
fun File.patchManifestForHardwareAcceleration(outputFile: File = this): PatchResult {
    val result = ThemeManifestModifier.patchAndroidManifestXml(this.readText())
    if (result.isModified) {
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(result.patchedXml)
    }
    return result
}
