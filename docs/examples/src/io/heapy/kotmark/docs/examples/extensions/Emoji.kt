package io.heapy.kotmark.docs.examples.extensions

// region emoji
import io.heapy.kotmark.ext.emoji.emoji
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

fun renderWithEmoji(markdown: String): String {
    val extensions = extensions { emoji() }
    val document = parser { extensions(extensions) }.parse(markdown)
    return htmlRenderer { extensions(extensions) }.render(document)
}
// endregion emoji
