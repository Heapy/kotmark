package io.heapy.kotmark.docs.examples.extensions

// region heading-anchor
import io.heapy.kotmark.ext.heading.anchor.headingAnchorExtension
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

fun renderWithHeadingIds(markdown: String): String {
    val anchors = headingAnchorExtension {
        idPrefix = "section-"
        defaultId = "untitled"
    }
    // Heading IDs are an HTML attribute; the parser needs no extension.
    val document = parser().parse(markdown)
    return htmlRenderer { extensions(listOf(anchors)) }.render(document)
}
// endregion heading-anchor
