package io.heapy.kotmark.docs.examples.extensions

// region ins
import io.heapy.kotmark.ext.ins.InsExtension
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

fun renderWithIns(markdown: String): String {
    val extensions = listOf(InsExtension.create())
    val document = parser { extensions(extensions) }.parse(markdown)
    return htmlRenderer { extensions(extensions) }.render(document)
}
// endregion ins
