package io.heapy.kotmark.ext.footnotes

import io.heapy.kotmark.Extension
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
    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder
            .customBlockParserFactory(FootnoteBlockParser.Factory())
            .linkProcessor(FootnoteLinkProcessor())
        if (inlineFootnotes) {
            parserBuilder.linkMarker('^')
        }
    }

    override fun extend(rendererBuilder: HtmlRenderer.Builder) {
        rendererBuilder.nodeRendererFactory(::FootnoteHtmlNodeRenderer)
    }

    override fun extend(rendererBuilder: MarkdownRenderer.Builder) {
        rendererBuilder.nodeRendererFactory(
            object : MarkdownNodeRendererFactory {
                override val specialCharacters: Set<Char> = emptySet()

                override fun create(context: MarkdownNodeRendererContext): NodeRenderer =
                    FootnoteMarkdownNodeRenderer(context)
            },
        )
    }

    class Builder internal constructor() {
        private var inlineFootnotes = false

        fun inlineFootnotes(inlineFootnotes: Boolean): Builder = apply {
            this.inlineFootnotes = inlineFootnotes
        }

        fun build(): FootnotesExtension = FootnotesExtension(inlineFootnotes)
    }

    companion object {
        @JvmStatic
        fun create(): Extension = builder().build()

        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
