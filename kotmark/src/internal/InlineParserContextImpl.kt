package io.heapy.kotmark.internal

import io.heapy.kotmark.parser.InlineParserContext
import io.heapy.kotmark.parser.beta.InlineContentParserFactory
import io.heapy.kotmark.parser.beta.LinkProcessor
import io.heapy.kotmark.parser.delimiter.DelimiterProcessor
import kotlin.reflect.KClass

class InlineParserContextImpl(
    override val customInlineContentParserFactories: List<InlineContentParserFactory>,
    override val customDelimiterProcessors: List<DelimiterProcessor>,
    override val customLinkProcessors: List<LinkProcessor>,
    override val customLinkMarkers: Set<Char>,
    private val definitions: Definitions,
) : InlineParserContext {
    override fun <D : Any> getDefinition(type: KClass<D>, label: String): D? =
        definitions.getDefinition(type, label)
}
