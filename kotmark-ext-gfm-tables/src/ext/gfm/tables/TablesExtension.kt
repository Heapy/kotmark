package io.heapy.kotmark.ext.gfm.tables

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ext.gfm.tables.internal.TableBlockParser
import io.heapy.kotmark.ext.gfm.tables.internal.TableHtmlNodeRenderer
import io.heapy.kotmark.ext.gfm.tables.internal.TableMarkdownNodeRenderer
import io.heapy.kotmark.ext.gfm.tables.internal.TableTextContentNodeRenderer
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererContext
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererFactory
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import io.heapy.kotmark.renderer.text.TextContentRenderer
import kotlin.jvm.JvmStatic

class TablesExtension private constructor() :
    Parser.ParserExtension,
    HtmlRenderer.HtmlRendererExtension,
    TextContentRenderer.TextContentRendererExtension,
    MarkdownRenderer.MarkdownRendererExtension {
    override fun extend(parserConfiguration: Parser.Configuration) {
        parserConfiguration.customBlockParserFactory(TableBlockParser.Factory())
    }

    override fun extend(rendererConfiguration: HtmlRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(::TableHtmlNodeRenderer)
    }

    override fun extend(rendererConfiguration: TextContentRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(::TableTextContentNodeRenderer)
    }

    override fun extend(rendererConfiguration: MarkdownRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(
            object : MarkdownNodeRendererFactory {
                override val specialCharacters: Set<Char> = setOf('|')

                override fun create(context: MarkdownNodeRendererContext): NodeRenderer =
                    TableMarkdownNodeRenderer(context)
            },
        )
    }

    companion object {
        @JvmStatic
        fun create(): Extension = TablesExtension()
    }
}
