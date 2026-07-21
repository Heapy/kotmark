package org.commonmark.ext.front.matter.internal

import org.commonmark.ext.front.matter.YamlFrontMatterNode
import org.commonmark.node.Node
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext
import org.commonmark.renderer.markdown.MarkdownWriter

class YamlFrontMatterMarkdownNodeRenderer(
    context: MarkdownNodeRendererContext,
) : YamlFrontMatterNodeRenderer() {
    private val writer: MarkdownWriter = context.writer

    override fun render(node: Node) {
        renderBoundary()
        var child = node.firstChild
        while (child != null) {
            if (child is YamlFrontMatterNode) {
                renderNode(child)
            }
            child = child.next
        }
        renderBoundary()
        writer.line()
    }

    private fun renderBoundary() {
        writer.raw("---")
        writer.line()
    }

    private fun renderNode(node: YamlFrontMatterNode) {
        val values = node.values
        when {
            values.isEmpty() -> renderEmptyValue(node.key)
            values.size == 1 && '\n' in values[0] ->
                renderMultiLineValue(node.key, values[0].split('\n'))
            values.size == 1 -> renderSingleValue(node.key, values[0])
            else -> renderListValue(node.key, values)
        }
    }

    private fun renderEmptyValue(key: String) {
        writer.raw("$key:")
        writer.line()
    }

    private fun renderSingleValue(key: String, value: String) {
        writer.raw("$key: ${escapeValue(value)}")
        writer.line()
    }

    private fun renderMultiLineValue(key: String, lines: List<String>) {
        writer.raw("$key: |")
        writer.line()
        for (line in lines) {
            writer.raw("  $line")
            writer.line()
        }
    }

    private fun renderListValue(key: String, values: List<String>) {
        writer.raw("$key:")
        writer.line()
        for (value in values) {
            writer.raw("  - ${escapeValue(value)}")
            writer.line()
        }
    }

    private fun escapeValue(value: String): String =
        if (needsQuoting(value)) "'${value.replace("'", "''")}'" else value

    private fun needsQuoting(value: String): Boolean {
        if (isFlowCollection(value)) {
            return false
        }
        return value.isEmpty() ||
            ": " in value ||
            " #" in value ||
            value.startsWith("-") ||
            "'" in value ||
            "\"" in value ||
            value.startsWith("[") ||
            value.startsWith("{")
    }

    private fun isFlowCollection(value: String): Boolean =
        (value.startsWith("[") && value.endsWith("]")) ||
            (value.startsWith("{") && value.endsWith("}"))
}
