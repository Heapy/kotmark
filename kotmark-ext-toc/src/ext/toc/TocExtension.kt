package io.heapy.kotmark.ext.toc

import io.heapy.kotmark.ExtensionsBuilder
import io.heapy.kotmark.KotmarkDsl
import io.heapy.kotmark.ext.toc.internal.TocBlockParser
import io.heapy.kotmark.ext.toc.internal.TocHtmlNodeRenderer
import io.heapy.kotmark.ext.toc.internal.TocTextNodeRenderer
import io.heapy.kotmark.ext.toc.internal.documentNodes
import io.heapy.kotmark.node.Document
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import io.heapy.kotmark.renderer.markdown.nodeRenderer
import io.heapy.kotmark.renderer.text.TextContentRenderer

/** Opt-in TOC markers, HTML heading IDs and outlines. See the module README for rendering contracts. */
class TocExtension private constructor(configuration: Configuration) :
    Parser.ParserExtension,
    HtmlRenderer.HtmlRendererExtension,
    MarkdownRenderer.MarkdownRendererExtension,
    TextContentRenderer.TextContentRendererExtension {
    private val markers = configuration.markers.toList()
    private val levels = configuration.headingLevels
    private val placement = configuration.placement

    override fun extend(parserConfiguration: Parser.Configuration) {
        parserConfiguration.customBlockParserFactory(TocBlockParser.Factory(markers.toSet()))
        if (placement != TocPlacement.MARKERS) {
            parserConfiguration.postProcessor { root ->
                if (root is Document && documentNodes(root).none { it is TableOfContents }) {
                    val toc = TableOfContents(markers.first())
                    if (placement == TocPlacement.PREPEND_IF_MISSING) root.prependChild(toc) else root.appendChild(toc)
                }
                root
            }
        }
    }

    override fun extend(rendererConfiguration: HtmlRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory { TocHtmlNodeRenderer(it, levels) }
    }

    override fun extend(rendererConfiguration: MarkdownRenderer.Configuration) {
        rendererConfiguration.nodeRenderer<TableOfContents> { node ->
            writer.raw(node.marker)
            writer.block()
        }
    }

    override fun extend(rendererConfiguration: TextContentRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory { TocTextNodeRenderer(it, levels) }
    }

    @KotmarkDsl
    class Configuration internal constructor() {
        /** Case-sensitive, standalone-line markers. Replacing this list replaces both defaults. */
        var markers: List<String> = listOf("[TOC]", "[[_TOC_]]")
        /** Inclusive heading levels, within 1..6. Filtering does not change ID allocation. */
        var headingLevels: IntRange = 1..6
        var placement: TocPlacement = TocPlacement.MARKERS

        internal fun create(): TocExtension {
            require(markers.isNotEmpty() && markers.all {
                it.isNotEmpty() && it == it.trim() && '\n' !in it && '\r' !in it
            }) { "TOC markers must be nonempty single lines without surrounding whitespace" }
            require(!headingLevels.isEmpty() && headingLevels.first >= 1 && headingLevels.last <= 6) {
                "TOC headingLevels must be a nonempty range within 1..6"
            }
            return TocExtension(this)
        }
    }
}

/** Creates [TocExtension], snapshotting its configuration. */
fun tocExtension(configure: TocExtension.Configuration.() -> Unit = {}): TocExtension =
    TocExtension.Configuration().apply(configure).create()

/** Creates and appends [TocExtension]. */
fun ExtensionsBuilder.toc(configure: TocExtension.Configuration.() -> Unit = {}) {
    add(tocExtension(configure))
}
