package io.heapy.kotmark.docs.examples.extensions

// region autolink
import io.heapy.kotmark.ext.autolink.AutolinkType
import io.heapy.kotmark.ext.autolink.autolink
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

fun renderWithAutolinks(markdown: String): String {
    val extensions = extensions {
        autolink { linkTypes = setOf(AutolinkType.URL, AutolinkType.EMAIL) }
    }
    // Autolinks are found while parsing; the HTML renderer sees ordinary links.
    val document = parser { extensions(extensions) }.parse(markdown)
    return htmlRenderer().render(document)
}
// endregion autolink
