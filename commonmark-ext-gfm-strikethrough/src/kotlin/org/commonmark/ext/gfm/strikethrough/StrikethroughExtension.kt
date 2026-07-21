package org.commonmark.ext.gfm.strikethrough

import org.commonmark.Extension
import org.commonmark.ext.gfm.strikethrough.internal.StrikethroughDelimiterProcessor
import org.commonmark.ext.gfm.strikethrough.internal.StrikethroughHtmlNodeRenderer
import org.commonmark.ext.gfm.strikethrough.internal.StrikethroughMarkdownNodeRenderer
import org.commonmark.ext.gfm.strikethrough.internal.StrikethroughTextContentNodeRenderer
import org.commonmark.parser.Parser
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext
import org.commonmark.renderer.markdown.MarkdownNodeRendererFactory
import org.commonmark.renderer.markdown.MarkdownRenderer
import org.commonmark.renderer.text.TextContentRenderer
import kotlin.jvm.JvmStatic

class StrikethroughExtension private constructor(
    private val requireTwoTildes: Boolean,
) : Parser.ParserExtension,
    HtmlRenderer.HtmlRendererExtension,
    TextContentRenderer.TextContentRendererExtension,
    MarkdownRenderer.MarkdownRendererExtension {
    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.customDelimiterProcessor(
            StrikethroughDelimiterProcessor(requireTwoTildes),
        )
    }

    override fun extend(rendererBuilder: HtmlRenderer.Builder) {
        rendererBuilder.nodeRendererFactory(::StrikethroughHtmlNodeRenderer)
    }

    override fun extend(rendererBuilder: TextContentRenderer.Builder) {
        rendererBuilder.nodeRendererFactory(::StrikethroughTextContentNodeRenderer)
    }

    override fun extend(rendererBuilder: MarkdownRenderer.Builder) {
        rendererBuilder.nodeRendererFactory(
            object : MarkdownNodeRendererFactory {
                override val specialCharacters: Set<Char> = setOf('~')

                override fun create(context: MarkdownNodeRendererContext): NodeRenderer =
                    StrikethroughMarkdownNodeRenderer(context)
            },
        )
    }

    class Builder internal constructor() {
        private var requireTwoTildes = false

        fun requireTwoTildes(requireTwoTildes: Boolean): Builder = apply {
            this.requireTwoTildes = requireTwoTildes
        }

        fun build(): Extension = StrikethroughExtension(requireTwoTildes)
    }

    companion object {
        @JvmStatic
        fun create(): Extension = builder().build()

        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
