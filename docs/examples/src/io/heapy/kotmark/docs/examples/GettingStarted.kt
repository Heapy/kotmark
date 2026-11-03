package io.heapy.kotmark.docs.examples

// region getting-started
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

fun renderMarkdown(markdown: String): String {
    val document = parser().parse(markdown)
    return htmlRenderer().render(document)
}
// endregion getting-started