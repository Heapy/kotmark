package org.commonmark.ext.footnotes.internal

import org.commonmark.ext.footnotes.FootnoteDefinition
import org.commonmark.ext.footnotes.FootnoteReference
import org.commonmark.ext.footnotes.InlineFootnote
import org.commonmark.node.LinkReferenceDefinition
import org.commonmark.parser.InlineParserContext
import org.commonmark.parser.beta.LinkInfo
import org.commonmark.parser.beta.LinkProcessor
import org.commonmark.parser.beta.LinkResult
import org.commonmark.parser.beta.Scanner

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
        val text = linkInfo.text()
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
