package io.heapy.kotmark.ext.front.matter

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ext.front.matter.internal.YamlFrontMatterBlockParser
import io.heapy.kotmark.ext.front.matter.internal.YamlFrontMatterMarkdownNodeRenderer
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererContext
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererFactory
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import kotlin.jvm.JvmStatic

class YamlFrontMatterExtension private constructor() :
    Parser.ParserExtension,
    MarkdownRenderer.MarkdownRendererExtension {
    override fun extend(parserConfiguration: Parser.Configuration) {
        parserConfiguration.customBlockParserFactory(YamlFrontMatterBlockParser.Factory())
    }

    override fun extend(rendererConfiguration: MarkdownRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(
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
