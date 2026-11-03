package io.heapy.kotmark.docs.examples.extensions

// region highlight
import io.heapy.kotmark.ext.highlight.highlight
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

fun renderWithHighlight(markdown: String): String {
    val extensions = extensions { highlight() }
    val document = parser { extensions(extensions) }.parse(markdown)
    return htmlRenderer { extensions(extensions) }.render(document)
}
// endregion highlight
