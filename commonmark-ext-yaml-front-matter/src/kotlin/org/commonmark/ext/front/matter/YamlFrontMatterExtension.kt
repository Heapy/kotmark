package org.commonmark.ext.front.matter

import org.commonmark.Extension
import org.commonmark.ext.front.matter.internal.YamlFrontMatterBlockParser
import org.commonmark.ext.front.matter.internal.YamlFrontMatterMarkdownNodeRenderer
import org.commonmark.parser.Parser
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext
import org.commonmark.renderer.markdown.MarkdownNodeRendererFactory
import org.commonmark.renderer.markdown.MarkdownRenderer
import kotlin.jvm.JvmStatic

class YamlFrontMatterExtension private constructor() :
    Parser.ParserExtension,
    MarkdownRenderer.MarkdownRendererExtension {
    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.customBlockParserFactory(YamlFrontMatterBlockParser.Factory())
    }

    override fun extend(rendererBuilder: MarkdownRenderer.Builder) {
        rendererBuilder.nodeRendererFactory(
            object : MarkdownNodeRendererFactory {
                override val specialCharacters: Set<Char> = emptySet()

                override fun create(context: MarkdownNodeRendererContext): NodeRenderer =
                    YamlFrontMatterMarkdownNodeRenderer(context)
            },
        )
    }

    companion object {
        @JvmStatic
        fun create(): Extension = YamlFrontMatterExtension()
    }
}
