package io.heapy.kotmark.ext.gfm.strikethrough

import io.heapy.kotmark.Extension
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
