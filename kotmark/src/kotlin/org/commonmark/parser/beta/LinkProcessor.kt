package org.commonmark.parser.beta

import org.commonmark.parser.InlineParserContext

fun interface LinkProcessor {
    fun process(
        linkInfo: LinkInfo,
        scanner: Scanner,
        context: InlineParserContext,
    ): LinkResult?
}
