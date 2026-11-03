package io.heapy.kotmark.docs.examples.extensions

// region math
import io.heapy.kotmark.ext.math.math
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

fun renderWithMath(markdown: String): String {
    val extensions = extensions { math() }
    val document = parser { extensions(extensions) }.parse(markdown)
    // Formula source is escaped and wrapped; no TeX engine is involved.
    return htmlRenderer { extensions(extensions) }.render(document)
}
// endregion math
