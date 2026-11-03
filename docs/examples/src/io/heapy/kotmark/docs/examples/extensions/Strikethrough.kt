package io.heapy.kotmark.docs.examples.extensions

// region gfm-strikethrough
import io.heapy.kotmark.ext.gfm.strikethrough.strikethroughExtension
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

fun renderWithStrikethrough(markdown: String): String {
    // With requireTwoTildes, a single ~ stays literal text.
    val extensions = listOf(strikethroughExtension { requireTwoTildes = true })
    val document = parser { extensions(extensions) }.parse(markdown)
    return htmlRenderer { extensions(extensions) }.render(document)
}
// endregion gfm-strikethrough
