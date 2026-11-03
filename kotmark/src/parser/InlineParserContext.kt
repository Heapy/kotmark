package io.heapy.kotmark.parser

import io.heapy.kotmark.parser.beta.InlineContentParserFactory
import io.heapy.kotmark.parser.beta.LinkProcessor
import io.heapy.kotmark.parser.delimiter.DelimiterProcessor
import kotlin.reflect.KClass

interface InlineParserContext {
    val customInlineContentParserFactories: List<InlineContentParserFactory>
    val customDelimiterProcessors: List<DelimiterProcessor>
    val customLinkProcessors: List<LinkProcessor>
    val customLinkMarkers: Set<Char>
    /** The configured [Parser.Configuration.maxInlineNesting]. */
    val maxInlineNesting: Int

    fun <D : Any> getDefinition(type: KClass<D>, label: String): D?
}
