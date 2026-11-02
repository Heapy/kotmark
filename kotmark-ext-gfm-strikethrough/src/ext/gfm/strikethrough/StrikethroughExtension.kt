package io.heapy.kotmark.ext.gfm.strikethrough

import io.heapy.kotmark.Extension
import io.heapy.kotmark.KotmarkDsl
import io.heapy.kotmark.ext.gfm.strikethrough.internal.StrikethroughDelimiterProcessor
import io.heapy.kotmark.ext.gfm.strikethrough.internal.StrikethroughHtmlNodeRenderer
import io.heapy.kotmark.ext.gfm.strikethrough.internal.StrikethroughMarkdownNodeRenderer
import io.heapy.kotmark.ext.gfm.strikethrough.internal.StrikethroughTextContentNodeRenderer
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererContext
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererFactory
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import io.heapy.kotmark.renderer.text.TextContentRenderer
import kotlin.jvm.JvmStatic

class StrikethroughExtension private constructor(
    private val requireTwoTildes: Boolean,
) : Parser.ParserExtension,
    HtmlRenderer.HtmlRendererExtension,
    TextContentRenderer.TextContentRendererExtension,
    MarkdownRenderer.MarkdownRendererExtension {
    override fun extend(parserConfiguration: Parser.Configuration) {
        parserConfiguration.customDelimiterProcessor(
            StrikethroughDelimiterProcessor(requireTwoTildes),
        )
    }

    override fun extend(rendererConfiguration: HtmlRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(::StrikethroughHtmlNodeRenderer)
    }

    override fun extend(rendererConfiguration: TextContentRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(::StrikethroughTextContentNodeRenderer)
    }

    override fun extend(rendererConfiguration: MarkdownRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(
            object : MarkdownNodeRendererFactory {
                override val specialCharacters: Set<Char> = setOf('~')

                override fun create(context: MarkdownNodeRendererContext): NodeRenderer =
                    StrikethroughMarkdownNodeRenderer(context)
            },
        )
    }

    @KotmarkDsl
    class Configuration internal constructor() {
        var requireTwoTildes = false

        internal fun create(): StrikethroughExtension = StrikethroughExtension(requireTwoTildes)
    }

    companion object {
        @JvmStatic
        fun create(): Extension = strikethroughExtension()
    }
}

/** Creates [StrikethroughExtension] with the supplied configuration. */
fun strikethroughExtension(configure: StrikethroughExtension.Configuration.() -> Unit = {}): StrikethroughExtension =
    StrikethroughExtension.Configuration().apply(configure).create()
