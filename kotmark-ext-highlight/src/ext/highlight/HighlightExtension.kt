package io.heapy.kotmark.ext.highlight

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ext.highlight.internal.HighlightDelimiterProcessor
import io.heapy.kotmark.ext.highlight.internal.HighlightHtmlNodeRenderer
import io.heapy.kotmark.ext.highlight.internal.HighlightMarkdownNodeRenderer
import io.heapy.kotmark.ext.highlight.internal.HighlightTextContentNodeRenderer
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererContext
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererFactory
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import io.heapy.kotmark.renderer.text.TextContentRenderer
import kotlin.jvm.JvmStatic

class HighlightExtension private constructor() :
    Parser.ParserExtension,
    HtmlRenderer.HtmlRendererExtension,
    TextContentRenderer.TextContentRendererExtension,
    MarkdownRenderer.MarkdownRendererExtension {
    override fun extend(parserConfiguration: Parser.Configuration) {
        parserConfiguration.customDelimiterProcessor(HighlightDelimiterProcessor())
    }

    override fun extend(rendererConfiguration: HtmlRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(::HighlightHtmlNodeRenderer)
    }

    override fun extend(rendererConfiguration: TextContentRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(::HighlightTextContentNodeRenderer)
    }

    override fun extend(rendererConfiguration: MarkdownRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(
            object : MarkdownNodeRendererFactory {
                override val specialCharacters: Set<Char> = setOf('=')

                override fun create(context: MarkdownNodeRendererContext): NodeRenderer =
                    HighlightMarkdownNodeRenderer(context)
            },
        )
    }

    companion object {
        @JvmStatic
        fun create(): Extension = HighlightExtension()
    }
}
