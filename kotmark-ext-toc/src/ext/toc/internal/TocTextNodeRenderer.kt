package io.heapy.kotmark.ext.toc.internal

import io.heapy.kotmark.ext.toc.TableOfContents
import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.text.TextContentNodeRendererContext

internal class TocTextNodeRenderer(
    private val context: TextContentNodeRendererContext,
    private val levels: IntRange,
) : NodeRenderer {
    override val nodeTypes = setOf(TableOfContents::class)
    private var entries: List<Entry>? = null

    override fun render(node: Node) {
        val current = entries ?: run {
            var root = node
            while (root.parent != null) root = root.parent!!
            documentNodes(root).filterIsInstance<Heading>()
                .filter { it.level in levels }.map { Entry(it.level, headingText(it)) }.toList()
                .also { entries = it }
        }
        if (current.isNotEmpty()) context.render(outline(current))
    }
}
