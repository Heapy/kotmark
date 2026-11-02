package io.heapy.kotmark.parser.beta

import io.heapy.kotmark.parser.InlineParserContext

fun interface LinkProcessor {
    fun process(
        linkInfo: LinkInfo,
        scanner: Scanner,
        context: InlineParserContext,
    ): LinkResult?
}
