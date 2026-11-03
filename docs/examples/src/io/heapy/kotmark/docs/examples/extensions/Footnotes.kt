package io.heapy.kotmark.docs.examples.extensions

// region footnotes
import io.heapy.kotmark.ext.footnotes.footnotesExtension
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

fun renderWithFootnotes(markdown: String): String {
    val extensions = listOf(footnotesExtension { inlineFootnotes = true })
    val document = parser { extensions(extensions) }.parse(markdown)
    return htmlRenderer { extensions(extensions) }.render(document)
}
// endregion footnotes
