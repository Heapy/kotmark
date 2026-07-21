package org.commonmark.ext.footnotes

import org.commonmark.Extension
import org.commonmark.ext.footnotes.internal.FootnoteBlockParser
import org.commonmark.ext.footnotes.internal.FootnoteHtmlNodeRenderer
import org.commonmark.ext.footnotes.internal.FootnoteLinkProcessor
import org.commonmark.ext.footnotes.internal.FootnoteMarkdownNodeRenderer
import org.commonmark.parser.Parser
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext
import org.commonmark.renderer.markdown.MarkdownNodeRendererFactory
import org.commonmark.renderer.markdown.MarkdownRenderer
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
