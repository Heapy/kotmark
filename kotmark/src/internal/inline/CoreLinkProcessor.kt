package io.heapy.kotmark.internal.inline

import io.heapy.kotmark.node.Image
import io.heapy.kotmark.node.Link
import io.heapy.kotmark.node.LinkReferenceDefinition
import io.heapy.kotmark.parser.InlineParserContext
import io.heapy.kotmark.parser.beta.LinkInfo
import io.heapy.kotmark.parser.beta.LinkProcessor
import io.heapy.kotmark.parser.beta.LinkResult
import io.heapy.kotmark.parser.beta.Scanner

class CoreLinkProcessor : LinkProcessor {
    override fun process(
        linkInfo: LinkInfo,
        scanner: Scanner,
        context: InlineParserContext,
    ): LinkResult? {
        val destination = linkInfo.destination()
        if (destination != null) {
            return process(linkInfo, scanner, destination, linkInfo.title())
        }

        val label = linkInfo.label()
        val reference = if (!label.isNullOrEmpty()) label else linkInfo.text(scanner)
        val definition = context.getDefinition(LinkReferenceDefinition::class, reference)
            ?: return LinkResult.none()
        return process(linkInfo, scanner, definition.destination, definition.title)
    }

    private fun process(
        linkInfo: LinkInfo,
        scanner: Scanner,
        destination: String?,
        title: String?,
    ): LinkResult {
        if (linkInfo.marker()?.literal == "!") {
            return LinkResult.wrapTextIn(Image(destination, title), scanner.position()).includeMarker()
        }
        return LinkResult.wrapTextIn(Link(destination, title), scanner.position())
    }
}
