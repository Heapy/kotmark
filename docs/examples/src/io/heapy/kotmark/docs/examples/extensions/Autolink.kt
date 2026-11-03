package io.heapy.kotmark.docs.examples.extensions

// region autolink
import io.heapy.kotmark.ext.autolink.AutolinkType
import io.heapy.kotmark.ext.autolink.autolinkExtension
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

fun renderWithAutolinks(markdown: String): String {
    val autolinks = autolinkExtension {
        linkTypes = setOf(AutolinkType.URL, AutolinkType.EMAIL)
    }
    // Autolinks are found while parsing; the HTML renderer sees ordinary links.
    val document = parser { extensions(listOf(autolinks)) }.parse(markdown)
    return htmlRenderer().render(document)
}
// endregion autolink
