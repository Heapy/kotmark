package io.heapy.kotmark.docs.examples.extensions

// region footnotes
import io.heapy.kotmark.ext.footnotes.footnotes
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

fun renderWithFootnotes(markdown: String): String {
    val extensions = extensions {
        footnotes { inlineFootnotes = true }
    }
    val document = parser { extensions(extensions) }.parse(markdown)
    return htmlRenderer { extensions(extensions) }.render(document)
}
// endregion footnotes
