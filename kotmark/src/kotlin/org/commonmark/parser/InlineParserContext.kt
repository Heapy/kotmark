package org.commonmark.parser

import org.commonmark.node.LinkReferenceDefinition
import org.commonmark.parser.beta.InlineContentParserFactory
import org.commonmark.parser.beta.LinkProcessor
import org.commonmark.parser.delimiter.DelimiterProcessor
import kotlin.reflect.KClass

interface InlineParserContext {
    val customInlineContentParserFactories: List<InlineContentParserFactory>
    val customDelimiterProcessors: List<DelimiterProcessor>
    val customLinkProcessors: List<LinkProcessor>
    val customLinkMarkers: Set<Char>

    @Deprecated("Use getDefinition(LinkReferenceDefinition::class, label)")
    fun getLinkReferenceDefinition(label: String): LinkReferenceDefinition? =
        getDefinition(LinkReferenceDefinition::class, label)

    fun <D : Any> getDefinition(type: KClass<D>, label: String): D?
}
