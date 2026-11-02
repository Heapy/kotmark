package io.heapy.kotmark.ext.ins

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ext.ins.internal.InsDelimiterProcessor
import io.heapy.kotmark.ext.ins.internal.InsHtmlNodeRenderer
import io.heapy.kotmark.ext.ins.internal.InsMarkdownNodeRenderer
import io.heapy.kotmark.ext.ins.internal.InsTextContentNodeRenderer
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererContext
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererFactory
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import io.heapy.kotmark.renderer.text.TextContentRenderer
import kotlin.jvm.JvmStatic

class InsExtension private constructor() :
    Parser.ParserExtension,
    HtmlRenderer.HtmlRendererExtension,
    TextContentRenderer.TextContentRendererExtension,
    MarkdownRenderer.MarkdownRendererExtension {
    override fun extend(parserConfiguration: Parser.Configuration) {
        parserConfiguration.customDelimiterProcessor(InsDelimiterProcessor())
    }

    override fun extend(rendererConfiguration: HtmlRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(::InsHtmlNodeRenderer)
    }

    override fun extend(rendererConfiguration: TextContentRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(::InsTextContentNodeRenderer)
    }

    override fun extend(rendererConfiguration: MarkdownRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(
            object : MarkdownNodeRendererFactory {
                override val specialCharacters: Set<Char> = setOf('+')

                override fun create(context: MarkdownNodeRendererContext): NodeRenderer =
                    InsMarkdownNodeRenderer(context)
            },
        )
    }

    companion object {
        @JvmStatic
        fun create(): Extension = InsExtension()
    }
}
