package io.heapy.kotmark.docs.examples.extensions

// region ins
import io.heapy.kotmark.ext.ins.ins
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

fun renderWithIns(markdown: String): String {
    val extensions = extensions { ins() }
    val document = parser { extensions(extensions) }.parse(markdown)
    return htmlRenderer { extensions(extensions) }.render(document)
}
// endregion ins
