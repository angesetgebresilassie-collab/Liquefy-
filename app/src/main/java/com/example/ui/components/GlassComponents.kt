package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CodeAttr
import com.example.ui.theme.CodeBackground
import com.example.ui.theme.CodeComment
import com.example.ui.theme.CodeKeyword
import com.example.ui.theme.CodeString
import com.example.ui.theme.CodeTag
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassBorderWhite
import com.example.ui.theme.GlassCyan
import com.example.ui.theme.GlassObsidian
import com.example.ui.theme.GlassObsidianCard
import com.example.ui.theme.GlassObsidianSurface
import com.example.ui.theme.GlassPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Standard Glassmorphic Container Card with specular border and translucent background.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    strokeWidth: Dp = 1.2.dp,
    backgroundColor: Color = GlassObsidianCard,
    borderBrush: Brush = Brush.linearGradient(
        listOf(
            Color(0x80FFFFFF),
            Color(0x1AFFFFFF),
            Color(0x05FFFFFF),
            Color(0x4000F0FF)
        )
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .border(
                width = strokeWidth,
                brush = borderBrush,
                shape = RoundedCornerShape(cornerRadius)
            ),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * Glass pill badge with icon and label.
 */
@Composable
fun GlassPillBadge(
    text: String,
    icon: ImageVector? = null,
    color: Color = GlassCyan,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(100.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                    fontSize = 11.sp
                )
            )
        }
    }
}

/**
 * Interactive slider control with current numerical value display.
 */
@Composable
fun GlassSliderControl(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueFormatter: (Float) -> String = { "%.2f".format(it) },
    accentColor: Color = GlassCyan,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            )
            Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = accentColor.copy(alpha = 0.2f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )
    }
}

/**
 * Switch row with title and subtitle description.
 */
@Composable
fun GlassSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GlassObsidian,
                checkedTrackColor = GlassCyan,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = GlassObsidianCard
            )
        )
    }
}

/**
 * Syntax-highlighted code block with line numbers, copy button, and horizontal scrolling.
 */
@Composable
fun CodeBlockView(
    code: String,
    language: String = "xml",
    title: String? = null,
    maxLines: Int = 100,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val lines = remember(code) { code.lines() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CodeBackground)
            .border(1.dp, GlassBorderWhite, RoundedCornerShape(14.dp))
    ) {
        // Code Block Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassObsidianSurface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Window dots
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title ?: language.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                )
            }

            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Source Code", code)
                    clipboard.setPrimaryClip(clip)
                    copied = true
                    Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                    scope.launch {
                        delay(2000)
                        copied = false
                    }
                },
                modifier = Modifier.size(32.dp).testTag("copy_code_button")
            ) {
                Icon(
                    imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = if (copied) GlassCyan else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Code content with line numbers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            // Line numbers column
            Column(
                modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp)
            ) {
                lines.take(maxLines).forEachIndexed { index, _ ->
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    )
                }
            }

            // Syntax-highlighted code lines
            Column(modifier = Modifier.padding(end = 16.dp)) {
                lines.take(maxLines).forEach { line ->
                    val annotated = highlightCodeLine(line, language)
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Basic syntax highlighting parser for XML, Kotlin, Java, and AGSL Shaders.
 */
private fun highlightCodeLine(line: String, language: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val trimmed = line.trim()
        when {
            // Comments
            trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*") || trimmed.startsWith("<!--") -> {
                withStyle(SpanStyle(color = CodeComment)) {
                    append(line)
                }
            }
            language.equals("xml", ignoreCase = true) -> {
                // Highlight XML tags & attributes
                var i = 0
                while (i < line.length) {
                    val char = line[i]
                    if (char == '<') {
                        val closeAngle = line.indexOf('>', i)
                        val endPos = if (closeAngle != -1) closeAngle + 1 else line.length
                        val tagContent = line.substring(i, endPos)
                        
                        // Parse tag name and attributes
                        withStyle(SpanStyle(color = CodeTag)) {
                            append("<")
                        }
                        
                        val inside = tagContent.removePrefix("<").removeSuffix(">")
                        val parts = inside.split(Regex("\\s+"))
                        val tagName = parts.firstOrNull() ?: ""
                        withStyle(SpanStyle(color = CodeTag, fontWeight = FontWeight.Bold)) {
                            append(tagName)
                        }
                        
                        val remainder = inside.removePrefix(tagName)
                        // Simple regex for attr="value"
                        val attrPattern = Regex("""([a-zA-Z0-9_:]+)=(".*?"|'.*?')""")
                        var lastIdx = 0
                        for (match in attrPattern.findAll(remainder)) {
                            val attrName = match.groupValues[1]
                            val attrVal = match.groupValues[2]
                            val between = remainder.substring(lastIdx, match.range.first)
                            append(between)
                            withStyle(SpanStyle(color = CodeAttr)) {
                                append(attrName)
                            }
                            append("=")
                            withStyle(SpanStyle(color = CodeString)) {
                                append(attrVal)
                            }
                            lastIdx = match.range.last + 1
                        }
                        if (lastIdx < remainder.length) {
                            append(remainder.substring(lastIdx))
                        }
                        
                        if (tagContent.endsWith(">")) {
                            withStyle(SpanStyle(color = CodeTag)) {
                                append(">")
                            }
                        }
                        i = endPos
                    } else {
                        append(char)
                        i++
                    }
                }
            }
            else -> {
                // Kotlin / Java / AGSL
                val keywords = setOf(
                    "package", "import", "class", "fun", "val", "var", "override", "if", "else", "return",
                    "uniform", "shader", "float", "float2", "float3", "float4", "half4", "public", "protected",
                    "void", "private", "new", "true", "false", "null"
                )
                val tokens = line.split(Regex("(?<=\\b)|(?=\\b)|(?<=[(),;{}])|(?=[(),;{}])"))
                for (token in tokens) {
                    when {
                        token in keywords -> {
                            withStyle(SpanStyle(color = CodeKeyword, fontWeight = FontWeight.SemiBold)) {
                                append(token)
                            }
                        }
                        token.startsWith("\"") && token.endsWith("\"") -> {
                            withStyle(SpanStyle(color = CodeString)) {
                                append(token)
                            }
                        }
                        token.startsWith("@") -> {
                            withStyle(SpanStyle(color = GlassPurple)) {
                                append(token)
                            }
                        }
                        else -> {
                            withStyle(SpanStyle(color = TextPrimary)) {
                                append(token)
                            }
                        }
                    }
                }
            }
        }
    }
}
