package io.heapy.kotmark.ext.footnotes.internal

import io.heapy.kotmark.ext.footnotes.FootnoteDefinition
import io.heapy.kotmark.ext.footnotes.FootnoteReference
import io.heapy.kotmark.ext.footnotes.InlineFootnote
import io.heapy.kotmark.node.LinkReferenceDefinition
import io.heapy.kotmark.parser.InlineParserContext
import io.heapy.kotmark.parser.beta.LinkInfo
import io.heapy.kotmark.parser.beta.LinkProcessor
import io.heapy.kotmark.parser.beta.LinkResult
import io.heapy.kotmark.parser.beta.Scanner

class FootnoteLinkProcessor : LinkProcessor {
    override fun process(
        linkInfo: LinkInfo,
        scanner: Scanner,
        context: InlineParserContext,
    ): LinkResult? {
        if (linkInfo.marker()?.literal == "^") {
            return LinkResult.wrapTextIn(InlineFootnote(), linkInfo.afterTextBracket())
                .includeMarker()
        }
        if (linkInfo.destination() != null) {
            return LinkResult.none()
        }
        val text = linkInfo.text(scanner)
        if (!text.startsWith("^")) {
            return LinkResult.none()
        }
        val linkLabel = linkInfo.label()
        if (
            linkLabel != null &&
            context.getDefinition(LinkReferenceDefinition::class, linkLabel) != null
        ) {
            return LinkResult.none()
        }
        val label = text.substring(1)
        if (context.getDefinition(FootnoteDefinition::class, label) == null) {
            return LinkResult.none()
        }
        return LinkResult.replaceWith(FootnoteReference(label), linkInfo.afterTextBracket())
    }
}
