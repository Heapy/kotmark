package io.heapy.kotmark.docs.examples.extensions

// region image-attributes
import io.heapy.kotmark.ext.image.attributes.imageAttributes
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

fun renderWithImageAttributes(markdown: String): String {
    val extensions = extensions { imageAttributes() }
    val document = parser { extensions(extensions) }.parse(markdown)
    return htmlRenderer { extensions(extensions) }.render(document)
}
// endregion image-attributes
