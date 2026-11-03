package io.heapy.kotmark.ext.math

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ext.math.internal.MathBlockParser
import io.heapy.kotmark.ext.math.internal.MathInlineParser
import io.heapy.kotmark.ext.math.internal.mathNodeRenderers
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import io.heapy.kotmark.renderer.text.TextContentRenderer
import kotlin.jvm.JvmStatic

/** Dollar-delimited math with opaque content; see this module's README for the grammar. */
class MathExtension private constructor() :
    Parser.ParserExtension,
    HtmlRenderer.HtmlRendererExtension,
    TextContentRenderer.TextContentRendererExtension,
    MarkdownRenderer.MarkdownRendererExtension {
    override fun extend(parserConfiguration: Parser.Configuration) {
        parserConfiguration.customInlineContentParserFactory(MathInlineParser.Factory())
        parserConfiguration.customBlockParserFactory(MathBlockParser.Factory())
    }

    override fun extend(rendererConfiguration: HtmlRenderer.Configuration) {
        rendererConfiguration.mathNodeRenderers()
    }

    override fun extend(rendererConfiguration: TextContentRenderer.Configuration) {
        rendererConfiguration.mathNodeRenderers()
    }

    override fun extend(rendererConfiguration: MarkdownRenderer.Configuration) {
        rendererConfiguration.mathNodeRenderers()
    }

    companion object {
        @JvmStatic
        fun create(): Extension = MathExtension()
    }
}
