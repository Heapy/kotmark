package io.heapy.kotmark.ext.footnotes

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ExtensionsBuilder
import io.heapy.kotmark.KotmarkDsl
import io.heapy.kotmark.ext.footnotes.internal.FootnoteBlockParser
import io.heapy.kotmark.ext.footnotes.internal.FootnoteHtmlNodeRenderer
import io.heapy.kotmark.ext.footnotes.internal.FootnoteLinkProcessor
import io.heapy.kotmark.ext.footnotes.internal.FootnoteMarkdownNodeRenderer
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererContext
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererFactory
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import kotlin.jvm.JvmStatic

class FootnotesExtension private constructor(
    private val inlineFootnotes: Boolean,
) : Parser.ParserExtension,
    HtmlRenderer.HtmlRendererExtension,
    MarkdownRenderer.MarkdownRendererExtension {
    override fun extend(parserConfiguration: Parser.Configuration) {
        parserConfiguration.customBlockParserFactory(FootnoteBlockParser.Factory())
        parserConfiguration.linkProcessor(FootnoteLinkProcessor())
        if (inlineFootnotes) {
            parserConfiguration.linkMarker('^')
        }
    }

    override fun extend(rendererConfiguration: HtmlRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(::FootnoteHtmlNodeRenderer)
    }

    override fun extend(rendererConfiguration: MarkdownRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(
            object : MarkdownNodeRendererFactory {
                override val specialCharacters: Set<Char> = emptySet()

                override fun create(context: MarkdownNodeRendererContext): NodeRenderer =
                    FootnoteMarkdownNodeRenderer(context)
            },
        )
    }

    @KotmarkDsl
    class Configuration internal constructor() {
        var inlineFootnotes = false

        internal fun create(): FootnotesExtension = FootnotesExtension(inlineFootnotes)
    }

    companion object {
        @JvmStatic
        fun create(): Extension = footnotesExtension()
    }
}

/** Creates [FootnotesExtension] with the supplied configuration. */
fun footnotesExtension(configure: FootnotesExtension.Configuration.() -> Unit = {}): FootnotesExtension =
    FootnotesExtension.Configuration().apply(configure).create()

/** Creates and appends [FootnotesExtension] with the supplied configuration. */
fun ExtensionsBuilder.footnotes(configure: FootnotesExtension.Configuration.() -> Unit = {}) {
    add(footnotesExtension(configure))
}
