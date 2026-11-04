package io.heapy.kotmark.ext.toc.internal

import io.heapy.kotmark.ext.heading.anchor.idGenerator
import io.heapy.kotmark.ext.toc.TableOfContents
import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.html.HtmlNodeRendererContext
import io.heapy.kotmark.renderer.html.renderChildren

internal class TocHtmlNodeRenderer(
    private val context: HtmlNodeRendererContext,
    private val levels: IntRange,
) : NodeRenderer {
    override val nodeTypes = setOf(TableOfContents::class, Heading::class)
    private val attributes = mutableMapOf<Heading, Map<String, String?>>()
    private val fallback = idGenerator()
    private val usedIds = mutableSetOf<String>()
    private var entries: List<Entry> = emptyList()

    override fun beforeRoot(rootNode: Node) {
        val headings = documentNodes(rootNode).filterIsInstance<Heading>().toList()
        // Invoke user providers exactly once, reserving all supplied IDs before filling gaps.
        for (heading in headings) {
            val attrs = context.extendAttributes(heading, "h${heading.level}", emptyMap()).toMap()
            attributes[heading] = attrs
            attrs["id"]?.let(usedIds::add)
        }
        entries = headings.mapNotNull { heading ->
            val attrs = headingAttributes(heading)
            if (heading.level in levels) Entry(heading.level, headingText(heading), fragment(attrs.getValue("id")!!))
            else null
        }
    }

    private fun headingAttributes(heading: Heading): Map<String, String?> {
        val attrs = attributes.getOrPut(heading) {
            context.extendAttributes(heading, "h${heading.level}", emptyMap()).toMap()
        }
        if (attrs["id"] != null) return attrs
        var id: String
        do { id = fallback.generateId(headingText(heading)) } while (!usedIds.add(id))
        return (attrs + ("id" to id)).also { attributes[heading] = it }
    }

    override fun render(node: Node) {
        when (node) {
            is Heading -> {
                val html = context.writer
                val tag = "h${node.level}"
                html.line()
                html.tag(tag, headingAttributes(node))
                context.renderChildren(node)
                html.tag("/$tag")
                html.line()
            }
            is TableOfContents -> if (entries.isNotEmpty()) context.render(outline(entries))
        }
    }
}
