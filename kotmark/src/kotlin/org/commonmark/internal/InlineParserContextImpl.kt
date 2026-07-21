package org.commonmark.internal

import org.commonmark.parser.InlineParserContext
import org.commonmark.parser.beta.InlineContentParserFactory
import org.commonmark.parser.beta.LinkProcessor
import org.commonmark.parser.delimiter.DelimiterProcessor
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
