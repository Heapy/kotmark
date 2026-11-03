package io.heapy.kotmark.ext.front.matter

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ext.front.matter.extractor.YamlDataExtractor
import io.heapy.kotmark.ext.front.matter.internal.YamlFrontMatterBlockParser
import io.heapy.kotmark.ext.front.matter.internal.YamlFrontMatterMarkdownNodeRenderer
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererContext
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererFactory
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import kotlin.jvm.JvmStatic

class YamlFrontMatterExtension private constructor(
    private val extractorFactory: YamlFrontMatterExtractor.Factory,
) :
    Parser.ParserExtension,
    MarkdownRenderer.MarkdownRendererExtension {
    override fun extend(parserConfiguration: Parser.Configuration) {
        parserConfiguration.customBlockParserFactory(YamlFrontMatterBlockParser.Factory(extractorFactory))
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
        fun create(): Extension = create(YamlDataExtractor.Factory())

        /** Uses [extractorFactory] to create a separate extractor for every front matter block. */
        @JvmStatic
        fun create(extractorFactory: YamlFrontMatterExtractor.Factory): Extension =
            YamlFrontMatterExtension(extractorFactory)
    }
}
